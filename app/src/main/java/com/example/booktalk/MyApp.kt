package com.example.booktalk


import android.app.Application
import androidx.room.Room

class MyApp : Application() {

    /** Singleton של Room זמין לכל האפליקציה */
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
        // אפשר לייצר כאן services גלובליים אחרים
    }
}
