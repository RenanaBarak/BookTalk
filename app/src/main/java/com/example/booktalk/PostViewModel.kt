package com.example.booktalk

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class PostViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")

    fun createPost(bookTitle: String, recommendation: String, userId: String, onResult: (Boolean) -> Unit) {
        val postId = postsCollection.document().id
        val post = Post(id = postId, bookTitle = bookTitle, recommendation = recommendation, userId = userId)

        Log.d("PostViewModel", "Attempting to write post: $post")
        Log.d("PostViewModel", "Post data = $post")

        postsCollection.document(postId).set(post)
            .addOnSuccessListener {
                Log.d("PostViewModel", "Post created successfully")
                onResult(true)
            }
            .addOnFailureListener {
                Log.e("PostViewModel", "Failed to create post: ${it.message}", it)
                onResult(false)
            }
    }
}
