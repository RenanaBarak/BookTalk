package com.example.booktalk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.booktalk.databinding.FragmentCreatePostBinding
import com.google.firebase.auth.FirebaseAuth

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val postViewModel: PostViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private var editingPostId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(requireContext(), "המשתמש אינו מחובר. נא להתחבר כדי לפרסם פוסט.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // בדיקת האם מדובר בעריכת פוסט
        arguments?.let {
            editingPostId = it.getString("postId")
            val bookTitle = it.getString("bookTitle")
            val recommendation = it.getString("recommendation")
            binding.etBookTitle.setText(bookTitle)
            binding.etRecommendation.setText(recommendation)
        }

        binding.btnSubmit.setOnClickListener {
            val bookTitle = binding.etBookTitle.text.toString().trim()
            val recommendation = binding.etRecommendation.text.toString().trim()
            val userId = currentUser.uid

            if (bookTitle.isEmpty() || recommendation.isEmpty()) {
                Toast.makeText(requireContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (editingPostId != null) {
                // עריכת פוסט קיים
                postViewModel.updatePost(editingPostId!!, bookTitle, recommendation) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "הפוסט עודכן", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_createPost_to_feed)
                    } else {
                        Toast.makeText(requireContext(), "שגיאה בעדכון הפוסט", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // יצירת פוסט חדש
                postViewModel.createPost(bookTitle, recommendation, userId) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "הפוסט נוצר בהצלחה", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_createPost_to_feed)
                    } else {
                        Toast.makeText(requireContext(), "שגיאה ביצירת פוסט", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
