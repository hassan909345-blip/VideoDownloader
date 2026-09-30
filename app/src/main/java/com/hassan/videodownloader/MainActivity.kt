package com.hassan.videodownloader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hassan.videodownloader.download.DownloadScheduler
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf("")
    private var status by mutableStateOf("")
    private var activeId by mutableStateOf<UUID?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readSharedIntent(intent)
        DownloadScheduler.activeId(this)?.let { watch(it) }
        setContent {
            var quality by remember { mutableStateOf("1080") }
            var menu by remember { mutableStateOf(false) }
            var overlayReady by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Video Downloader", style = MaterialTheme.typography.headlineMedium)
                    Text("الصق رابط فيديو أو شاركه من التطبيق الآخر هنا")
                    OutlinedTextField(
                        value = sharedUrl,
                        onValueChange = { sharedUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رابط الفيديو") }
                    )
                    Box {
                        OutlinedButton(onClick = { menu = true }) { Text("الجودة: ${quality}p") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf("2160", "1440", "1080", "720", "480", "360").forEach { q ->
                                DropdownMenuItem(text = { Text("${q}p") }, onClick = {
                                    quality = q
                                    menu = false
                                })
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val link = extractUrl(sharedUrl) ?: return@Button
                            val id = DownloadScheduler.enqueue(this@MainActivity, link, quality)
                            status = "في انتظار بدء التنزيل..."
                            watch(id)
                        },
                        enabled = extractUrl(sharedUrl) != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تحميل") }
                    if (activeId != null) {
                        OutlinedButton(onClick = {
                            DownloadScheduler.stop(this@MainActivity)
                            status = "تم إيقاف التنزيل"
                            activeId = null
                        }, modifier = Modifier.fillMaxWidth()) { Text("إيقاف التنزيل") }
                    }
                    if (status.isNotEmpty()) Text(status)
                    OutlinedButton(onClick = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            ))
                            status = "اسمح بالظهور فوق التطبيقات ثم اضغط تفعيل مرة أخرى"
                        } else {
                            startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                            overlayReady = true
                            status = "الأيقونة العائمة جاهزة؛ انسخ رابط الفيديو واضغط ↓"
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (overlayReady) "تشغيل الأيقونة العائمة" else "السماح بالأيقونة العائمة")
                    }
                    OutlinedButton(onClick = {
                        stopService(Intent(this@MainActivity, OverlayService::class.java))
                        status = "تم إخفاء الأيقونة العائمة"
                    }, modifier = Modifier.fillMaxWidth()) { Text("إخفاء الأيقونة العائمة") }
                    Text("الأيقونة تظهر فوق التطبيقات بعد تفعيلها. انسخ الرابط واضغط ↓ للتنزيل، أو ■ للإيقاف.")
                    Text("بعض المواقع تتطلب تسجيل دخول أو تمنع استخراج الفيديو المحمي.")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readSharedIntent(intent)
    }

    private fun readSharedIntent(intent: Intent?) {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        extractUrl(text.orEmpty())?.let { sharedUrl = it }
    }

    private fun extractUrl(value: String): String? =
        Regex("https?://[^\\s]+").find(value)?.value?.trimEnd('.', ',', ')')

    private fun watch(id: UUID) {
        activeId = id
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(id).observe(this) { info ->
            status = when (info?.state) {
                WorkInfo.State.ENQUEUED -> "في انتظار الاتصال..."
                WorkInfo.State.RUNNING -> "جاري التنزيل ${info.progress.getInt("progress", 0)}%"
                WorkInfo.State.SUCCEEDED -> "تم حفظ الفيديو في Downloads/VideoDownloader"
                WorkInfo.State.FAILED -> info.outputData.getString("error") ?: "فشل التنزيل"
                WorkInfo.State.CANCELLED -> "تم إيقاف التنزيل"
                else -> status
            }
            if (info?.state?.isFinished == true && activeId == id) activeId = null
        }
    }
}
