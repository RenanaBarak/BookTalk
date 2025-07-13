package com.example.booktalk

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    @androidx.room.Ignore var id: String = "",
    @androidx.room.Ignore var bookTitle: String = "",
    @androidx.room.Ignore var recommendation: String = "",
    @androidx.room.Ignore var userId: String = "",
    @androidx.room.Ignore var timestamp: Timestamp? = null,
    @androidx.room.Ignore var latitude: Double? = null,
    @androidx.room.Ignore var longitude: Double? = null,
    @androidx.room.Ignore var imageUri: String? = null
) {
    constructor() : this(
        "", "", "", "", null, null, null, null
    )
}
