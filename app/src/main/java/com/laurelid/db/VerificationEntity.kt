package com.laurelid.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verification_log")
data class VerificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val success: Boolean,
    val subjectDid: String,
    val docType: String,
    val ageOver21: Boolean,
    val issuer: String,
    val error: String?,
    val tsMillis: Long
)
