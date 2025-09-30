package com.laurelid.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VerificationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun verificationDao(): VerificationDao
}
