package it.matato.dietreminder.data.auth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.common.model.AccountDto
import it.matato.dietreminder.common.model.SessionDto
import it.matato.dietreminder.util.UpdateManagerStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal class TestSessionCipher : SessionCipher {
    override fun encrypt(value: String) = "protected:" + value.reversed()
    override fun decrypt(value: String): String {
        require(value.startsWith("protected:"))
        return value.removePrefix("protected:").reversed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestDietApplication::class)
class AccountIsolationTest {
    private lateinit var context: Application
    private lateinit var sessions: AccountSessionStore
    private fun session(id: String, token: String = "token-$id") =
        SessionDto(token, System.currentTimeMillis() + 60_000, AccountDto(id, "$id@example.com", id))

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("auth_session", Context.MODE_PRIVATE).edit().clear().commit()
        context.databaseList().forEach { context.deleteDatabase(it) }
        sessions = AccountSessionStore(context, TestSessionCipher())
        UpdateManagerStatus.isOffline = false
        UpdateManagerStatus.isChecking = false
    }

    @Test
    fun localAndCloudAccountsKeepSeparateDataAndOldOperationsStayBoundToTheirAccount() = runBlocking {
        val scoped = ScopedAccountRepository(AccountRepositories(context, sessions))
        scoped.create("Local diet", 90)
        sessions.save(session("alice"))
        assertTrue(scoped.all.first().isEmpty())
        scoped.create("Alice diet", 90)
        val oldAliceRepository = scoped.snapshot()
        sessions.save(session("bob"))
        assertTrue(scoped.all.first().isEmpty())
        scoped.create("Bob diet", 90)
        assertTrue(runCatching { oldAliceRepository.create("Alice pending operation", 90) }.isFailure)
        assertEquals(listOf("Bob diet"), scoped.all.first().map { it.name })
        sessions.enterCloud()
        assertTrue(scoped.all.first().isEmpty())
        sessions.useLocal()
        assertEquals(listOf("Local diet"), scoped.all.first().map { it.name })
        sessions.save(session("alice"))
        assertEquals(setOf("Alice diet"), scoped.all.first().map { it.name }.toSet())
    }

    @Test
    fun lateUnauthorizedFromPreviousSessionCannotLogOutTheNewAccount() {
        val alice = session("alice")
        val bob = session("bob")
        sessions.save(alice)
        sessions.save(bob)
        sessions.unauthorized(alice.token)
        assertEquals(bob, sessions.state.value.session)
        sessions.unauthorized(bob.token)
        assertTrue(sessions.state.value.cloud)
        assertNull(sessions.state.value.session)
    }

    @Test
    fun savedSessionUsesCipherAndIsRestoredWithoutSavingThePassword() {
        val account = session("alice")
        sessions.save(account)
        val stored = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE).getString("session", null)!!
        assertFalse(stored.contains(account.token))
        assertEquals(account, AccountSessionStore(context, TestSessionCipher()).state.value.session)
    }

    @Test
    fun unreadableSessionRemainsLockedAndDoesNotFallBackToLocalData() {
        sessions.save(session("alice"))
        context.getSharedPreferences("auth_session", Context.MODE_PRIVATE).edit().putString("session", "corrupted")
            .commit()
        val restored = AccountSessionStore(context, TestSessionCipher())
        assertTrue(restored.state.value.cloud)
        assertNull(restored.state.value.session)
        assertEquals("locked", restored.state.value.scope)
    }

    @Test
    fun logoutNeverDeletesLocalDataAndDatabaseNamesCannotUsePathTraversal() {
        assertEquals("diet.db", AccountSessionStore.databaseName("local"))
        assertFalse(AccountSessionStore.databaseName("../../alice").contains(".."))
        assertNotEquals(AccountSessionStore.databaseName("alice"), AccountSessionStore.databaseName("bob"))
    }
}
