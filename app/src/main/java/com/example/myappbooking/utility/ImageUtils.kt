package com.example.myappbooking.utility

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myappbooking.R

object ImageUtils {
    private const val BASE_URL = "https://testapijo.my.id/storage/"

    fun loadImage(
        context: Context,
        imagePath: String?,
        imageView: ImageView,
        placeholderResId: Int = R.drawable.pic_check
    ) {
        if (!imagePath.isNullOrEmpty()) {
            val fullImageUrl = BASE_URL + imagePath

            Glide.with(context)
                .load(fullImageUrl)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .centerCrop()
                .into(imageView)
        } else {
            imageView.setImageResource(placeholderResId)
        }
    }

    fun loadImageWithoutURL(
        context: Context,
        imagePath: String?,
        imageView: ImageView,
        placeholderResId: Int = R.drawable.pic_check
    ) {
        if (!imagePath.isNullOrEmpty()) {
            Log.d("ImageUtils", "Loading image from: $imagePath")

            Glide.with(context)
                .load(imagePath)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .centerCrop()
                .into(imageView)
        } else {
            imageView.setImageResource(placeholderResId)
        }
    }

    fun loadImageWithOptions(
        context: Context,
        imagePath: String?,
        imageView: ImageView,
        options: RequestOptions,
        placeholderResId: Int = R.drawable.pic_check
    ) {
        if (!imagePath.isNullOrEmpty()) {
            val fullImageUrl = BASE_URL + imagePath

            Glide.with(context)
                .load(fullImageUrl)
                .apply(options)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .into(imageView)
        } else {
            imageView.setImageResource(placeholderResId)
        }
    }
}