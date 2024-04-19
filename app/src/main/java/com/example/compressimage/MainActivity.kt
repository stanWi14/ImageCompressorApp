package com.example.compressimage

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    private val PICK_IMAGE = 1
    private lateinit var originalImageView: ImageView
    private lateinit var compressedImageView: ImageView
    private lateinit var imageSizeTextView: TextView
    private lateinit var compressedImageSizeTextView: TextView
    private lateinit var qualityEditText: EditText
    private lateinit var qualitySeekBar: SeekBar
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        originalImageView = findViewById(R.id.imageView)
        compressedImageView = findViewById(R.id.compressedImageView)
        imageSizeTextView = findViewById(R.id.imageSizeTextView)
        compressedImageSizeTextView = findViewById(R.id.compressedImageSizeTextView)
        qualityEditText = findViewById(R.id.etQuality)
        qualitySeekBar = findViewById(R.id.qualitySeekBar)
        val saveImageButton: Button = findViewById(R.id.saveImageButton)
        val selectImageButton: Button = findViewById(R.id.selectImageButton)

        selectImageButton.setOnClickListener {
            openGallery()
        }
        saveImageButton.setOnClickListener {
            saveCompressedImageToStorage()
        }

        //TextWatcher for Seekbar
        qualityEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Update compression quality when text changes
                saveImageButton.visibility = View.VISIBLE
                updateCompression()
            }
        })

        // Add a seek bar change listener
        qualitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update EditText when seek bar changes
                qualityEditText.setText(progress.toString())
                // Update compression quality
                updateCompression()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            originalImageView.setImageURI(selectedImageUri)
            val originalSize = getImageFileSize(selectedImageUri!!)
            imageSizeTextView.text = "Original Image Size: $originalSize"

            updateCompression()
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


    private val compressionHandler = Handler(Looper.getMainLooper())
    private var compressionRunnable: Runnable? = null
    private val COMPRESSION_DELAY_MS = 500 // Adjust the delay as needed

    private fun updateCompression() {
        val quality = qualityEditText.text.toString().toIntOrNull() ?: 0
        selectedImageUri?.let { uri ->
            // Cancel the previous compression task if it exists
            compressionRunnable?.let {
                compressionHandler.removeCallbacks(it)
            }

            // Post a delayed compression task
            compressionRunnable = Runnable {
                val compressedImageByteArray = compressImage(uri, quality)
                val compressedSizeInKB = compressedImageByteArray.size / 1024 // Convert bytes to KB
                compressedImageSizeTextView.text = "Compressed Image Size: $compressedSizeInKB KB"
                val compressedBitmap = BitmapFactory.decodeByteArray(
                    compressedImageByteArray,
                    0,
                    compressedImageByteArray.size
                )
                compressedImageView.setImageBitmap(compressedBitmap)
            }

            compressionHandler.postDelayed(compressionRunnable!!, COMPRESSION_DELAY_MS.toLong())
        }
    }

    private fun compressImage(uri: Uri, quality: Int): ByteArray {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    private fun saveCompressedImageToStorage() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
            )
            return
        }

        val bitmap = (compressedImageView.drawable as BitmapDrawable).bitmap
        val fileName = "compressed_image.jpg"
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "/CompressedImages"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)

        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            Toast.makeText(this, "Image saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    saveCompressedImageToStorage()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
                return
            }

            else -> {
                // Handle other permissions if needed
            }
        }
    }

}