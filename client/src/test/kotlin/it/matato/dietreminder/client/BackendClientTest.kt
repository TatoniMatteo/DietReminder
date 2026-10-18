package it.matato.dietreminder.client

import com.sun.net.httpserver.HttpServer
import it.matato.dietreminder.common.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import java.net.InetSocketAddress

class BackendClientTest {
    private val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0).apply { start() }
    private val base="http://127.0.0.1:${server.address.port}/"
    @After fun stop() { server.stop(0) }
    private fun response(path: String, status: Int, body: String, inspect: (com.sun.net.httpserver.HttpExchange)->Unit = {}) {
        server.createContext(path) { exchange ->
            try { inspect(exchange); val bytes=body.toByteArray(); exchange.responseHeaders.set("Content-Type","application/json"); exchange.sendResponseHeaders(status,if(status==204)-1 else bytes.size.toLong()); if(status!=204)exchange.responseBody.write(bytes) }
            finally { exchange.close() }
        }
    }
    @Test fun addsPinnedBearerAndDecodesForwardCompatibleAccount() = runBlocking {
        var authorization: String?=null
        response("/api/auth/me",200,"""{"userId":"user","email":"test@example.com","username":"Test","futureField":true}""") { authorization=it.requestHeaders.getFirst("Authorization") }
        val account=BackendClient.create(base,tokenProvider={"pinned"}).me()
        assertEquals("Bearer pinned",authorization); assertEquals("user",account.userId)
    }
    @Test fun unauthorizedProtectedRequestInvalidatesExactlyTheSentToken() {
        response("/api/auth/me",401,"{}")
        val revoked=mutableListOf<String>()
        val api=BackendClient.create(base,tokenProvider={"old-token"},onUnauthorized={revoked+=it})
        assertThrows(HttpException::class.java) { runBlocking { api.me() } }
        assertEquals(listOf("old-token"),revoked)
    }
    @Test fun loginFailureDoesNotInvalidateAnExistingSession() {
        response("/api/auth/login",401,"{}")
        val revoked=mutableListOf<String>()
        val api=BackendClient.create(base,tokenProvider={"session"},onUnauthorized={revoked+=it})
        assertThrows(HttpException::class.java) { runBlocking { api.login(LoginRequest("test@example.com","wrong")) } }
        assertTrue(revoked.isEmpty())
    }
    @Test fun anonymousFailureDoesNotRevokeOtherCredentials() {
        response("/api/auth/me",401,"{}")
        val revoked=mutableListOf<String>()
        val api=BackendClient.create(base,onUnauthorized={revoked+=it})
        assertThrows(HttpException::class.java) { runBlocking { api.me() } }; assertTrue(revoked.isEmpty())
    }
    @Test fun passwordChangeSendsCorrectFieldsAndAcceptsNoContent() = runBlocking {
        var body=""
        response("/api/auth/change-password",204,"") { body=it.requestBody.bufferedReader().readText() }
        BackendClient.create(base).changePassword(ChangePasswordRequest("Old password!","New password!"))
        assertTrue(body.contains("\"currentPassword\":\"Old password!\"")); assertTrue(body.contains("\"newPassword\":\"New password!\""))
    }
    @Test fun requiresTrailingSlash() { assertThrows(IllegalArgumentException::class.java) { BackendClient.create(base.removeSuffix("/")) } }
}
