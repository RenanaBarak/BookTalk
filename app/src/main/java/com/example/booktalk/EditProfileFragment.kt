package com.example.booktalk

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.booktalk.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import java.io.FileOutputStream
import android.util.Log
import com.cloudinary.android.callback.ErrorInfo



class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST = 2
    private val CAMERA_PERMISSION_CODE = 100
    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUser = auth.currentUser ?: return

        loadProfile(currentUser.uid)
        setupButtons()
        showOrHidePasswordSection(currentUser)
    }

    private fun loadProfile(uid: String) {
        usersCollection.document(uid).get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.toObject(UserProfile::class.java)
                binding.etName.setText(profile?.name ?: "")
                binding.etBio.setText(profile?.bio ?: "")
                if (!profile?.profileImageUrl.isNullOrEmpty()) {
                    Picasso.get().load(profile?.profileImageUrl).into(binding.profileImageView)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtons() {
        binding.btnChangeProfilePicture.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()
            val uid = auth.currentUser!!.uid

            binding.progressBar.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false

            if (selectedImageUri != null) {
                uploadToCloudinaryAndSaveProfile(uid, name, bio, selectedImageUri!!)
            } else {
                saveProfile(uid, name, bio, null)
            }
        }

        binding.btnBackToProfile.setOnClickListener {
            findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
        }

        binding.btnChangePassword.setOnClickListener {
            handleChangePassword()
        }
    }

    private fun uploadToCloudinaryAndSaveProfile(uid: String, name: String, bio: String, imageUri: Uri) {
        MediaManager.get().upload(imageUri)
            .unsigned("BookTalk") // בדיוק השם של ה־preset כפי שמופיע אצלך
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "Upload started")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as? String
                    Log.d("Cloudinary", "Upload success: $imageUrl")
                    saveProfile(uid, name, bio, imageUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Upload error: ${error?.description}")
                    showSaveError()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Upload rescheduled: ${error?.description}")
                    showSaveError()
                }
            })
            .dispatch()
    }


    private fun saveProfile(uid: String, name: String, bio: String, imageUrl: String?) {
        val profile = UserProfile(uid, name, bio, imageUrl ?: "")
        usersCollection.document(uid).set(profile)
            .addOnSuccessListener { showSaveSuccess() }
            .addOnFailureListener { showSaveError() }
    }

    private fun showSaveSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.btnSave.isEnabled = true
        Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
    }

    private fun showSaveError() {
        binding.progressBar.visibility = View.GONE
        binding.btnSave.isEnabled = true
        Toast.makeText(requireContext(), "Failed to save profile", Toast.LENGTH_SHORT).show()
    }

    private fun showOrHidePasswordSection(user: FirebaseUser) {
        val isEmailPasswordUser = user.providerData.any { it.providerId == "password" }
        if (!isEmailPasswordUser) binding.passwordSection.visibility = View.GONE
    }

    private fun handleChangePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val user = auth.currentUser

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        user?.email?.let { email ->
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
                            clearPasswordFields()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to change password", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearPasswordFields() {
        binding.etCurrentPassword.text.clear()
        binding.etNewPassword.text.clear()
        binding.etConfirmPassword.text.clear()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermissionAndOpen()
                1 -> checkStoragePermissionAndOpen()
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndOpen() {
        val permission = android.Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    private fun checkStoragePermissionAndOpen() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), STORAGE_PERMISSION_CODE)
        } else {
            openGallery()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data
                    binding.profileImageView.setImageURI(selectedImageUri)
                }

                CAMERA_REQUEST -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    if (photo != null) {
                        val uri = saveImageToCache(photo)
                        selectedImageUri = uri
                        binding.profileImageView.setImageBitmap(photo)
                    }
                }
            }
        }
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "profile_pic.png")
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) openCamera()
            STORAGE_PERMISSION_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) openGallery()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
