package com.example.handparticle.pose

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
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseLandmarkerHelper(
    private val context: Context,
    private val poseLandmarkerHelperListener: LandmarkerListener,
    private val minPoseDetectionConfidence: Float = 0.5f,
    private val minPoseTrackingConfidence: Float = 0.5f,
    private val minPosePresenceConfidence: Float = 0.5f,
    private val numPoses: Int = 1,
    private val currentDelegate: Int = DELEGATE_CPU,
    private val runningMode: RunningMode = RunningMode.LIVE_STREAM
) {
    private var poseLandmarker: PoseLandmarker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var backgroundExecutor: ExecutorService
    
    init {
        setupPoseLandmarker()
    }
    
    private fun setupPoseLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
        
        when (currentDelegate) {
            DELEGATE_CPU -> baseOptionsBuilder.setDelegate(Delegate.CPU)
            DELEGATE_GPU -> baseOptionsBuilder.setDelegate(Delegate.GPU)
        }
        
        baseOptionsBuilder.setModelAssetPath(MP_POSE_LANDMARKER_TASK)
        
        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                .setMinTrackingConfidence(minPoseTrackingConfidence)
                .setMinPosePresenceConfidence(minPosePresenceConfidence)
                .setNumPoses(numPoses)
                .setRunningMode(runningMode)
            
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, optionsBuilder.build())
            Log.d(TAG, "PoseLandmarker initialized successfully")
            
        } catch (e: Exception) {
            poseLandmarkerHelperListener.onError(
                "Pose Landmarker failed to initialize: ${e.message}",
                OTHER_ERROR
            )
            Log.e(TAG, "Error initializing PoseLandmarker", e)
        }
    }
    
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        backgroundExecutor = Executors.newSingleThreadExecutor()
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(lifecycleOwner, previewView)
        }, ContextCompat.getMainExecutor(context))
    }
    
    @androidx.camera.core.ExperimentalGetImage
    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProvider = cameraProvider 
            ?: throw IllegalStateException("Camera initialization failed.")
        
        // Use front camera for pose detection
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
        
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { imageProxy ->
                    detectPose(imageProxy)
                }
            }
        
        cameraProvider.unbindAll()
        
        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
            Log.d(TAG, "Camera bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            poseLandmarkerHelperListener.onError(
                "Camera binding failed: ${e.message}",
                OTHER_ERROR
            )
        }
    }
    
    @androidx.camera.core.ExperimentalGetImage
    private fun detectPose(imageProxy: ImageProxy) {
        if (poseLandmarker == null) {
            imageProxy.close()
            return
        }
        
        val frameTime = SystemClock.uptimeMillis()
        
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()
        
        // Mirror for front camera
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, imageProxy.width / 2f, imageProxy.height / 2f)
        }
        
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
        )
        
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        
        poseLandmarker?.detectAsync(mpImage, frameTime)
    }
    
    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        poseLandmarkerHelperListener.onResults(
            ResultBundle(
                result,
                input.width,
                input.height
            )
        )
    }
    
    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener.onError(
            error.message ?: "An unknown error occurred",
            OTHER_ERROR
        )
    }
    
    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
    
    fun stopCamera() {
        cameraProvider?.unbindAll()
        if (::backgroundExecutor.isInitialized) {
            backgroundExecutor.shutdown()
        }
    }
    
    data class ResultBundle(
        val results: PoseLandmarkerResult,
        val inputImageWidth: Int,
        val inputImageHeight: Int
    )
    
    interface LandmarkerListener {
        fun onResults(resultBundle: ResultBundle)
        fun onError(error: String, errorCode: Int)
    }
    
    companion object {
        const val TAG = "PoseLandmarkerHelper"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        
        const val MP_POSE_LANDMARKER_TASK = "pose_landmarker_lite.task"
        
        // Pose landmark indices
        const val NOSE = 0
        const val LEFT_EYE_INNER = 1
        const val LEFT_EYE = 2
        const val LEFT_EYE_OUTER = 3
        const val RIGHT_EYE_INNER = 4
        const val RIGHT_EYE = 5
        const val RIGHT_EYE_OUTER = 6
        const val LEFT_EAR = 7
        const val RIGHT_EAR = 8
        const val MOUTH_LEFT = 9
        const val MOUTH_RIGHT = 10
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_ELBOW = 13
        const val RIGHT_ELBOW = 14
        const val LEFT_WRIST = 15
        const val RIGHT_WRIST = 16
        const val LEFT_PINKY = 17
        const val RIGHT_PINKY = 18
        const val LEFT_INDEX = 19
        const val RIGHT_INDEX = 20
        const val LEFT_THUMB = 21
        const val RIGHT_THUMB = 22
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_KNEE = 25
        const val RIGHT_KNEE = 26
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val LEFT_HEEL = 29
        const val RIGHT_HEEL = 30
        const val LEFT_FOOT_INDEX = 31
        const val RIGHT_FOOT_INDEX = 32
    }
}
