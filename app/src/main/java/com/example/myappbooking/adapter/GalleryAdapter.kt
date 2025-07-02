package com.example.myappbooking.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myappbooking.utility.ImageUtils
import com.example.myappbooking.data.Images
import com.example.myappbooking.databinding.ItemGalleryImageBinding

class GalleryAdapter(
    private val images: List<Images>,
    private val onImageClick: (Images) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemGalleryImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(image: Images) {
            // Add logging to debug the URL
            Log.d("GalleryAdapter", "Loading image URL: ${image.image_path}")

            // Use loadImageWithoutURL instead of loadImage
            ImageUtils.loadImage(
                context = binding.itemFacility.context,
                imagePath = image.image_path,
                imageView = binding.itemFacility
            )

            binding.root.setOnClickListener {
                onImageClick(image)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size
}