package com.example.booktalk.controller

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booktalk.view.PostAdapterProfile
import com.example.booktalk.view.ProfileHeaderData
import com.example.booktalk.R
import com.example.booktalk.databinding.FragmentProfileBinding
import com.example.booktalk.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val postVM: PostViewModel by lazy {
        val app = requireActivity().application as MyApp
        val factory = PostViewModelFactory(app)
        ViewModelProvider(requireActivity(), factory)[PostViewModel::class.java]
    }

    private lateinit var postAdapter: PostAdapterProfile
    private val posts = mutableListOf<Post>()

    private var profileData: ProfileHeaderData? = null

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

        setupRecyclerView()

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Username"
                val bio = doc.getString("bio") ?: "Bio"
                val imageUrl = doc.getString("profileImageUrl")

                val newProfileData = ProfileHeaderData(name, bio, user.email ?: "", imageUrl)
                profileData = newProfileData
                postAdapter.updateProfileData(newProfileData)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }

        postVM.listenToUserPosts(user.uid)
        postVM.posts.observe(viewLifecycleOwner) { userPosts ->
            Log.d("ProfileFragment", "LiveData: ${userPosts.size} posts")
            posts.clear()
            posts.addAll(userPosts)
            postAdapter.notifyDataSetChanged()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapterProfile(
            posts = posts,
            profileData = profileData ?: ProfileHeaderData("Username", "Bio", "email@example.com"),
            onEditClick = { post ->
                val action = ProfileFragmentDirections.actionProfileFragmentToEditPostFragment(
                    postId = post.id,
                    bookTitle = post.bookTitle,
                    recommendation = post.recommendation,
                    imageUrl = post.imageUri ?: ""
                )
                findNavController().navigate(action)
            },
            onDeleteClick = { post ->
                postVM.deletePost(post.id) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to delete post",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
            },
            onEditProfileClick = {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            },
            onPostClick = { post ->
                val action = ProfileFragmentDirections.actionProfileFragmentToEditPostFragment(
                    postId = post.id,
                    bookTitle = post.bookTitle,
                    recommendation = post.recommendation,
                    imageUrl = post.imageUri ?: ""
                )
                findNavController().navigate(action)
            }
        )


        binding.profilePostsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}