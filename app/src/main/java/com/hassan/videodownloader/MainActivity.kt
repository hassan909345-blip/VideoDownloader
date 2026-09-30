package com.hassan.videodownloader

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var url by remember { mutableStateOf("") }
            var quality by remember { mutableStateOf("1080") }
            var menu by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf("") }
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Video Downloader", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
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
                            val id = DownloadScheduler.enqueue(this@MainActivity, url.trim(), quality)
                            status = "في انتظار بدء التنزيل..."
                            WorkManager.getInstance(this@MainActivity)
                                .getWorkInfoByIdLiveData(id)
                                .observe(this@MainActivity) { info ->
                                    status = when (info?.state) {
                                        WorkInfo.State.ENQUEUED -> "في انتظار الاتصال..."
                                        WorkInfo.State.RUNNING -> "جاري التنزيل..."
                                        WorkInfo.State.SUCCEEDED -> "تم الحفظ في Downloads/VideoDownloader"
                                        WorkInfo.State.FAILED -> info.outputData.getString("error") ?: "فشل التنزيل"
                                        WorkInfo.State.CANCELLED -> "تم إلغاء التنزيل"
                                        else -> status
                                    }
                                }
                        },
                        enabled = url.trim().startsWith("https://") || url.trim().startsWith("http://"),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تحميل") }
                    if (status.isNotEmpty()) Text(status)
                    Text("نزّل فقط المحتوى الذي تملك حق تنزيله.")
                }
            }
        }
    }
}
