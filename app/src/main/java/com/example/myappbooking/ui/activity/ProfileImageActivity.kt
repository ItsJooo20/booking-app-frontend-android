package com.example.myappbooking.ui.activity

import android.app.Activity
import android.content.Intent
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
import com.bumptech.glide.Glide
import com.example.myappbooking.R
import com.example.myappbooking.databinding.ActivityProfileImageBinding
import com.example.myappbooking.utility.ImageUtils
import com.example.myappbooking.utility.NetworkUtils
import com.example.myappbooking.utility.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileImageBinding

    private var selectedImageUri: Uri? = null
    private var photoFile: File? = null
    private var hasSelectedNewImage = false
    private var isImageSavedLocally = false

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

        NetworkUtils.init(this)

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
                saveProfileImageLocally()
            } else {
                Toast.makeText(this, "Please select a new photo first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ivProfileImage.setOnClickListener {
            showImageSourceOptions()
        }
    }

    private fun loadCurrentProfileImage() {
        showLoading(true)

        val prefman = SharedPreferencesManager.getInstance(this@ProfileImageActivity)
        val profilePic = prefman.getUserPhoto() // expected to be local file path

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
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            else -> {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickImageFromGallery()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    pickImageFromGallery()
                }
                else -> {
                    storagePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
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
            binding.btnSavePhoto.isEnabled = true
            binding.btnSavePhoto.alpha = 1.0f

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

    // Save the selected file path into SharedPreferences and finish with RESULT_OK
    private fun saveProfileImageLocally() {
        photoFile?.let { file ->
            if (!file.exists()) {
                Toast.makeText(this, "Image file not found. Please select a new image.", Toast.LENGTH_SHORT).show()
                return
            }

            showLoading(true)

            try {
                val prefman = SharedPreferencesManager.getInstance(this)
                // Save absolute path so ImageUtils.loadImageWithoutURL can use it
                prefman.saveUpdatedProfile(file.absolutePath)

                isImageSavedLocally = true
                hasSelectedNewImage = false

                // Return result back to fragment/activity
                val data = Intent().apply {
                    putExtra("profile_image_path", file.absolutePath)
                }
                setResult(Activity.RESULT_OK, data)

                showLoading(false)
                Toast.makeText(this, "Foto profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                showLoading(false)
                Log.e("ProfileImageActivity", "Error saving profile image locally: ${e.message}")
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeProfileImage() {
        showLoading(true)

        try {
            val prefman = SharedPreferencesManager.getInstance(this)
            val current = prefman.getUserPhoto()

            // Clear from SharedPreferences
            prefman.saveUpdatedProfile("") // or null depending on your manager

            // Try delete the file if it exists and is inside app files
            try {
                if (!current.isNullOrEmpty()) {
                    val f = File(current)
                    if (f.exists()) {
                        f.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileImageActivity", "Error deleting old profile file: ${e.message}")
            }

            // Show placeholder
            binding.ivProfileImage.setImageResource(R.drawable.pic_check)

            // Return result to caller
            val data = Intent().apply {
                putExtra("profile_image_path", "")
            }
            setResult(Activity.RESULT_OK, data)

            isImageSavedLocally = false
            hasSelectedNewImage = false

            showLoading(false)
            Toast.makeText(this, "Foto profil berhasil dihapus!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            showLoading(false)
            Log.e("ProfileImageActivity", "Error removing profile image: ${e.message}")
            Toast.makeText(this, "Failed to remove image", Toast.LENGTH_SHORT).show()
        }
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
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkUtils.cleanup()
        // Delete temporary file only if user did not save it locally
        if (!isImageSavedLocally && photoFile != null && !photoFile!!.absolutePath.isNullOrEmpty()) {
            try {
                // Only delete if it exists and is in our app files dir
                val file = photoFile!!
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("ProfileImageActivity", "Failed to delete temp photoFile: ${e.message}")
            }
        }
    }
}