package com.example.tusome.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Keeps assessment diagrams in app-private storage so a paper remains usable offline.
 * Only HTTPS image responses under the configured size limit are cached.
 */
object AssessmentMediaCache {
    private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L

    fun getCachedImage(context: Context, remoteUrl: String): Bitmap? {
        val file = cachedFile(context, remoteUrl) ?: return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun cacheImage(context: Context, remoteUrl: String): Bitmap? {
        if (!remoteUrl.startsWith("https://")) return null
        cachedFile(context, remoteUrl)?.let { existing ->
            BitmapFactory.decodeFile(existing.absolutePath)?.let { return it }
        }

        val connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "image/*")
        }
        return try {
            val status = connection.responseCode
            val contentType = connection.contentType.orEmpty().substringBefore(";")
            val length = connection.contentLengthLong
            if (status !in 200..299 || !contentType.startsWith("image/") ||
                (length > 0 && length > MAX_IMAGE_BYTES)) return null

            val directory = File(context.filesDir, "assessment_media").apply { mkdirs() }
            val target = File(directory, fileName(remoteUrl))
            val temporary = File(directory, target.name + ".part")
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_IMAGE_BYTES) {
                            temporary.delete()
                            return null
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            BitmapFactory.decodeFile(target.absolutePath)
        } finally {
            connection.disconnect()
        }
    }

    private fun cachedFile(context: Context, remoteUrl: String): File? {
        if (!remoteUrl.startsWith("https://")) return null
        val file = File(File(context.filesDir, "assessment_media"), fileName(remoteUrl))
        return file.takeIf { it.isFile && it.length() in 1..MAX_IMAGE_BYTES }
    }

    private fun fileName(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) } + ".img"
    }
}
