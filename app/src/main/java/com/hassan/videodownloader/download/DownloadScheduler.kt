package com.hassan.videodownloader.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID

object DownloadScheduler {
    fun enqueue(context: Context, url: String, quality: String): UUID {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("url" to url, "quality" to quality))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
        return request.id
    }
}
