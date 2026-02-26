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
import com.example.handparticle.databinding.ActivityTextInputBinding
import com.example.handparticle.pose.PoseGestureDetector
import com.example.handparticle.pose.PoseLandmarkerHelper

class TextInputActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var binding: ActivityTextInputBinding
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private lateinit var poseGestureDetector: PoseGestureDetector
    
    // QWERTY keyboard layout with numbers
    // Row 0: 1 2 3 4 5 6 7 8 9 0
    // Row 1: Q W E R T Y U I O P
    // Row 2: A S D F G H J K L
    // Row 3: Z X C V B N M
    // Row 4: SPACE
    
    private val keyboardLayout = arrayOf(
        arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        arrayOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        arrayOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        arrayOf("Z", "X", "C", "V", "B", "N", "M"),
        arrayOf(" ") // Space
    )
    
    private var currentRow = 1  // Start at QWERTY row
    private var currentCol = 4  // Start at Y (middle)
    private var displayedText = ""
    
    private lateinit var keyViews: Array<Array<TextView>>
    private val handler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "TextInputActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextInputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide system UI
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        
        poseGestureDetector = PoseGestureDetector()
        
        // Initialize key views array
        initKeyViews()
        
        // Set up click listeners for manual input
        setupKeyClickListeners()
        
        binding.btnBackspace.setOnClickListener {
            if (displayedText.isNotEmpty()) {
                displayedText = displayedText.dropLast(1)
                updateDisplay()
            }
        }
        
        binding.btnClear.setOnClickListener {
            displayedText = ""
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
    
    private fun initKeyViews() {
        keyViews = arrayOf(
            // Row 0: Numbers
            arrayOf(
                binding.key1, binding.key2, binding.key3, binding.key4, binding.key5,
                binding.key6, binding.key7, binding.key8, binding.key9, binding.key0
            ),
            // Row 1: QWERTYUIOP
            arrayOf(
                binding.keyQ, binding.keyW, binding.keyE, binding.keyR, binding.keyT,
                binding.keyY, binding.keyU, binding.keyI, binding.keyO, binding.keyP
            ),
            // Row 2: ASDFGHJKL
            arrayOf(
                binding.keyA, binding.keyS, binding.keyD, binding.keyF, binding.keyG,
                binding.keyH, binding.keyJ, binding.keyK, binding.keyL
            ),
            // Row 3: ZXCVBNM
            arrayOf(
                binding.keyZ, binding.keyX, binding.keyC, binding.keyV,
                binding.keyB, binding.keyN, binding.keyM
            ),
            // Row 4: Space
            arrayOf(binding.keySpace)
        )
    }
    
    private fun setupKeyClickListeners() {
        keyViews.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, keyView ->
                keyView.setOnClickListener {
                    currentRow = rowIndex
                    currentCol = colIndex
                    updateSelection()
                    addCurrentKeyToDisplay()
                }
            }
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
    
    private fun updateSelection() {
        // Clear all selections
        keyViews.forEach { row ->
            row.forEach { it.isSelected = false }
        }
        
        // Clamp current position to valid range
        currentRow = currentRow.coerceIn(0, keyViews.size - 1)
        currentCol = currentCol.coerceIn(0, keyViews[currentRow].size - 1)
        
        // Set current selection
        keyViews[currentRow][currentCol].isSelected = true
    }
    
    private fun addCurrentKeyToDisplay() {
        val key = keyboardLayout[currentRow][currentCol]
        displayedText += key
        updateDisplay()
        
        // Visual feedback
        val keyView = keyViews[currentRow][currentCol]
        keyView.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(80)
            .withEndAction {
                keyView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .start()
            }
            .start()
    }
    
    private fun updateDisplay() {
        binding.textDisplay.text = displayedText
    }
    
    private fun moveSelection(gesture: PoseGestureDetector.PoseGesture) {
        val oldRow = currentRow
        val oldCol = currentCol
        
        when (gesture) {
            PoseGestureDetector.PoseGesture.MOVE_UP -> {
                if (currentRow > 0) {
                    currentRow--
                    // Adjust column to stay within new row bounds
                    currentCol = currentCol.coerceIn(0, keyViews[currentRow].size - 1)
                }
            }
            PoseGestureDetector.PoseGesture.MOVE_DOWN -> {
                if (currentRow < keyViews.size - 1) {
                    currentRow++
                    // Adjust column to stay within new row bounds
                    currentCol = currentCol.coerceIn(0, keyViews[currentRow].size - 1)
                }
            }
            PoseGestureDetector.PoseGesture.MOVE_LEFT -> {
                if (currentCol > 0) {
                    currentCol--
                }
            }
            PoseGestureDetector.PoseGesture.MOVE_RIGHT -> {
                if (currentCol < keyViews[currentRow].size - 1) {
                    currentCol++
                }
            }
            else -> {}
        }
        
        if (oldRow != currentRow || oldCol != currentCol) {
            updateSelection()
        }
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
                    addCurrentKeyToDisplay()
                }
                PoseGestureDetector.PoseGesture.NONE -> {
                    binding.gestureDisplay.text = "Chỉ tay để di chuyển..."
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
