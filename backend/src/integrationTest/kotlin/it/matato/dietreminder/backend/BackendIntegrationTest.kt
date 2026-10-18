package it.matato.dietreminder.backend

import it.matato.dietreminder.backend.auth.*
import it.matato.dietreminder.backend.profile.UserProfileRepository
import it.matato.dietreminder.backend.sync.SyncRecordRepository
import it.matato.dietreminder.client.BackendClient
import it.matato.dietreminder.client.UserService
import it.matato.dietreminder.common.model.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.context.annotation.Import
import jakarta.mail.internet.MimeMessage
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import java.util.Properties
import org.springframework.mail.javamail.JavaMailSender
import retrofit2.HttpException

@SpringBootTest(
    classes = [DietBackendApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // Delivery is invoked explicitly in outbox tests; the scheduler must not race DB resets.
    properties = ["auth.notifications.poll-ms=3600000", "auth.attempt-limit=1000", "google.client-id=test-client", "google.client-secret=test-secret",
        "google.drive-redirect-uri=http://localhost/callback", "google.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "web.secure-cookies=false"]
)
@Import(PostgresTestConfiguration::class)
class BackendIntegrationTest {
    @LocalServerPort private var port: Int = 0
    @Autowired lateinit var rest: TestRestTemplate
    @Autowired lateinit var profiles: UserProfileRepository
    @Autowired lateinit var records: SyncRecordRepository
    @Autowired lateinit var accounts: AccountRepository
    private val encoder = org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    @Autowired lateinit var enrollmentMail: AccountNotificationMail
    @Autowired lateinit var creation: AccountCreationService
    @Autowired lateinit var transaction: org.springframework.transaction.support.TransactionTemplate
    @Autowired lateinit var dataSource: javax.sql.DataSource
    @Autowired lateinit var tokens: AccountTokenRepository
    @Autowired lateinit var flyway: org.flywaydb.core.Flyway
    @Autowired lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate
    @MockBean lateinit var mail: JavaMailSender
    @MockBean(name = "googleJwtDecoder") lateinit var googleJwt: org.springframework.security.oauth2.jwt.JwtDecoder
    @MockBean lateinit var drive: it.matato.dietreminder.backend.backup.GoogleDriveGateway
    @Autowired lateinit var driveConnections: it.matato.dietreminder.backend.backup.GoogleDriveConnectionRepository
    @Autowired lateinit var googleCipher: it.matato.dietreminder.backend.backup.GoogleTokenCipher
    private lateinit var anon: UserService
    private lateinit var alice: UserService
    private lateinit var bob: UserService
    private lateinit var bootstrapSession: SessionDto
    private lateinit var aliceSession: SessionDto
    private lateinit var bobSession: SessionDto
    private val password = "A long test password 123!"
    private val record = SyncPushRequest(dataType = "DIET", payloadJson = """{"name":"Dieta è buona","meals":[]}""")
    private fun client(token: String? = null) = BackendClient.create("http://localhost:$port/", tokenProvider = { token })

    @BeforeEach
    fun prepare() = runBlocking {
        org.mockito.Mockito.`when`(mail.createMimeMessage()).thenAnswer { MimeMessage(Session.getInstance(Properties())) }
        clearInstallation()
        anon = client()
        bootstrapSession = registerApproved(RegisterRequest("owner@example.com", password, "Owner"))
        aliceSession = registerApproved(RegisterRequest("alice@example.com", password, "Alice"))
        bobSession = registerApproved(RegisterRequest("bob@example.com", password, "Bob"))
        alice = client(aliceSession.token)
        bob = client(bobSession.token)
    }
    private suspend fun registerApproved(request: RegisterRequest): SessionDto = approveFixture(anon.register(request))
    private suspend fun googleApproved(request: GoogleSignInRequest): SessionDto = approveFixture(anon.googleSignIn(request))
    private suspend fun approveFixture(session: SessionDto): SessionDto {
        jdbc.update("UPDATE accounts SET email_verified = true, enrollment_status = 'APPROVED' WHERE id = ?", session.account.userId)
        jdbc.update("DELETE FROM account_mail_outbox WHERE account_id = ?", session.account.userId)
        jdbc.update("DELETE FROM account_tokens WHERE account_id = ? AND purpose = 'VERIFY_EMAIL'", session.account.userId)
        clearInvocations(mail)
        return session.copy(account = client(session.token).me())
    }

    private fun clearInstallation() {
        // Test-only reset on the isolated Testcontainers database. Production
        // account operations cannot reset the installation guard.
        jdbc.update("UPDATE account_administration_guard SET initialized = false, active_admin_count = 0 WHERE id = 1")
        jdbc.execute("TRUNCATE accounts CASCADE")
    }

    private fun expectError(status: Int, code: String, action: suspend () -> Unit) {
        val ex = assertThrows(HttpException::class.java) { runBlocking { action() } }
        assertEquals(status, ex.code())
        val error = Json.decodeFromString<ApiErrorDto>(ex.response()!!.errorBody()!!.string())
        assertEquals(code, error.code)
        assertTrue(error.message.isNotBlank())
    }
    private fun raw(path: String, body: String, token: String = aliceSession.token): ResponseEntity<String> {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON; setBearerAuth(token) }
        return rest.postForEntity(path, HttpEntity(body, headers), String::class.java)
    }

    @Test
    fun `public configuration requires first registration only while no accounts exist`() = runBlocking {
        assertFalse(anon.googleProviders().firstRegistrationRequired)
        clearInstallation()
        assertTrue(anon.googleProviders().firstRegistrationRequired)
        val first = anon.register(RegisterRequest("first-setup@example.com", password, "First"))
        assertEquals(EnrollmentStatus.PENDING_EMAIL, first.account.enrollmentStatus)
        assertFalse(anon.googleProviders().firstRegistrationRequired)
    }

    @Test
    fun `migrations use PostgreSQL and subsequent startup preserves accounts and profiles`() = runBlocking {
        assertTrue(jdbc.queryForObject("SELECT version()", String::class.java)!!.startsWith("PostgreSQL 17"))
        val profile = alice.saveProfile(ProfileUpdateRequest("Persisted", 2350))
        flyway.validate()
        assertEquals(0, flyway.migrate().migrationsExecuted)
        assertEquals(profile, alice.getProfile())
        assertEquals(aliceSession.account.copy(username = "Persisted"), alice.me())
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Int::class.java)!! >= 1)
    }

    @Test
    fun `database rejects profiles without an owning account`() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException::class.java) {
            jdbc.update("INSERT INTO user_profiles(user_id, username, daily_calorie_goal, updated_at) VALUES (?, ?, ?, ?)",
                "missing-account", "Orphan", 2000, 0L)
        }
    }

    @Test
    fun `health endpoints are public and administrative endpoints remain inaccessible`() {
        for (path in listOf("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
            val health = rest.getForEntity(path, com.fasterxml.jackson.databind.JsonNode::class.java)
            assertEquals(HttpStatus.OK, health.statusCode)
            assertEquals("UP", health.body!!.path("status").asText())
            assertFalse(health.body!!.has("components"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity("/actuator/env", String::class.java).statusCode)
    }

    @Test
    fun `OpenAPI and Swagger are public while account data remains protected`() {
        val docs = rest.getForEntity("/v3/api-docs", com.fasterxml.jackson.databind.JsonNode::class.java)
        assertEquals(HttpStatus.OK, docs.statusCode)
        val spec = docs.body!!
        assertEquals("Diet Reminder API", spec.path("info").path("title").asText())
        assertEquals("bearer", spec.path("components").path("securitySchemes").path("bearerAuth").path("scheme").asText())
        assertTrue(spec.path("security").first().has("bearerAuth"))
        val paths = spec.path("paths")
        listOf("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").forEach { path ->
            assertTrue(paths.path(path).path("post").path("security").isArray)
            assertEquals(0, paths.path(path).path("post").path("security").size(), "$path must not require a session")
        }
        assertTrue(paths.path("/api/workspace").has("put"))
        assertFalse(paths.path("/api/workspace").path("get").path("parameters").any {
            it.path("name").asText() in listOf("userId", "email", "username", "account")
        }, "The authenticated account must not appear as client-supplied parameters")
        assertEquals(HttpStatus.OK, rest.getForEntity("/swagger-ui/index.html", String::class.java).statusCode)
        assertEquals(HttpStatus.OK, rest.getForEntity("/swagger-ui/swagger-ui-bundle.js", String::class.java).statusCode)
        assertEquals(HttpStatus.OK, rest.getForEntity("/v3/api-docs/swagger-config", String::class.java).statusCode)
        listOf("/api/auth/me", "/api/user/profile", "/api/workspace", "/api/user/sync/pull").forEach { path ->
            assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity(path, String::class.java).statusCode)
        }
    }

    @Test
    fun `registration and normalized email login return valid revocable sessions`() = runBlocking {
        val session = registerApproved(RegisterRequest("  Mixed@EXAMPLE.com ", password, "Mixed"))
        assertEquals("mixed@example.com", session.account.email)
        assertTrue(session.expiresAt > System.currentTimeMillis())
        val signedIn = anon.login(LoginRequest("MIXED@example.COM", password))
        assertEquals(session.account.userId, signedIn.account.userId)
        assertEquals(session.account, client(signedIn.token).me())
        assertNotEquals(password, accounts.findByEmail("mixed@example.com")!!.passwordHash)
        assertFalse(tokens.findAll().any { it.hash == session.token || it.hash == signedIn.token })
    }
    @Test
    fun `invalid passwords and duplicate emails are rejected`() {
        expectError(400, "INVALID_REQUEST") { registerApproved(RegisterRequest("short@example.com", "short", "Short")) }
        expectError(409, "ACCOUNT_EXISTS") { registerApproved(RegisterRequest("ALICE@example.com", password, "Duplicate")) }
        expectError(401, "INVALID_CREDENTIALS") { anon.login(LoginRequest("alice@example.com", "wrong")) }
        expectError(401, "INVALID_CREDENTIALS") { anon.login(LoginRequest("unknown@example.com", "wrong")) }
    }
    @Test
    fun `all account APIs require a valid session`() {
        expectError(401, "UNAUTHENTICATED") { anon.me() }
        expectError(401, "UNAUTHENTICATED") { anon.getProfile() }
        expectError(401, "UNAUTHENTICATED") { anon.saveProfile(ProfileUpdateRequest("Forged")) }
        expectError(401, "UNAUTHENTICATED") { anon.pushSync(record) }
        expectError(401, "UNAUTHENTICATED") { anon.pullSync() }
        expectError(401, "UNAUTHENTICATED") { client("invalid-token").me() }
    }
    @Test
    fun `logout revokes only the current session`() = runBlocking {
        val second = anon.login(LoginRequest("alice@example.com", password))
        alice.logout()
        expectError(401, "UNAUTHENTICATED") { alice.me() }
        assertEquals(aliceSession.account.userId, client(second.token).me().userId)
        assertEquals(bobSession.account, bob.me())
    }
    @Test
    fun `expired sessions cannot access account data`() {
        val existing = tokens.findAll().first { it.accountId == aliceSession.account.userId && it.purpose == "SESSION" }
        tokens.save(AccountTokenEntity(existing.hash, existing.accountId, existing.purpose, 1))
        expectError(401, "UNAUTHENTICATED") { alice.getProfile() }
    }
    @Test
    fun `profile updates belong to the authenticated account`() = runBlocking {
        val profile = alice.saveProfile(ProfileUpdateRequest("Updated", 2200))
        assertEquals(aliceSession.account.userId, profile.userId)
        assertEquals(2200, alice.getProfile().dailyCalorieGoal)
        assertEquals("Updated", alice.getProfile().username)
        assertEquals("Bob", bob.getProfile().username)
        assertEquals(3L, profiles.count())
    }
    @Test
    fun `forged ownership fields cannot read or overwrite another account`() = runBlocking {
        val victimId = bobSession.account.userId
        assertEquals(HttpStatus.BAD_REQUEST, raw("/api/user/profile", """{"userId":"$victimId","username":"Attacker"}""").statusCode)
        assertEquals("Bob", bob.getProfile().username)
        val pushed = raw("/api/user/sync/push", """{"userId":"$victimId","dataType":"DIET","payloadJson":"{}"}""")
        assertEquals(HttpStatus.BAD_REQUEST, pushed.statusCode)
        assertTrue(bob.pullSync().isEmpty())
        assertTrue(alice.pullSync().isEmpty())
        val headers = HttpHeaders().apply { setBearerAuth(aliceSession.token) }
        val response = rest.exchange("/api/user/profile/$victimId", HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)
        assertTrue(response.statusCode.is4xxClientError)
    }
    @Test
    fun `invalid profiles are rejected without modifying the account`() = runBlocking {
        expectError(400, "INVALID_REQUEST") { alice.saveProfile(ProfileUpdateRequest(" ")) }
        expectError(400, "INVALID_REQUEST") { alice.saveProfile(ProfileUpdateRequest("Name", -1)) }
        assertEquals("Alice", alice.getProfile().username)
    }
    @Test
    fun `sync payloads and idempotency are isolated by authenticated user`() = runBlocking {
        val request = record.copy(requestId = "retry-123")
        val first = alice.pushSync(request)
        assertEquals(record.payloadJson, first.payloadJson)
        assertEquals(first, alice.pushSync(request))
        val other = bob.pushSync(request)
        assertNotEquals(first.id, other.id)
        assertEquals(listOf(first), alice.pullSync())
        assertEquals(listOf(other), bob.pullSync())
    }
    @Test
    fun `concurrent retries store exactly one record`() = runBlocking {
        val responses = coroutineScope { (1..6).map { async { alice.pushSync(record.copy(requestId = "concurrent-123")) } }.awaitAll() }
        assertEquals(1, responses.distinct().size)
        assertEquals(1L, records.count())
    }
    @Test
    fun `reusing a request ID for different data returns conflict`() = runBlocking {
        val request = record.copy(requestId = "retry-123")
        val first = alice.pushSync(request)
        expectError(409, "REQUEST_ID_CONFLICT") { alice.pushSync(request.copy(payloadJson = "{}")) }
        assertEquals(listOf(first), alice.pullSync())
    }
    @Test
    fun `pull supports bounded pages and never includes other accounts`() = runBlocking {
        val saved = (1..5).map { alice.pushSync(record.copy(payloadJson = """{"index":$it}""")) }
        bob.pushSync(record)
        val first = alice.pullSync(limit = 2)
        val second = alice.pullSync(afterId = first.last().id, limit = 2)
        val third = alice.pullSync(afterId = second.last().id, limit = 2)
        assertEquals(saved, first + second + third)
        assertTrue(alice.pullSync(afterId = third.last().id).isEmpty())
    }
    @Test
    fun `invalid sync requests are rejected`() {
        for (invalid in listOf(record.copy(dataType = ""), record.copy(payloadJson = "not json"), record.copy(payloadJson = "null"), record.copy(payloadJson = "{} garbage"), record.copy(payloadJson = """{"data":"${"a".repeat(1_048_576)}"}"""), record.copy(requestId = " "))) {
            expectError(400, "INVALID_REQUEST") { alice.pushSync(invalid) }
        }
        assertEquals(0L, records.count())
    }
    @Test
    fun `invalid page parameters and malformed requests return structured errors`() {
        expectError(400, "INVALID_REQUEST") { alice.pullSync(afterId = -1) }
        expectError(400, "INVALID_REQUEST") { alice.pullSync(limit = 0) }
        expectError(400, "INVALID_REQUEST") { alice.pullSync(limit = 501) }
        for (body in listOf("not-json", "{}", """{"username":null}""")) {
            val response = raw("/api/user/profile", body)
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertEquals("INVALID_REQUEST", Json.decodeFromString<ApiErrorDto>(response.body!!).code)
        }
    }
    @Test
    fun `password recovery sends an expiring single use code and revokes all old sessions`() = runBlocking {
        val anotherSession = anon.login(LoginRequest("alice@example.com", password))
        anon.forgotPassword(ForgotPasswordRequest("alice@example.com"))
        val captor = ArgumentCaptor.forClass(MimeMessage::class.java)
        verify(mail, timeout(5000)).send(captor.capture())
        captor.value.saveChanges()
        val code = Regex("[A-Za-z0-9_-]{43}").find(plainText(captor.value))!!.value
        val newPassword = "A different long password 456!"
        for (invalid in listOf("una password lunga!", "UNA PASSWORD LUNGA!", "Una password lunga")) {
            expectError(400, "INVALID_REQUEST") { anon.resetPassword(ResetPasswordRequest(code, invalid)) }
        }
        anon.resetPassword(ResetPasswordRequest(code, newPassword))
        expectError(400, "INVALID_RESET_TOKEN") { anon.resetPassword(ResetPasswordRequest(code, password)) }
        expectError(401, "UNAUTHENTICATED") { alice.me() }
        expectError(401, "UNAUTHENTICATED") { client(anotherSession.token).me() }
        expectError(401, "INVALID_CREDENTIALS") { anon.login(LoginRequest("alice@example.com", password)) }
        assertEquals(aliceSession.account.userId, anon.login(LoginRequest("alice@example.com", newPassword)).account.userId)
        assertEquals(bobSession.account, bob.me())
    }
    @Test
    fun `unknown emails get the same recovery response without sending mail`() = runBlocking {
        anon.forgotPassword(ForgotPasswordRequest("unknown@example.com"))
        verifyNoInteractions(mail)
        expectError(400, "INVALID_RESET_TOKEN") { anon.resetPassword(ResetPasswordRequest("invalid", password)) }
    }
    @Test
    fun `new passwords require uppercase lowercase and a special character but no digit`() = runBlocking {
        for (invalid in listOf("una password lunga!", "UNA PASSWORD LUNGA!", "Una password lunga")) {
            expectError(400, "INVALID_REQUEST") { registerApproved(RegisterRequest("policy@example.com", invalid, "Policy")) }
            expectError(400, "INVALID_REQUEST") { alice.changePassword(ChangePasswordRequest(password, invalid)) }
        }
        val session = registerApproved(RegisterRequest("policy@example.com", "Una password lunga!", "Policy"))
        assertEquals(session.account, client(session.token).me())
        assertEquals(aliceSession.account, alice.me())
    }
    @Test
    fun `changing password requires current password and revokes every old session`() = runBlocking {
        val second = client(anon.login(LoginRequest("alice@example.com", password)).token)
        val replacement = "Una nuova password!"
        expectError(400, "INVALID_CURRENT_PASSWORD") { alice.changePassword(ChangePasswordRequest("Incorrect password!", replacement)) }
        expectError(400, "PASSWORD_UNCHANGED") { alice.changePassword(ChangePasswordRequest(password, password)) }
        assertEquals(aliceSession.account, alice.me())
        alice.changePassword(ChangePasswordRequest(password, replacement))
        expectError(401, "UNAUTHENTICATED") { alice.me() }
        expectError(401, "UNAUTHENTICATED") { second.me() }
        expectError(401, "INVALID_CREDENTIALS") { anon.login(LoginRequest("alice@example.com", password)) }
        assertEquals(aliceSession.account.userId, anon.login(LoginRequest("alice@example.com", replacement)).account.userId)
        assertEquals(bobSession.account, bob.me())
    }
    @Test
    fun `legacy passwords still allow sign in after strengthening new password requirements`() = runBlocking {
        val legacy = "legacy password without special"
        jdbc.update("UPDATE accounts SET password_hash = ? WHERE id = ?", encoder.encode(legacy), aliceSession.account.userId)
        assertEquals(aliceSession.account.userId, anon.login(LoginRequest("alice@example.com", legacy)).account.userId)
    }
    @Test
    fun `updating username alone preserves legacy profile fields`() = runBlocking {
        alice.saveProfile(ProfileUpdateRequest("Alice", 2350))
        assertEquals(2350, alice.saveProfile(ProfileUpdateRequest("New name")).dailyCalorieGoal)
    }
    private fun workspace(name: String = "Weekly diet") = WorkspaceSnapshot(
        diets = listOf(CloudDiet(1, "diet-uuid", name, isActive = true)),
        meals = listOf(CloudMeal(1, 1, 1, MealType.LUNCH, 720)),
        courses = listOf(CloudCourse(1, 1, "Main")),
        foods = listOf(CloudFood(1, 1, "Rice", "100")),
        lists = listOf(CloudShoppingList(1, "Groceries", 1, 1)),
        items = listOf(CloudShoppingItem(1, 1, "Rice", "100", isFresh = true)),
        days = listOf(CloudShoppingDay(1, 1, 1, "100"))
    )
    @Test
    fun `workspace automatically shared between devices but never between accounts`() = runBlocking {
        expectError(401, "UNAUTHENTICATED") { anon.getWorkspace() }
        expectError(401, "UNAUTHENTICATED") { anon.putWorkspace(WorkspacePutRequest(0, "anonymous", workspace())) }
        assertEquals(WorkspaceDto(0, WorkspaceSnapshot()), alice.getWorkspace())
        val ack = alice.putWorkspace(WorkspacePutRequest(0, "first", workspace()))
        assertEquals(1, ack.version)
        val secondDevice = client(anon.login(LoginRequest("alice@example.com", password)).token)
        assertEquals(WorkspaceDto(1, workspace()), secondDevice.getWorkspace())
        assertEquals(WorkspaceDto(0, WorkspaceSnapshot()), bob.getWorkspace())
        assertEquals(1, bob.putWorkspace(WorkspacePutRequest(0, "first", workspace("Bob diet"))).version)
        assertEquals("Weekly diet", alice.getWorkspace().snapshot.diets.single().name)
    }
    @Test
    fun `workspace replay is idempotent even after another device advances the version`() = runBlocking {
        val first = WorkspacePutRequest(0, "first", workspace())
        val ack = alice.putWorkspace(first)
        alice.putWorkspace(WorkspacePutRequest(1, "second", workspace("Changed")))
        assertEquals(ack, alice.putWorkspace(first))
        assertEquals(WorkspaceDto(2, workspace("Changed")), alice.getWorkspace())
        expectError(409, "REQUEST_ID_CONFLICT") { alice.putWorkspace(first.copy(snapshot = workspace("Forged replay"))) }
    }
    @Test
    fun `two concurrent devices cannot overwrite each other from the same base version`() = runBlocking {
        val results = coroutineScope {
            listOf("Device A", "Device B").map { name -> async {
                runCatching { alice.putWorkspace(WorkspacePutRequest(0, name.replace(" ", "-"), workspace(name))) }
            } }.awaitAll()
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(409, (results.first { it.isFailure }.exceptionOrNull() as HttpException).code())
        assertEquals(1, alice.getWorkspace().version)
    }
    @Test
    fun `deletions propagate and a stale device cannot resurrect deleted records`() = runBlocking {
        alice.putWorkspace(WorkspacePutRequest(0, "create", workspace()))
        alice.putWorkspace(WorkspacePutRequest(1, "delete", WorkspaceSnapshot()))
        assertEquals(WorkspaceDto(2, WorkspaceSnapshot()), alice.getWorkspace())
        expectError(409, "WORKSPACE_CONFLICT") { alice.putWorkspace(WorkspacePutRequest(1, "stale-device", workspace())) }
        assertTrue(alice.getWorkspace().snapshot.diets.isEmpty())
    }
    @Test
    fun `incomplete graphs and unsupported schema versions never replace a valid workspace`() = runBlocking {
        alice.putWorkspace(WorkspacePutRequest(0, "valid", workspace()))
        for (invalid in listOf(
            workspace().copy(courses = emptyList()),
            workspace().copy(schemaVersion = 3),
            workspace().copy(diets = workspace().diets + workspace().diets),
            workspace().copy(meals = listOf(CloudMeal(1, 1, 9, MealType.LUNCH, 720)))
        )) expectError(400, "INVALID_WORKSPACE") { alice.putWorkspace(WorkspacePutRequest(1, "invalid", invalid)) }
        assertEquals(WorkspaceDto(1, workspace()), alice.getWorkspace())
    }

    @Test
    fun `shopping planning metadata survives round trip and prevents legacy overwrites`() = runBlocking {
        val snapshot = workspace().copy(
            lists = listOf(CloudShoppingList(1, "Spesa", 1, 1, true, 1080)),
            items = listOf(CloudShoppingItem(1, 1, "Insalata", isFresh = true, category = "Orto")),
            days = listOf(CloudShoppingDay(1, 1, 1, "150", purchaseDayOfWeek = 7))
        )
        alice.putWorkspace(WorkspacePutRequest(0, "shopping", snapshot))
        assertEquals(snapshot, alice.getWorkspace().snapshot)
        expectError(409, "WORKSPACE_CLIENT_OUTDATED") { alice.putWorkspace(WorkspacePutRequest(1, "legacy", workspace().copy(schemaVersion = 1))) }
        for (invalid in listOf(snapshot.copy(days = listOf(snapshot.days.first().copy(purchaseDayOfWeek = 8))), snapshot.copy(lists = listOf(snapshot.lists.first().copy(reminderTimeMinutes = 1440))), snapshot.copy(items = listOf(snapshot.items.first().copy(category = " "))))) {
            expectError(400, "INVALID_WORKSPACE") { alice.putWorkspace(WorkspacePutRequest(1, "invalid-shopping", invalid)) }
        }
        assertEquals(snapshot, alice.getWorkspace().snapshot)
    }

    private fun http(path: String, method: String = "GET", body: String? = null, token: String? = null, cookie: String? = null, csrf: String? = null, webHeader: Boolean = true): java.net.http.HttpResponse<String> {
        val request = java.net.http.HttpRequest.newBuilder(java.net.URI("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .method(method, body?.let(java.net.http.HttpRequest.BodyPublishers::ofString) ?: java.net.http.HttpRequest.BodyPublishers.noBody())
        if (webHeader) request.header("X-DietReminder-Web", "1")
        if (token != null) request.header("Authorization", "Bearer $token")
        if (cookie != null) request.header("Cookie", cookie)
        if (csrf != null) request.header("X-CSRF-Token", csrf)
        return java.net.http.HttpClient.newHttpClient().send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString())
    }
    private fun tree(value: String) = com.fasterxml.jackson.databind.ObjectMapper().readTree(value)
    private fun makeAdmin() { val account = accounts.findById(aliceSession.account.userId).orElseThrow(); account.role = "ADMIN"; accounts.saveAndFlush(account) }
    private fun googleToken(nonce: String, email: String = "google@gmail.com", subject: String = "google-subject"): String {
        val token = "google-token-" + java.util.UUID.randomUUID()
        `when`(googleJwt.decode(token)).thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token).header("alg", "RS256")
            .issuer("https://accounts.google.com").audience(listOf("test-client")).subject(subject)
            .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300))
            .claim("email", email).claim("email_verified", true).claim("name", "Google user").claim("nonce", nonce).build())
        return token
    }
    @Test
    fun `Google sign in uses stable subjects and a nonce can only be consumed once`() = runBlocking {
        val token = googleToken(anon.googleChallenge().nonce)
        val first = googleApproved(GoogleSignInRequest(token))
        assertTrue(first.account.googleLinked)
        assertEquals("USER", first.account.role)
        assertNull(accounts.findById(first.account.userId).orElseThrow().passwordHash)
        expectError(400, "INVALID_GOOGLE_STATE") { googleApproved(GoogleSignInRequest(token)) }
        val second = googleApproved(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce)))
        assertEquals(first.account.userId, second.account.userId)
        assertNotEquals(first.token, second.token)
        assertEquals("Google user", client(second.token).getProfile().username)
    }
    @Test
    fun `Google cannot silently take over an existing password account`() = runBlocking {
        val idToken = googleToken(anon.googleChallenge().nonce, "alice@example.com")
        expectError(409, "GOOGLE_LINK_REQUIRED") { googleApproved(GoogleSignInRequest(idToken)) }
        expectError(409, "GOOGLE_ACCOUNT_MISMATCH") { bob.linkGoogle(GoogleSignInRequest(idToken)) }
        val linked = alice.linkGoogle(GoogleSignInRequest(idToken))
        assertTrue(linked.googleLinked)
        val googleSession = googleApproved(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "alice@example.com")))
        assertEquals(aliceSession.account.userId, googleSession.account.userId)
        assertEquals("Alice", client(googleSession.token).getProfile().username)
    }
    @Test
    fun `invalid Google signatures are rejected without creating accounts`() {
        `when`(googleJwt.decode("forged-token")).thenThrow(org.springframework.security.oauth2.jwt.JwtException("invalid signature"))
        expectError(401, "INVALID_GOOGLE_TOKEN") { googleApproved(GoogleSignInRequest("forged-token")) }
        assertEquals(3, accounts.count())
    }
    @Test
    fun `web sessions keep tokens HttpOnly and reject cross site writes`() {
        val body = """{"email":"alice@example.com","password":"$password"}"""
        assertEquals(403, http("/api/web/auth/login", "POST", body, webHeader = false).statusCode())
        val login = http("/api/web/auth/login", "POST", body)
        assertEquals(200, login.statusCode())
        val cookie = login.headers().firstValue("set-cookie").orElseThrow()
        assertTrue(cookie.contains("HttpOnly")); assertTrue(cookie.contains("SameSite=Lax"))
        assertFalse(tree(login.body()).has("token"))
        val csrf = tree(login.body()).path("csrfToken").asText()
        val pair = cookie.substringBefore(';')
        val profile = """{"username":"Browser","dailyCalorieGoal":2250}"""
        assertEquals(403, http("/api/user/profile", "POST", profile, cookie = pair).statusCode())
        assertEquals(403, http("/api/user/profile", "POST", profile, cookie = pair, csrf = "wrong").statusCode())
        assertEquals(200, http("/api/user/profile", "POST", profile, cookie = pair, csrf = csrf).statusCode())
        assertEquals(200, http("/api/web/auth/me", cookie = pair).statusCode())
        assertEquals(200, http("/api/web/auth/logout", "POST", cookie = pair, csrf = csrf).statusCode())
        assertEquals(401, http("/api/web/auth/me", cookie = pair).statusCode())
    }
    @Test
    fun `administration is enforced on the server and suspending a user revokes sessions`() = runBlocking {
        assertEquals(401, http("/api/admin/users").statusCode())
        assertEquals(403, http("/api/admin/users", token = aliceSession.token).statusCode())
        makeAdmin()
        val users = http("/api/admin/users?search=Bob", token = aliceSession.token)
        assertEquals(200, users.statusCode())
        assertEquals(1, tree(users.body()).path("total").asInt())
        assertFalse(users.body().contains("passwordHash")); assertFalse(users.body().contains(bobSession.token))
        val suspend = http("/api/admin/users/${bobSession.account.userId}/status", "PATCH", """{"enabled":false}""", aliceSession.token)
        assertEquals(200, suspend.statusCode())
        expectError(401, "UNAUTHENTICATED") { bob.me() }
        expectError(403, "ACCOUNT_DISABLED") { anon.login(LoginRequest("bob@example.com", password)) }
        assertEquals(409, http("/api/admin/users/${aliceSession.account.userId}/status", "PATCH", """{"enabled":false}""", aliceSession.token).statusCode())
        assertEquals(200, http("/api/admin/users/${bobSession.account.userId}/status", "PATCH", """{"enabled":true}""", aliceSession.token).statusCode())
        val signedIn = anon.login(LoginRequest("bob@example.com", password))
        assertEquals(200, http("/api/admin/users/${bobSession.account.userId}/revoke-sessions", "POST", token = aliceSession.token).statusCode())
        expectError(401, "UNAUTHENTICATED") { client(signedIn.token).me() }
        assertTrue(tree(http("/api/admin/audit", token = aliceSession.token).body()).any { it.path("action").asText() == "USER_SUSPENDED" && it.path("targetId").asText() == bobSession.account.userId })
        assertEquals("Bob", profiles.findByUserId(bobSession.account.userId)!!.username)
    }
    @Test
    fun `release metadata is public and only an administrator can publish validated revisions`() {
        val current = http("/api/app/version-policy")
        assertEquals(200, current.statusCode()); assertTrue(tree(current.body()).has("latestVersionCode"))
        assertEquals(403, http("/api/admin/version-policy", "PUT", "{}", bobSession.token).statusCode())
        makeAdmin()
        val managed = tree(http("/api/admin/version-policy", token = aliceSession.token).body())
        val updated = managed.deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
        (updated.path("policy") as com.fasterxml.jackson.databind.node.ObjectNode).put("latestVersionCode", 100).put("latestVersionName", "9.0.0")
        val saved = http("/api/admin/version-policy", "PUT", updated.toString(), aliceSession.token)
        assertEquals(200, saved.statusCode())
        assertEquals(100, tree(http("/api/app/version-policy").body()).path("latestVersionCode").asInt())
        assertEquals(409, http("/api/admin/version-policy", "PUT", updated.toString(), aliceSession.token).statusCode())
        val invalid = tree(saved.body()).deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
        (invalid.path("policy") as com.fasterxml.jackson.databind.node.ObjectNode).put("updateUrl", "javascript:alert(1)")
        assertEquals(400, http("/api/admin/version-policy", "PUT", invalid.toString(), aliceSession.token).statusCode())
        (managed as com.fasterxml.jackson.databind.node.ObjectNode).put("revision", tree(saved.body()).path("revision").asLong())
        assertEquals(200, http("/api/admin/version-policy", "PUT", managed.toString(), aliceSession.token).statusCode())
    }
    @Test
    fun `Drive authorization validates state identity and granted scope and encrypts refresh tokens`() = runBlocking {
        val authorization = alice.authorizeGoogleDrive()
        val query = java.net.URI(authorization.url).rawQuery.split('&').associate { val parts = it.split('=', limit = 2); parts[0] to java.net.URLDecoder.decode(parts[1], Charsets.UTF_8) }
        assertEquals("S256", query["code_challenge_method"])
        assertTrue(query["scope"]!!.contains(it.matato.dietreminder.backend.backup.DRIVE_SCOPE))
        val state = query["state"]!!
        val identityToken = googleToken(state, "alice@example.com")
        `when`(drive.exchange(eqString("authorization-code"), anyString())).thenReturn(it.matato.dietreminder.backend.backup.GoogleTokens("access", "refresh-secret", identityToken, it.matato.dietreminder.backend.backup.DRIVE_SCOPE))
        assertEquals(200, http("/api/google/drive/callback?code=authorization-code&state=$state").statusCode())
        val connection = driveConnections.findById(aliceSession.account.userId).orElseThrow()
        assertFalse(connection.encryptedRefreshToken.contains("refresh-secret"))
        assertEquals("refresh-secret", googleCipher.decrypt(connection.accountId, connection.encryptedRefreshToken))
        assertEquals(400, http("/api/google/drive/callback?code=authorization-code&state=$state").statusCode())
        assertTrue(alice.googleDriveStatus().connected)
        assertFalse(bob.googleDriveStatus().connected)
    }
    private fun eqString(value: String): String = eq(value) ?: value
    private fun connectDrive() {
        val id = aliceSession.account.userId
        driveConnections.saveAndFlush(it.matato.dietreminder.backend.backup.GoogleDriveConnection(id, googleCipher.encrypt(id, "refresh-secret"), "alice@example.com"))
        `when`(drive.refresh("refresh-secret")).thenReturn("drive-access")
    }
    @Test
    fun `Drive backup exports account data and restore respects ownership versions and atomic rollback`() = runBlocking {
        connectDrive()
        alice.putWorkspace(WorkspacePutRequest(0, "initial-backup", workspace("Before")))
        val backup = it.matato.dietreminder.backend.backup.GoogleBackupEnvelope(accountId = aliceSession.account.userId, exportedAt = java.time.Instant.now().toString(), snapshot = workspace("Restored"), profile = UserProfileDto(aliceSession.account.userId, "Restored profile", 2400))
        val payload = Json.encodeToString(it.matato.dietreminder.backend.backup.GoogleBackupEnvelope.serializer(), backup)
        `when`(drive.list("drive-access", aliceSession.account.userId)).thenReturn(emptyList())
        `when`(drive.upload(eqString("drive-access"), eqString(aliceSession.account.userId), anyString(), anyString())).thenAnswer { call ->
            val exported = Json.decodeFromString<it.matato.dietreminder.backend.backup.GoogleBackupEnvelope>(call.getArgument<String>(3))
            assertEquals(aliceSession.account.userId, exported.accountId)
            assertEquals("Before", exported.snapshot.diets.first().name)
            GoogleBackupDto("file-1", "backup.json", java.time.Instant.now().toString())
        }
        assertEquals("file-1", alice.createGoogleBackup().id)
        `when`(drive.download("drive-access", aliceSession.account.userId, "file-1")).thenReturn(payload)
        expectError(409, "WORKSPACE_CONFLICT") { alice.restoreGoogleBackup(GoogleRestoreRequest("file-1", 0, "stale-restore")) }
        assertEquals("Alice", alice.getProfile().username)
        val ack = alice.restoreGoogleBackup(GoogleRestoreRequest("file-1", 1, "restore-success"))
        assertEquals(2, ack.version); assertEquals("Restored", alice.getWorkspace().snapshot.diets.first().name)
        assertEquals(2400, alice.getProfile().dailyCalorieGoal)
        alice.saveProfile(ProfileUpdateRequest("Updated after restore", 2500))
        assertEquals(2, alice.restoreGoogleBackup(GoogleRestoreRequest("file-1", 1, "restore-success")).version)
        assertEquals(2500, alice.getProfile().dailyCalorieGoal)
        val foreign = backup.copy(accountId = bobSession.account.userId, profile = backup.profile.copy(userId = bobSession.account.userId))
        `when`(drive.download("drive-access", aliceSession.account.userId, "foreign")).thenReturn(Json.encodeToString(it.matato.dietreminder.backend.backup.GoogleBackupEnvelope.serializer(), foreign))
        expectError(400, "INVALID_BACKUP") { alice.restoreGoogleBackup(GoogleRestoreRequest("foreign", 2, "foreign-restore")) }
        assertEquals(2, alice.getWorkspace().version)
        val invalidProfile = backup.copy(profile = backup.profile.copy(dailyCalorieGoal = -10))
        `when`(drive.download("drive-access", aliceSession.account.userId, "bad-profile")).thenReturn(Json.encodeToString(it.matato.dietreminder.backend.backup.GoogleBackupEnvelope.serializer(), invalidProfile))
        expectError(400, "INVALID_REQUEST") { alice.restoreGoogleBackup(GoogleRestoreRequest("bad-profile", 2, "bad-restore")) }
        assertEquals(2, alice.getWorkspace().version)
        alice.disconnectGoogleDrive()
        verify(drive).revoke("refresh-secret")
        assertFalse(alice.googleDriveStatus().connected)
        expectError(409, "DRIVE_NOT_CONNECTED") { alice.googleBackups() }
    }

    @Test
    fun `empty workspaces and default account fields have a stable TypeScript wire contract`() {
        val response = tree(http("/api/workspace", token = aliceSession.token).body())
        assertEquals(0, response.path("version").asLong())
        for (field in listOf("diets", "meals", "courses", "foods", "lists", "items", "days")) {
            assertTrue(response.path("snapshot").path(field).isArray, "$field must be sent even when empty")
        }
        assertEquals(2, response.path("snapshot").path("schemaVersion").asInt())
        val account = tree(http("/api/auth/me", token = aliceSession.token).body())
        assertEquals("USER", account.path("role").asText())
        assertTrue(account.has("googleLinked")); assertFalse(account.path("googleLinked").asBoolean())
    }

    @Test
    fun `first password registration is administrator in its initial session and later accounts are users`() = runBlocking {
        clearInstallation()
        expectError(400, "INVALID_REQUEST") { registerApproved(RegisterRequest("invalid@example.com", "short", "Invalid")) }
        val first = registerApproved(RegisterRequest("first@example.com", password, "First"))
        val second = registerApproved(RegisterRequest("second@example.com", password, "Second"))
        assertEquals("ADMIN", first.account.role)
        assertEquals("ADMIN", client(first.token).me().role)
        assertEquals(200, http("/api/admin/overview", token = first.token).statusCode())
        assertEquals("USER", second.account.role)
        assertEquals(403, http("/api/admin/overview", token = second.token).statusCode())
    }

    @Test
    fun `first web registration receives the administrator role without signing in again`() {
        clearInstallation()
        val response = http("/api/web/auth/register", "POST",
            """{"email":"web-owner@example.com","password":"$password","username":"Web owner"}""")
        assertEquals(200, response.statusCode())
        assertEquals("ADMIN", tree(response.body()).path("account").path("role").asText())
        assertTrue(response.headers().firstValue("set-cookie").orElseThrow().contains("HttpOnly"))
    }

    @Test
    fun `first Google registration is administrator and later Google accounts are users`() = runBlocking {
        clearInstallation()
        val first = googleApproved(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "first@gmail.com", "first-google")))
        val second = googleApproved(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "second@gmail.com", "second-google")))
        assertEquals("ADMIN", first.account.role)
        assertEquals(200, http("/api/admin/overview", token = first.token).statusCode())
        assertEquals("USER", second.account.role)
        assertEquals(first.account.userId, googleApproved(GoogleSignInRequest(
            googleToken(anon.googleChallenge().nonce, "first@gmail.com", "first-google"))).account.userId)
    }

    @Test
    fun `concurrent password and Google registrations assign exactly one initial administrator`() = runBlocking {
        clearInstallation()
        val google = GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "race@gmail.com", "race-google"))
        val start = CompletableDeferred<Unit>()
        val registrations = (1..3).map { index -> async(Dispatchers.IO) {
            start.await()
            registerApproved(RegisterRequest("race-$index@example.com", password, "Race $index"))
        } } + async(Dispatchers.IO) { start.await(); googleApproved(google) }
        start.complete(Unit)
        val sessions = registrations.awaitAll()
        assertEquals(1, sessions.count { it.account.role == "ADMIN" })
        assertEquals(3, sessions.count { it.account.role == "USER" })
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE role = 'ADMIN' AND enabled", Int::class.java)!!)
        assertEquals(1L, jdbc.queryForObject("SELECT active_admin_count FROM account_administration_guard", Long::class.java)!!)
    }

    @Test
    fun `database rejects removal demotion suspension and truncation of the last administrator`() {
        val id = bootstrapSession.account.userId
        for (sql in listOf("DELETE FROM accounts WHERE id = ?", "UPDATE accounts SET role = 'USER' WHERE id = ?",
            "UPDATE accounts SET enabled = false WHERE id = ?")) {
            assertThrows(org.springframework.dao.DataIntegrityViolationException::class.java) { jdbc.update(sql, id) }
            assertEquals("ADMIN", accounts.findById(id).orElseThrow().role)
            assertTrue(accounts.findById(id).orElseThrow().enabled)
        }
        assertThrows(org.springframework.dao.DataIntegrityViolationException::class.java) { jdbc.execute("TRUNCATE accounts CASCADE") }
        assertEquals(409, http("/api/admin/users/$id/status", "PATCH", """{"enabled":false}""", bootstrapSession.token).statusCode())
        assertEquals(200, http("/api/admin/overview", token = bootstrapSession.token).statusCode())
    }

    @Test
    fun `administrator can be removed when another active administrator remains`() {
        jdbc.update("UPDATE accounts SET role = 'ADMIN' WHERE id = ?", bobSession.account.userId)
        jdbc.update("DELETE FROM accounts WHERE id = ?", bootstrapSession.account.userId)
        assertEquals(1L, jdbc.queryForObject("SELECT active_admin_count FROM account_administration_guard", Long::class.java)!!)
        assertEquals(200, http("/api/admin/overview", token = bobSession.token).statusCode())
        assertThrows(org.springframework.dao.DataIntegrityViolationException::class.java) {
            jdbc.update("UPDATE accounts SET enabled = false WHERE id = ?", bobSession.account.userId)
        }
    }

    @Test
    fun `concurrent administrator removals cannot both succeed`() = runBlocking {
        jdbc.update("UPDATE accounts SET role = 'ADMIN' WHERE id = ?", bobSession.account.userId)
        val start = CompletableDeferred<Unit>()
        val changes = listOf(bootstrapSession.account.userId, bobSession.account.userId).map { id -> async(Dispatchers.IO) {
            start.await()
            try { jdbc.update("UPDATE accounts SET role = 'USER' WHERE id = ?", id); true }
            catch (_: org.springframework.dao.DataIntegrityViolationException) { false }
        } }
        start.complete(Unit)
        assertEquals(1, changes.awaitAll().count { it })
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM accounts WHERE role = 'ADMIN' AND enabled", Int::class.java)!!)
        assertEquals(1L, jdbc.queryForObject("SELECT active_admin_count FROM account_administration_guard", Long::class.java)!!)
    }

    @Test
    fun `rolled back first registration does not consume administrator bootstrap`() = runBlocking {
        clearInstallation()
        assertThrows(IllegalStateException::class.java) {
            transaction.executeWithoutResult {
                val account = creation.create(AccountEntity("rolled-back", "rollback@example.com", null, "Rollback"))
                assertEquals("ADMIN", account.role)
                throw IllegalStateException("Roll back this registration")
            }
        }
        assertEquals(0, accounts.count())
        val next = registerApproved(RegisterRequest("actual-first@example.com", password, "First committed account"))
        assertEquals("ADMIN", next.account.role)
    }

    @Test
    fun `upgrade repairs a legacy installation without replacing an existing active administrator`() {
        for (existingAdmin in listOf(false, true)) {
            val schema = "admin_upgrade_" + java.util.UUID.randomUUID().toString().replace("-", "")
            fun migration(target: String) = org.flywaydb.core.Flyway.configure().dataSource(dataSource)
                .schemas(schema).defaultSchema(schema).target(target).load()
            try {
                migration("3").migrate()
                jdbc.update("INSERT INTO $schema.accounts(id, email, username, role, enabled, created_at) VALUES ('oldest', 'oldest@example.com', 'Oldest', 'USER', true, 10)")
                jdbc.update("INSERT INTO $schema.accounts(id, email, username, role, enabled, created_at) VALUES ('later', 'later@example.com', 'Later', ?, true, 20)", if (existingAdmin) "ADMIN" else "USER")
                migration("latest").migrate()
                assertEquals(if (existingAdmin) "USER" else "ADMIN", jdbc.queryForObject("SELECT role FROM $schema.accounts WHERE id = 'oldest'", String::class.java))
                assertEquals(if (existingAdmin) "ADMIN" else "USER", jdbc.queryForObject("SELECT role FROM $schema.accounts WHERE id = 'later'", String::class.java))
                assertEquals(1L, jdbc.queryForObject("SELECT active_admin_count FROM $schema.account_administration_guard", Long::class.java)!!)
                assertEquals(0, migration("latest").migrate().migrationsExecuted)
                jdbc.update("INSERT INTO $schema.accounts(id, email, username, created_at) VALUES ('newcomer', 'newcomer@example.com', 'Newcomer', 30)")
                assertEquals("USER", jdbc.queryForObject("SELECT role FROM $schema.accounts WHERE id = 'newcomer'", String::class.java))
                assertEquals(1L, jdbc.queryForObject("SELECT active_admin_count FROM $schema.account_administration_guard", Long::class.java)!!)

            } finally { jdbc.execute("DROP SCHEMA IF EXISTS $schema CASCADE") }
        }
    }

    @Test
    fun `upgrade removes obsolete Google verification while preserving password enrollment`() {
        val schema = "google_upgrade_" + java.util.UUID.randomUUID().toString().replace("-", "")
        fun migration(target: String) = org.flywaydb.core.Flyway.configure().dataSource(dataSource)
            .schemas(schema).defaultSchema(schema).target(target).load()
        try {
            migration("5").migrate()
            for ((id, google) in listOf("owner" to true, "google" to true, "password" to false)) {
                jdbc.update("INSERT INTO $schema.accounts(id,email,username,created_at,google_subject) VALUES (?, ?, ?, 10, ?)", id, "$id@example.com", id, if (google) id else null)
                jdbc.update("INSERT INTO $schema.account_tokens(hash,account_id,purpose,expires_at) VALUES (?, ?, 'VERIFY_EMAIL', 9999999999999)", id, id)
                jdbc.update("INSERT INTO $schema.account_mail_outbox(account_id,kind,recipient,subject,body,next_attempt_at) VALUES (?, 'VERIFY_EMAIL', ?, 'Verify', 'old-code', 0)", id, "$id@example.com")
            }
            migration("latest").migrate()
            assertEquals("APPROVED", jdbc.queryForObject("SELECT enrollment_status FROM $schema.accounts WHERE id = 'owner'", String::class.java))
            assertEquals("PENDING_APPROVAL", jdbc.queryForObject("SELECT enrollment_status FROM $schema.accounts WHERE id = 'google'", String::class.java))
            assertEquals("PENDING_EMAIL", jdbc.queryForObject("SELECT enrollment_status FROM $schema.accounts WHERE id = 'password'", String::class.java))
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM $schema.accounts WHERE email_verified", Int::class.java)!!)
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM $schema.account_tokens WHERE purpose = 'VERIFY_EMAIL'", Int::class.java)!!)
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM $schema.account_mail_outbox WHERE kind = 'VERIFY_EMAIL'", Int::class.java)!!)
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM $schema.account_mail_outbox WHERE kind = 'STATUS'", Int::class.java)!!)
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM $schema.enrollment_events", Int::class.java)!!)
            assertEquals(0, migration("latest").migrate().migrationsExecuted)
        } finally { jdbc.execute("DROP SCHEMA IF EXISTS $schema CASCADE") }
    }

    private fun plainText(part: Part): String {
        if (part.isMimeType("text/plain")) return part.content as String
        val content = part.content
        if (content is Multipart) return (0 until content.count).joinToString("\n") { plainText(content.getBodyPart(it)) }
        return ""
    }

    private fun verificationCode(id: String): String {
        val body = jdbc.queryForObject("SELECT body FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL' ORDER BY id DESC LIMIT 1", String::class.java, id)!!
        return Regex("[A-Za-z0-9_-]{43}").find(body)!!.value
    }
    private fun reviewAccount(session: SessionDto, approved: Boolean = true, grants: Set<AccountPermission> = AccountPermission.entries.toSet(), reason: String = ""): java.net.http.HttpResponse<String> {
        val current = tree(http("/api/auth/me", token = session.token).body())
        return http("/api/admin/users/${session.account.userId}/review", "POST",
            """{"version":${current.path("version").asLong()},"approved":$approved,"permissions":[${grants.joinToString(",") { "\"${it.name}\"" }}],"reason":${com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(reason)}}""", bootstrapSession.token)
    }

    @Test
    fun `first administrator must verify email before access and introduction acknowledgement persists`() = runBlocking {
        clearInstallation()
        val owner = anon.register(RegisterRequest("new-owner@example.com", password, "Owner"))
        val api = client(owner.token)
        assertEquals("ADMIN", owner.account.role)
        assertFalse(owner.account.ready)
        assertEquals(EnrollmentStatus.PENDING_EMAIL, api.me().enrollmentStatus)
        expectError(403, "ACCOUNT_NOT_READY") { api.getWorkspace() }
        assertEquals(403, http("/api/admin/overview", token = owner.token).statusCode())
        assertEquals(403, http("/api/auth/admin-introduction", "POST", token = owner.token).statusCode())
        expectError(400, "INVALID_VERIFICATION_TOKEN") { api.verifyEmail(VerifyEmailRequest("invalid")) }
        val verified = api.verifyEmail(VerifyEmailRequest(verificationCode(owner.account.userId)))
        assertTrue(verified.ready)
        assertTrue(verified.adminIntroductionPending)
        assertEquals(200, http("/api/admin/overview", token = owner.token).statusCode())
        assertFalse(api.acknowledgeAdminIntroduction().adminIntroductionPending)
        assertFalse(anon.login(LoginRequest("new-owner@example.com", password)).account.adminIntroductionPending)
        expectError(409, "EMAIL_ALREADY_VERIFIED") { api.verifyEmail(VerifyEmailRequest("A".repeat(43))) }
    }

    @Test
    fun `Google registration trusts verified provider email but still requires administrator approval`() = runBlocking {
        val pending = anon.googleSignIn(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "pending@gmail.com", "pending-google")))
        val api = client(pending.token)
        assertTrue(pending.account.emailVerified)
        assertFalse(pending.account.ready)
        assertEquals(EnrollmentStatus.PENDING_APPROVAL, pending.account.enrollmentStatus)
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_tokens WHERE account_id = ? AND purpose = 'VERIFY_EMAIL'", Int::class.java, pending.account.userId)!!)
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL'", Int::class.java, pending.account.userId)!!)
        expectError(403, "ACCOUNT_NOT_READY") { api.getProfile() }
        assertEquals(200, reviewAccount(pending).statusCode())
        assertTrue(api.me().ready)
        assertEquals("USER", api.me().role)
    }

    @Test
    fun `first Google administrator is immediately ready and sees administrator introduction`() = runBlocking {
        clearInstallation()
        val owner = anon.googleSignIn(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "google-owner@gmail.com", "google-owner")))
        assertEquals("ADMIN", owner.account.role)
        assertTrue(owner.account.emailVerified)
        assertTrue(owner.account.ready)
        assertTrue(owner.account.adminIntroductionPending)
        assertEquals(200, http("/api/admin/overview", token = owner.token).statusCode())
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM account_tokens WHERE purpose = 'VERIFY_EMAIL'", Int::class.java)!!)
    }

    @Test
    fun `returning Google account awaiting old email verification progresses without a code`() = runBlocking {
        val signIn = anon.googleSignIn(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "returning@gmail.com", "returning-google")))
        jdbc.update("UPDATE accounts SET email_verified = false, enrollment_status = 'PENDING_EMAIL' WHERE id = ?", signIn.account.userId)
        val returning = anon.googleSignIn(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "returning@gmail.com", "returning-google")))
        assertTrue(returning.account.emailVerified)
        assertEquals(EnrollmentStatus.PENDING_APPROVAL, returning.account.enrollmentStatus)
        val version = returning.account.version
        val again = anon.googleSignIn(GoogleSignInRequest(googleToken(anon.googleChallenge().nonce, "returning@gmail.com", "returning-google")))
        assertEquals(version, again.account.version)
    }

    @Test
    fun `unverified accounts cannot read application data self approve or alter other accounts`() = runBlocking {
        val pending = anon.register(RegisterRequest("pending@example.com", password, "Pending"))
        val api = client(pending.token)
        assertEquals(EnrollmentStatus.PENDING_EMAIL, api.enrollment().history.single().status)
        expectError(403, "ACCOUNT_NOT_READY") { api.getWorkspace() }
        expectError(403, "ACCOUNT_NOT_READY") { api.pushSync(record) }
        expectError(403, "ACCOUNT_NOT_READY") { api.googleBackups() }
        assertEquals(403, http("/api/admin/users/${pending.account.userId}/review", "POST", "{}", pending.token).statusCode())
        val other = anon.register(RegisterRequest("other-pending@example.com", password, "Other pending"))
        expectError(400, "INVALID_VERIFICATION_TOKEN") { client(other.token).verifyEmail(VerifyEmailRequest(verificationCode(pending.account.userId))) }
        assertEquals(EnrollmentStatus.PENDING_EMAIL, api.me().enrollmentStatus)
    }

    @Test
    fun `verification expires and resend invalidates the previous code without resetting application state`() = runBlocking {
        val pending = anon.register(RegisterRequest("codes@example.com", password, "Codes"))
        val api = client(pending.token)
        val old = verificationCode(pending.account.userId)
        expectError(429, "RATE_LIMITED") { api.resendVerification() }
        jdbc.update("UPDATE account_mail_outbox SET created_at = ? WHERE account_id = ?", System.currentTimeMillis() - 61_000, pending.account.userId)
        api.resendVerification()
        val fresh = verificationCode(pending.account.userId)
        assertNotEquals(old, fresh)
        expectError(400, "INVALID_VERIFICATION_TOKEN") { api.verifyEmail(VerifyEmailRequest(old)) }
        jdbc.update("UPDATE account_tokens SET expires_at = 0 WHERE account_id = ? AND purpose = 'VERIFY_EMAIL'", pending.account.userId)
        expectError(400, "INVALID_VERIFICATION_TOKEN") { api.verifyEmail(VerifyEmailRequest(fresh)) }
        jdbc.update("DELETE FROM account_mail_outbox WHERE account_id = ?", pending.account.userId)
        api.resendVerification()
        api.verifyEmail(VerifyEmailRequest(verificationCode(pending.account.userId)))
        expectError(409, "EMAIL_ALREADY_VERIFIED") { api.resendVerification() }
    }

    @Test
    fun `administrator assigns effective permissions while approval and notifications are recorded atomically`() = runBlocking {
        val pending = anon.register(RegisterRequest("review@example.com", password, "Review"))
        val api = client(pending.token)
        api.verifyEmail(VerifyEmailRequest(verificationCode(pending.account.userId)))
        val queue = tree(http("/api/admin/users?enrollmentStatus=PENDING_APPROVAL", token = bootstrapSession.token).body())
        assertTrue(queue.path("users").any { it.path("id").asText() == pending.account.userId })
        val grants = setOf(AccountPermission.WORKSPACE_READ, AccountPermission.SHOPPING_WRITE)
        assertEquals(200, reviewAccount(pending, grants = grants).statusCode())
        assertEquals(grants, api.me().permissions)
        assertEquals("USER", api.me().role)
        expectError(403, "PERMISSION_DENIED") { api.putWorkspace(WorkspacePutRequest(0, "denied-diet", workspace("Denied"))) }
        val shopping = WorkspaceSnapshot(lists = listOf(CloudShoppingList(1, "Allowed shopping", 0)))
        assertEquals(1, api.putWorkspace(WorkspacePutRequest(0, "allowed-shopping", shopping)).version)
        expectError(403, "PERMISSION_DENIED") { api.saveProfile(ProfileUpdateRequest("Not allowed")) }
        expectError(403, "PERMISSION_DENIED") { api.createGoogleBackup() }
        expectError(403, "PERMISSION_DENIED") { api.restoreGoogleBackup(GoogleRestoreRequest("file", 1, "denied-restore")) }
        expectError(403, "PERMISSION_DENIED") { api.pushSync(record) }
        expectError(403, "FORBIDDEN") { api.acknowledgeAdminIntroduction() }
        assertEquals(listOf(EnrollmentStatus.PENDING_EMAIL, EnrollmentStatus.PENDING_APPROVAL, EnrollmentStatus.APPROVED), api.enrollment().history.map { it.status })
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM account_mail_outbox WHERE account_id = ? AND kind = 'REVIEW'", Int::class.java, pending.account.userId)!!)
        assertTrue(tree(http("/api/admin/audit", token = bootstrapSession.token).body()).any { it.path("action").asText() == "ACCOUNT_APPROVED" })
    }

    @Test
    fun `rejection gives the applicant a reason and pending sessions remain restricted on every login`() = runBlocking {
        val pending = anon.register(RegisterRequest("rejected@example.com", password, "Rejected"))
        val api = client(pending.token)
        api.verifyEmail(VerifyEmailRequest(verificationCode(pending.account.userId)))
        assertEquals(400, reviewAccount(pending, approved = false).statusCode())
        assertEquals(200, reviewAccount(pending, approved = false, reason = "Richiesta non riconosciuta").statusCode())
        val status = api.enrollment()
        assertEquals(EnrollmentStatus.REJECTED, status.account.enrollmentStatus)
        assertEquals("Richiesta non riconosciuta", status.account.reviewReason)
        assertFalse(anon.login(LoginRequest("rejected@example.com", password)).account.ready)
        expectError(403, "ACCOUNT_NOT_READY") { api.getWorkspace() }
        assertEquals(409, reviewAccount(pending).statusCode())
        assertEquals(409, reviewAccount(bootstrapSession).statusCode())
    }

    @Test
    fun `stale reviews cannot override permissions and required read access cannot be removed`() = runBlocking {
        val pending = anon.register(RegisterRequest("stale-review@example.com", password, "Stale"))
        val api = client(pending.token)
        api.verifyEmail(VerifyEmailRequest(verificationCode(pending.account.userId)))
        assertEquals(400, reviewAccount(pending, grants = emptySet()).statusCode())
        val version = api.me().version
        val body = """{"version":$version,"approved":true,"permissions":["WORKSPACE_READ"]}"""
        assertEquals(200, http("/api/admin/users/${pending.account.userId}/review", "POST", body, bootstrapSession.token).statusCode())
        assertEquals(409, http("/api/admin/users/${pending.account.userId}/review", "POST", body, bootstrapSession.token).statusCode())
        val forbidden = """{"version":${api.me().version},"approved":true,"permissions":["ADMIN"]}"""
        assertEquals(400, http("/api/admin/users/${pending.account.userId}/review", "POST", forbidden, bootstrapSession.token).statusCode())
        assertEquals("USER", api.me().role)
        assertEquals(400, http("/api/admin/users?enrollmentStatus=invalid", token = bootstrapSession.token).statusCode())
    }

    @Test
    fun `failed email delivery is retained and retried without losing the application`() = runBlocking {
        doThrow(org.springframework.mail.MailSendException("SMTP unavailable")).`when`(mail).send(any(MimeMessage::class.java))
        val pending = anon.register(RegisterRequest("mail-retry@example.com", password, "Retry"))
        enrollmentMail.deliver()
        assertTrue(jdbc.queryForObject("SELECT attempts FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL'", Int::class.java, pending.account.userId)!! >= 1)
        assertEquals(EnrollmentStatus.PENDING_EMAIL, client(pending.token).me().enrollmentStatus)
        doNothing().`when`(mail).send(any(MimeMessage::class.java))
        jdbc.update("UPDATE account_mail_outbox SET next_attempt_at = 0 WHERE account_id = ?", pending.account.userId)
        enrollmentMail.deliver()
        assertNotNull(jdbc.queryForObject("SELECT sent_at FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL'", Long::class.java, pending.account.userId))
    }

}
