package com.example.handparticle.gesture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

/**
 * Gesture types detected by the system
 */
enum class HandGesture {
    OPEN_HAND,    // All fingers extended - expand particles
    CLOSED_FIST,  // All fingers closed - collapse particles
    NONE          // No hand detected
}

/**
 * Data class representing a 3D point
 */
data class Point3D(val x: Float, val y: Float, val z: Float)

/**
 * Gesture detector using MediaPipe hand landmarks
 * 
 * Hand Landmark indices:
 * 0: WRIST
 * 1-4: THUMB (CMC, MCP, IP, TIP)
 * 5-8: INDEX (MCP, PIP, DIP, TIP)
 * 9-12: MIDDLE (MCP, PIP, DIP, TIP)
 * 13-16: RING (MCP, PIP, DIP, TIP)
 * 17-20: PINKY (MCP, PIP, DIP, TIP)
 */
class GestureDetector {
    
    companion object {
        // Landmark indices
        const val WRIST = 0
        const val THUMB_TIP = 4
        const val INDEX_TIP = 8
        const val MIDDLE_TIP = 12
        const val RING_TIP = 16
        const val PINKY_TIP = 20
        
        const val THUMB_MCP = 2
        const val INDEX_MCP = 5
        const val MIDDLE_MCP = 9
        const val RING_MCP = 13
        const val PINKY_MCP = 17
        
        const val INDEX_PIP = 6
        const val MIDDLE_PIP = 10
        const val RING_PIP = 14
        const val PINKY_PIP = 18
        
        // Thresholds
        const val FINGER_EXTENDED_THRESHOLD = 0.06f
        const val FIST_THRESHOLD = 0.12f
    }
    
    /**
     * Detect the current hand gesture
     */
    fun detectGesture(landmarks: List<NormalizedLandmark>): HandGesture {
        if (landmarks.size < 21) return HandGesture.NONE
        
        val fingersExtended = countExtendedFingers(landmarks)
        
        return when {
            fingersExtended >= 4 -> HandGesture.OPEN_HAND
            fingersExtended <= 1 -> HandGesture.CLOSED_FIST
            else -> HandGesture.NONE
        }
    }
    
    /**
     * Calculate hand openness from 0 (closed fist) to 1 (fully open)
     */
    fun calculateOpenness(landmarks: List<NormalizedLandmark>): Float {
        if (landmarks.size < 21) return 0.5f
        
        // Calculate average distance from fingertips to palm center
        val palmCenter = getPalmCenter(landmarks) ?: return 0.5f
        
        val fingerTips = listOf(THUMB_TIP, INDEX_TIP, MIDDLE_TIP, RING_TIP, PINKY_TIP)
        var totalDistance = 0f
        
        fingerTips.forEach { tipIndex ->
            val tip = landmarks[tipIndex]
            totalDistance += distance(tip.x(), tip.y(), palmCenter.x, palmCenter.y)
        }
        
        val avgDistance = totalDistance / fingerTips.size
        
        // Normalize: typical closed fist ~0.08, open hand ~0.25
        val normalized = ((avgDistance - 0.08f) / 0.17f).coerceIn(0f, 1f)
        
        return normalized
    }
    
    /**
     * Get palm center position (normalized 0-1 coordinates)
     */
    fun getPalmCenter(landmarks: List<NormalizedLandmark>): Point3D? {
        if (landmarks.size < 21) return null
        
        // Palm center is approximated by averaging wrist and MCP joints
        val palmIndices = listOf(WRIST, INDEX_MCP, MIDDLE_MCP, RING_MCP, PINKY_MCP)
        
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        
        palmIndices.forEach { index ->
            sumX += landmarks[index].x()
            sumY += landmarks[index].y()
            sumZ += landmarks[index].z()
        }
        
        return Point3D(
            sumX / palmIndices.size,
            sumY / palmIndices.size,
            sumZ / palmIndices.size
        )
    }
    
    /**
     * Count how many fingers are extended
     */
    private fun countExtendedFingers(landmarks: List<NormalizedLandmark>): Int {
        var count = 0
        
        // Check thumb (different logic - horizontal movement)
        if (isThumbExtended(landmarks)) count++
        
        // Check other fingers
        if (isFingerExtended(landmarks, INDEX_TIP, INDEX_PIP, INDEX_MCP)) count++
        if (isFingerExtended(landmarks, MIDDLE_TIP, MIDDLE_PIP, MIDDLE_MCP)) count++
        if (isFingerExtended(landmarks, RING_TIP, RING_PIP, RING_MCP)) count++
        if (isFingerExtended(landmarks, PINKY_TIP, PINKY_PIP, PINKY_MCP)) count++
        
        return count
    }
    
    /**
     * Check if a finger is extended by comparing tip to PIP joint
     */
    private fun isFingerExtended(
        landmarks: List<NormalizedLandmark>,
        tipIndex: Int,
        pipIndex: Int,
        mcpIndex: Int
    ): Boolean {
        val tip = landmarks[tipIndex]
        val pip = landmarks[pipIndex]
        val mcp = landmarks[mcpIndex]
        
        // Finger is extended if tip is further from wrist than PIP
        // Using Y coordinate (in image space, Y increases downward)
        val wrist = landmarks[WRIST]
        
        val tipToWrist = distance(tip.x(), tip.y(), wrist.x(), wrist.y())
        val pipToWrist = distance(pip.x(), pip.y(), wrist.x(), wrist.y())
        
        return tipToWrist > pipToWrist - FINGER_EXTENDED_THRESHOLD
    }
    
    /**
     * Check if thumb is extended (uses different logic)
     */
    private fun isThumbExtended(landmarks: List<NormalizedLandmark>): Boolean {
        val thumbTip = landmarks[THUMB_TIP]
        val thumbMcp = landmarks[THUMB_MCP]
        val indexMcp = landmarks[INDEX_MCP]
        
        // Thumb is extended if tip is far from index MCP
        val distance = distance(thumbTip.x(), thumbTip.y(), indexMcp.x(), indexMcp.y())
        return distance > FIST_THRESHOLD
    }
    
    /**
     * Calculate 2D distance between two points
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
