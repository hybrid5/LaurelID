package com.laurelid.data

data class VerificationResult(
    val success: Boolean,
    val ageOver21: Boolean,
    val issuer: String,
    val subjectDid: String,
    val docType: String,
    val error: String?
)
