package it.matato.dietreminder.data.sync

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.client.UserService
import it.matato.dietreminder.common.model.*
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.repository.room.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestDietApplication::class)
class WorkspaceSyncTest {
    private lateinit var db: AppDatabase
    private lateinit var journal: SyncJournal
    private lateinit var api: TestWorkspaceApi
    @Before fun prepare() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        journal = SyncJournal(db)
        api = TestWorkspaceApi()
    }
    @After fun close() { db.close() }
    private fun sample(name: String = "Diet") = WorkspaceSnapshot(
        diets = listOf(CloudDiet(1, "uuid", name, isActive = true)),
        meals = listOf(CloudMeal(1, 1, 2, MealType.LUNCH, 720, "Lunch")),
        courses = listOf(CloudCourse(1, 1, "Course")),
        foods = listOf(CloudFood(1, 1, "Rice", "100")),
        lists = listOf(CloudShoppingList(1, "Shopping", 1, 1)),
        items = listOf(CloudShoppingItem(1, 1, "Rice", "100", isFresh = true)),
        days = listOf(CloudShoppingDay(1, 1, 2, "100", isBought = true))
    )
    private suspend fun edit(name: String) = journal.mutate {
        val diet = db.dietDao().get(1) ?: Diet(id = 1, uuid = "uuid", name = name)
        if (db.dietDao().get(1) == null) db.dietDao().insert(diet) else db.dietDao().update(diet.copy(name = name))
    }
    private suspend fun state() = db.withTransaction { journal.state() }
    private suspend fun snapshot() = db.withTransaction { WorkspaceCache(db).read() }

    @Test fun pullingCompleteWorkspacePreservesRelationsAndDevicePreferences() = runBlocking {
        db.configDao().insertConfig(AppConfig(ConfigKey.THEME, "dark"))
        api.remote = WorkspaceDto(1, sample())
        assertTrue(WorkspaceSync(journal, api).run())
        assertEquals(sample(), snapshot())
        assertEquals("dark", db.configDao().getConfig("THEME")?.value)
        assertEquals(1, state().baseVersion)
        assertFalse(state().dirty)
    }
    @Test fun lostAcknowledgementSurvivesRestartAndRetriesTheSameRequestWithoutDuplicates() = runBlocking {
        edit("Initial")
        api.loseNextAck = true
        assertFalse(WorkspaceSync(journal, api).run())
        val request = state().pending!!.request
        val restartedJournal = SyncJournal(db)
        assertTrue(WorkspaceSync(restartedJournal, api).run())
        assertEquals(request, api.requests[1])
        assertEquals(1, api.accepted)
        assertFalse(state().dirty)
        assertNull(state().pending)
    }
    @Test fun editsArrivingDuringUploadAreNotMarkedAsAcknowledgedByTheOlderResponse() = runBlocking {
        edit("Initial")
        api.afterAccept = { edit("During upload") }
        assertTrue(WorkspaceSync(journal, api).run())
        assertEquals("During upload", api.remote.snapshot.diets.single().name)
        assertEquals(2, api.remote.version)
        assertFalse(state().dirty)
    }
    @Test fun conflictPreservesLocalEditsUntilExplicitResolution() = runBlocking {
        api.remote = WorkspaceDto(1, sample("Original"))
        WorkspaceSync(journal, api).run()
        edit("Local edit")
        api.remote = WorkspaceDto(2, sample("Other device"))
        assertTrue(WorkspaceSync(journal, api).run())
        assertEquals(2L, state().conflictVersion)
        assertEquals("Local edit", snapshot().diets.single().name)
        assertEquals("Other device", api.remote.snapshot.diets.single().name)
        val before = state().generation
        edit("Newer local edit")
        assertFalse(journal.applyRemote(api.remote, force = true, expectedGeneration = before))
        assertEquals("Newer local edit", snapshot().diets.single().name)
        assertTrue(journal.applyRemote(api.remote, force = true, expectedGeneration = state().generation))
        assertEquals("Other device", snapshot().diets.single().name)
        assertFalse(state().dirty)
    }
    @Test fun explicitlyKeepingLocalDataRebasesTheNextUploadOnTheCurrentCloudVersion() = runBlocking {
        api.remote = WorkspaceDto(1, sample("Original"))
        WorkspaceSync(journal, api).run()
        edit("Local edit")
        api.remote = WorkspaceDto(2, sample("Other device"))
        WorkspaceSync(journal, api).run()
        journal.keepLocal(api.remote.version)
        WorkspaceSync(journal, api).run()
        assertEquals(3, api.remote.version)
        assertEquals("Local edit", api.remote.snapshot.diets.single().name)
        assertFalse(state().dirty)
    }
    @Test fun offlineFailureNeverReplacesValidCacheWithAnEmptyResponse() = runBlocking {
        journal.applyRemote(WorkspaceDto(1, sample()))
        api.offline = true
        assertFalse(WorkspaceSync(journal, api).run())
        assertEquals(sample(), snapshot())
        assertEquals("NETWORK_ERROR", state().error)
    }
    @Test fun malformedRemoteGraphRollsBackWithoutDeletingTheCache() = runBlocking {
        journal.applyRemote(WorkspaceDto(1, sample()))
        assertTrue(runCatching { journal.applyRemote(WorkspaceDto(2, sample().copy(meals = emptyList()))) }.isFailure)
        assertEquals(sample(), snapshot())
        assertEquals(1, state().baseVersion)
    }
    @Test fun remoteDeletionRemovesChildrenAndShoppingListsAtomically() = runBlocking {
        journal.applyRemote(WorkspaceDto(1, sample()))
        api.remote = WorkspaceDto(2, WorkspaceSnapshot())
        WorkspaceSync(journal, api).run()
        assertEquals(WorkspaceSnapshot(), snapshot())
    }
    @Test fun importedExistingCacheIsNotSilentlyReplacedByFirstCloudDownload() = runBlocking {
        db.withTransaction { WorkspaceCache(db).replace(sample("Unsynchronized existing data")) }
        api.remote = WorkspaceDto(3, sample("Cloud data"))
        WorkspaceSync(journal, api).run()
        assertNotNull(state().conflictVersion)
        assertEquals("Unsynchronized existing data", snapshot().diets.single().name)
    }
    @Test fun cloudWritesAndDeveloperResetKeepTheJournalInTheSameTransaction() = runBlocking {
        val repository = CloudSharedRepository(
            RoomDietRepository(db, db.dietDao(), db.mealDao(), db.courseDao(), db.foodItemDao()),
            RoomShoppingListRepository(db, db.shoppingListDao(), db.dietDao(), db.mealDao()),
            RoomConfigRepository(db, db.configDao(), db.dietDao(), db.shoppingListDao()), journal
        )
        repository.create("Queued diet", 90)
        val listId = repository.createShoppingList("Queued shopping", null, emptyList())
        repository.addShoppingListItem(listId, "Rice", "100", QuantityUnit.GRAMS, true)
        assertTrue(state().dirty)
        WorkspaceSync(journal, api).run()
        assertEquals(1, api.remote.version)
        assertEquals(1, api.remote.snapshot.items.size)
        repository.resetDatabase()
        assertEquals(1, state().baseVersion)
        assertTrue(state().dirty)
        WorkspaceSync(journal, api).run()
        assertEquals(WorkspaceSnapshot(), api.remote.snapshot)
        assertEquals(2, api.remote.version)
    }
    @Test fun switchingAccountDuringUploadLeavesThePendingRequestForTheOriginalAccount() = runBlocking {
        edit("Alice data")
        var current = true
        api.afterAccept = { current = false }
        WorkspaceSync(journal, api) { current }.run()
        assertNotNull(state().pending)
        assertTrue(state().dirty)
        current = true
        WorkspaceSync(journal, api) { current }.run()
        assertEquals(1, api.accepted)
        assertFalse(state().dirty)
    }
}

