package com.laurelid.db

import android.content.Context
import androidx.room.Room

object DbModule {
    @Volatile
    private var instance: AppDb? = null

    fun provideDatabase(context: Context): AppDb {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext).also { instance = it }
        }
    }

    fun provideVerificationDao(context: Context): VerificationDao =
        provideDatabase(context).verificationDao()

    private fun buildDatabase(context: Context): AppDb {
        return Room.databaseBuilder(
            context,
            AppDb::class.java,
            "laurelid.db"
        ).fallbackToDestructiveMigration()
            .build()
    }
}
