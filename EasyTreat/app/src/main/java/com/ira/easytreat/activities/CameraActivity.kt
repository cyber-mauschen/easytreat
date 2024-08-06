package com.ira.easytreat.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.RestrictionsManager.RESULT_ERROR
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ira.easytreat.R
import com.ira.easytreat.utils.UIUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_IMAGE_PATH = "captured_image_path"
    }

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: ImageButton
    private lateinit var imageButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var launcher: ActivityResultLauncher<PickVisualMediaRequest>
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        previewView = findViewById(R.id.cameraPreviewView)
        captureButton = findViewById(R.id.cameraButton)
        backButton = findViewById(R.id.backButton)
        imageButton = findViewById(R.id.imageButton)
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestCameraPermission()

        backButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        launcher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            try {
                if (uri != null) {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val url = saveImageToAppStorage(bitmap)
                        val capturedImagePath = url?.path
                        val file = File(url?.path)
                        if (file.exists()) {
                            setResult(RESULT_OK, Intent().apply {
                                putExtra(EXTRA_IMAGE_PATH, capturedImagePath)
                            })
                        } else {
                            setResult(RESULT_ERROR)
                        }
                        finish()
                    }
                }
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
            setResult(RESULT_ERROR)
            finish()
        }

        imageButton.setOnClickListener {
            launcher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    @SuppressLint("RestrictedApi")
    private fun copyImageToAppStorage(imageUri: Uri): URI? {
        val inputStream = contentResolver.openInputStream(imageUri) ?: return null
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(getOutputFile()).build()
        val outputStream = openFileOutput(outputFileOptions.file?.name, Context.MODE_PRIVATE)
        val buffer = ByteArray(1024)
        var readBytes: Int
        while (inputStream.read(buffer).also { readBytes = it } > 0) {
            outputStream.write(buffer, 0, readBytes)
        }
        inputStream.close()
        outputStream.close()
        return outputFileOptions.file?.toURI()
    }
    @SuppressLint("RestrictedApi")
    private fun saveImageToAppStorage(bitmap: Bitmap): URI? {
        val dir = this.filesDir
        val file = File(dir, getOutputFileName())

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return file.toURI()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun requestCameraPermission() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val REQUEST_CODE_PERMISSIONS = 10

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                setResult(RESULT_ERROR)
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PermissionChecker.PERMISSION_GRANTED
    }

    private fun getOutputFile(): File {
        val storageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "CameraX_Images")
        storageDir.mkdirs()
        return File(storageDir, "${System.currentTimeMillis()}.jpg")
    }

    private fun getOutputFileName(): String {
        return "${System.currentTimeMillis()}.jpg"
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // Change to DEFAULT_FRONT_CAMERA for front camera
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val imageCapture = ImageCapture.Builder().build()

            val cameraCase = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, cameraCase)

            // Capture button click listener (example)
            captureButton.setOnClickListener {
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(getOutputFile()).build()
                imageCapture.takePicture(outputFileOptions, cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraActivity", "Image capturing failed")
                            setResult(RESULT_ERROR)
                            finish()
                        }
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            try {
                                if (output.savedUri != null) {
                                    // Proceed with copying the image using the retrieved filename
                                    //var originUrl = copyImageToAppStorage(output.savedUri!!)
                                    var capturedImagePath = output.savedUri?.path

                                    capturedImagePath?.let {
                                        var bitmap = UIUtils.getBitmapFromFilePath(it)
                                        if (bitmap != null) {
                                            bitmap = UIUtils.cropAndRotateBitmap(bitmap)
                                            if (bitmap != null) {
                                                val url = saveImageToAppStorage(bitmap)
                                                capturedImagePath = url?.path

                                                val file = File(url?.path)
                                                if (file.exists()) {
                                                    setResult(RESULT_OK, Intent().apply {
                                                        putExtra(EXTRA_IMAGE_PATH, capturedImagePath)
                                                    })
                                                } else {
                                                    print("error")
                                                }
                                                finish()
                                            }
                                        }
                                    }
                                }
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                            }
                        }
                    })
            }
        }, ContextCompat.getMainExecutor(this))
    }
}