package it.matato.dietreminder.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.data.repository.github.GitHubVersionPolicyRepository
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
class GitHubVersionPolicyRepositoryTest {

	private lateinit var context: Context
	private lateinit var repository: GitHubVersionPolicyRepository

	@Before
	fun setup() {
		context = ApplicationProvider.getApplicationContext()
		repository = GitHubVersionPolicyRepository(context) {
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
		assertEquals(3, policy.latestVersionCode)
		assertEquals("1.2.1", policy.latestVersionName)
		assertEquals(true, isFromCache)
	}
}
