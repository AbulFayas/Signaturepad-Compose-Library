package com.example.signaturepad

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SignaturePadUtils {
    private val TAG: String = this::class.java.name

    /**
     * Retrieves a bitmap with a transparent background by cropping the provided signature bitmap
     * to its non-transparent boundaries.
     *
     * @param signatureBitmap The original bitmap that contains the signature.
     * @param trimBlankSpace If true, the function will trim the bitmap to remove any blank
     * (transparent) space around the signature.
     * @return Bitmap with a transparent background and cropped to the non-transparent content.
     */
    suspend fun getTransparentSignatureBitmap(
        signatureBitmap: Bitmap?,
        trimBlankSpace: Boolean = true
    ): Bitmap? {
        return withContext(Dispatchers.Default) {
            signatureBitmap?.let { bitmap ->
                val width = bitmap.width
                val height = bitmap.height
                val backgroundColor = Color.TRANSPARENT

                // If we do not need to trim, return the original bitmap
                if (!trimBlankSpace) {
                    return@withContext bitmap
                }

                var xMin = Int.MAX_VALUE
                var xMax = Int.MIN_VALUE
                var yMin = Int.MAX_VALUE
                var yMax = Int.MIN_VALUE
                var foundPixel = false

                // Traverse the bitmap to find the boundaries
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        if (bitmap.getPixel(x, y) != backgroundColor) {
                            xMin = minOf(xMin, x)
                            xMax = maxOf(xMax, x)
                            yMin = minOf(yMin, y)
                            yMax = maxOf(yMax, y)
                            foundPixel = true
                        }
                    }
                }

                // If no pixel is found, return null
                if (!foundPixel) return@withContext null

                // Crop the bitmap to the found boundaries
                runCatching {
                    Bitmap.createBitmap(bitmap, xMin, yMin, xMax - xMin + 1, yMax - yMin + 1)
                }.getOrElse {
                    Log.e(TAG, "createSignatureBitmap failed", it)
                    null
                }
            }
        }
    }
}