package com.laurelid.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitModule {
    const val DEFAULT_BASE_URL = "https://trustlist-placeholder.example.com/"
    private const val BASE_URL = "https://trustlist-placeholder.example.com/"
    private const val CACHE_SIZE_BYTES = 5L * 1024 * 1024 // 5 MiB

    private val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    fun provideTrustListApi(context: Context, baseUrl: String = DEFAULT_BASE_URL): TrustListApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl) // TODO: Replace with production trust list host.
    fun provideTrustListApi(context: Context): TrustListApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL) // TODO: Replace with production trust list host.
            .client(provideOkHttpClient(context))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(TrustListApi::class.java)
    }

    private fun provideOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE_BYTES)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
