package com.app.campgear.ui.admin

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.app.campgear.R
import com.app.campgear.data.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** Salin maksimal 2 MB di IO; caller VM tetap hidup ketika perangkat diputar. */
suspend fun copySelectedImage(context: Context, uri: Uri): ApiResult<SelectedImage> = withContext(Dispatchers.IO) {
    var target: File? = null
    try {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.lowercase()
        val extension = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> return@withContext ApiResult.Failure(context.getString(R.string.alat_image_invalid_type))
        }
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "gambar.$extension"
        val directory = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("alat-", ".$extension", directory)
        target = file
        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8_192)
                var size = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    size += count
                    if (size > 2 * 1024 * 1024) throw ImageTooLarge()
                    output.write(buffer, 0, count)
                }
            }
        } ?: error("Gambar tidak dapat dibaca")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            file.delete()
            return@withContext ApiResult.Failure(context.getString(R.string.alat_image_invalid_type))
        }
        ApiResult.Success(SelectedImage(file, mime.orEmpty(), name))
    } catch (e: CancellationException) {
        target?.delete()
        throw e
    } catch (e: ImageTooLarge) {
        target?.delete()
        ApiResult.Failure(context.getString(R.string.alat_image_too_large))
    } catch (e: Exception) {
        target?.delete()
        ApiResult.Failure(context.getString(R.string.alat_image_read_error))
    }
}

private class ImageTooLarge : Exception()
