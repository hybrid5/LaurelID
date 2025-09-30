package com.laurelid.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrustListRepository(private val api: TrustListApi) {
    private val mutex = Mutex()
    @Volatile
    private var cache: Map<String, String>? = null

    suspend fun refresh(): Map<String, String> = mutex.withLock {
        val remote = api.getTrustList()
        cache = remote
        remote
    }

    suspend fun get(): Map<String, String> = mutex.withLock {
        cache ?: run {
            try {
                val remote = api.getTrustList()
                cache = remote
                remote
            } catch (throwable: Throwable) {
                cache ?: throw throwable
            }
        }
    }

    fun cached(): Map<String, String>? = cache
}
