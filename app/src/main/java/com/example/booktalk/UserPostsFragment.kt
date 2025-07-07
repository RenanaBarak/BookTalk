package com.example.booktalk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log


class UserPostsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_user_posts, container, false)

        recyclerView = view.findViewById(R.id.recyclerUserPosts)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        postAdapter = PostAdapter(
            posts = postList,
            profileData = ProfileHeaderData(
                name = "",
                bio = "",
                email = "",
                imageUrl = null
            ),
            onEditClick = { post ->
                Toast.makeText(context, "Edit clicked: ${post.bookTitle}", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to edit screen if you want
            },
            onDeleteClick = { post ->
                deletePost(post)
            },
            onEditProfileClick = {
                // No profile edit here, or provide navigation if needed
            }
        )


        recyclerView.adapter = postAdapter

        loadUserPosts()

        return view
    }

    private fun loadUserPosts() {
        if (currentUserId == null) {
            Log.d("UserPostsFragment", "No current user ID")
            return
        }

        Log.d("UserPostsFragment", "Loading posts for user: $currentUserId")

        firestore.collection("posts")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val userPosts = snapshot.mapNotNull { it.toObject(Post::class.java) }
                    postAdapter.updatePosts(userPosts)
                }
            }

    }


    private fun deletePost(post: Post) {
        firestore.collection("posts").document(post.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                loadUserPosts() // refresh list
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }
}
