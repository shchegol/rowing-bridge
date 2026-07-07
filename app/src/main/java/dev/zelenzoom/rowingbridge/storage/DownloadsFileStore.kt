package dev.zelenzoom.rowingbridge.storage

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore

/**
 * Saves bytes into the public MediaStore Downloads collection (API 29+,
 * scoped-storage-safe, no storage permission needed). Shared by the debug
 * text export now and the real FIT file export later.
 */
object DownloadsFileStore {

    fun save(context: Context, fileName: String, mimeType: String, bytes: ByteArray): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }
}
