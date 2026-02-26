package com.example.handparticle.particle

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.handparticle.ParticleColor
import com.example.handparticle.gesture.Point3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*
import kotlin.random.Random

class ParticleRenderer(private var particleCount: Int = 500) : GLSurfaceView.Renderer {
    
    // Particle data
    private lateinit var particles: Array<Particle>
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var colorBuffer: FloatBuffer
    
    // OpenGL handles
    private var programHandle = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0
    private var pointSizeHandle = 0
    
    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    
    // Hand interaction
    private var handPosition: Point3D? = null
    private var targetOpenness = 0.5f
    private var currentOpenness = 0.5f
    
    // Color
    private var particleColor = ParticleColor.PURPLE
    
    // Animation
    private var time = 0f
    
    // Sphere radius range
    private val minRadius = 0.3f  // Closed fist
    private val maxRadius = 1.5f  // Open hand
    
    companion object {
        private const val COORDS_PER_VERTEX = 3
        private const val COLORS_PER_VERTEX = 4
        
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform float uPointSize;
            attribute vec4 aPosition;
            attribute vec4 aColor;
            varying vec4 vColor;
            
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                gl_PointSize = uPointSize * (1.0 - gl_Position.z * 0.3);
                vColor = aColor;
            }
        """
        
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec4 vColor;
            
            void main() {
                // Create circular particles with soft edges
                vec2 coord = gl_PointCoord - vec2(0.5);
                float dist = length(coord);
                
                if (dist > 0.5) {
                    discard;
                }
                
                float alpha = smoothstep(0.5, 0.2, dist) * vColor.a;
                gl_FragColor = vec4(vColor.rgb, alpha);
            }
        """
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set clear color to white background
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        
        // Compile shaders
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        
        // Create program
        programHandle = GLES30.glCreateProgram().also { program ->
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)
        }
        
        // Get handles
        positionHandle = GLES30.glGetAttribLocation(programHandle, "aPosition")
        colorHandle = GLES30.glGetAttribLocation(programHandle, "aColor")
        mvpMatrixHandle = GLES30.glGetUniformLocation(programHandle, "uMVPMatrix")
        pointSizeHandle = GLES30.glGetUniformLocation(programHandle, "uPointSize")
        
        // Initialize particles
        initParticles()
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)
        
        // Set camera position
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 4f,  // Eye position
            0f, 0f, 0f,  // Look at
            0f, 1f, 0f   // Up vector
        )
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        
        time += 0.016f  // ~60fps
        
        // Smooth interpolation of openness
        currentOpenness += (targetOpenness - currentOpenness) * 0.1f
        
        // Update particles
        updateParticles()
        
        // Calculate MVP matrix with slight rotation for 3D effect
        val rotationMatrix = FloatArray(16)
        Matrix.setRotateM(rotationMatrix, 0, time * 10f, 0f, 1f, 0f)
        
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        // Draw particles
        GLES30.glUseProgram(programHandle)
        
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(pointSizeHandle, 35f)
        
        // Update buffers
        updateBuffers()
        
        // Enable vertex arrays
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glEnableVertexAttribArray(colorHandle)
        
        // Set vertex data
        GLES30.glVertexAttribPointer(
            positionHandle, COORDS_PER_VERTEX, GLES30.GL_FLOAT,
            false, COORDS_PER_VERTEX * 4, vertexBuffer
        )
        
        GLES30.glVertexAttribPointer(
            colorHandle, COLORS_PER_VERTEX, GLES30.GL_FLOAT,
            false, COLORS_PER_VERTEX * 4, colorBuffer
        )
        
        // Draw
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, particleCount)
        
        // Disable vertex arrays
        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(colorHandle)
    }
    
    private fun initParticles() {
        particles = Array(particleCount) { 
            createParticle(Random.nextFloat() * 2 * PI.toFloat())
        }
        
        // Initialize buffers
        vertexBuffer = ByteBuffer.allocateDirect(particleCount * COORDS_PER_VERTEX * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        
        colorBuffer = ByteBuffer.allocateDirect(particleCount * COLORS_PER_VERTEX * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }
    
    private fun createParticle(phase: Float): Particle {
        // Random position on sphere surface
        val theta = Random.nextFloat() * 2 * PI.toFloat()
        val phi = acos(2 * Random.nextFloat() - 1)
        
        return Particle(
            theta = theta,
            phi = phi,
            phase = phase,
            speed = 0.5f + Random.nextFloat() * 0.5f,
            size = 0.8f + Random.nextFloat() * 0.4f
        )
    }
    
    private fun updateParticles() {
        // Calculate target radius based on openness
        val targetRadius = minRadius + (maxRadius - minRadius) * currentOpenness
        
        particles.forEachIndexed { index, particle ->
            // Update phase for animation
            particle.phase += particle.speed * 0.02f
            
            // Slight wobble in position
            val wobble = sin(time * 2f + particle.phase) * 0.05f
            
            // Calculate position on sphere
            val radius = targetRadius + wobble
            particle.x = radius * sin(particle.phi) * cos(particle.theta + time * 0.3f * particle.speed)
            particle.y = radius * cos(particle.phi)
            particle.z = radius * sin(particle.phi) * sin(particle.theta + time * 0.3f * particle.speed)
            
            // If hand position is available, add attraction
            handPosition?.let { hand ->
                // Convert hand position to 3D space (-1 to 1 range)
                val handX = (hand.x - 0.5f) * 2f
                val handY = -(hand.y - 0.5f) * 2f
                
                // Slight attraction towards hand
                val attractionStrength = 0.1f * (1f - currentOpenness)
                particle.x += (handX - particle.x) * attractionStrength
                particle.y += (handY - particle.y) * attractionStrength
            }
        }
    }
    
    private fun updateBuffers() {
        vertexBuffer.position(0)
        colorBuffer.position(0)
        
        particles.forEach { particle ->
            // Position
            vertexBuffer.put(particle.x)
            vertexBuffer.put(particle.y)
            vertexBuffer.put(particle.z)
            
            // Color with variation
            val colorVariation = 0.8f + sin(particle.phase) * 0.2f
            val alpha = 0.6f + sin(particle.phase * 2f) * 0.3f
            
            colorBuffer.put(particleColor.r * colorVariation)
            colorBuffer.put(particleColor.g * colorVariation)
            colorBuffer.put(particleColor.b * colorVariation)
            colorBuffer.put(alpha)
        }
        
        vertexBuffer.position(0)
        colorBuffer.position(0)
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }
    
    // Public methods for interaction
    
    fun setHandData(position: Point3D?, openness: Float) {
        handPosition = position
        targetOpenness = openness
    }
    
    fun setColor(color: ParticleColor) {
        particleColor = color
    }
    
    fun setParticleCount(count: Int) {
        if (count != particleCount && count > 0) {
            particleCount = count
            initParticles()
        }
    }
    
    /**
     * Particle data class
     */
    data class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var z: Float = 0f,
        var theta: Float,
        var phi: Float,
        var phase: Float,
        var speed: Float,
        var size: Float
    )
}
