package com.example.booktalk

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.booktalk.databinding.FragmentCreatePostBinding
import com.google.firebase.auth.FirebaseAuth
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val postViewModel: PostViewModel by activityViewModels()

    private lateinit var auth: FirebaseAuth
    private var editingPostId: String? = null
    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivPostImage.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }


        arguments?.let {
            editingPostId = it.getString("postId")
            val bookTitle = it.getString("bookTitle") ?: ""
            val recommendation = it.getString("recommendation") ?: ""

            binding.etBookTitle.setText(bookTitle)
            binding.etRecommendation.setText(recommendation)
        }

        if (editingPostId.isNullOrEmpty()) {
            binding.etBookTitle.text?.clear()
            binding.etRecommendation.text?.clear()
        }

        binding.btnSubmit.setOnClickListener {
            val bookTitle = binding.etBookTitle.text.toString().trim()
            val recommendation = binding.etRecommendation.text.toString().trim()
            val userId = currentUser.uid

            if (bookTitle.isEmpty() || recommendation.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmit.isEnabled = false

            if (!editingPostId.isNullOrEmpty()) {
                postViewModel.updatePost(editingPostId!!, bookTitle, recommendation) { success ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true

                    if (success) {
                        Toast.makeText(requireContext(), "Post updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_createPost_to_profileFragment)
                    } else {
                        Toast.makeText(requireContext(), "Failed to update post", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                postViewModel.createPost(bookTitle, recommendation, userId, selectedImageUri?.toString()) { success ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true

                    if (success) {
                        Toast.makeText(requireContext(), "Post created successfully", Toast.LENGTH_SHORT).show()
                        binding.etBookTitle.text?.clear()
                        binding.etRecommendation.text?.clear()
                        binding.ivPostImage.setImageResource(0)
                        selectedImageUri = null
                        findNavController().navigate(R.id.action_createPost_to_feed)
                    } else {
                        Toast.makeText(requireContext(), "Failed to create post", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnPickImage.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
