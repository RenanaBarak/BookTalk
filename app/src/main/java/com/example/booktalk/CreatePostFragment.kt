package com.example.booktalk

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.booktalk.databinding.FragmentCreatePostBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val postViewModel: PostViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth

    private var editingPostId: String? = null
    private var selectedImageUri: Uri? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double? = null
    private var currentLng: Double? = null

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        requestLocation()

        // עריכת פוסט קיים
        arguments?.let {
            editingPostId = it.getString("postId")
            val bookTitle = it.getString("bookTitle") ?: ""
            val recommendation = it.getString("recommendation") ?: ""

            binding.etBookTitle.setText(bookTitle)
            binding.etRecommendation.setText(recommendation)
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
                    handlePostResult(success, goToProfile = true)
                }
            } else {
                postViewModel.createPost(
                    bookTitle,
                    recommendation,
                    userId,
                    selectedImageUri?.toString(),
                    currentLat,
                    currentLng
                ) { success ->
                    handlePostResult(success, goToProfile = false)
                }
            }
        }

        binding.btnPickImage.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun handlePostResult(success: Boolean, goToProfile: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.btnSubmit.isEnabled = true

        if (success) {
            Toast.makeText(requireContext(), "Post saved successfully", Toast.LENGTH_SHORT).show()
            if (goToProfile) {
                findNavController().navigate(R.id.action_createPost_to_profileFragment)
            } else {
                findNavController().navigate(R.id.action_createPost_to_feed)
            }
        } else {
            Toast.makeText(requireContext(), "Failed to save post", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
                Log.d("CreatePostFragment", "📍 Location from device: $currentLat, $currentLng")
            } else {
                // ברירת מחדל: תל אביב
                currentLat = 32.0853
                currentLng = 34.7818
                Log.d("CreatePostFragment", "📍 No location available. Using default: $currentLat, $currentLng")
            }
        }.addOnFailureListener {
            // גם כאן – ברירת מחדל
            currentLat = 32.0853
            currentLng = 34.7818
            Log.e("CreatePostFragment", "❌ Failed to get location. Using default: $currentLat, $currentLng")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
