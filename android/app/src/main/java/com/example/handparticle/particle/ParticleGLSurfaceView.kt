package com.example.handparticle.particle

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.example.handparticle.ParticleColor

class ParticleGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    
    private var particleRenderer: ParticleRenderer
    
    init {
        // Use OpenGL ES 3.0
        setEGLContextClientVersion(3)
        
        // Enable transparency
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        setZOrderOnTop(false)
        
        // Create and set renderer immediately - MUST be before view is attached
        particleRenderer = ParticleRenderer(500)
        setRenderer(particleRenderer)
        
        // Set render mode AFTER setRenderer
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    
    fun getParticleRenderer(): ParticleRenderer = particleRenderer
    
    fun setParticleCount(count: Int) {
        queueEvent {
            particleRenderer.setParticleCount(count)
        }
    }
    
    fun setColor(color: ParticleColor) {
        queueEvent {
            particleRenderer.setColor(color)
        }
    }
    
    fun setHandData(palmCenter: com.example.handparticle.gesture.Point3D?, openness: Float) {
        queueEvent {
            particleRenderer.setHandData(palmCenter, openness)
        }
    }
}