private class TestWorkspaceApi : it.matato.dietreminder.TestGoogleApi() {
    var remote = WorkspaceDto(0, WorkspaceSnapshot())
    var loseNextAck = false
    var offline = false
    var accepted = 0
    var afterAccept: (suspend () -> Unit)? = null
    val requests = mutableListOf<WorkspacePutRequest>()
    private val receipts = mutableMapOf<String, WorkspaceAck>()
    override suspend fun getWorkspace(): WorkspaceDto { if (offline) throw IOException("Offline"); return remote }
    override suspend fun putWorkspace(request: WorkspacePutRequest): WorkspaceAck {
        if (offline) throw IOException("Offline")
        requests += request
        receipts[request.requestId]?.let { return it }
        if (request.baseVersion != remote.version) throw HttpException(Response.error<Any>(409, """{"code":"WORKSPACE_CONFLICT","message":"Conflict"}""".toResponseBody()))
        remote = WorkspaceDto(remote.version + 1, request.snapshot)
        val ack = WorkspaceAck(remote.version, request.requestId)
        receipts[request.requestId] = ack
        accepted++
        val callback = afterAccept; afterAccept = null; callback?.invoke()
        if (loseNextAck) { loseNextAck = false; throw IOException("Response lost after commit") }
        return ack
    }
    override suspend fun register(request: RegisterRequest): SessionDto = error("Unused")
    override suspend fun login(request: LoginRequest): SessionDto = error("Unused")
    override suspend fun me(): AccountDto = error("Unused")
    override suspend fun logout(authorization: String?) = Unit
    override suspend fun forgotPassword(request: ForgotPasswordRequest) = Unit
    override suspend fun resetPassword(request: ResetPasswordRequest) = Unit
    override suspend fun getProfile(): UserProfileDto = error("Unused")
    override suspend fun saveProfile(profile: ProfileUpdateRequest): UserProfileDto = error("Unused")
    override suspend fun pushSync(record: SyncPushRequest): SyncRecordDto = error("Unused")
    override suspend fun pullSync(afterId: Long, limit: Int): List<SyncRecordDto> = error("Unused")
}
