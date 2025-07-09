package com.example.booktalk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PostViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")

    private val _posts = MutableLiveData<List<Post>>()      // LiveData לפוסטים
    val posts: LiveData<List<Post>> = _posts

    private var postsListenerRegistration: ListenerRegistration? = null

    /** מאזין בזמן אמת לכל הפוסטים (Feed) */
    fun listenToAllPosts() {
        // מבטל מאזין קודם אם קיים
        postsListenerRegistration?.remove()

        postsListenerRegistration = postsCollection
            .orderBy("timestamp")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("PostViewModel", "Listen failed: $err")
                    return@addSnapshotListener
                }
                _posts.value = snap?.toObjects(Post::class.java) ?: emptyList()
            }
    }

    /** מאזין בזמן אמת לפוסטים של משתמש ספציפי (פרופיל) */
    fun listenToUserPosts(uid: String) {
        postsListenerRegistration?.remove()

        postsListenerRegistration = postsCollection
            .whereEqualTo("userId", uid)
            .orderBy("timestamp")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("PostViewModel", "Listen failed: $err")
                    return@addSnapshotListener
                }
                _posts.value = snap?.toObjects(Post::class.java) ?: emptyList()
            }
    }

    /** עצירת המאזין כש־ViewModel נהרס */
    override fun onCleared() {
        super.onCleared()
        postsListenerRegistration?.remove()
    }

    // שאר הפונקציות שלך נשארות כמו שהן, אפשר לשמור אותן בלי שינוי

    fun createPost(
        bookTitle: String,
        recommendation: String,
        userId: String,
        imageUri: String?,
        onResult: (Boolean) -> Unit
    ) {
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

    fun updatePost(
        postId: String,
        bookTitle: String,
        recommendation: String,
        onResult: (Boolean) -> Unit
    ) {
        postsCollection.document(postId)
            .update(
                "bookTitle", bookTitle,
                "recommendation", recommendation,
                "timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            .addOnSuccessListener {
                Log.d("PostViewModel", "Post updated successfully")
                onResult(true)
            }
            .addOnFailureListener {
                Log.e("PostViewModel", "Failed to update post: ${it.message}", it)
                onResult(false)
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
}
