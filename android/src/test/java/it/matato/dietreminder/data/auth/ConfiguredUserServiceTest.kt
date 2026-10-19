package it.matato.dietreminder.data.auth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sun.net.httpserver.HttpServer
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.common.model.AccountDto
import it.matato.dietreminder.common.model.SessionDto
import java.io.IOException
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestDietApplication::class)
class ConfiguredUserServiceTest {
    private val first = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val second = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private lateinit var sessions: AccountSessionStore
    private fun address(server: HttpServer) = "http://127.0.0.1:${server.address.port}/"
    private fun login(token: String) = sessions.save(SessionDto(token, System.currentTimeMillis() + 60_000, AccountDto("same-user", "user@example.com", "User")))

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("auth_session", Context.MODE_PRIVATE).edit().clear().commit()
        sessions = AccountSessionStore(context, TestSessionCipher())
        first.start(); second.start()
    }
    @After fun cleanup() { first.stop(0); second.stop(0) }

    @Test fun oneServiceFollowsServerChangesWithoutSendingThePreviousToken() = runBlocking {
        val received = mutableListOf<String?>()
        for (server in listOf(first, second)) server.createContext("/api/auth/providers") { exchange ->
            received.add(exchange.requestHeaders.getFirst("Authorization"))
            val body = "{}".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
            exchange.close()
        }
        val api = ConfiguredUserService(sessions)
        sessions.configureServer(address(first))
        login("first-token")
        api.googleProviders()
        sessions.configureServer(address(second))
        api.googleProviders()
        login("second-token")
        api.googleProviders()
        assertEquals(listOf("Bearer first-token", null, "Bearer second-token"), received)
    }

    @Test fun responseFromPreviousServerIsDiscardedAfterASwitch() {
        first.createContext("/api/auth/providers") { exchange ->
            sessions.configureServer(address(second))
            val body = "{}".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
            exchange.close()
        }
        sessions.configureServer(address(first))
        assertThrows(IOException::class.java) { runBlocking { ConfiguredUserService(sessions).googleProviders() } }
        assertEquals(address(second), sessions.state.value.serverUrl)
        assertNull(sessions.token())
    }
}
