package com.example.booktalk

import androidx.lifecycle.LiveData
import androidx.room.*
@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): LiveData<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(list: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}


