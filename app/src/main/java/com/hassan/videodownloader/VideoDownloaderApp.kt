package com.hassan.videodownloader
import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.ffmpeg.FFmpeg
class VideoDownloaderApp : Application() {
    override fun onCreate() { super.onCreate(); YoutubeDL.getInstance().init(this); FFmpeg.getInstance().init(this) }
}
