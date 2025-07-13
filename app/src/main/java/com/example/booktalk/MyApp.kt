package com.example.booktalk


import android.app.Application
import androidx.room.Room

class MyApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "booktalk.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    override fun onCreate() {
        super.onCreate()
    }
}
