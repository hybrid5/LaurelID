package com.laurelid.network

import com.laurelid.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrustListRepository(private val api: TrustListApi) {
    private val mutex = Mutex()
    @Volatile
    private var cache: Map<String, String>? = null

    suspend fun refresh(): Map<String, String> = mutex.withLock {
        try {
            val remote = api.getTrustList()
            cache = remote
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
                Logger.i(TAG, "Loaded trust list with ${'$'}{remote.size} entries")
                remote
            } catch (throwable: Throwable) {
                Logger.e(TAG, "Unable to load trust list, falling back to cache", throwable)
                cache ?: throw throwable
            }
        }
    }

    fun cached(): Map<String, String>? = cache

    private companion object {
        private const val TAG = "TrustListRepo"
    }
}
