package com.example.booktalk.controller

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.booktalk.view.PostAdapterFeed
import com.example.booktalk.controller.PostViewModel
import com.example.booktalk.controller.PostViewModelFactory
import com.example.booktalk.R
import com.example.booktalk.databinding.FragmentFeedBinding

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: PostViewModel
    private lateinit var postAdapter: PostAdapterFeed

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MyApp
        val factory = PostViewModelFactory(app)
        postViewModel = ViewModelProvider(requireActivity(), factory)[PostViewModel::class.java]

        postAdapter = PostAdapterFeed(mutableListOf()) { post ->
            val action = FeedFragmentDirections.actionFeedFragmentToPostDetailsFragment(
                postId = post.id,
                bookTitle = post.bookTitle,
                recommendation = post.recommendation,
                imageUri = post.imageUri ?: ""
            )
            findNavController().navigate(action)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
            isNestedScrollingEnabled = true
        }

        binding.btnOpenMap.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_postsMapFragment)
        }

        showLoading(true)

        postViewModel.posts.observe(viewLifecycleOwner) { posts ->
            showLoading(false)
            postAdapter.updatePosts(posts)
            Log.d("FeedFragment", "Loaded ${posts.size} posts")
        }

        postViewModel.listenToAllPosts()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}