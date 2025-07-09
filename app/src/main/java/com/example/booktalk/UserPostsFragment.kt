package com.example.booktalk

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booktalk.databinding.FragmentUserPostsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class UserPostsFragment : Fragment() {

    private var _binding: FragmentUserPostsBinding? = null
    private val binding get() = _binding!!

    private val postVM: PostViewModel by activityViewModels()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var postAdapter: PostAdapter
    private val posts = mutableListOf<Post>()

    private var profileData: ProfileHeaderData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                profileData = ProfileHeaderData(
                    name = doc.getString("name") ?: "Username",
                    bio = doc.getString("bio") ?: "",
                    email = doc.getString("email") ?: "",
                    imageUrl = doc.getString("profileImageUrl")
                )
                setupRecycler()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load user info", Toast.LENGTH_SHORT).show()
                profileData = ProfileHeaderData("Username", "", "", null)
                setupRecycler()
            }

        postVM.listenToUserPosts(userId)

        postVM.posts.observe(viewLifecycleOwner) { userPosts ->
            Log.d("UserPostsFragment", "Loaded ${userPosts.size} posts")
            posts.clear()
            posts.addAll(userPosts)
            postAdapter.notifyDataSetChanged()
        }
    }

    private fun setupRecycler() {
        val header = profileData ?: return

        postAdapter = PostAdapter(
            posts = posts,
            profileData = header,
            onEditClick = { post ->
                Toast.makeText(requireContext(), "Edit clicked: ${post.bookTitle}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { post ->
                postVM.deletePost(post.id) { ok ->
                    Toast.makeText(
                        requireContext(),
                        if (ok) "Post deleted" else "Failed to delete",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onEditProfileClick = {}
        )

        binding.recyclerUserPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
