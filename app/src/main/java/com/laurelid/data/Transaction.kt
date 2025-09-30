package com.laurelid.data

data class Transaction(
    val id: String,
    val verificationResult: VerificationResult,
    val createdAtMillis: Long
)
