package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

object ImageOptimizer {

    private const val MAX_DIMENSION = 800f
    private const val QUALITY = 85

    /**
     * Reads a Uri, validates its MIME type, rescales it if it exceeds MAX_DIMENSION,
     * and compresses it to WebP format.
     * @return A ByteArray containing the compressed WebP data, or null if invalid/failed.
     */
    fun optimizeImageUri(context: Context, uri: Uri): ByteArray? {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != "image/jpeg" && mimeType != "image/png" && mimeType != "image/webp") {
            return null // Reject unsupported types
        }

        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Calculate new dimensions
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                if (width > MAX_DIMENSION) MAX_DIMENSION / width else 1f
            } else {
                if (height > MAX_DIMENSION) MAX_DIMENSION / height else 1f
            }

            val finalBitmap = if (scale < 1f) {
                val newWidth = (width * scale).roundToInt()
                val newHeight = (height * scale).roundToInt()
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            // Compress to WebP
            val outputStream = ByteArrayOutputStream()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                finalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, QUALITY, outputStream)
            } else {
                @Suppress("DEPRECATION")
                finalBitmap.compress(Bitmap.CompressFormat.WEBP, QUALITY, outputStream)
            }
            
            // Clean up
            if (finalBitmap != originalBitmap) {
                finalBitmap.recycle()
            }
            originalBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
