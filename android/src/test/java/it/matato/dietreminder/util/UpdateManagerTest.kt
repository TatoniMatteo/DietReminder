package it.matato.dietreminder.util

import it.matato.dietreminder.BuildConfig
import it.matato.dietreminder.data.model.AppVersionState
import it.matato.dietreminder.data.model.VersionPolicy
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateManagerTest {

	private lateinit var policyRepository: FakeVersionPolicyRepository
	private lateinit var updateManager: UpdateManager

	@Before
	fun setup() {
		UpdateManagerStatus.isOffline = false
		UpdateManagerStatus.isChecking = false
		policyRepository = FakeVersionPolicyRepository()
		updateManager = UpdateManager(policyRepository)
	}

	@After
	fun tearDown() {
		UpdateManagerStatus.isOffline = false
		UpdateManagerStatus.isChecking = false
	}

	@Test
	fun testForceState_OverridesCorrectly() = runTest {
		updateManager.forceState(AppVersionState.OBSOLETE)
		assertEquals(AppVersionState.OBSOLETE, updateManager.versionStatus.value.state)

		updateManager.forceState(AppVersionState.DEPRECATED)
		assertEquals(AppVersionState.DEPRECATED, updateManager.versionStatus.value.state)

		updateManager.forceState(AppVersionState.RECENT)
		assertEquals(AppVersionState.RECENT, updateManager.versionStatus.value.state)

		updateManager.forceState(AppVersionState.CURRENT)
		assertEquals(AppVersionState.CURRENT, updateManager.versionStatus.value.state)

		updateManager.forceState(null)
	}

	@Test
	fun testSemVerParsing() {
		val semVer = SemVer.parse("1.2.3-SNAPSHOT")
		assertEquals(1, semVer.major)
		assertEquals(2, semVer.minor)
		assertEquals(3, semVer.patch)
	}

	@Test
	fun checkUpdate_evaluatesPolicyBoundaries() = runTest {
		val currentCode = BuildConfig.VERSION_CODE
		val currentVersion = BuildConfig.VERSION_NAME
		val currentMajor = SemVer.parse(currentVersion).major

		policyRepository.policy = VersionPolicy(
			minSupportedVersionCode = currentCode,
			deprecatedVersionCode = currentCode,
			latestVersionCode = currentCode,
			latestVersionName = currentVersion,
			minSupportedMajor = currentMajor,
		)
		updateManager.checkUpdate()
		assertEquals(AppVersionState.CURRENT, updateManager.versionStatus.value.state)

		policyRepository.policy = policyRepository.policy.copy(latestVersionCode = currentCode + 1)
		updateManager.checkUpdate()
		assertEquals(AppVersionState.RECENT, updateManager.versionStatus.value.state)

		policyRepository.policy = policyRepository.policy.copy(
			deprecatedVersionCode = currentCode + 1,
			latestVersionCode = currentCode + 2,
		)
		updateManager.checkUpdate()
		assertEquals(AppVersionState.DEPRECATED, updateManager.versionStatus.value.state)

		policyRepository.policy = policyRepository.policy.copy(minSupportedVersionCode = currentCode + 1)
		updateManager.checkUpdate()
		assertEquals(AppVersionState.OBSOLETE, updateManager.versionStatus.value.state)
		assertTrue(updateManager.versionStatus.value.isOffline)
	}

	@Test
	fun checkUpdate_propagatesOfflineCacheStatus() = runTest {
		policyRepository.isOffline = true

		updateManager.checkUpdate()

		assertTrue(updateManager.versionStatus.value.isOffline)
		assertTrue(UpdateManagerStatus.isOffline)
		assertFalse(updateManager.versionStatus.value.state == AppVersionState.OFFLINE)
	}

	@Test
	fun writesAreBlockedUntilInitialPolicyCheckCompletes() = runTest {
		assertTrue(updateManager.versionStatus.value.isChecking)
		assertTrue(UpdateManagerStatus.writesBlocked)

		updateManager.checkUpdate()

		assertFalse(updateManager.versionStatus.value.isChecking)
		assertFalse(UpdateManagerStatus.writesBlocked)
	}

	private class FakeVersionPolicyRepository : VersionPolicyRepository {
		var policy = VersionPolicy()
		var isOffline = false

		override suspend fun fetchPolicy(): Pair<VersionPolicy, Boolean> = policy to isOffline
	}
}
