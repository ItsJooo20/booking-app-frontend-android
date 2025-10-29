package com.example.myappbooking.ui.activity

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.R
import com.example.myappbooking.utility.SharedPreferencesManager
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.utility.NetworkUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ReturnItemActivity : AppCompatActivity() {

    private lateinit var tvItemCode: TextView
    private lateinit var cardTakePhoto: MaterialCardView
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnGallery: MaterialButton
    private lateinit var ivReturnPhoto: ImageView
    private lateinit var cardPhotoPreview: MaterialCardView
    private lateinit var btnRemovePhoto: ImageButton
    private lateinit var spinnerCondition: Spinner
    private lateinit var tilNotes: TextInputLayout
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSubmitReturn: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var loadingOverlay: FrameLayout

    private var bookingId: Int = 0
    private var selectedImageUri: Uri? = null
    private var photoFile: File? = null
    private val authApiService = ApiClient.authService

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
        const val EXTRA_BOOKING_ID = "booking_id"
        const val EXTRA_ITEM_CODE = "item_code"
        const val EXTRA_ITEM_NAME = "item_name"
        const val EXTRA_START_DATE = "start_date"
        const val EXTRA_END_DATE = "end_date"
        const val EXTRA_ITEM_PHOTO = "item_photo"
    }

    // Updated: Use new Activity Result API for camera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let {
                displaySelectedImage()
            }
        }
    }

    // Updated: Use new Activity Result API for gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            copyImageToInternalStorage(it)
            displaySelectedImage()
        }
    }

    // Updated: Use new permission request API
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    // Updated: For Android 13+ (API 33+), we need different permission
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
        setContentView(R.layout.activity_return_item)

        NetworkUtils.init(this)

        initViews()
        setupToolbar()
        getIntentData()
        setupConditionSpinner()
        setupClickListeners()
    }

    private fun initViews() {
        tvItemCode = findViewById(R.id.tvItemCode)
        cardTakePhoto = findViewById(R.id.cardTakePhoto)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnGallery = findViewById(R.id.btnGallery)
        ivReturnPhoto = findViewById(R.id.ivReturnPhoto)
        cardPhotoPreview = findViewById(R.id.cardPhotoPreview)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
        spinnerCondition = findViewById(R.id.spinnerCondition)
        tilNotes = findViewById(R.id.tilNotes)
        etNotes = findViewById(R.id.etNotes)
        btnSubmitReturn = findViewById(R.id.btnSubmitReturn)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Return Item"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getIntentData() {
        bookingId = intent.getIntExtra(EXTRA_BOOKING_ID, 0)
        val itemCode = intent.getStringExtra(EXTRA_ITEM_CODE) ?: ""
        val itemPhoto = intent.getStringExtra(EXTRA_ITEM_PHOTO)

        tvItemCode.text = itemCode

        // Load item photo if available
        itemPhoto?.let {
            // You can use Glide, Picasso, or any image loading library here
            // Glide.with(this).load(it).into(ivItemPhoto)
        }
    }

    private fun setupConditionSpinner() {
        val conditions = arrayOf(
            "Select Condition",
            "Normal",
            "Damage",
            "Missing"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCondition.adapter = adapter
    }

    private fun setupClickListeners() {
        btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }

        btnGallery.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }

        btnRemovePhoto.setOnClickListener {
            removeSelectedPhoto()
        }

        btnSubmitReturn.setOnClickListener {
            validateAndSubmitReturn()
        }
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
        // For Android 13+ (API 33+), we don't need READ_EXTERNAL_STORAGE for gallery access
        // But if you target API 32 or below, you still need it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: No permission needed for gallery access
            pickImageFromGallery()
        } else {
            // Android 12 and below: Need READ_EXTERNAL_STORAGE permission
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
                applicationContext.packageName + ".fileprovider", // Use variable package name
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
            val imageFileName = "RETURN_${timeStamp}_"
            val storageDir = File(filesDir, "returns")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (ex: IOException) {
            Log.e("ReturnItemActivity", "Error creating image file: ${ex.message}")
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
            Log.e("ReturnItemActivity", "Error copying image: ${e.message}")
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displaySelectedImage() {
        selectedImageUri?.let { uri ->
            try {
                val bitmap = if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path)
                } else {
                    val inputStream = contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)?.also {
                        inputStream?.close()
                    }
                }

                bitmap?.let {
                    ivReturnPhoto.setImageBitmap(it)
                    cardTakePhoto.visibility = View.GONE
                    cardPhotoPreview.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("ReturnItemActivity", "Error loading image: ${e.message}")
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeSelectedPhoto() {
        selectedImageUri = null
        photoFile?.delete() // Clean up the file
        photoFile = null
        ivReturnPhoto.setImageDrawable(null)
        cardTakePhoto.visibility = View.VISIBLE
        cardPhotoPreview.visibility = View.GONE
    }

    private fun validateAndSubmitReturn() {
        // Validate photo
        if (selectedImageUri == null || photoFile == null) {
            Toast.makeText(this, "Please take a photo of the returned item", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate condition
        if (spinnerCondition.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select the item condition", Toast.LENGTH_SHORT).show()
            return
        }

        val condition = spinnerCondition.selectedItem.toString()
        val notes = etNotes.text.toString().trim()

        showConfirmationDialog(condition, notes)
    }

    private fun showConfirmationDialog(condition: String, notes: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Return")
            .setMessage("Are you sure you want to submit this return?\n\nCondition: $condition\nNotes: ${if (notes.isEmpty()) "None" else notes}")
            .setPositiveButton("Confirm") { _, _ ->
                submitReturn(condition, notes)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun submitReturn(condition: String, notes: String) {
        photoFile?.let { file ->
            if (!file.exists()) {
                Toast.makeText(this, "Photo file not found. Please take a new photo.", Toast.LENGTH_SHORT).show()
                return
            }

            showLoading(true)

            lifecycleScope.launch {
                try {
                    val prefMan = SharedPreferencesManager.getInstance(this@ReturnItemActivity)
                    val token = prefMan.getAuthToken()

                    if (token.isNullOrEmpty()) {
                        showLoading(false)
                        Toast.makeText(this@ReturnItemActivity, "Authentication token not found. Please login again.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val authHeader = "Bearer $token"

                    // Create multipart request
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val photoPart = MultipartBody.Part.createFormData("return_photo", file.name, requestFile)
                    val conditionPart = condition.toRequestBody("text/plain".toMediaTypeOrNull())
                    val notesPart = notes.toRequestBody("text/plain".toMediaTypeOrNull())

                    val response = authApiService.submitReturn(
                        authHeader,
                        bookingId,
                        photoPart,
                        conditionPart,
                        notesPart
                    )

                    showLoading(false)

                    if (response.isSuccessful) {
                        showSuccessDialog()
                    } else {
                        val errorMessage = when (response.code()) {
                            409 -> "Return already submitted for this booking"
                            400 -> "Cannot return before the booking start time"
                            401 -> "Authentication failed. Please login again."
                            422 -> "Invalid data provided. Please check your inputs."
                            else -> "Failed to submit return. Please try again."
                        }
                        Toast.makeText(this@ReturnItemActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    showLoading(false)
                    Log.e("ReturnItemActivity", "Network error: ${e.message}", e)
                    Toast.makeText(this@ReturnItemActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper method to show/hide the loading overlay
    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE

        // Disable UI elements when loading
        btnTakePhoto.isEnabled = !show
        btnGallery.isEnabled = !show
        btnRemovePhoto.isEnabled = !show
        spinnerCondition.isEnabled = !show
        etNotes.isEnabled = !show
        btnSubmitReturn.isEnabled = !show
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Return Submitted")
            .setMessage("Your return has been submitted successfully. Staff will review and confirm the return shortly.")
            .setPositiveButton("OK") { _, _ ->
                setResult(Activity.RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun formatDateTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("d-MMMM-yyyy, HH:mm", Locale.ENGLISH)
            val date = inputFormat.parse(dateTimeString)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {
            Log.e("ReturnItemActivity", "Error formatting date: ${e.message}")
            dateTimeString
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()

        NetworkUtils.cleanup()
        // Clean up temporary files
        photoFile?.let { file ->
            if (file.exists() && selectedImageUri == null) {
                file.delete()
            }
        }
    }
}