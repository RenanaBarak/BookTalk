package com.example.booktalk.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.booktalk.model.PostDao
import com.example.booktalk.model.PostEntity

@Database(entities = [PostEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}