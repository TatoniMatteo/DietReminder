package it.matato.dietreminder.data.auth

import it.matato.dietreminder.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Server roots only: Retrofit appends api/... to this address. */
object ServerAddress {
    fun normalize(input: String, allowLocalHttp: Boolean = BuildConfig.DEBUG): String {
        val url = input.trim().toHttpUrl()
        require(url.username.isEmpty() && url.password.isEmpty())
        require(url.query == null && url.fragment == null)
        require(url.encodedPath == "/")
        val local = url.host == "localhost" || url.host == "10.0.2.2" || url.host == "127.0.0.1" || url.host == "::1"
        require(url.scheme == "https" || (allowLocalHttp && local))
        return url.toString()
    }
}
