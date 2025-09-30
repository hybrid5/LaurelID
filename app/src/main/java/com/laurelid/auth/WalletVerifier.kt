package com.laurelid.auth

import com.laurelid.data.VerificationResult
import com.laurelid.network.TrustListRepository
import com.laurelid.util.Logger

class WalletVerifier(private val trustListRepository: TrustListRepository) {

    suspend fun verify(parsed: ParsedMdoc, maxCacheAgeMillis: Long): VerificationResult {
        val trustList = try {
            trustListRepository.getOrRefresh(maxCacheAgeMillis)
    suspend fun verify(parsed: ParsedMdoc): VerificationResult {
        val trustList = try {
            trustListRepository.get()
        } catch (throwable: Throwable) {
            Logger.w(TAG, "Trust list fetch failed, falling back to cache", throwable)
            trustListRepository.cached() ?: emptyMap()
        }

        val issuerTrusted = !trustList.isNullOrEmpty() && trustList.containsKey(parsed.issuer)
        val success = parsed.ageOver21 && issuerTrusted
        val error = if (success) null else ERROR_UNTRUSTED_OR_UNDERAGE

        if (!issuerTrusted) {
            Logger.w(TAG, "Issuer ${parsed.issuer} not trusted by current list")
        }

        if (!parsed.ageOver21) {
            Logger.w(TAG, "Age policy not satisfied for ${parsed.subjectDid}")
        }

        // TODO: Implement full COSE signature validation and revocation checks once trust infrastructure is ready.
        return VerificationResult(
            success = success,
            ageOver21 = parsed.ageOver21,
            issuer = parsed.issuer,
            subjectDid = parsed.subjectDid,
            docType = parsed.docType,
            error = error
        )
    }

    companion object {
        private const val TAG = "WalletVerifier"
        private const val ERROR_UNTRUSTED_OR_UNDERAGE = "UNTRUSTED_OR_UNDERAGE"
    }
}
