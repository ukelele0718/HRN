package com.example.handparticle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.handparticle.databinding.ActivityNumberInputBinding
import com.example.handparticle.pose.PoseGestureDetector
import com.example.handparticle.pose.PoseLandmarkerHelper

class NumberInputActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var binding: ActivityNumberInputBinding
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private lateinit var poseGestureDetector: PoseGestureDetector
    
    // Grid position: 0-8 (3x3 grid)
    // Layout:
    // 0(1) 1(2) 2(3)
    // 3(4) 4(5) 5(6)
    // 6(7) 7(8) 8(9)
    private var currentPosition = 4 // Default to center (number 5)
    private var displayedNumber = ""
    
    private lateinit var numberButtons: Array<TextView>
    private val handler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "NumberInputActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNumberInputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide system UI
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        
        poseGestureDetector = PoseGestureDetector()
        
        // Initialize button array
        numberButtons = arrayOf(
            binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6,
            binding.btn7, binding.btn8, binding.btn9
        )
        
        // Set up click listeners for manual input (optional)
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectPosition(index)
                addCurrentNumberToDisplay()
            }
        }
        
        binding.btnClear.setOnClickListener {
            displayedNumber = ""
            updateDisplay()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Initial selection
        updateSelection()
        
        // Check camera permission
        if (checkCameraPermission()) {
            initPoseLandmarker()
        } else {
            requestCameraPermission()
        }
    }
    
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initPoseLandmarker()
            } else {
                Toast.makeText(this, "Cần quyền camera để nhận diện cử chỉ", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun initPoseLandmarker() {
        poseLandmarkerHelper = PoseLandmarkerHelper(
            context = this,
            poseLandmarkerHelperListener = this,
            minPoseDetectionConfidence = 0.5f,
            minPoseTrackingConfidence = 0.5f,
            minPosePresenceConfidence = 0.5f,
            numPoses = 1
        )
        poseLandmarkerHelper?.startCamera(this, binding.cameraPreview)
    }
    
    private fun selectPosition(position: Int) {
        if (position in 0..8) {
            currentPosition = position
            updateSelection()
        }
    }
    
    private fun updateSelection() {
        // Clear all selections
        numberButtons.forEach { it.isSelected = false }
        
        // Set current selection
        numberButtons[currentPosition].isSelected = true
    }
    
    private fun addCurrentNumberToDisplay() {
        val number = currentPosition + 1 // Position 0 = number 1, etc.
        displayedNumber += number.toString()
        updateDisplay()
        
        // Visual feedback
        numberButtons[currentPosition].animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                numberButtons[currentPosition].animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    private fun updateDisplay() {
        binding.numberDisplay.text = displayedNumber
    }
    
    private fun moveSelection(gesture: PoseGestureDetector.PoseGesture) {
        val row = currentPosition / 3
        val col = currentPosition % 3
        
        val newPosition = when (gesture) {
            PoseGestureDetector.PoseGesture.MOVE_UP -> {
                if (row > 0) currentPosition - 3 else currentPosition
            }
            PoseGestureDetector.PoseGesture.MOVE_DOWN -> {
                if (row < 2) currentPosition + 3 else currentPosition
            }
            PoseGestureDetector.PoseGesture.MOVE_LEFT -> {
                if (col > 0) currentPosition - 1 else currentPosition
            }
            PoseGestureDetector.PoseGesture.MOVE_RIGHT -> {
                if (col < 2) currentPosition + 1 else currentPosition
            }
            else -> currentPosition
        }
        
        if (newPosition != currentPosition) {
            selectPosition(newPosition)
            playMoveSound()
        }
    }
    
    private fun playMoveSound() {
        // Optional: Add sound feedback
        // Could use MediaPlayer or SoundPool here
    }
    
    // PoseLandmarkerHelper.LandmarkerListener implementation
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        val gesture = poseGestureDetector.detectGesture(resultBundle.results)
        
        handler.post {
            when (gesture) {
                PoseGestureDetector.PoseGesture.MOVE_UP -> {
                    binding.gestureDisplay.text = "⬆️ LÊN"
                    moveSelection(gesture)
                }
                PoseGestureDetector.PoseGesture.MOVE_DOWN -> {
                    binding.gestureDisplay.text = "⬇️ XUỐNG"
                    moveSelection(gesture)
                }
                PoseGestureDetector.PoseGesture.MOVE_LEFT -> {
                    binding.gestureDisplay.text = "⬅️ TRÁI"
                    moveSelection(gesture)
                }
                PoseGestureDetector.PoseGesture.MOVE_RIGHT -> {
                    binding.gestureDisplay.text = "➡️ PHẢI"
                    moveSelection(gesture)
                }
                PoseGestureDetector.PoseGesture.ENTER -> {
                    binding.gestureDisplay.text = "⭕ ENTER!"
                    addCurrentNumberToDisplay()
                }
                PoseGestureDetector.PoseGesture.NONE -> {
                    binding.gestureDisplay.text = "Chờ cử chỉ..."
                }
            }
        }
    }
    
    override fun onError(error: String, errorCode: Int) {
        Log.e(TAG, "Pose detection error: $error")
        handler.post {
            binding.gestureDisplay.text = "Lỗi: $error"
        }
    }
    
    override fun onResume() {
        super.onResume()
        poseGestureDetector.reset()
    }
    
    override fun onPause() {
        super.onPause()
        poseLandmarkerHelper?.stopCamera()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        poseLandmarkerHelper?.clearPoseLandmarker()
        poseLandmarkerHelper?.stopCamera()
    }
}
