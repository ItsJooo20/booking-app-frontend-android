package com.example.myappbooking.utility

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.ref.WeakReference

object NetworkUtils {
    private var noInternetDialog: AlertDialog? = null
    private var connectivityManager: ConnectivityManager? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun init(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Cek awal
        if (!isInternetAvailable()) showNoInternetDialog()

        // Daftarkan callback
        setupNetworkCallback()
    }

    fun attach(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    fun detach() {
        dismissDialog()
        currentActivityRef = null
    }

    fun cleanup() {
        connectivityManager?.unregisterNetworkCallback(networkCallback ?: return)
        dismissDialog()
        currentActivityRef = null
    }

    private fun setupNetworkCallback() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                currentActivityRef?.get()?.runOnUiThread {
                    dismissDialog()
                }
            }

            override fun onLost(network: Network) {
                currentActivityRef?.get()?.runOnUiThread {
                    if (!isInternetAvailable()) {
                        showNoInternetDialog()
                    }
                }
            }
        }

        connectivityManager?.registerDefaultNetworkCallback(callback)
        networkCallback = callback
    }

    private fun isInternetAvailable(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager?.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun showNoInternetDialog() {
        val activity = currentActivityRef?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (noInternetDialog?.isShowing == true) return

        noInternetDialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Tidak Ada Koneksi Internet")
            .setMessage("Periksa koneksi internet Anda, lalu coba lagi.")
            .setCancelable(false)
            .setPositiveButton("Coba Lagi") { _, _ ->
                dismissDialog()
                Handler(activity.mainLooper).postDelayed({
                    if (!isInternetAvailable()) {
                        showNoInternetDialog()
                    }
                }, 1000)
            }
            .setNegativeButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun dismissDialog() {
        noInternetDialog?.dismiss()
        noInternetDialog = null
    }
}
