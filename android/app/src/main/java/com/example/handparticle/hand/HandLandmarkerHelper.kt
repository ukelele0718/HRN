package com.example.handparticle.hand

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HandLandmarkerHelper(
    private val context: Context,
    private val handLandmarkerHelperListener: LandmarkerListener,
    private val minHandDetectionConfidence: Float = 0.5f,
    private val minHandTrackingConfidence: Float = 0.5f,
    private val minHandPresenceConfidence: Float = 0.5f,
    private val maxNumHands: Int = 1,
    private val currentDelegate: Int = DELEGATE_CPU,
    private val runningMode: RunningMode = RunningMode.LIVE_STREAM
) {
    private var handLandmarker: HandLandmarker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var backgroundExecutor: ExecutorService
    
    // FPS calculation
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0
    
    init {
        setupHandLandmarker()
    }
    
    private fun setupHandLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
        
        when (currentDelegate) {
            DELEGATE_CPU -> baseOptionsBuilder.setDelegate(Delegate.CPU)
            DELEGATE_GPU -> baseOptionsBuilder.setDelegate(Delegate.GPU)
        }
        
        baseOptionsBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)
        
        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(minHandDetectionConfidence)
                .setMinTrackingConfidence(minHandTrackingConfidence)
                .setMinHandPresenceConfidence(minHandPresenceConfidence)
                .setNumHands(maxNumHands)
                .setRunningMode(runningMode)
            
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }
            
            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
            Log.d(TAG, "HandLandmarker initialized successfully")
            
        } catch (e: Exception) {
            handLandmarkerHelperListener.onError(
                "Hand Landmarker failed to initialize: ${e.message}",
                OTHER_ERROR
            )
            Log.e(TAG, "Error initializing HandLandmarker", e)
        }
    }
    
    fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        backgroundExecutor = Executors.newSingleThreadExecutor()
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(previewView, lifecycleOwner)
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun bindCameraUseCases(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return
        
        // Preview
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display.rotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image Analysis
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { imageProxy ->
                    detectHand(imageProxy)
                }
            }
        
        // Use front camera
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }
    
    private fun detectHand(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        
        // Convert to bitmap
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        
        // Mirror for front camera
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, imageProxy.width / 2f, imageProxy.height / 2f)
        }
        
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )
        
        // Convert to MPImage
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        
        // Run detection
        handLandmarker?.detectAsync(mpImage, frameTime)
        
        imageProxy.close()
    }
    
    private fun returnLivestreamResult(result: HandLandmarkerResult, input: MPImage) {
        // Calculate FPS
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsTime = currentTime
        }
        
        handLandmarkerHelperListener.onResults(
            ResultBundle(
                results = listOf(result),
                inputImageWidth = input.width,
                inputImageHeight = input.height,
                fps = currentFps
            )
        )
    }
    
    private fun returnLivestreamError(error: RuntimeException) {
        handLandmarkerHelperListener.onError(
            error.message ?: "Unknown error",
            OTHER_ERROR
        )
    }
    
    fun clearHandLandmarker() {
        handLandmarker?.close()
        handLandmarker = null
        if (::backgroundExecutor.isInitialized) {
            backgroundExecutor.shutdown()
        }
    }
    
    companion object {
        const val TAG = "HandLandmarkerHelper"
        private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"
        
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }
    
    data class ResultBundle(
        val results: List<HandLandmarkerResult>,
        val inputImageWidth: Int,
        val inputImageHeight: Int,
        val fps: Int
    )
    
    interface LandmarkerListener {
        fun onResults(resultBundle: ResultBundle)
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
    }
}
