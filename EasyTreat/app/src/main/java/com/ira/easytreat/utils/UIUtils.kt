package com.ira.easytreat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import java.io.File

class UIUtils {
    companion object {
        fun getBitmapFromFilePath(filePath: String): Bitmap? {
            return try {
                val options = BitmapFactory.Options()
                // Consider downscaling the image to avoid memory issues
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(filePath, options)
                // Calculate inSampleSize based on desired image dimensions
                val targetWidth = 1200 // Adjust as needed
                val targetHeight = 1200 // Adjust as needed
                var inSampleSize = 1
                if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
                    val heightRatio =
                        Math.ceil((options.outHeight.toFloat() / targetHeight).toDouble()).toInt()
                    val widthRatio =
                        Math.ceil((options.outWidth.toFloat() / targetWidth).toDouble()).toInt()
                    inSampleSize = Math.max(heightRatio, widthRatio)
                }
                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                BitmapFactory.decodeFile(filePath, options)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun cropAndRotateBitmap(bitmap: Bitmap): Bitmap? {
            // Calculate dimensions for the square crop
            val width = bitmap.width
            val height = bitmap.height
            val sideLength = Math.min(width, height) / 2
            val left = (width - sideLength) / 2
            val top = (height - sideLength) / 2

            // Create a new Bitmap for the cropped square
            val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, sideLength, sideLength)

            // Rotate the cropped Bitmap
            val matrix = Matrix()
            matrix.postRotate(90f)
            val rotatedBitmap = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.width, croppedBitmap.height, matrix, true)

            return rotatedBitmap
        }
    }
}