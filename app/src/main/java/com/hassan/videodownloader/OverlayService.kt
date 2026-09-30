package com.hassan.videodownloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.getSystemService
import com.hassan.videodownloader.download.DownloadScheduler

class OverlayService : Service() {
    private var bubble: LinearLayout? = null
    private lateinit var windowManager: WindowManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("overlay", "زر التنزيل العائم", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, "overlay")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Video Downloader")
            .setContentText("زر التنزيل العائم يعمل")
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(100, notification)
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun showBubble() {
        val background = GradientDrawable().apply {
            setColor(Color.rgb(103, 80, 164))
            cornerRadius = 64f
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            this.background = background
        }
        val download = Button(this).apply {
            text = "↓"
            textSize = 22f
            setTextColor(Color.WHITE)
            setOnClickListener { downloadCopiedLink() }
        }
        val stop = Button(this).apply {
            text = "■"
            textSize = 19f
            setTextColor(Color.WHITE)
            setOnClickListener {
                DownloadScheduler.stop(this@OverlayService)
                Toast.makeText(this@OverlayService, "تم إيقاف التنزيل", Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(download)
        row.addView(stop)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        windowManager.addView(row, params)
        bubble = row
    }

    private fun downloadCopiedLink() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val copied = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        val link = Regex("https?://[^\\s]+").find(copied)?.value?.trimEnd('.', ',', ')')
        if (link == null) {
            Toast.makeText(this, "انسخ رابط الفيديو أو شاركه مع التطبيق أولًا", Toast.LENGTH_LONG).show()
            return
        }
        DownloadScheduler.enqueue(this, link, "1080")
        Toast.makeText(this, "بدأ تنزيل الرابط المنسوخ", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        bubble?.let { windowManager.removeView(it) }
        bubble = null
        super.onDestroy()
    }
}
