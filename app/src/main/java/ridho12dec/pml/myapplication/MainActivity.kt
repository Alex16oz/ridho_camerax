package ridho12dec.pml.myapplication

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import ridho12dec.pml.myapplication.ui.theme.RidhoCameraxTheme
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    // Variabel logika CameraX [cite: 234-236]
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    // Variabel UI State untuk tombol rekam
    private val isRecording = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi Executor [cite: 246]
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Cek permission saat aplikasi dimulai [cite: 240]
        if (allPermissionsGranted()) {
            // Kamera akan dimulai nanti saat PreviewView tersedia di UI
        } else {
            requestPermissions()
        }

        setContent {
            RidhoCameraxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraUIScreen(
                        modifier = Modifier.padding(innerPadding),
                        onPreviewViewCreated = { view ->
                            startCamera(view) // Mulai kamera saat View siap
                        },
                        onTakePhoto = { takePhoto() },
                        onRecordVideo = { captureVideo() },
                        isRecording = isRecording.value
                    )
                }
            }
        }
    }

    // Fungsi Memulai Kamera
    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Mendapatkan provider kamera [cite: 346]
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Menyiapkan Preview [cite: 348]
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Inisiasi ImageCapture (Foto) [cite: 354]
            imageCapture = ImageCapture.Builder().build()

            // Inisiasi VideoCapture (Video) [cite: 356]
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Memilih kamera belakang [cite: 361]
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use case sebelumnya [cite: 362]
                cameraProvider.unbindAll()

                // Bind use cases ke lifecycle [cite: 363]
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Penggabungan use case gagal", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Fungsi Ambil Foto
    private fun takePhoto() {
        // Mendapatkan referensi imageCapture yang stabil
        val imageCapture = imageCapture ?: return

        // Menyiapkan metadata [cite: 249]
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kamera_X")
            }
        }

        // Output Options [cite: 259]
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Mengambil gambar [cite: 266]
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Berhasil mengambil foto: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    // Fungsi Rekam Video
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        // Hentikan rekaman jika sedang merekam
        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            isRecording.value = false // Update UI
            return
        }

        // Mulai rekaman baru
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Kamera_X")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // Menyiapkan recording [cite: 306]
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording.value = true // Update UI button text
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Perekaman video sukses: ${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Perekaman video gagal: ${recordEvent.error}")
                        }
                        isRecording.value = false // Reset UI
                    }
                }
            }
    }

    // Manajemen Permission
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permintaan izin ditolak", Toast.LENGTH_SHORT).show()
            } else {
                // startCamera() akan dipanggil via UI
            }
        }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Kamera X"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}

// Komponen UI Compose yang Dimodifikasi
@Composable
fun CameraUIScreen(
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit, // Callback untuk mengirim View ke Activity
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    isRecording: Boolean
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Tampilan Kamera
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Kirim view ini kembali ke Activity untuk digunakan di startCamera
                    onPreviewViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Tombol-tombol di bagian bawah
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Tombol Take Photo
            Button(onClick = onTakePhoto) {
                Text(text = "Take Photo")
            }

            // Tombol Record Video
            Button(onClick = onRecordVideo) {
                Text(text = if (isRecording) "Stop Record" else "Record Video")
            }
        }
    }
}