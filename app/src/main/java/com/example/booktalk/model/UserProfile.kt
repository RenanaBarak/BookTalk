package com.example.booktalk.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties

data class UserProfile(
    var uid: String = "",
    var name: String = "",
    var bio: String = "",
    val profileImageUrl: String? = null

)