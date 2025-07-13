package com.example.booktalk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PostViewModelFactory(private val app: MyApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = PostRepository(app.database)
        return PostViewModel(repo) as T
    }
}
