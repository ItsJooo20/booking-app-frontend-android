package com.example.myappbooking.services

import android.util.Log
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.TokenRequest
import com.example.myappbooking.helpers.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
        saveTokenToPreferences(token)
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")

        // Gabungkan data dari notification dan data payload
        val allData = mutableMapOf<String, String>()
        allData.putAll(message.data)

        // Ambil image URL dari data atau notification
        val imageUrl = message.notification?.imageUrl?.toString()
            ?: message.data["image"]
            ?: message.data["image_url"]

        if (!imageUrl.isNullOrEmpty()) {
            allData["image_url"] = imageUrl
        }

        // Cek apakah ada notification payload
        message.notification?.let {
            val title = it.title ?: "Notification"
            val body = it.body ?: ""

            Log.d(TAG, "Notification Title: $title")
            Log.d(TAG, "Notification Body: $body")
            Log.d(TAG, "Image URL: $imageUrl")

            // Tampilkan notifikasi dengan gambar
            NotificationHelper.showNotification(
                context = this,
                title = title,
                message = body,
                data = allData
            )
        } ?: run {
            // Jika tidak ada notification payload, cek data payload
            if (message.data.isNotEmpty()) {
                Log.d(TAG, "Message data: ${message.data}")

                // Buat notifikasi dari data payload
                val title = message.data["title"] ?: "Notification"
                val body = message.data["body"] ?: message.data["message"] ?: ""

                NotificationHelper.showNotification(
                    context = this,
                    title = title,
                    message = body,
                    data = allData
                )
            }
        }
    }

    private fun saveTokenToPreferences(token: String) {
        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        prefs.edit().putString("FCM_TOKEN", token).apply()
    }

    private fun sendTokenToServer(token: String) {
        val userId = getUserId()

        if (userId == 0) {
            Log.w(TAG, "User ID not found, skipping token registration")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenRequest = TokenRequest(
                    userId = userId,
                    deviceToken = token,
                    deviceType = "android"
                )

                val response = ApiClient.authService.registerToken(tokenRequest)

                if (response.isSuccessful) {
                    Log.d(TAG, "Token registered successfully")
                } else {
                    Log.e(TAG, "Failed to register token: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering token: ${e.message}")
            }
        }
    }

    private fun getUserId(): Int {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getInt("user_id", 0)
    }
}