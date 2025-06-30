package com.example.booktalk

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    var id: String = "",
    var bookTitle: String = "",
    var recommendation: String = "",
    var userId: String = "",
    var timestamp: Long = System.currentTimeMillis()
)
