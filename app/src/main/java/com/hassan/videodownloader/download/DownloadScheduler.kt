package com.hassan.videodownloader.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import java.util.UUID

object DownloadScheduler {
    private const val PREFS = "downloads"
    private const val ACTIVE_ID = "active_id"

    fun enqueue(context: Context, url: String, quality: String): UUID {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("url" to url, "quality" to quality))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ACTIVE_ID, request.id.toString()).apply()
        WorkManager.getInstance(context).enqueue(request)
        return request.id
    }

    fun activeId(context: Context): UUID? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACTIVE_ID, null)?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

    fun stop(context: Context) {
        val id = activeId(context) ?: return
        runCatching { YoutubeDL.getInstance().destroyProcessById(id.toString()) }
        WorkManager.getInstance(context).cancelWorkById(id)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(ACTIVE_ID).apply()
    }
}
