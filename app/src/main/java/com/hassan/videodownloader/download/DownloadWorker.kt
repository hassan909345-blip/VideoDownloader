package com.hassan.videodownloader.download
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.work.*
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
 override suspend fun doWork(): Result = try {
  val url = inputData.getString("url") ?: return Result.failure(); val q = inputData.getString("quality") ?: "1080"
  val dir = File(applicationContext.cacheDir,"downloads").apply { mkdirs() }
  val request = YoutubeDLRequest(url).apply { addOption("-f","bestvideo[height<=${q}]+bestaudio/best[height<=${q}]"); addOption("--merge-output-format","mp4"); addOption("--no-playlist"); addOption("-o",File(dir,"%(title)s.%(ext)s").absolutePath) }
  YoutubeDL.getInstance().execute(request)
  val file = dir.listFiles()?.maxByOrNull { it.lastModified() } ?: return Result.failure()
  val values = ContentValues().apply { put(MediaStore.Video.Media.DISPLAY_NAME,file.name); put(MediaStore.Video.Media.MIME_TYPE,"video/mp4"); put(MediaStore.Video.Media.RELATIVE_PATH,"Download/VideoDownloader"); put(MediaStore.Video.Media.IS_PENDING,1) }
  val uri = applicationContext.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values) ?: return Result.failure()
  applicationContext.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
  values.clear(); values.put(MediaStore.Video.Media.IS_PENDING,0); applicationContext.contentResolver.update(uri,values,null,null); file.delete(); Result.success()
 } catch (e: Exception) { Result.failure(workDataOf("error" to (e.message ?: "Download failed"))) }
}
