package com.example.booktalk

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.booktalk.databinding.FragmentFeedBinding
import com.google.firebase.firestore.FirebaseFirestore


class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAdapter: PostAdapterSimple

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postAdapter = PostAdapterSimple(mutableListOf())

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
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("posts")
            .orderBy("timestamp") // or whatever field you want to sort by
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FeedFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                    postAdapter.updatePosts(posts)
                    Log.d("FeedFragment", "Loaded ${posts.size} posts")
                }
            }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
