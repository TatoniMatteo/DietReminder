package it.matato.dietreminder.data.repository.remote

import android.content.Context
import it.matato.dietreminder.BuildConfig
import it.matato.dietreminder.data.model.VersionPolicy
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** The backend owns release metadata; cached and bundled policies support offline use. */
class ServerVersionPolicyRepository(
    private val context: Context,
    private val fetchPolicyText: () -> String = {
        val connection = URL(BuildConfig.BACKEND_BASE_URL + "api/app/version-policy").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != 200) throw IOException("Version policy is unavailable")
            val bytes = connection.inputStream.use { it.readNBytes(65537) }
            if (bytes.size > 65536) throw IOException("Version policy is too large")
            String(bytes, Charsets.UTF_8)
        } finally { connection.disconnect() }
    }
) : VersionPolicyRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile get() = File(context.cacheDir, "version_policy_cache.json")
    private val timestampFile get() = File(context.cacheDir, "version_policy_timestamp.txt")
    override suspend fun fetchPolicy(): Pair<VersionPolicy, Boolean> {
        try {
            val text = fetchPolicyText()
            val policy = parse(text)
            runCatching { cacheFile.writeText(text); timestampFile.writeText(System.currentTimeMillis().toString()) }
            return policy to false
        } catch (ex: CancellationException) { throw ex }
        catch (_: Exception) { /* Invalid remote JSON must not overwrite the last valid policy. */ }
        val age = System.currentTimeMillis() - (runCatching { timestampFile.readText().toLong() }.getOrDefault(0))
        if (age in 0 until 24 * 60 * 60 * 1000L) runCatching { parse(cacheFile.readText()) }.getOrNull()?.let { return it to true }
        return try { parse(context.assets.open("version-policy.json").bufferedReader().use { it.readText() }) to true }
        catch (ex: Exception) { throw IOException("No valid update policy is available", ex) }
    }
    private fun parse(text: String): VersionPolicy = json.decodeFromString<VersionPolicy>(text).also {
        val url = URI(it.updateUrl)
        require(it.minSupportedVersionCode >= 1 && it.deprecatedVersionCode >= it.minSupportedVersionCode && it.latestVersionCode >= it.deprecatedVersionCode && it.minSupportedMajor >= 1)
        require(it.latestVersionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?")))
        require(url.scheme == "https" && !url.host.isNullOrBlank() && url.userInfo == null)
    }
}
