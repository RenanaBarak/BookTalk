package com.example.booktalk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booktalk.databinding.FragmentFeedBinding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth



class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val postViewModel: PostViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        postAdapter = PostAdapter(
            posts = mutableListOf(),
            onEditClick = { post ->
                // TODO: אפשרות לעריכת פוסט בהמשך
                Toast.makeText(requireContext(), "עריכה עדיין לא זמינה", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { post ->
                if (post.userId == currentUserId) {
                    postViewModel.deletePost(post.id) { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "הפוסט נמחק", Toast.LENGTH_SHORT).show()
                            loadPosts()
                        } else {
                            Toast.makeText(requireContext(), "שגיאה במחיקה", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "לא ניתן למחוק פוסט של משתמש אחר", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }

        binding.btnAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_createPost)
        }

        loadPosts()
    }

    private fun loadPosts() {
        postViewModel.getAllPosts { posts ->
            postAdapter.updatePosts(posts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
