package com.example.handparticle.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Gesture detector for pose-based navigation
 * Detects: LEFT, RIGHT, UP, DOWN movements based on arm direction (elbow -> wrist -> index finger)
 * and "O" circle gesture for ENTER
 */
class PoseGestureDetector {
    
    enum class PoseGesture {
        NONE,
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_UP,
        MOVE_DOWN,
        ENTER  // Circle "O" gesture with both hands above head
    }
    
    private var lastGestureTime = 0L
    private var lastGesture = PoseGesture.NONE
    private val gestureCooldownMs = 600L // Prevent rapid-fire gesture detection
    
    // Angle thresholds for direction detection (in degrees)
    private val angleThreshold = 30f // Degrees from cardinal direction
    
    fun detectGesture(result: PoseLandmarkerResult): PoseGesture {
        if (result.landmarks().isEmpty()) {
            return PoseGesture.NONE
        }
        
        val landmarks = result.landmarks()[0]
        if (landmarks.size < 33) {
            return PoseGesture.NONE
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGestureTime < gestureCooldownMs && lastGesture != PoseGesture.NONE) {
            return PoseGesture.NONE
        }
        
        // Get key landmarks
        val nose = landmarks[PoseLandmarkerHelper.NOSE]
        val leftShoulder = landmarks[PoseLandmarkerHelper.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmarkerHelper.RIGHT_SHOULDER]
        val leftElbow = landmarks[PoseLandmarkerHelper.LEFT_ELBOW]
        val rightElbow = landmarks[PoseLandmarkerHelper.RIGHT_ELBOW]
        val leftWrist = landmarks[PoseLandmarkerHelper.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmarkerHelper.RIGHT_WRIST]
        val leftIndex = landmarks[PoseLandmarkerHelper.LEFT_INDEX]
        val rightIndex = landmarks[PoseLandmarkerHelper.RIGHT_INDEX]
        
        // Check for circle "O" gesture first (highest priority)
        if (isCircleGesture(nose, leftShoulder, rightShoulder, leftElbow, rightElbow, leftWrist, rightWrist)) {
            lastGesture = PoseGesture.ENTER
            lastGestureTime = currentTime
            return PoseGesture.ENTER
        }
        
        // Detect direction based on arm arrow (elbow -> wrist -> index finger)
        // Use dominant arm (the one with stronger extension)
        val leftArmDirection = getArmDirection(leftElbow, leftWrist, leftIndex)
        val rightArmDirection = getArmDirection(rightElbow, rightWrist, rightIndex)
        
        // Check if arm is extended enough to be considered a "pointing" gesture
        val leftArmExtended = isArmExtended(leftShoulder, leftElbow, leftWrist, leftIndex)
        val rightArmExtended = isArmExtended(rightShoulder, rightElbow, rightWrist, rightIndex)
        
        val gesture = when {
            leftArmExtended && rightArmExtended -> {
                // Both arms extended - use the average or dominant direction
                val avgDirection = averageDirection(leftArmDirection, rightArmDirection)
                directionToGesture(avgDirection)
            }
            leftArmExtended -> directionToGesture(leftArmDirection)
            rightArmExtended -> directionToGesture(rightArmDirection)
            else -> PoseGesture.NONE
        }
        
        if (gesture != PoseGesture.NONE) {
            lastGesture = gesture
            lastGestureTime = currentTime
        }
        
        return gesture
    }
    
    /**
     * Calculate the direction angle from elbow -> wrist -> index finger
     * Returns angle in degrees: 0=right, 90=up, 180=left, 270=down
     */
    private fun getArmDirection(
        elbow: NormalizedLandmark,
        wrist: NormalizedLandmark,
        index: NormalizedLandmark
    ): Float {
        // Vector from elbow to wrist
        val v1x = wrist.x() - elbow.x()
        val v1y = wrist.y() - elbow.y()
        
        // Vector from wrist to index finger
        val v2x = index.x() - wrist.x()
        val v2y = index.y() - wrist.y()
        
        // Average direction vector (weighted towards fingertip)
        val avgX = v1x * 0.4f + v2x * 0.6f
        val avgY = v1y * 0.4f + v2y * 0.6f
        
        // Calculate angle (note: Y is inverted in image coordinates)
        val angle = Math.toDegrees(atan2(-avgY.toDouble(), avgX.toDouble())).toFloat()
        
        // Normalize to 0-360
        return if (angle < 0) angle + 360 else angle
    }
    
