package com.hassan.videodownloader.download

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure(workDataOf("error" to "الرابط غير موجود"))
        val quality = inputData.getString("quality") ?: "1080"
        val dir = File(applicationContext.cacheDir, "download-$id").apply { mkdirs() }
        return try {
            YoutubeDL.getInstance().init(applicationContext)
            val request = YoutubeDLRequest(url).apply {
                addOption("-f", "best[height<=$quality][ext=mp4]/best[height<=$quality]/best")
                addOption("--no-playlist")
                addOption("-o", File(dir, "%(title).180B.%(ext)s").absolutePath)
            }
            YoutubeDL.getInstance().execute(request, id.toString()) { progress: Float, _: Long, _: String ->
                setProgressAsync(workDataOf("progress" to progress.toInt()))
                Unit
            }
            if (isStopped) return Result.failure(workDataOf("error" to "تم إيقاف التنزيل"))
            val file = dir.listFiles()?.firstOrNull { it.isFile && !it.name.endsWith(".part") }
                ?: return Result.failure(workDataOf("error" to "لم يتم العثور على ملف الفيديو"))
            val mime = if (file.extension.equals("mp4", true)) "video/mp4" else "video/${file.extension.lowercase()}"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/VideoDownloader")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = applicationContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return Result.failure(workDataOf("error" to "تعذر حفظ الفيديو"))
            try {
                applicationContext.contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { it.copyTo(output) }
                } ?: throw IllegalStateException("تعذر كتابة الفيديو")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                applicationContext.contentResolver.update(uri, values, null, null)
                Result.success()
            } catch (e: Exception) {
                applicationContext.contentResolver.delete(uri, null, null)
                throw e
            }
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "فشل التنزيل")))
        } finally {
            dir.deleteRecursively()
        }
    }
}
