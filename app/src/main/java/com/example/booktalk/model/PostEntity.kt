package com.example.booktalk.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val bookTitle: String,
    val recommendation: String,
    val userId: String,
    val imageUri: String?,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long?
)