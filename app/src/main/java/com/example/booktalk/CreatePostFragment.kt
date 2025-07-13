package com.example.booktalk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.booktalk.databinding.FragmentCreatePostBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: PostViewModel
    private lateinit var auth: FirebaseAuth

    private var editingPostId: String? = null
    private var selectedImageUri: Uri? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private val CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/ddaexvcdu/image/upload"
    private val UPLOAD_PRESET = "BookTalk"

    private val CAMERA_PERMISSION_CODE = 100

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivPostImage.setImageURI(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoBitmap = result.data?.extras?.get("data") as? Bitmap
            if (photoBitmap != null) {
                selectedImageUri = saveImageToCache(photoBitmap)
                binding.ivPostImage.setImageBitmap(photoBitmap)
            }
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

        val app = requireActivity().application as MyApp
        val factory = PostViewModelFactory(app)
        postViewModel = ViewModelProvider(this, factory)[PostViewModel::class.java]

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        requestLocation()

        arguments?.let {
            editingPostId = it.getString("postId")
            val bookTitle = it.getString("bookTitle") ?: ""
            val recommendation = it.getString("recommendation") ?: ""
            binding.etBookTitle.setText(bookTitle)
            binding.etRecommendation.setText(recommendation)
        }

        binding.btnPickImage.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnSubmit.setOnClickListener {
            submitPost(currentUser.uid)
        }
    }

    private fun submitPost(userId: String) {
        val bookTitle = binding.etBookTitle.text.toString().trim()
        val recommendation = binding.etRecommendation.text.toString().trim()

        if (bookTitle.isEmpty() || recommendation.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        if (!editingPostId.isNullOrEmpty()) {
            postViewModel.updatePost(editingPostId!!, bookTitle, recommendation) { success ->
                handlePostResult(success, goToProfile = true)
            }
        } else {
            if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri!!) { imageUrl ->
                    if (imageUrl.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        binding.btnSubmit.isEnabled = true
                        return@uploadImageToCloudinary
                    }
                    createNewPost(bookTitle, recommendation, userId, imageUrl)
                }
            } else {
                createNewPost(bookTitle, recommendation, userId, null)
            }
        }
    }

    private fun createNewPost(title: String, recommendation: String, userId: String, imageUrl: String?) {
        postViewModel.createPost(title, recommendation, userId, imageUrl, currentLat, currentLng) { success ->
            handlePostResult(success, goToProfile = false)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermissionAndOpen()
                1 -> imagePicker.launch("image/*")
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "post_image.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildCloudinaryRequest(fileUri: Uri): Request? {
        val inputStream = requireContext().contentResolver.openInputStream(fileUri)
        val requestBody = inputStream?.readBytes()?.toRequestBody("image/*".toMediaTypeOrNull()) ?: return null

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "image.jpg", requestBody)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        return Request.Builder()
            .url(CLOUDINARY_UPLOAD_URL)
            .post(multipartBody)
            .build()
    }

    private fun uploadImageToCloudinary(fileUri: Uri, onComplete: (String?) -> Unit) {
        val request = buildCloudinaryRequest(fileUri) ?: run {
            onComplete(null)
            return
        }

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Cloudinary", "Upload failed: ${e.message}")
                requireActivity().runOnUiThread {
                    onComplete(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string() ?: ""
                val json = JSONObject(bodyString)
                val imageUrl = json.optString("secure_url")
                Log.d("Cloudinary", "Uploaded image URL: $imageUrl")

                requireActivity().runOnUiThread {
                    onComplete(imageUrl)
                }
            }
        })
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
            } else {
                currentLat = 32.0853
                currentLng = 34.7818
            }
        }.addOnFailureListener {
            currentLat = 32.0853
            currentLng = 34.7818
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
