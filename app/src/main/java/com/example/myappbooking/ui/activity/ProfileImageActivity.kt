package com.example.myappbooking.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myappbooking.R
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.databinding.ActivityProfileImageBinding
import com.example.myappbooking.utility.ImageUtils
import com.example.myappbooking.utility.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileImageBinding
    private val authApiService = ApiClient.authService

    private var selectedImageUri: Uri? = null
    private var photoFile: File? = null
    private var currentProfileImageUrl: String? = null
    private var hasSelectedNewImage = false

    // Camera result handler
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let {
                displaySelectedImage(it)
                hasSelectedNewImage = true
            }
        }
    }

    // Gallery result handler
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            copyImageToInternalStorage(it)
            displaySelectedImage(it)
            hasSelectedNewImage = true
        }
    }

    // Permission handlers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageFromGallery()
        } else {
            Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadCurrentProfileImage()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }

        binding.btnGallery.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }

        binding.btnRemovePhoto.setOnClickListener {
            showRemovePhotoConfirmationDialog()
        }

        binding.btnSavePhoto.setOnClickListener {
            if (hasSelectedNewImage && photoFile != null) {
                uploadProfileImage()
            } else {
                Toast.makeText(this, "Please select a new photo first", Toast.LENGTH_SHORT).show()
            }
        }

        // Make profile image clickable
        binding.ivProfileImage.setOnClickListener {
            showImageSourceOptions()
        }
    }

    private fun loadCurrentProfileImage() {
        showLoading(true)

        val prefman = SharedPreferencesManager.getInstance(this@ProfileImageActivity)
        val profilePic = prefman.getUserPhoto()

        ImageUtils.loadImageWithoutURL(
            context = binding.ivProfileImage.context,
            imagePath = profilePic,
            imageView = binding.ivProfileImage
        )

        showLoading(false)
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need READ_EXTERNAL_STORAGE for picking images
            pickImageFromGallery()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    pickImageFromGallery()
                }
                else -> {
                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun takePhoto() {
        photoFile = createImageFile()
        photoFile?.let { file ->
            selectedImageUri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".fileprovider",
                file
            )
            selectedImageUri?.let { uri ->
                takePictureLauncher.launch(uri)
            }
        }
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "PROFILE_${timeStamp}_"
            val storageDir = File(filesDir, "profile_images")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (ex: IOException) {
            Log.e("ProfileImageActivity", "Error creating image file: ${ex.message}")
            null
        }
    }

    private fun copyImageToInternalStorage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            photoFile = createImageFile()
            photoFile?.let { file ->
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                selectedImageUri = Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e("ProfileImageActivity", "Error copying image: ${e.message}")
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displaySelectedImage(uri: Uri) {
        try {
            // Enable save button since we've selected a new image
            binding.btnSavePhoto.isEnabled = true
            binding.btnSavePhoto.alpha = 1.0f

            // Load image with Glide for better memory management
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(binding.ivProfileImage)

        } catch (e: Exception) {
            Log.e("ProfileImageActivity", "Error displaying image: ${e.message}")
            Toast.makeText(this, "Error displaying image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImageSourceOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> checkStoragePermissionAndPickImage()
                    // 2 is Cancel, do nothing
                }
            }
            .show()
    }

    private fun showRemovePhotoConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Profile Picture")
            .setMessage("Are you sure you want to remove your profile picture?")
            .setPositiveButton("Remove") { _, _ ->
                removeProfileImage()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadProfileImage() {
        photoFile?.let { file ->
            if (!file.exists()) {
                Toast.makeText(this, "Image file not found. Please select a new image.", Toast.LENGTH_SHORT).show()
                return
            }

            showLoading(true)

            lifecycleScope.launch {
                try {
                    val prefMan = SharedPreferencesManager.getInstance(this@ProfileImageActivity)
                    val token = prefMan.getAuthToken()

                    if (token.isNullOrEmpty()) {
                        showLoading(false)
                        showError("Authentication token not found. Please login again.")
                        return@launch
                    }

                    val authHeader = "Bearer $token"

                    // Create multipart request
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("profile_image", file.name, requestFile)

                    val response = authApiService.updateProfileImage(authHeader, imagePart)

                    showLoading(false)

                    if (response.isSuccessful) {
                        // Save new image URL if returned in response
                        val responseBody = response.body()
                        val jsonData = responseBody?.data as? Map<*, *>
                        val newImageUrl = jsonData?.get("profile_image_url") as? String

                        if (!newImageUrl.isNullOrEmpty()) {
                            currentProfileImageUrl = newImageUrl
                        }

                        // Reset state
                        hasSelectedNewImage = false
                        binding.btnSavePhoto.isEnabled = false
                        binding.btnSavePhoto.alpha = 0.5f
                        binding.btnRemovePhoto.visibility = View.VISIBLE

                        showSuccess("Profile picture updated successfully!")

                        prefMan.saveUpdatedProfile(newImageUrl)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                JSONObject(errorBody).getString("message")
                            } catch (e: Exception) {
                                "Failed to update profile picture"
                            }
                        } else {
                            "Failed to update profile picture"
                        }
                        showError(errorMessage)
                    }
                } catch (e: Exception) {
                    showLoading(false)
                    showError("Network error: ${e.message}")
                }
            }
        } ?: run {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeProfileImage() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val prefMan = SharedPreferencesManager.getInstance(this@ProfileImageActivity)
                val token = prefMan.getAuthToken()

                if (token.isNullOrEmpty()) {
                    showLoading(false)
                    showError("Authentication token not found. Please login again.")
                    return@launch
                }

                val authHeader = "Bearer $token"
                val response = authApiService.removeProfileImage(authHeader)

                showLoading(false)

                if (response.isSuccessful) {
                    // Reset image to default
                    binding.ivProfileImage.setImageResource(R.drawable.ic_profile)
                    currentProfileImageUrl = null
                    photoFile = null
                    selectedImageUri = null
                    hasSelectedNewImage = false

                    // Update button states
                    binding.btnSavePhoto.isEnabled = false
                    binding.btnSavePhoto.alpha = 0.5f
                    binding.btnRemovePhoto.visibility = View.GONE

                    showSuccess("Profile picture removed successfully!")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (!errorBody.isNullOrEmpty()) {
                        try {
                            JSONObject(errorBody).getString("message")
                        } catch (e: Exception) {
                            "Failed to remove profile picture"
                        }
                    } else {
                        "Failed to remove profile picture"
                    }
                    showError(errorMessage)
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onBackPressed() {
        if (hasSelectedNewImage) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Discard Changes")
                .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setPositiveButton("Discard") { _, _ ->
                    super.onBackPressed()
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temporary files if not used
        if (!hasSelectedNewImage) {
            photoFile?.delete()
        }
    }
}