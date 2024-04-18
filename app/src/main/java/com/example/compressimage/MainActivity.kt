package com.example.compressimage

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    private val PICK_IMAGE = 1
    private lateinit var originalImageView: ImageView
    private lateinit var compressedImageView: ImageView
    private lateinit var imageSizeTextView: TextView
    private lateinit var compressedImageSizeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        originalImageView = findViewById(R.id.imageView)
        compressedImageView = findViewById(R.id.compressedImageView)
        imageSizeTextView = findViewById(R.id.imageSizeTextView)
        compressedImageSizeTextView = findViewById(R.id.compressedImageSizeTextView)
        val selectImageButton: Button = findViewById(R.id.selectImageButton)

        selectImageButton.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            originalImageView.setImageURI(selectedImageUri)
            val originalSize = getImageFileSize(selectedImageUri!!)
            imageSizeTextView.text = "Original Image Size: $originalSize"

            val compressedImageByteArray = compressImage(selectedImageUri)
            val compressedSizeInKB = compressedImageByteArray.size / 1024 // Convert bytes to KB
            compressedImageSizeTextView.text = "Compressed Image Size: $compressedSizeInKB KB"
            val compressedBitmap = BitmapFactory.decodeByteArray(
                compressedImageByteArray,
                0,
                compressedImageByteArray.size
            )
            compressedImageView.setImageBitmap(compressedBitmap)
        }
    }

    private fun getImageFileSize(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val fileSize = cursor.getLong(sizeIndex)
            val sizeInKB = fileSize / 1024 // Convert bytes to KB
            return "$sizeInKB KB"
        }
        return "Unknown"
    }

    private fun compressImage(uri: Uri): ByteArray {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        // Compress the bitmap with quality 50 and write it to the output stream
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 25, outputStream)
        return outputStream.toByteArray()
    }

}
