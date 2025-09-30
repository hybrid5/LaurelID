package com.laurelid.network

import retrofit2.http.GET

interface TrustListApi {
    @GET("trust-list.json")
    suspend fun getTrustList(): Map<String, String>
}
