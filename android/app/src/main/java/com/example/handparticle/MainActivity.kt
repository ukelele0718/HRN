package com.example.handparticle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.handparticle.databinding.ActivityMainBinding
import com.example.handparticle.gesture.GestureDetector
import com.example.handparticle.gesture.HandGesture
import com.example.handparticle.hand.HandLandmarkerHelper

class MainActivity : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {
    
    private lateinit var binding: ActivityMainBinding
    private var handLandmarkerHelper: HandLandmarkerHelper? = null
    private lateinit var gestureDetector: GestureDetector
    
    private var currentColor = ParticleColor.PURPLE
    private var particleCount = 500
    private var isPanelExpanded = true
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "HandParticleApp"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide system UI for immersive experience
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        
        gestureDetector = GestureDetector()
        
        // Initial gesture display
        updateGestureDisplay(HandGesture.NONE)
        binding.particleCountText.text = "$particleCount"
        
        checkCameraPermission()
    }
    
    private fun setupUI() {
        // Toggle panel button
        setupPanelToggle()
        
        // Color picker setup
        setupColorPicker()
        
        // Particle count slider
        binding.particleSlider.apply {
            max = 1000
            progress = particleCount
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    particleCount = maxOf(100, progress)
                    binding.particleCountText.text = "$particleCount"
                    binding.particleGLView.setParticleCount(particleCount)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        binding.particleCountText.text = "$particleCount"
    }
    
    private fun setupPanelToggle() {
        binding.togglePanelBtn.setOnClickListener {
            isPanelExpanded = !isPanelExpanded
            
            if (isPanelExpanded) {
                // Expand panel
                binding.collapsibleContent.visibility = View.VISIBLE
                binding.togglePanelBtn.text = "▲"
                binding.togglePanelBtn.animate().rotation(0f).setDuration(200).start()
            } else {
                // Collapse panel
                binding.collapsibleContent.visibility = View.GONE
                binding.togglePanelBtn.text = "▼"
                binding.togglePanelBtn.animate().rotation(180f).setDuration(200).start()
            }
        }
    }
    
    private fun setupColorPicker() {
        val colorViews = listOf(
            binding.colorPurple to ParticleColor.PURPLE,
            binding.colorBlue to ParticleColor.BLUE,
            binding.colorCyan to ParticleColor.CYAN,
            binding.colorGreen to ParticleColor.GREEN,
            binding.colorYellow to ParticleColor.YELLOW,
            binding.colorOrange to ParticleColor.ORANGE,
            binding.colorPink to ParticleColor.PINK,
            binding.colorWhite to ParticleColor.WHITE
        )
        
        colorViews.forEach { (view, color) ->
            view.setOnClickListener {
                currentColor = color
                binding.particleGLView.setColor(color)
                updateColorSelection(view)
            }
        }
        
        // Set initial selection
        updateColorSelection(binding.colorPurple)
    }
    
    private fun updateColorSelection(selectedView: View) {
        listOf(
            binding.colorPurple, binding.colorBlue, binding.colorCyan,
            binding.colorGreen, binding.colorYellow, binding.colorOrange,
            binding.colorPink, binding.colorWhite
        ).forEach { view ->
            view.alpha = if (view == selectedView) 1.0f else 0.5f
            view.scaleX = if (view == selectedView) 1.2f else 1.0f
            view.scaleY = if (view == selectedView) 1.2f else 1.0f
        }
    }
    
    private fun updateGestureDisplay(gesture: HandGesture) {
        runOnUiThread {
            when (gesture) {
                HandGesture.OPEN_HAND -> {
                    binding.gestureIcon.text = "🖐️"
                    binding.gestureText.text = "Xòe tay - Mở rộng"
                    binding.gestureIcon.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
                }
                HandGesture.CLOSED_FIST -> {
                    binding.gestureIcon.text = "✊"
                    binding.gestureText.text = "Nắm tay - Thu gọn"
                    binding.gestureIcon.animate().scaleX(0.8f).scaleY(0.8f).setDuration(200).start()
                }
                HandGesture.NONE -> {
                    binding.gestureIcon.text = "👋"
                    binding.gestureText.text = "Đưa tay vào camera"
                    binding.gestureIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }
            }
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            initializeComponents()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeComponents()
            } else {
                Toast.makeText(this, "Cần quyền camera để sử dụng ứng dụng", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun initializeComponents() {
        try {
            // Setup UI
            setupUI()
            
            // Set initial color
            binding.particleGLView.setColor(currentColor)
            
            // Initialize hand landmarker
            handLandmarkerHelper = HandLandmarkerHelper(
                context = this,
                handLandmarkerHelperListener = this
            )
            
            // Start camera
            handLandmarkerHelper?.startCamera(binding.cameraPreview, this)
            
            // Update status
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_active)
            binding.statusText.text = "Đang hoạt động"
            
            Log.d(TAG, "Components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_inactive)
            binding.statusText.text = "Lỗi: ${e.message}"
            Toast.makeText(this, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // HandLandmarkerHelper.LandmarkerListener implementation
    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            try {
                // Update FPS
                binding.fpsText.text = "FPS: ${resultBundle.fps}"
                
                if (resultBundle.results.isNotEmpty()) {
                    val landmarks = resultBundle.results[0].landmarks()
                    
                    if (landmarks.isNotEmpty()) {
                        val handLandmarks = landmarks[0]
                        
                        // Detect gesture
                        val gesture = gestureDetector.detectGesture(handLandmarks)
                        updateGestureDisplay(gesture)
                        
                        // Calculate openness (0.0 = closed fist, 1.0 = fully open)
                        val openness = gestureDetector.calculateOpenness(handLandmarks)
                        
                        // Get palm center for particle attraction point
                        val palmCenter = gestureDetector.getPalmCenter(handLandmarks)
                        
                        // Update particle system
                        binding.particleGLView.setHandData(palmCenter, openness)
                    } else {
                        updateGestureDisplay(HandGesture.NONE)
                        binding.particleGLView.setHandData(null, 0.5f)
                    }
                } else {
                    updateGestureDisplay(HandGesture.NONE)
                    binding.particleGLView.setHandData(null, 0.5f)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing results", e)
            }
        }
    }
    
    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_inactive)
            binding.statusText.text = "Lỗi: $error"
        }
    }
    
    override fun onResume() {
        super.onResume()
        binding.particleGLView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        binding.particleGLView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handLandmarkerHelper?.clearHandLandmarker()
    }
}

enum class ParticleColor(val r: Float, val g: Float, val b: Float) {
    PURPLE(0.6f, 0.3f, 0.9f),
    BLUE(0.3f, 0.5f, 1.0f),
    CYAN(0.3f, 0.9f, 0.9f),
    GREEN(0.3f, 0.9f, 0.5f),
    YELLOW(1.0f, 0.9f, 0.3f),
    ORANGE(1.0f, 0.6f, 0.2f),
    PINK(1.0f, 0.4f, 0.7f),
    WHITE(1.0f, 1.0f, 1.0f)
}
