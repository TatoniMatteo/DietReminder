package it.matato.dietreminder.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.data.repository.remote.ServerVersionPolicyRepository
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServerVersionPolicyRepositoryTest {

	private lateinit var context: Context
	private lateinit var repository: ServerVersionPolicyRepository

	@Before
	fun setup() {
		context = ApplicationProvider.getApplicationContext()
		repository = ServerVersionPolicyRepository(context) {
			throw IOException("Simulated offline connection")
		}
		File(context.cacheDir, "version_policy_cache.json").delete()
		File(context.cacheDir, "version_policy_timestamp.txt").delete()
	}

	@Test
	fun testCacheReadingWhenOffline() = runTest {
		val cacheFile = File(context.cacheDir, "version_policy_cache.json")
		val timestampFile = File(context.cacheDir, "version_policy_timestamp.txt")

		val jsonPolicy = """
			{
			  "minSupportedVersionCode": 1,
			  "deprecatedVersionCode": 1,
			  "latestVersionCode": 5,
			  "latestVersionName": "1.5.0",
			  "minSupportedMajor": 1,
			  "updateUrl": "https://github.com/test/update"
			}
		""".trimIndent()

		cacheFile.writeText(jsonPolicy)
		timestampFile.writeText(System.currentTimeMillis().toString())

		val (policy, isFromCache) = repository.fetchPolicy()
		assertNotNull(policy)
		assertEquals(5, policy.latestVersionCode)
		assertEquals("1.5.0", policy.latestVersionName)
		assertEquals("https://github.com/test/update", policy.updateUrl)
		assertEquals(true, isFromCache)
	}

	@Test
	fun testBundledAssetIsUsedWhenOfflineAndCacheIsMissing() = runTest {
		val (policy, isFromCache) = repository.fetchPolicy()

		assertNotNull(policy)
		assertEquals(7, policy.latestVersionCode)
		assertEquals(5, policy.deprecatedVersionCode)
		assertEquals("2.0.2", policy.latestVersionName)
		assertEquals(true, isFromCache)
	}
    @Test
    fun testValidServerPolicyIsCachedAndMalformedServerResponseDoesNotOverwriteIt() = runTest {
        var response = """{"latestVersionCode":9,"latestVersionName":"2.0.0","updateUrl":"https://example.com/release"}"""
        val remote = ServerVersionPolicyRepository(context) { response }
        assertEquals(9, remote.fetchPolicy().first.latestVersionCode)
        assertEquals(false, remote.fetchPolicy().second)
        response = "<html>Server unavailable</html>"
        val fallback = remote.fetchPolicy()
        assertEquals(9, fallback.first.latestVersionCode)
        assertEquals(true, fallback.second)
    }
    @Test
    fun testCorruptOrExpiredCacheFallsBackToBundledPolicy() = runTest {
        File(context.cacheDir, "version_policy_cache.json").writeText("corrupt")
        File(context.cacheDir, "version_policy_timestamp.txt").writeText(System.currentTimeMillis().toString())
        assertEquals(7, repository.fetchPolicy().first.latestVersionCode)
        File(context.cacheDir, "version_policy_cache.json").writeText("""{"latestVersionCode":100,"latestVersionName":"9.0.0"}""")
        File(context.cacheDir, "version_policy_timestamp.txt").writeText("1")
        assertEquals(7, repository.fetchPolicy().first.latestVersionCode)
    }

}
