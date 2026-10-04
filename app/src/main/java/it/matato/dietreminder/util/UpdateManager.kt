package it.matato.dietreminder.util

import it.matato.dietreminder.BuildConfig
import it.matato.dietreminder.data.model.AppVersionState
import it.matato.dietreminder.data.model.AppVersionStatus
import it.matato.dietreminder.data.model.VersionPolicy
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object UpdateManagerStatus {
	@Volatile
	var isOffline: Boolean = true

	@Volatile
	var isChecking: Boolean = true

	val writesBlocked: Boolean
		get() = isOffline || isChecking
}

data class SemVer(val major: Int, val minor: Int, val patch: Int) {
	companion object {
		fun parse(versionName: String): SemVer {
			val cleaned = versionName.substringBefore("-").trim()
			val parts = cleaned.split(".")
			val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
			val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
			val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
			return SemVer(major, minor, patch)
		}
	}
}

class UpdateManager(
	private val versionPolicyRepository: VersionPolicyRepository,
) {

	private val _versionStatus = MutableStateFlow(AppVersionStatus())
	val versionStatus: StateFlow<AppVersionStatus> = _versionStatus.asStateFlow()

	private var forcedState: AppVersionState? = null

	init {
		UpdateManagerStatus.isChecking = true
	}

	suspend fun checkUpdate() {
		_versionStatus.value = _versionStatus.value.copy(isChecking = true)
		UpdateManagerStatus.isChecking = true
		withContext(Dispatchers.IO) {
			try {
				val (policy, isOffline) = versionPolicyRepository.fetchPolicy()
				evaluatePolicy(policy, isOffline)
			} catch (e: Exception) {
				AppLog.e("Error checking app version status", e)
				evaluatePolicy(VersionPolicy(), isOffline = true)
			}
		}
	}

	fun forceState(state: AppVersionState?) {
		forcedState = state
		if (state != null) {
			_versionStatus.value = _versionStatus.value.copy(
				state = state,
				isOffline = _versionStatus.value.isOffline || state == AppVersionState.OBSOLETE
			)
			UpdateManagerStatus.isOffline = _versionStatus.value.isOffline
			_versionStatus.value = _versionStatus.value.copy(isChecking = false)
			UpdateManagerStatus.isChecking = false
		} else {
			evaluatePolicy(VersionPolicy(), isOffline = false)
		}
	}

	private fun evaluatePolicy(policy: VersionPolicy, isOffline: Boolean) {
		val currentCode = BuildConfig.VERSION_CODE
		val currentSemVer = SemVer.parse(BuildConfig.VERSION_NAME)
		val latestSemVer = SemVer.parse(policy.latestVersionName)

		val state = forcedState ?: when {
			(currentCode < policy.minSupportedVersionCode) || (currentSemVer.major < policy.minSupportedMajor) -> AppVersionState.OBSOLETE
			(currentCode < policy.deprecatedVersionCode) || (currentSemVer.major < latestSemVer.major) -> AppVersionState.DEPRECATED
			(currentCode < policy.latestVersionCode) -> AppVersionState.RECENT
			else -> AppVersionState.CURRENT
		}

		_versionStatus.value = AppVersionStatus(
			state = state,
			currentVersionCode = currentCode,
			latestVersionCode = policy.latestVersionCode,
			updateUrl = policy.updateUrl,
			isOffline = isOffline || (state == AppVersionState.OBSOLETE),
			isChecking = false,
		)
		UpdateManagerStatus.isOffline = _versionStatus.value.isOffline
		UpdateManagerStatus.isChecking = false
	}
}
