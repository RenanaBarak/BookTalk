package com.example.booktalk

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    var id: String = "",
    var bookTitle: String = "",
    var recommendation: String = "",
    var userId: String = "",
    var imagePath: String? = null,
    var timestamp: Timestamp? = null,

    var latitude: Double? = null,
    var longitude: Double? = null
)
