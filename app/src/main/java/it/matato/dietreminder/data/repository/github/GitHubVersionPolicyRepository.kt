package it.matato.dietreminder.data.repository.github

import android.content.Context
import it.matato.dietreminder.data.model.VersionPolicy
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import it.matato.dietreminder.util.AppLog
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json

class GitHubVersionPolicyRepository(
	private val context: Context,
	private val fetchPolicyText: () -> String = {
		(URL(POLICY_URL).openConnection() as HttpURLConnection).run {
			connectTimeout = CONNECTION_TIMEOUT_MS
			readTimeout = READ_TIMEOUT_MS
			inputStream.bufferedReader().use { it.readText() }
		}
	},
) : VersionPolicyRepository {

	private val json = Json { ignoreUnknownKeys = true }
	private val cacheFile by lazy { File(context.cacheDir, "version_policy_cache.json") }
	private val cacheTimestampFile by lazy { File(context.cacheDir, "version_policy_timestamp.txt") }
	private val cacheValidityMs = 24 * 60 * 60 * 1000L // 24 hours

	override suspend fun fetchPolicy(): Pair<VersionPolicy, Boolean> {
		var isFromCache = false

		val jsonString = try {
			val text = fetchPolicyText()
			// Save to cache on successful network fetch
			cacheFile.writeText(text)
			cacheTimestampFile.writeText(System.currentTimeMillis().toString())
			text
		} catch (e: Exception) {
			AppLog.e("Network fetch failed for version policy, checking local cache", e)
			val timestamp = cacheTimestampFile.readTextOrNull()?.toLongOrNull() ?: 0L
			val now = System.currentTimeMillis()

			if ((cacheFile.exists()) && ((now - timestamp) < cacheValidityMs)) {
				isFromCache = true
				cacheFile.readText()
			} else {
				// Try assets as secondary offline fallback if cache is expired or missing
				try {
					isFromCache = true
					context.assets.open("version-policy.json").bufferedReader().use { it.readText() }
				} catch (e2: Exception) {
					AppLog.e("No valid cached policy available and device is offline", e)
					throw IOException("No valid cached policy available and device is offline", e)
				}
			}
		}

		val policy = try {
			json.decodeFromString<VersionPolicy>(jsonString)
		} catch (e: Exception) {
			AppLog.e("Failed to parse version policy JSON", e)
			throw IOException("Malformed version policy JSON", e)
		}

		return Pair(policy, isFromCache)
	}

	private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()

	private companion object {
		const val POLICY_URL =
			"https://raw.githubusercontent.com/TatoniMatteo/DietReminder/master/version-policy.json"
		const val CONNECTION_TIMEOUT_MS = 10_000
		const val READ_TIMEOUT_MS = 10_000
	}
}
