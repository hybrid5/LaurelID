package com.laurelid.network

import com.laurelid.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrustListRepository(private val api: TrustListApi) {
    private val mutex = Mutex()
    @Volatile
    private var cache: Map<String, String>? = null
    @Volatile
    private var lastUpdatedMillis: Long = 0L

    suspend fun refresh(): Map<String, String> = mutex.withLock {
        try {
            val remote = api.getTrustList()
            cache = remote
            lastUpdatedMillis = System.currentTimeMillis()
            Logger.i(TAG, "Trust list refreshed with ${'$'}{remote.size} entries")
            remote
        } catch (throwable: Throwable) {
            Logger.e(TAG, "Failed to refresh trust list", throwable)
            throw throwable
        }
    }

    suspend fun get(): Map<String, String> = mutex.withLock {
        cache ?: run {
            try {
                val remote = api.getTrustList()
                cache = remote
                lastUpdatedMillis = System.currentTimeMillis()
                Logger.i(TAG, "Loaded trust list with ${'$'}{remote.size} entries")
                remote
            } catch (throwable: Throwable) {
                Logger.e(TAG, "Unable to load trust list, falling back to cache", throwable)
                cache ?: throw throwable
            }
        }
    }

    suspend fun getOrRefresh(maxAgeMillis: Long): Map<String, String> = mutex.withLock {
        val now = System.currentTimeMillis()
        val cached = cache
        val ageExceeded = maxAgeMillis > 0 && (cached == null || now - lastUpdatedMillis > maxAgeMillis)
        if (ageExceeded) {
            try {
                val remote = api.getTrustList()
                cache = remote
                lastUpdatedMillis = now
                Logger.i(TAG, "Trust list refreshed with ${remote.size} entries (policy ${maxAgeMillis}ms)")
                return remote
            } catch (throwable: Throwable) {
                Logger.e(TAG, "Trust list refresh failed, using cached copy", throwable)
                if (cached != null) {
                    return cached
                }
                throw throwable
            }
        }
        if (cached != null) {
            return cached
        }
        return try {
            val remote = api.getTrustList()
            cache = remote
            lastUpdatedMillis = now
            Logger.i(TAG, "Loaded trust list with ${remote.size} entries")
            remote
        } catch (throwable: Throwable) {
            Logger.e(TAG, "Unable to load trust list", throwable)
            throw throwable
        }
    }

    fun cached(): Map<String, String>? = cache

    private companion object {
        private const val TAG = "TrustListRepo"
    }
}
