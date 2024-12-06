package com.ipsa.ipscache.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.min

class ImageCache private constructor(context: Context) {

    // In-memory cache using LruCache
    private val memoryCache: LruCache<String, Bitmap>

    // Disk cache directory
    private val diskCacheDir: File = File(context.cacheDir, "image_cache")

    init {
        // Set the max size of the cache to 1/8th of the available memory
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }

        // Create the disk cache directory if it doesn't exist
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    // Load and resize drawable images
    suspend fun loadDrawableImage(context: Context, resId: Int): Bitmap? {
        val key = "drawable_$resId" // Unique key for drawable resources

        // Check memory cache
        memoryCache.get(key)?.let {
            return it
        }

        return withContext(Dispatchers.IO) {
            try {
                // Decode the drawable resource with resizing
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, resId, options)

                options.inSampleSize = calculateInSampleSize(options, 1024, 1024) // Target max size
                options.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
                bitmap?.also {
                    memoryCache.put(key, it)

                    // Optionally, save to disk cache
                    val diskFile = File(diskCacheDir, key)
                    saveBitmapToDisk(diskFile, it)
                }
            } catch (e: Exception) {
                Log.e("ImageCache", "Failed to load drawable: ${e.message}")
                null
            }
        }
    }

    // Load image from cache or network
    suspend fun loadImage(url: String): Bitmap? {
        val key = hashKeyForDisk(url)

        // Check memory cache
        memoryCache.get(key)?.let {
            Log.d("ImageCache", "Loaded image from memory cache")
            return it
        }

        return withContext(Dispatchers.IO) {
            try {
                val diskFile = File(diskCacheDir, key)
                if (diskFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                    if (bitmap != null) {
                        Log.d("ImageCache", "Loaded image from disk cache")
                        memoryCache.put(key, bitmap)
                        return@withContext bitmap
                    }
                }

                // Download and cache the image
                val bitmap = downloadImage(url)
                bitmap?.also {
                    memoryCache.put(key, it)
                    saveBitmapToDisk(diskFile, it)
                }
            } catch (e: Exception) {
                Log.e("ImageCache", "Failed to load image: ${e.message}")
                null
            }
        }
    }

    // Download image from URL
    private fun downloadImage(urlString: String): Bitmap? {
        return try {
            Log.d("ImageCache", "Downloading image from URL: $urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)

            options.inSampleSize = calculateInSampleSize(options, 1024, 1024) // Target size
            options.inJustDecodeBounds = false

            val input2: InputStream = url.openConnection().getInputStream()
            val bitmap = BitmapFactory.decodeStream(input2, null, options)
            Log.d("ImageCache", "Image downloaded and resized successfully")
            bitmap
        } catch (e: Exception) {
            Log.e("ImageCache", "Error downloading image: ${e.message}")
            null
        }
    }

    // Save bitmap to disk cache
    private fun saveBitmapToDisk(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Calculate the sample size for resizing
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (reqWidth == 0 || reqHeight == 0) {
            // Avoid division by zero; fall back to default scaling
            return inSampleSize
        }
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = min(heightRatio.toDouble(), widthRatio.toDouble()).toInt()
        }
        return if (inSampleSize > 0) inSampleSize else 1
    }


    // Hash key for disk storage
    private fun hashKeyForDisk(key: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(key.toByteArray())
            bytesToHexString(digest.digest())
        } catch (e: Exception) {
            key.hashCode().toString()
        }
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val builder = StringBuilder()
        for (byte in bytes) {
            val hex = Integer.toHexString(0xFF and byte.toInt())
            if (hex.length == 1) {
                builder.append('0')
            }
            builder.append(hex)
        }
        return builder.toString()
    }

    companion object {
        @Volatile
        private var INSTANCE: ImageCache? = null

        fun getInstance(context: Context): ImageCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageCache(context).also { INSTANCE = it }
            }
        }
    }
}
