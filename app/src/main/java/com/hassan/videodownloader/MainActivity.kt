package com.hassan.videodownloader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hassan.videodownloader.download.DownloadScheduler
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent {
  MaterialTheme { var url by remember { mutableStateOf("") }; var quality by remember { mutableStateOf("1080") }; var menu by remember { mutableStateOf(false) }
   Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("Video Downloader", style = MaterialTheme.typography.headlineMedium)
    Text("نزّل فقط المحتوى الذي تملك حق تنزيله.")
    OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("رابط الفيديو") })
    Box { OutlinedButton(onClick = { menu = true }) { Text("الجودة: ${quality}p") }
     DropdownMenu(menu, { menu = false }) { listOf("2160","1440","1080","720","480","360").forEach { q -> DropdownMenuItem({ Text("${q}p") }, { quality=q; menu=false }) } }
    }
    Button(onClick = { DownloadScheduler.enqueue(this@MainActivity,url,quality) }, enabled = url.startsWith("http"), modifier = Modifier.fillMaxWidth()) { Text("تحميل") }
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text("يستمر التنزيل في الخلفية ويُحفظ داخل Downloads/VideoDownloader")
   }
  }
 } }
}
