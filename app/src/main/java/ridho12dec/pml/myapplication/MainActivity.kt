package ridho12dec.pml.myapplication

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ridho12dec.pml.myapplication.ui.theme.RidhoCameraxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RidhoCameraxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraUIScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CameraUIScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Tampilan Kamera (PreviewView dari CameraX)
        // Menggunakan AndroidView karena PreviewView adalah View XML (Legacy)
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Tombol-tombol di bagian bawah [cite: 230-245]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Posisi di bawah tengah
                .padding(bottom = 50.dp),      // Jarak dari bawah 50dp sesuai XML
            horizontalArrangement = Arrangement.SpaceEvenly // Jarak antar tombol merata
        ) {
            // Tombol Take Photo
            Button(onClick = { /* Belum ada fungsi */ }) {
                Text(text = "Take Photo")
            }

            // Tombol Record Video
            Button(onClick = { /* Belum ada fungsi */ }) {
                Text(text = "Record Video")
            }
        }
    }
}