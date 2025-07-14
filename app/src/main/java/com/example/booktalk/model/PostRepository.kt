package com.example.booktalk.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository(private val db: AppDatabase) {

    private val postDao = db.postDao()

    val allPosts: LiveData<List<Post>> =
        postDao.getAllPosts()
            .map { list -> list.map { it.toDomain() } }


    private val fs = FirebaseFirestore.getInstance()
    private val postsCol = fs.collection("posts")
    private var registration: ListenerRegistration? = null


    fun listenToAllPostsFromFirebase(callback: (List<Post>) -> Unit) {
        registration?.remove()
        registration = postsCol.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("PostRepo", "listenAll failed: $err")
                    return@addSnapshotListener
                }
                callback(snap?.toObjects(Post::class.java) ?: emptyList())
            }
    }

    fun listenToUserPostsFromFirebase(uid: String, callback: (List<Post>) -> Unit) {
        registration?.remove()
        registration = postsCol.whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("PostRepo", "listenUser failed: $err")
                    return@addSnapshotListener
                }
                callback(snap?.toObjects(Post::class.java) ?: emptyList())
            }
    }


    fun removeListener() {
        registration?.remove()
        registration = null
    }


    suspend fun savePostsToLocal(list: List<Post>) = withContext(Dispatchers.IO) {
        postDao.deleteAll()
        postDao.insertPosts(list.map { it.toEntity() })
    }

    fun createPost(
        bookTitle: String,
        recommendation: String,
        userId: String,
        imageUri: String?,
        lat: Double?,
        lng: Double?,
        onResult: (Boolean) -> Unit
    ) {
        val newId = postsCol.document().id
        val data = hashMapOf(
            "id" to newId,
            "bookTitle" to bookTitle,
            "recommendation" to recommendation,
            "userId" to userId,
            "timestamp" to FieldValue.serverTimestamp(),
            "latitude" to lat,
            "longitude" to lng,
            "imageUri" to imageUri
        )

        postsCol.document(newId).set(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }


    fun updatePost(
        postId: String,
        bookTitle: String,
        recommendation: String,
        onResult: (Boolean) -> Unit
    ) {
        postsCol.document(postId).update(
            mapOf(
                "bookTitle" to bookTitle,
                "recommendation" to recommendation,
                "timestamp" to FieldValue.serverTimestamp()
            )
        )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    fun updatePostWithImage(
        postId: String,
        bookTitle: String,
        recommendation: String,
        imageUrl: String,
        onResult: (Boolean) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "bookTitle" to bookTitle,
            "recommendation" to recommendation,
            "imageUri" to imageUrl
        )
        FirebaseFirestore.getInstance()
            .collection("posts")
            .document(postId)
            .update(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }


    fun deletePost(postId: String, onResult: (Boolean) -> Unit) {
        postsCol.document(postId).delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }


    private fun Post.toEntity() = PostEntity(
        id = id,
        bookTitle = bookTitle,
        recommendation = recommendation,
        userId = userId,
        imageUri = imageUri,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp?.seconds
    )

    private fun PostEntity.toDomain() = Post(
        id = id,
        bookTitle = bookTitle,
        recommendation = recommendation,
        userId = userId,
        timestamp = timestamp?.let { Timestamp(it, 0) },
        latitude = latitude,
        longitude = longitude,
        imageUri = imageUri
    )

}