package com.example.booktalk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map          // ← extension KTX במקום Transformations
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository(private val db: AppDatabase) {

    /* ---------- Room ---------- */
    private val postDao = db.postDao()

    /** LiveData שה‑ViewModel צורך (Domain Model) */
    val allPosts: LiveData<List<Post>> =
        postDao.getAllPosts()                      // LiveData<List<PostEntity>>
            .map { list -> list.map { it.toDomain() } }   // LiveData<List<Post>>

    /* ---------- Firebase ---------- */
    private val fs = FirebaseFirestore.getInstance()
    private val postsCol = fs.collection("posts")
    private var registration: ListenerRegistration? = null

    /* ---------- Firebase – Listeners ---------- */

    fun listenToAllPostsFromFirebase(callback: (List<Post>) -> Unit) {
        registration?.remove()
        registration = postsCol.orderBy("timestamp")
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
            .orderBy("timestamp")
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

    /* ---------- Room <‑‑> Firebase סנכרון מקומי ---------- */

    /** שומר את הרשימה מפיירבייס בטבלת Room (IO) */
    suspend fun savePostsToLocal(list: List<Post>) = withContext(Dispatchers.IO) {
        postDao.deleteAll()
        postDao.insertPosts(list.map { it.toEntity() })
    }

    /* ---------- CRUD – Firebase + עדכון Room ברקע ---------- */

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
        val post = Post(
            id = newId,
            bookTitle = bookTitle,
            recommendation = recommendation,
            userId = userId,
            imagePath = imageUri,
            timestamp = null,              // יתמלא ע״י ה‑server
            latitude = lat,
            longitude = lng,
            imageUri = imageUri
        )

        postsCol.document(newId).set(post)
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

    fun deletePost(postId: String, onResult: (Boolean) -> Unit) {
        postsCol.document(postId).delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /* ---------- Entity ↔︎ Domain Converters ---------- */

    private fun Post.toEntity() = PostEntity(
        id = id,
        bookTitle = bookTitle,
        recommendation = recommendation,
        userId = userId,
        imageUri = imagePath,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp?.seconds          // Long?
    )

    private fun PostEntity.toDomain() = Post(
        id = id,
        bookTitle = bookTitle,
        recommendation = recommendation,
        userId = userId,
        imagePath = imageUri,
        timestamp = timestamp?.let { Timestamp(it, 0) },
        latitude = latitude,
        longitude = longitude,
        imageUri = imageUri                     // עדיין דרוש בשדה העזר
    )

}
