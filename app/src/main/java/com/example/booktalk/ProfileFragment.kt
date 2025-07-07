package com.example.booktalk

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booktalk.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var postAdapter: PostAdapter
    private var postsListenerRegistration: ListenerRegistration? = null

    private var profileData: ProfileHeaderData? = null
    private val posts = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Load profile info
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Username"
                val bio = doc.getString("bio") ?: "Bio"
                val imageUrl = doc.getString("profileImageUrl")

                profileData = ProfileHeaderData(name, bio, user.email ?: "", imageUrl)

                // Initialize adapter only after profile data is loaded
                setupRecyclerView()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        profileData?.let { data ->
            postAdapter = PostAdapter(
                posts = posts,
                profileData = data,
                onEditClick = { post ->
                    // Navigate to edit post page
                    val action = ProfileFragmentDirections.actionProfileFragmentToCreatePostFragment(
                        postId = post.id,
                        bookTitle = post.bookTitle,
                        recommendation = post.recommendation
                    )
                    findNavController().navigate(action)
                },
                onDeleteClick = { post ->
                    db.collection("posts").document(post.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to delete post", Toast.LENGTH_SHORT).show()
                        }
                },
                onEditProfileClick = {
                    findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
                }
            )

            binding.profilePostsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = postAdapter
                isNestedScrollingEnabled = false
            }

            // Now load posts after adapter is set
            loadUserPosts()
        }
    }

    private fun loadUserPosts() {
        val currentUser = auth.currentUser ?: return

        postsListenerRegistration?.remove()

        postsListenerRegistration = db.collection("posts")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val fetchedPosts = snapshot.documents
                        .mapNotNull { it.toObject(Post::class.java) }
                        .filter { it.timestamp != null }

                    Log.d("ProfileFragment", "Fetched ${fetchedPosts.size} posts")

                    // Update posts list & notify adapter
                    posts.clear()
                    posts.addAll(fetchedPosts)
                    postAdapter.notifyDataSetChanged()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postsListenerRegistration?.remove()
        _binding = null
    }
}
