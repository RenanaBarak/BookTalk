package com.example.booktalk

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class PostViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")

    fun createPost(bookTitle: String, recommendation: String, userId: String, imageUri: String?, onResult: (Boolean) -> Unit)
    {
        val postId = postsCollection.document().id
        val post = hashMapOf(
            "id" to postId,
            "bookTitle" to bookTitle,
            "recommendation" to recommendation,
            "userId" to userId,
            "imageUri" to imageUri,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

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



    fun fetchPosts(onResult: (List<Post>) -> Unit) {
        postsCollection.orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.toObjects(Post::class.java)
                onResult(posts)
            }
            .addOnFailureListener {
                Log.e("PostViewModel", "Failed to fetch posts: ${it.message}", it)
                onResult(emptyList())
            }
    }

    fun deletePost(postId: String, onResult: (Boolean) -> Unit) {
        postsCollection.document(postId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener {
                Log.e("PostViewModel", "Failed to delete post: ${it.message}", it)
                onResult(false)
            }
    }

    fun updatePost(postId: String, bookTitle: String, recommendation: String, onResult: (Boolean) -> Unit) {
        postsCollection.document(postId)
            .update("bookTitle", bookTitle, "recommendation", recommendation, "timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp())
            .addOnSuccessListener {
                Log.d("PostViewModel", "Post updated successfully")
                onResult(true)
            }
            .addOnFailureListener {
                Log.e("PostViewModel", "Failed to update post: ${it.message}", it)
                onResult(false)
            }
    }

    fun getAllPosts(onResult: (List<Post>) -> Unit) {
        postsCollection.orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                val postList = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(Post::class.java)
                    } catch (e: Exception) {
                        Log.e("PostViewModel", "Error parsing post: ${e.message}", e)
                        null
                    }
                }
                onResult(postList)
            }
            .addOnFailureListener {
                Log.e("PostViewModel", "Failed to fetch posts: ${it.message}", it)
                onResult(emptyList())
            }
    }



}
