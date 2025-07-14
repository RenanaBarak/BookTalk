package com.example.booktalk.controller

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.booktalk.R
import com.example.booktalk.databinding.FragmentEditProfileBinding
import com.example.booktalk.model.UserProfile
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.get

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE = 1
    private val CAMERA    = 2
    private val CAM_PERM  = 100
    private val STO_PERM  = 101

    override fun onCreateView(
        inflater: LayoutInflater, c: ViewGroup?, s: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        val uid = auth.currentUser?.uid ?: return
        loadProfile(uid)
        setupUi(uid)
        showOrHidePasswordSection(auth.currentUser!!)
    }

    private fun setupUi(uid: String) = with(binding) {

        btnChangeProfilePicture.setOnClickListener { showImagePickerDialog() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val bio  = etBio.text.toString().trim()

            showLoading(true)
            if (selectedImageUri != null)
                uploadToCloudinaryAndSaveProfile(uid, name, bio, selectedImageUri!!)
            else
                saveProfile(uid, name, bio, null)
        }

        btnBackToProfile.setOnClickListener {
            findNavController()
                .navigate(R.id.action_editProfileFragment_to_profileFragment)
        }

        btnChangePassword.setOnClickListener { handleChangePassword() }
    }

    private fun loadProfile(uid: String) {
        showLoading(true)
        usersCollection.document(uid).get()
            .addOnSuccessListener { snap ->
                snap.toObject(UserProfile::class.java)?.let { p ->
                    binding.etName.setText(p.name)
                    binding.etBio.setText(p.bio)
                    if (!p.profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(p.profileImageUrl)
                            .into(binding.profileImageView)
                    }
                }
                    showLoading(false)
            }
            .addOnFailureListener {
                showLoading(false)
                toast("Failed to load profile")
            }
    }

    private fun uploadToCloudinaryAndSaveProfile(
        uid: String, name: String, bio: String, img: Uri
    ) {
        MediaManager.get().upload(img)
            .unsigned("BookTalk")
            .callback(object : UploadCallback {
                override fun onStart(rId: String?) {}
                override fun onProgress(rId: String?, b: Long, t: Long) {}

                override fun onSuccess(rId: String?, res: Map<*, *>) {
                    val url = res["secure_url"] as? String
                    saveProfile(uid, name, bio, url)
                }

                override fun onError(rId: String?, err: ErrorInfo?) {
                    showLoading(false)
                    toast("Upload failed: ${err?.description}")
                }

                override fun onReschedule(rId: String?, err: ErrorInfo?) =
                    onError(rId, err)
            })
            .dispatch()
    }

    private fun saveProfile(uid: String, name: String, bio: String, url: String?) {
        usersCollection.document(uid)
            .set(UserProfile(uid, name, bio, url ?: ""))
            .addOnSuccessListener {
                showLoading(false)
                toast("Profile saved")
            }
            .addOnFailureListener {
                showLoading(false)
                toast("Failed to save profile")
            }
    }

    private fun showOrHidePasswordSection(user: FirebaseUser) {
        if (user.providerData.none { it.providerId == "password" })
            binding.passwordSection.visibility = View.GONE
    }

    private fun handleChangePassword(): Unit = with(binding) {
        val current = etCurrentPassword.text.toString()
        val newPwd  = etNewPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        val user    = auth.currentUser ?: return

        if (newPwd != confirm) {
            toast("Passwords do not match")
            return
        }

        user.email?.let { email ->
            val cred = EmailAuthProvider
                .getCredential(email, current)
            user.reauthenticate(cred)
                .addOnSuccessListener {
                    user.updatePassword(newPwd)
                        .addOnSuccessListener { toast("Password changed"); clearPwd() }
                        .addOnFailureListener { toast("Failed to change password") }
                }
                .addOnFailureListener { toast("Authentication failed") }
        }
    }

    private fun clearPwd() = with(binding) {
        etCurrentPassword.text.clear()
        etNewPassword.text.clear()
        etConfirmPassword.text.clear()
    }

    private fun showImagePickerDialog() {
        val opts = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image")
            .setItems(opts) { _, which ->
                if (which == 0) checkPermAndOpenCam()
                else checkPermAndOpenGallery()
            }
            .show()
    }

    private fun checkPermAndOpenCam() {
        val perm = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(requireContext(), perm) !=
            PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(perm), CAM_PERM)
        else openCamera()
    }

    private fun checkPermAndOpenGallery() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), perm) !=
            PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(perm), STO_PERM)
        else openGallery()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null)
            startActivityForResult(intent, CAMERA)
        else toast("No camera app found")
    }

    private fun openGallery() {
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != Activity.RESULT_OK) return
        when (req) {
            PICK_IMAGE -> {
                selectedImageUri = data?.data
                binding.profileImageView.setImageURI(selectedImageUri)
            }
            CAMERA -> {
                (data?.extras?.get("data") as? Bitmap)?.let { bmp ->
                    selectedImageUri = saveImageToCache(bmp)
                    binding.profileImageView.setImageBitmap(bmp)
                }
            }
        }
    }

    private fun saveImageToCache(bmp: Bitmap): Uri? = try {
        val dir  = File(requireContext().cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "profile_pic.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
    } catch (e: Exception) { e.printStackTrace(); null }

    override fun onRequestPermissionsResult(
        req: Int, perms: Array<out String>, res: IntArray
    ) {
        super.onRequestPermissionsResult(req, perms, res)
        when (req) {
            CAM_PERM -> if (res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) openCamera()
            STO_PERM -> if (res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) openGallery()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        val enable = !show
        binding.btnSave.isEnabled                 = enable
        binding.btnChangeProfilePicture.isEnabled = enable
        binding.btnBackToProfile.isEnabled        = enable
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}