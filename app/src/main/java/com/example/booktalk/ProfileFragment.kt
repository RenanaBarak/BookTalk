package com.example.booktalk

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booktalk.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val postVM: PostViewModel by activityViewModels()

    private lateinit var postAdapter: PostAdapter
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

        // אתחול RecyclerView
        setupRecyclerView()

        // טען פרטי פרופיל
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

        // האזנה לפוסטים של המשתמש
        postVM.listenToUserPosts(user.uid)
        postVM.posts.observe(viewLifecycleOwner) { userPosts ->
            Log.d("ProfileFragment", "LiveData: ${userPosts.size} posts")
            posts.clear()
            posts.addAll(userPosts)
            postAdapter.notifyDataSetChanged()
        }

        // כפתור יציאה
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            posts = posts,
            profileData = profileData ?: ProfileHeaderData("Username", "Bio", "email@example.com"),
            onEditClick = { post ->
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToCreatePostFragment(
                        postId = post.id,
                        bookTitle = post.bookTitle,
                        recommendation = post.recommendation
                    )
                findNavController().navigate(action)
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
            onEditProfileClick = {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
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
