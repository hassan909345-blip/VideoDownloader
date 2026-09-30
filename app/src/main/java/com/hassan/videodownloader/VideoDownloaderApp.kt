package com.hassan.videodownloader
import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL
class VideoDownloaderApp : Application() {
    override fun onCreate() { super.onCreate(); YoutubeDL.getInstance().init(this) }
}
