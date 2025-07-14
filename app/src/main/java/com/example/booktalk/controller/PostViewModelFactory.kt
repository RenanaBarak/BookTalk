package com.example.booktalk.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.booktalk.model.PostRepository

class PostViewModelFactory(private val app: MyApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = PostRepository(app.database)
        return PostViewModel(repo) as T
    }
}