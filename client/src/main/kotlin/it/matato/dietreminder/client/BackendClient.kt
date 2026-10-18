package it.matato.dietreminder.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object BackendClient {
    private val sharedHttpClient by lazy { defaultHttpClient() }
    fun create(
        baseUrl: String,
        httpClient: OkHttpClient = sharedHttpClient,
        tokenProvider: () -> String? = { null },
        onUnauthorized: (String) -> Unit = {}
    ): UserService {
        require(baseUrl.endsWith("/")) { "Backend URL must end with /" }
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient.newBuilder().addInterceptor { chain ->
                val token = chain.request().header("Authorization")?.removePrefix("Bearer ") ?: tokenProvider()
                val request = chain.request().newBuilder().apply {
                    if (token != null) header("Authorization", "Bearer $token")
                }.build()
                val response = chain.proceed(request)
                if (response.code == 401 && token != null && !request.url.encodedPath.startsWith("/api/auth/login")) {
                    onUnauthorized(token)
                }
                response
            }.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(UserService::class.java)
    }

    private fun defaultHttpClient() = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
}
