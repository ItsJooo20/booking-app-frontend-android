package com.example.myappbooking.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myappbooking.R
import com.example.myappbooking.ui.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {
    private const val CHANNEL_ID = "default_channel"
    private const val CHANNEL_NAME = "Default Notifications"
    private const val TAG = "NotificationHelper"

    /**
     * Buat notification channel (diperlukan untuk Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Channel untuk notifikasi aplikasi"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Tampilkan notifikasi dengan gambar (jika ada)
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent ketika notifikasi diklik
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Tambahkan data dari notifikasi ke intent
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notifikasi dasar
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Ganti dengan icon Anda
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Cek apakah ada image URL di data
        val imageUrl = data["image"] ?: data["image_url"]

        if (!imageUrl.isNullOrEmpty()) {
            // Download dan tampilkan gambar
            try {
                val bitmap = downloadImage(imageUrl)
                if (bitmap != null) {
                    notificationBuilder.setLargeIcon(bitmap)
                    notificationBuilder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null as Bitmap?) // Hide large icon when expanded
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image for notification: ${e.message}")
            }
        } else {
            // Jika tidak ada gambar, gunakan BigTextStyle
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
        }

        // Generate unique notification ID
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Download gambar dari URL
     */
    private fun downloadImage(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: ${e.message}")
            null
        }
    }

    /**
     * Versi async untuk download gambar (lebih baik untuk performa)
     */
    suspend fun downloadImageAsync(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: ${e.message}")
            null
        }
    }
}