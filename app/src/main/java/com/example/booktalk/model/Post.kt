package com.example.booktalk.model

import androidx.room.Ignore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    @Ignore var id: String = "",
    @Ignore var bookTitle: String = "",
    @Ignore var recommendation: String = "",
    @Ignore var userId: String = "",
    @Ignore var timestamp: Timestamp? = null,
    @Ignore var latitude: Double? = null,
    @Ignore var longitude: Double? = null,
    @Ignore var imageUri: String? = null
) {
    constructor() : this(
        "", "", "", "", null, null, null, null
    )
}