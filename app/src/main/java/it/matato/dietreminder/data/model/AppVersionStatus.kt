package it.matato.dietreminder.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppVersionState {
	CURRENT,
	RECENT,
	DEPRECATED,
	OBSOLETE,
	OFFLINE
}

@Serializable
data class VersionPolicy(
	val minSupportedVersionCode: Int = 1,
	val deprecatedVersionCode: Int = 1,
	val latestVersionCode: Int = 1,
	val latestVersionName: String = "1.1.0",
	val minSupportedMajor: Int = 1,
	val updateUrl: String = "https://github.com/TatoniMatteo/DietReminder/releases/latest"
)

data class AppVersionStatus(
	val state: AppVersionState = AppVersionState.CURRENT,
	val currentVersionCode: Int = 1,
	val latestVersionCode: Int = 1,
	val updateUrl: String = "https://github.com/TatoniMatteo/DietReminder/releases/latest",
	val isOffline: Boolean = false,
	val isChecking: Boolean = true,
)
