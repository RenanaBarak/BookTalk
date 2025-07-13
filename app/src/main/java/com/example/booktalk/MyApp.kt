package com.example.booktalk


import android.app.Application
import androidx.room.Room
import com.cloudinary.android.MediaManager


class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val config = HashMap<String, String>()
        config["cloud_name"] = "ddaexvcdu"
        config["api_key"] = "269842741757611"
        MediaManager.init(this, config)
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "booktalk.db"
        ).fallbackToDestructiveMigration()
            .build()
    }


}
