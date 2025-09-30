package com.laurelid.auth

import com.laurelid.util.Logger
import java.util.UUID
import kotlin.math.absoluteValue

/**
 * Lightweight placeholder parser for ISO 18013-5 mobile driving licences.
 * The real implementation must perform the full mdoc engagement, device retrieval,
 * COSE/SD-JWT validation, and data element verification as defined by the spec.
 */
class ISO18013Parser {

    fun parseFromQrPayload(payload: String): ParsedMdoc {
        Logger.d(TAG, "Parsing QR payload: ${payload.take(64)}")
        // TODO: Replace placeholder parsing with ISO 18013-5 engagement handoff.
        val ageOver21 = payload.contains("age21=1", ignoreCase = true)
        val issuer = findValue(payload, "issuer") ?: DEFAULT_ISSUER
        val subject = findValue(payload, "subject") ?: createSubjectFromPayload(payload)
        return ParsedMdoc(
            subjectDid = subject,
            docType = DEFAULT_DOC_TYPE,
            issuer = issuer,
            ageOver21 = ageOver21
        )
    }

    fun parseFromNfc(bytes: ByteArray): ParsedMdoc {
        val payload = bytes.toString(Charsets.UTF_8)
        Logger.d(TAG, "Parsing NFC payload: ${payload.take(64)}")
        // TODO: Replace placeholder NFC parsing with full device engagement + session transcript decode.
        val ageOver21 = payload.contains("age21=1", ignoreCase = true)
        val issuer = findValue(payload, "issuer") ?: DEFAULT_ISSUER
        val subject = findValue(payload, "subject") ?: createSubjectFromPayload(payload)
        return ParsedMdoc(
            subjectDid = subject,
            docType = DEFAULT_DOC_TYPE,
            issuer = issuer,
            ageOver21 = ageOver21
        )
    }

    private fun findValue(payload: String, key: String): String? {
        val regex = Regex("(?i)$key=([^&;]+)")
        return regex.find(payload)?.groupValues?.getOrNull(1)
    }

    private fun createSubjectFromPayload(payload: String): String {
        val hash = payload.hashCode().absoluteValue
        return "did:example:${UUID.nameUUIDFromBytes("$hash".toByteArray()).toString()}"
    }

    companion object {
        private const val TAG = "ISO18013Parser"
        private const val DEFAULT_DOC_TYPE = "org.iso.18013.5.1.mDL"
        private const val DEFAULT_ISSUER = "AZ-MVD"
    }
}

data class ParsedMdoc(
    val subjectDid: String,
    val docType: String,
    val issuer: String,
    val ageOver21: Boolean
)
