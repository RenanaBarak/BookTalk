package com.example.booktalk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {
    // LiveData שתצפה בטבלת הפוסטים מ-Room
    val posts = repository.allPosts


    fun listenToAllPosts() {
        repository.removeListener()
        repository.listenToAllPostsFromFirebase { firebasePosts ->
            // שומר את הפוסטים גם בבסיס הנתונים המקומי
            viewModelScope.launch(Dispatchers.IO) {
                repository.savePostsToLocal(firebasePosts)
            }
        }
    }

    fun listenToUserPosts(uid: String) {
        repository.removeListener()
        repository.listenToUserPostsFromFirebase(uid) { firebasePosts ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.savePostsToLocal(firebasePosts)
            }
        }
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
        repository.createPost(bookTitle, recommendation, userId, imageUri, lat, lng, onResult)
    }

    fun updatePost(postId: String, bookTitle: String, recommendation: String, onResult: (Boolean) -> Unit) {
        repository.updatePost(postId, bookTitle, recommendation, onResult)
    }

    fun deletePost(postId: String, onResult: (Boolean) -> Unit) {
        repository.deletePost(postId, onResult)
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeListener()
    }
}