    /**
     * Check if arm is extended enough to be considered pointing
     */
    private fun isArmExtended(
        shoulder: NormalizedLandmark,
        elbow: NormalizedLandmark,
        wrist: NormalizedLandmark,
        index: NormalizedLandmark
    ): Boolean {
        // Calculate distances
        val shoulderToElbow = distance(shoulder, elbow)
        val elbowToWrist = distance(elbow, wrist)
        val wristToIndex = distance(wrist, index)
        
        // Arm should be relatively straight (elbow angle not too bent)
        val shoulderToWrist = distance(shoulder, wrist)
        val expectedStraight = shoulderToElbow + elbowToWrist
        val straightRatio = shoulderToWrist / expectedStraight
        
        // Consider extended if arm is at least 70% straight and has some length
        return straightRatio > 0.7f && shoulderToElbow > 0.08f && wristToIndex > 0.02f
    }
    
    /**
     * Convert angle to gesture direction
     */
    private fun directionToGesture(angle: Float): PoseGesture {
        // Note: In camera view (front camera, mirrored), we need to flip left/right
        return when {
            // Right (pointing right in mirror = move left on grid) - around 0 or 360 degrees
            angle < angleThreshold || angle > 360 - angleThreshold -> PoseGesture.MOVE_LEFT
            // Up - around 90 degrees
            angle > 90 - angleThreshold && angle < 90 + angleThreshold -> PoseGesture.MOVE_UP
            // Left (pointing left in mirror = move right on grid) - around 180 degrees
            angle > 180 - angleThreshold && angle < 180 + angleThreshold -> PoseGesture.MOVE_RIGHT
            // Down - around 270 degrees
            angle > 270 - angleThreshold && angle < 270 + angleThreshold -> PoseGesture.MOVE_DOWN
            else -> PoseGesture.NONE
        }
    }
    
    /**
     * Average two direction angles
     */
    private fun averageDirection(angle1: Float, angle2: Float): Float {
        // Handle wrap-around at 0/360
        val diff = abs(angle1 - angle2)
        return if (diff > 180) {
            val sum = angle1 + angle2
            ((sum + 360) / 2) % 360
        } else {
            (angle1 + angle2) / 2
        }
    }
    
    /**
     * Check if both arms form a circle "O" shape above the head
     */
    private fun isCircleGesture(
        nose: NormalizedLandmark,
        leftShoulder: NormalizedLandmark,
        rightShoulder: NormalizedLandmark,
        leftElbow: NormalizedLandmark,
        rightElbow: NormalizedLandmark,
        leftWrist: NormalizedLandmark,
        rightWrist: NormalizedLandmark
    ): Boolean {
        // Both wrists should be above the nose (y coordinate is smaller = higher in image)
        val wristsAboveHead = leftWrist.y() < nose.y() - 0.05f && rightWrist.y() < nose.y() - 0.05f
        
        // Wrists should be close together (forming the top of O)
        val wristDistance = distance(leftWrist, rightWrist)
        val wristsClose = wristDistance < 0.25f
        
        // Elbows should be raised (above or near shoulder level)
        val elbowsRaised = leftElbow.y() < leftShoulder.y() + 0.05f && rightElbow.y() < rightShoulder.y() + 0.05f
        
        // Elbows should be spread out (forming sides of O)
        val shoulderWidth = abs(leftShoulder.x() - rightShoulder.x())
        val elbowSpread = abs(leftElbow.x() - rightElbow.x()) > shoulderWidth * 0.6f
        
        return wristsAboveHead && wristsClose && elbowsRaised && elbowSpread
    }
    
    private fun distance(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        return sqrt(dx * dx + dy * dy)
    }
    
    fun reset() {
        lastGesture = PoseGesture.NONE
        lastGestureTime = 0L
    }
}
