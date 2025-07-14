package com.example.booktalk.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.booktalk.model.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {
    val posts = repository.allPosts


    fun listenToAllPosts() {
        repository.removeListener()
        repository.listenToAllPostsFromFirebase { firebasePosts ->
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

    fun updatePost(
        postId: String,
        bookTitle: String,
        recommendation: String,
        onResult: (Boolean) -> Unit
    ) {
        repository.updatePost(postId, bookTitle, recommendation, onResult)
    }

    fun updatePostWithImage(
        postId: String,
        bookTitle: String,
        recommendation: String,
        imageUrl: String,
        onResult: (Boolean) -> Unit
    ) {
        repository.updatePostWithImage(postId, bookTitle, recommendation, imageUrl, onResult)
    }
    fun deletePost(postId: String, onResult: (Boolean) -> Unit) {
        repository.deletePost(postId, onResult)
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeListener()
    }


    class PostViewModelFactory(private val repository: PostRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PostViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}