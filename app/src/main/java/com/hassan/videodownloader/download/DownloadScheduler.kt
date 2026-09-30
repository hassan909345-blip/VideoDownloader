package com.hassan.videodownloader.download
import android.content.Context
import androidx.work.*
object DownloadScheduler {
 fun enqueue(context: Context, url: String, quality: String) {
  val req = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(workDataOf("url" to url, "quality" to quality)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
  WorkManager.getInstance(context).enqueue(req)
 }
}
