package com.miafotocamera.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.miafotocamera.app.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: ExecutorService

    // Modalita Sfondo Bianco + Ombra Morbida attiva di default per ogni scatto
    private var isStudioWhiteBackgroundEnabled = true

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permessi fotocamera necessari!", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Controllo e richiesta permessi fotocamera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Pulsante Scatta Foto (ottimizzato con elaborazione sfondo bianco + ombra)
        binding.imageCaptureButton.setOnClickListener { takePhoto() }

        // Pulsante Inverti Fotocamera (Retro / Frontale)
        binding.switchCameraButton.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        // Switch o toggle modalita studio
        binding.studioModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isStudioWhiteBackgroundEnabled = isChecked
            val stato = if (isChecked) "ATTIVO: Sfondo Bianco + Ombra Morbida" else "Standard (Senza sfondo bianco)"
            Toast.makeText(this, stato, Toast.LENGTH_SHORT).show()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        Toast.makeText(this, "Elaborazione scatto in corso...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Errore scatto: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val originalBitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()

                    // Elabora l'immagine in background: inserisce sfondo bianco e ombra morbida
                    CoroutineScope(Dispatchers.Default).launch {
                        val finalBitmap = if (isStudioWhiteBackgroundEnabled) {
                            applyWhiteBackgroundWithSoftShadow(originalBitmap)
                        } else {
                            originalBitmap
                        }

                        saveBitmapToGallery(finalBitmap)
                    }
                }
            }
        )
    }

    /**
     * Applica automaticamente uno sfondo bianco puro (#FFFFFF)
     * e genera un'elegante ombra morbida e sfumata attorno/sotto al soggetto.
     */
    private fun applyWhiteBackgroundWithSoftShadow(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height

        // 1. Crea il bitmap di destinazione su sfondo bianco puro
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Sfondo bianco puro da studio fotografico (#FFFFFF)
        canvas.drawColor(Color.WHITE)

        // 2. Calcola maschera alpha del soggetto per generare l'ombra morbida
        val alphaMask = source.extractAlpha()

        // 3. Genera l'ombra morbida usando BlurMaskFilter con raggio sfumato proporzionale
        val blurRadius = (width * 0.035f).coerceAtLeast(18f)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0, 0, 0) // Nero semitrasparente morbido (28% opacita)
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }

        // Offset dell'ombra morbida (leggermente verso il basso per profondita naturale da studio)
        val shadowOffsetX = 0f
        val shadowOffsetY = height * 0.022f

        // 4. Disegna l'ombra morbida posteriore
        canvas.drawBitmap(alphaMask, shadowOffsetX, shadowOffsetY, shadowPaint)

        // 5. Aggiunge una seconda ombra d'appoggio diffusa alla base (contact shadow per tablet)
        val contactShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 15, 23, 42)
            maskFilter = BlurMaskFilter(blurRadius * 0.5f, BlurMaskFilter.Blur.NORMAL)
        }
        val contactRect = RectF(
            width * 0.18f,
            height * 0.82f,
            width * 0.82f,
            height * 0.94f
        )
        canvas.drawOval(contactRect, contactShadowPaint)

        // 6. Disegna il soggetto principale in alta definizione sopra lo sfondo bianco e l'ombra
        val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, 0f, 0f, subjectPaint)

        return outputBitmap
    }

    private suspend fun saveBitmapToGallery(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.ITALY).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "STUDIO_BIANCO_$name.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiaFotocamera")
                }
            }

            var outputStream: OutputStream? = null
            try {
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 96, outputStream)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                baseContext,
                                "Foto salvata con sfondo bianco e ombra morbida!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(baseContext, "Errore salvataggio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                outputStream?.close()
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        // Corregge rotazione orientamento se necessario
        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview a schermo intero (ottimizzata tablet)
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Errore avvio fotocamera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
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
