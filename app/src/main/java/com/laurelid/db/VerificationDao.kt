package com.laurelid.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VerificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VerificationEntity)

    @Query("SELECT * FROM verification_log ORDER BY tsMillis DESC LIMIT :limit")
    suspend fun latest(limit: Int = 10): List<VerificationEntity>
}
