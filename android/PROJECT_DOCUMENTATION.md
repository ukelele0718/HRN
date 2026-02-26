# 📱 Hand Particle App - Tài liệu Dự án

## 📋 Tổng quan

**Hand Particle App** là ứng dụng Android sử dụng AI để nhận diện cử chỉ tay và điều khiển hệ thống hạt 3D trong thời gian thực.

- **Xòe tay** → Hạt mở rộng ra
- **Nắm tay** → Hạt thu gọn thành hình cầu

---

## 🛠️ Công nghệ sử dụng

### 1. **Ngôn ngữ & Framework**

| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|----------|
| **Kotlin** | 1.9.22 | Ngôn ngữ lập trình chính |
| **Android SDK** | compileSdk 34, minSdk 24 | Nền tảng phát triển |
| **Gradle** | 8.7 | Build system |
| **Android Gradle Plugin** | 8.4.0 | Plugin build Android |

### 2. **Thư viện AI/ML**

| Thư viện | Phiên bản | Mục đích |
|----------|-----------|----------|
| **MediaPipe Tasks Vision** | 0.10.9 | Nhận diện hand landmarks (21 điểm trên bàn tay) |
| **MediaPipe Hand Landmarker** | Model | Mô hình AI phát hiện bàn tay |

### 3. **Camera & Graphics**

| Thư viện | Phiên bản | Mục đích |
|----------|-----------|----------|
| **CameraX** | 1.3.1 | Xử lý camera (camera2 API wrapper) |
| **OpenGL ES** | 3.0 | Render đồ họa 3D |
| **GLSurfaceView** | Android SDK | View hiển thị OpenGL |

### 4. **Android Jetpack**

| Thư viện | Mục đích |
|----------|----------|
| **ViewBinding** | Type-safe binding UI |
| **AppCompat** | Hỗ trợ tương thích ngược |
| **CardView** | UI component cho camera preview |
| **ConstraintLayout** | Layout linh hoạt |

---

## 📁 Cấu trúc dự án

```
HandParticleApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/handparticle/
│   │   │   ├── MainActivity.kt              # Activity chính
│   │   │   ├── gesture/
│   │   │   │   ├── GestureDetector.kt       # Phát hiện cử chỉ tay
│   │   │   │   ├── HandGesture.kt           # Enum các loại cử chỉ
│   │   │   │   └── Point3D.kt               # Data class điểm 3D
│   │   │   ├── hand/
│   │   │   │   └── HandLandmarkerHelper.kt  # Wrapper MediaPipe
│   │   │   └── particle/
│   │   │       ├── ParticleRenderer.kt      # OpenGL renderer
│   │   │       └── ParticleGLSurfaceView.kt # Custom GLSurfaceView
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml        # Layout chính
│   │   │   └── drawable/
│   │   │       ├── panel_background.xml     # Background panel
│   │   │       ├── color_swatch_*.xml       # Color picker swatches
│   │   │       └── ...
│   │   ├── assets/
│   │   │   └── hand_landmarker.task         # MediaPipe model
│   │   └── AndroidManifest.xml
│   └── build.gradle                         # App dependencies
├── build.gradle                             # Project config
├── settings.gradle                          # Project settings
└── gradle/wrapper/
    └── gradle-wrapper.properties            # Gradle version
```

---

## 🔄 Luồng hoạt động

### 1. **Luồng khởi động ứng dụng**

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity.onCreate()                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Kiểm tra quyền Camera (CAMERA permission)       │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
        [Có quyền]                      [Chưa có quyền]
              │                               │
              │                               ▼
              │                    ┌─────────────────────┐
              │                    │ Yêu cầu cấp quyền   │
              │                    └─────────────────────┘
              │                               │
              ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   initializeComponents()                     │
│  • Khởi tạo ParticleGLSurfaceView + ParticleRenderer        │
│  • Khởi tạo HandLandmarkerHelper                            │
│  • Bắt đầu Camera                                           │
└─────────────────────────────────────────────────────────────┘
```

### 2. **Luồng xử lý Camera & AI**

```
┌─────────────────┐
│  CameraX        │
│  PreviewView    │
└────────┬────────┘
         │ Frame hình ảnh
         ▼
┌─────────────────────────────────────────────────────────────┐
│               HandLandmarkerHelper                           │
│  • Nhận frame từ camera (ImageProxy)                        │
│  • Convert sang MPImage                                      │
│  • Gọi MediaPipe Hand Landmarker                            │
└─────────────────────────────────────────────────────────────┘
         │ HandLandmarkerResult (21 landmarks)
         ▼
┌─────────────────────────────────────────────────────────────┐
│                   GestureDetector                            │
│  • Phân tích 21 điểm landmark                               │
│  • Tính khoảng cách ngón tay - lòng bàn tay                 │
│  • Xác định: OPEN_HAND / CLOSED_FIST / NONE                 │
│  • Tính openness (0.0 - 1.0)                                │
└─────────────────────────────────────────────────────────────┘
         │ HandGesture + openness
         ▼
┌─────────────────────────────────────────────────────────────┐
│                   MainActivity.onResults()                   │
│  • Cập nhật UI (gesture icon, text)                         │
│  • Gửi dữ liệu đến ParticleRenderer                         │
└─────────────────────────────────────────────────────────────┘
```

### 3. **Luồng render hạt 3D (OpenGL ES)**

```
┌─────────────────────────────────────────────────────────────┐
│                 ParticleGLSurfaceView                        │
│  • Continuous rendering mode                                 │
│  • OpenGL ES 3.0 context                                     │
└─────────────────────────────────────────────────────────────┘
         │ onDrawFrame() ~60fps
         ▼
┌─────────────────────────────────────────────────────────────┐
│                   ParticleRenderer                           │
├─────────────────────────────────────────────────────────────┤
│  1. updateParticles()                                        │
│     • Tính bán kính dựa trên openness                       │
│       - Nắm tay (0.0) → radius = 0.3                        │
│       - Xòe tay (1.0) → radius = 1.5                        │
│     • Cập nhật vị trí mỗi hạt trên mặt cầu                  │
│     • Thêm hiệu ứng wobble animation                        │
│                                                              │
│  2. updateBuffers()                                          │
│     • Cập nhật vertex buffer (vị trí xyz)                   │
│     • Cập nhật color buffer (rgba)                          │
│                                                              │
│  3. Draw OpenGL                                              │
│     • Vertex Shader: tính vị trí + point size               │
│     • Fragment Shader: vẽ hạt tròn với soft edges           │
│     • GL_POINTS với blending                                │
└─────────────────────────────────────────────────────────────┘
```

### 4. **Luồng tương tác người dùng**

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Interactions                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  [Color Picker]                                              │
│       │                                                      │
│       ▼                                                      │
│  ParticleRenderer.setColor(ParticleColor)                   │
│       │                                                      │
│       ▼                                                      │
│  Thay đổi màu sắc hạt (PURPLE/BLUE/CYAN/GREEN/...)          │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  [Particle Count Slider]                                     │
│       │                                                      │
│       ▼                                                      │
│  ParticleRenderer.setParticleCount(100-1000)                │
│       │                                                      │
│       ▼                                                      │
│  Khởi tạo lại mảng particles + buffers                      │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  [Toggle Panel Button ▲/▼]                                   │
│       │                                                      │
│       ▼                                                      │
│  Ẩn/hiện phần collapsibleContent                            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Thuật toán chính

### 1. **Phát hiện cử chỉ tay (GestureDetector)**

```kotlin
// Tính khoảng cách trung bình từ đầu ngón tay đến lòng bàn tay
val fingerTips = [INDEX_TIP, MIDDLE_TIP, RING_TIP, PINKY_TIP]
val avgDistance = fingerTips.map { distance(it, WRIST) }.average()

// So sánh với ngưỡng
when {
    avgDistance > OPEN_THRESHOLD   → OPEN_HAND
    avgDistance < CLOSED_THRESHOLD → CLOSED_FIST
    else                           → NONE
}
```

### 2. **Vị trí hạt trên mặt cầu (Spherical Coordinates)**

```kotlin
// Chuyển từ tọa độ cầu sang Cartesian
x = radius * sin(phi) * cos(theta)
y = radius * cos(phi)
z = radius * sin(phi) * sin(theta)

// radius thay đổi theo openness
radius = minRadius + (maxRadius - minRadius) * openness
```

### 3. **Particle Soft Edge (Fragment Shader)**

```glsl
// Tạo hạt tròn với viền mềm
vec2 coord = gl_PointCoord - vec2(0.5);
float dist = length(coord);
float alpha = smoothstep(0.5, 0.2, dist);
```

---

## 📊 MediaPipe Hand Landmarks

```
         WRIST (0)
            │
    ┌───────┼───────┬───────┬───────┐
    │       │       │       │       │
 THUMB   INDEX   MIDDLE   RING   PINKY
  (1-4)  (5-8)   (9-12)  (13-16) (17-20)

21 điểm landmark được MediaPipe phát hiện:
- Mỗi ngón tay: 4 điểm (CMC, MCP, IP/PIP, TIP)
- Cổ tay: 1 điểm (WRIST)
```

---

## ⚡ Performance

| Metric | Giá trị |
|--------|---------|
| Target FPS | 60 fps |
| Hand detection latency | ~30-50ms |
| Particle count | 100 - 1000 |
| OpenGL render mode | Continuous |

---

## 📱 Yêu cầu hệ thống

- **Android**: 7.0+ (API 24)
- **Camera**: Cần camera trước hoặc sau
- **GPU**: Hỗ trợ OpenGL ES 3.0
- **RAM**: Tối thiểu 2GB

---

## 🔧 Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# APK output
app/build/outputs/apk/debug/app-debug.apk
```

Hoặc sử dụng Android Studio:
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. **Run → Run 'app'** (với thiết bị kết nối)

---

## 📝 Ghi chú kỹ thuật

1. **MediaPipe model** (`hand_landmarker.task`) được đặt trong `assets/` và load lúc runtime

2. **CameraX** sử dụng `RUNNING_MODE.LIVE_STREAM` để xử lý frame liên tục

3. **OpenGL ES 3.0** được chọn để có hiệu năng tốt và hỗ trợ rộng rãi trên các thiết bị Android

4. **ViewBinding** được sử dụng thay vì `findViewById()` để type-safe và hiệu quả hơn

5. **Render thread** độc lập với UI thread, giao tiếp qua `queueEvent()` nếu cần

---

# 📚 PHẦN 2: GIẢI THÍCH CHI TIẾT

## 🔤 Kotlin là gì?

### Định nghĩa
**Kotlin** là ngôn ngữ lập trình hiện đại, được phát triển bởi **JetBrains** (công ty tạo ra IntelliJ IDEA). Google đã công nhận Kotlin là ngôn ngữ chính thức cho phát triển Android từ năm 2017.

### Tại sao chọn Kotlin thay vì Java?

| Đặc điểm | Kotlin | Java |
|----------|--------|------|
| **Null Safety** | Có (compile-time) | Không (runtime exception) |
| **Code ngắn gọn** | Giảm ~40% code | Verbose |
| **Data classes** | 1 dòng | 50+ dòng boilerplate |
| **Coroutines** | Native support | Cần thư viện ngoài |
| **Extension functions** | Có | Không |

### Ví dụ so sánh

**Java (truyền thống):**
```java
public class Particle {
    private float x, y, z;
    private float speed;
    
    public Particle(float x, float y, float z, float speed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.speed = speed;
    }
    
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    // ... 6 methods nữa cho y, z, speed
}
```

**Kotlin (hiện đại):**
```kotlin
data class Particle(
    var x: Float,
    var y: Float, 
    var z: Float,
    var speed: Float
)
// Chỉ 1 dòng! Kotlin tự generate getter, setter, equals(), hashCode(), toString()
```

### Null Safety trong dự án

```kotlin
// Biến có thể null - phải khai báo với ?
private var handLandmarkerHelper: HandLandmarkerHelper? = null

// Safe call - chỉ gọi nếu không null
handLandmarkerHelper?.startCamera()

// Elvis operator - giá trị mặc định nếu null
val count = particleCount ?: 500
```

---

## 🤖 MediaPipe là gì?

### Định nghĩa
**MediaPipe** là framework AI/ML mã nguồn mở của **Google**, cung cấp các giải pháp ML có thể chạy trực tiếp trên thiết bị (on-device) mà không cần kết nối internet.

### Cách MediaPipe Hand Landmarker hoạt động

```
┌─────────────────────────────────────────────────────────────────────┐
│                    MEDIAPIPE HAND LANDMARKER                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  BƯỚC 1: Palm Detection (Phát hiện lòng bàn tay)                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Input: Frame hình ảnh từ camera                          │    │
│  │  • Model: BlazePalm (neural network nhẹ)                    │    │
│  │  • Output: Bounding box của bàn tay                         │    │
│  │  • Tốc độ: ~10ms trên thiết bị di động                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                       │
│                              ▼                                       │
│  BƯỚC 2: Hand Landmark Detection (Phát hiện 21 điểm)                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Input: Vùng ảnh chứa bàn tay (crop từ bước 1)            │    │
│  │  • Model: Hand Landmark Model                                │    │
│  │  • Output: 21 điểm 3D (x, y, z) trên bàn tay                │    │
│  │  • Độ chính xác: ~95% trong điều kiện tốt                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                       │
│                              ▼                                       │
│  BƯỚC 3: Tracking (Theo dõi liên tục)                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Sử dụng kết quả frame trước để dự đoán vị trí mới        │    │
│  │  • Giảm latency vì không cần detect lại từ đầu              │    │
│  │  • Tự động re-detect khi mất tracking                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 21 Hand Landmarks chi tiết

```
                    ┌── TIP (đầu ngón)
                    │
              ┌─────┴─────┐
              │           │
              │   DIP     │ ← Khớp xa (Distal Interphalangeal)
              │           │
              ├───────────┤
              │           │
              │   PIP     │ ← Khớp giữa (Proximal Interphalangeal)
              │           │
              ├───────────┤
              │           │
              │   MCP     │ ← Khớp gốc (Metacarpophalangeal)
              │           │
              └─────┬─────┘
                    │
                    ▼
              ┌───────────┐
              │   WRIST   │ ← Cổ tay (điểm 0)
              └───────────┘

Các điểm landmark:
┌─────────────┬────────────────────────────────────┐
│ Index       │ Tên điểm                           │
├─────────────┼────────────────────────────────────┤
│ 0           │ WRIST (cổ tay)                     │
│ 1-4         │ THUMB (ngón cái): CMC, MCP, IP, TIP│
│ 5-8         │ INDEX (ngón trỏ): MCP, PIP, DIP, TIP│
│ 9-12        │ MIDDLE (ngón giữa)                 │
│ 13-16       │ RING (ngón áp út)                  │
│ 17-20       │ PINKY (ngón út)                    │
└─────────────┴────────────────────────────────────┘
```

---

## 🧮 Giải thích thuật toán chi tiết

### 1. Thuật toán phát hiện cử chỉ (GestureDetector)

#### Nguyên lý hoạt động

Để phân biệt **xòe tay** và **nắm tay**, ta đo khoảng cách từ **đầu ngón tay** (TIP) đến **cổ tay** (WRIST):

- **Xòe tay**: Các ngón duỗi thẳng → khoảng cách TIP-WRIST **lớn**
- **Nắm tay**: Các ngón co lại → khoảng cách TIP-WRIST **nhỏ**

```
XÒE TAY (OPEN_HAND)                    NẮM TAY (CLOSED_FIST)
                                       
      8 (INDEX_TIP)                           8
     /                                       /|
    /   12 (MIDDLE_TIP)                   12 ||
   |   /                                    \||
   |  |   16 (RING_TIP)                      \|  ← Các ngón co lại
   |  |  /                                    |     gần cổ tay
   |  | |   20 (PINKY_TIP)                    |
   |  | |  /                                  |
   |  | | |                                   |
   |__|_|_|                                  _|_
      0 (WRIST)                               0
      
Khoảng cách: LỚN                     Khoảng cách: NHỎ
```

#### Code giải thích

```kotlin
fun detectGesture(landmarks: List<NormalizedLandmark>): HandGesture {
    // Lấy vị trí cổ tay (điểm gốc)
    val wrist = landmarks[0]  // Index 0 = WRIST
    
    // Lấy vị trí đầu các ngón tay
    val fingerTips = listOf(
        landmarks[8],   // INDEX_FINGER_TIP
        landmarks[12],  // MIDDLE_FINGER_TIP  
        landmarks[16],  // RING_FINGER_TIP
        landmarks[20]   // PINKY_TIP
    )
    
    // Tính khoảng cách trung bình từ đầu ngón đến cổ tay
    val avgDistance = fingerTips.map { tip ->
        calculateDistance(tip, wrist)
    }.average()
    
    // So sánh với ngưỡng để xác định cử chỉ
    return when {
        avgDistance > 0.35 -> HandGesture.OPEN_HAND    // Ngón duỗi xa
        avgDistance < 0.20 -> HandGesture.CLOSED_FIST  // Ngón co gần
        else -> HandGesture.NONE                        // Không rõ ràng
    }
}

// Công thức tính khoảng cách Euclidean 3D
fun calculateDistance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    val dz = p1.z - p2.z
    return sqrt(dx*dx + dy*dy + dz*dz)  // √(Δx² + Δy² + Δz²)
}
```

#### Tính độ mở của bàn tay (Openness)

```kotlin
fun calculateOpenness(landmarks: List<NormalizedLandmark>): Float {
    // Khoảng cách hiện tại
    val currentDistance = calculateAverageFingerDistance(landmarks)
    
    // Normalize về khoảng 0.0 - 1.0
    val minDist = 0.15f  // Nắm chặt nhất
    val maxDist = 0.45f  // Xòe rộng nhất
    
    val openness = (currentDistance - minDist) / (maxDist - minDist)
    
    // Clamp giá trị trong khoảng [0, 1]
    return openness.coerceIn(0f, 1f)
}
```

**Kết quả:**
- `openness = 0.0` → Nắm chặt hoàn toàn
- `openness = 0.5` → Nửa mở
- `openness = 1.0` → Xòe hoàn toàn

---

### 2. Thuật toán vị trí hạt trên mặt cầu (Spherical Coordinates)

#### Hệ tọa độ cầu

Thay vì dùng tọa độ Cartesian (x, y, z), ta dùng **tọa độ cầu** (r, θ, φ) để dễ dàng phân bố hạt đều trên mặt cầu:

```
                    y (up)
                    │
                    │     P(r, θ, φ)
                    │    /
                    │   /
                    │  /  φ (phi) = góc với trục y
                    │ /
                    │/θ (theta) = góc trên mặt phẳng xz
        ────────────┼────────────── x
                   /│
                  / │
                 /  │
                z   │

Công thức chuyển đổi:
┌─────────────────────────────────────────┐
│  x = r × sin(φ) × cos(θ)                │
│  y = r × cos(φ)                         │
│  z = r × sin(φ) × sin(θ)                │
└─────────────────────────────────────────┘
```

#### Tại sao dùng tọa độ cầu?

1. **Dễ phân bố đều**: Chỉ cần random θ và φ
2. **Dễ thay đổi bán kính**: Chỉ cần thay đổi r
3. **Animation mượt**: Xoay θ theo thời gian tạo hiệu ứng quay

#### Code chi tiết

```kotlin
private fun createParticle(phase: Float): Particle {
    // Random góc theta (0 đến 2π) - góc ngang
    val theta = Random.nextFloat() * 2 * PI.toFloat()
    
    // Random góc phi - sử dụng acos để phân bố đều
    // acos(2*random - 1) cho phân bố đều trên mặt cầu
    val phi = acos(2 * Random.nextFloat() - 1)
    
    return Particle(
        theta = theta,
        phi = phi,
        phase = phase,           // Pha animation riêng
        speed = 0.5f + Random.nextFloat() * 0.5f  // Tốc độ random
    )
}

private fun updateParticles() {
    // Bán kính thay đổi theo độ mở tay
    // openness: 0.0 (nắm) → 1.0 (xòe)
    val targetRadius = minRadius + (maxRadius - minRadius) * currentOpenness
    // minRadius = 0.3 (hạt tụ lại)
    // maxRadius = 1.5 (hạt tỏa ra)
    
    particles.forEach { particle ->
        // Thêm wobble (dao động nhẹ) cho tự nhiên
        val wobble = sin(time * 2f + particle.phase) * 0.05f
        val radius = targetRadius + wobble
        
        // Tính vị trí Cartesian từ tọa độ cầu
        // theta xoay theo thời gian để tạo hiệu ứng quay
        val animatedTheta = particle.theta + time * 0.3f * particle.speed
        
        particle.x = radius * sin(particle.phi) * cos(animatedTheta)
        particle.y = radius * cos(particle.phi)
        particle.z = radius * sin(particle.phi) * sin(animatedTheta)
    }
}
```

#### Minh họa bán kính thay đổi

```
openness = 0.0 (NẮM TAY)          openness = 1.0 (XÒE TAY)

        ┌───┐                           ╭─────────╮
       ╱     ╲                        ╱           ╲
      │ · · · │                      │  ·       ·  │
      │ ····· │                      │    ·   ·    │
      │ · · · │                      │  ·       ·  │
       ╲     ╱                        ╲           ╱
        └───┘                           ╰─────────╯
     
     radius = 0.3                      radius = 1.5
     Hạt tụ lại gần nhau               Hạt tỏa ra xa
```

---

### 3. OpenGL ES Rendering Pipeline

#### Pipeline tổng quan

```
┌─────────────────────────────────────────────────────────────────────┐
│                    OPENGL ES RENDERING PIPELINE                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐           │
│  │   CPU        │───▶│  Vertex      │───▶│  Fragment    │           │
│  │   (Kotlin)   │    │  Shader      │    │  Shader      │           │
│  │              │    │  (GPU)       │    │  (GPU)       │           │
│  └──────────────┘    └──────────────┘    └──────────────┘           │
│        │                   │                   │                     │
│        ▼                   ▼                   ▼                     │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐           │
│  │ Particle     │    │ Tính vị trí  │    │ Tính màu     │           │
│  │ positions    │    │ trên màn hình│    │ từng pixel   │           │
│  │ + colors     │    │ + point size │    │ + alpha      │           │
│  └──────────────┘    └──────────────┘    └──────────────┘           │
│                                                │                     │
│                                                ▼                     │
│                                          ┌──────────────┐           │
│                                          │  Framebuffer │           │
│                                          │  (Màn hình)  │           │
│                                          └──────────────┘           │
└─────────────────────────────────────────────────────────────────────┘
```

#### Vertex Shader giải thích

```glsl
// Vertex Shader - chạy 1 lần cho MỖI đỉnh (particle)
uniform mat4 uMVPMatrix;    // Ma trận Model-View-Projection
uniform float uPointSize;    // Kích thước điểm cơ bản

attribute vec4 aPosition;    // Vị trí particle (x, y, z)
attribute vec4 aColor;       // Màu particle (r, g, b, a)

varying vec4 vColor;         // Truyền màu sang Fragment Shader

void main() {
    // Chuyển đổi vị trí 3D → vị trí 2D trên màn hình
    gl_Position = uMVPMatrix * aPosition;
    
    // Kích thước điểm thay đổi theo độ sâu (z)
    // Hạt xa hơn (z lớn) sẽ nhỏ hơn → tạo cảm giác 3D
    gl_PointSize = uPointSize * (1.0 - gl_Position.z * 0.3);
    
    // Truyền màu sang fragment shader
    vColor = aColor;
}
```

#### Fragment Shader giải thích

```glsl
// Fragment Shader - chạy 1 lần cho MỖI pixel của particle
precision mediump float;
varying vec4 vColor;    // Màu từ Vertex Shader

void main() {
    // gl_PointCoord: tọa độ trong point sprite (0,0) → (1,1)
    //
    //  (0,0) ──────── (1,0)
    //    │              │
    //    │   (0.5,0.5)  │  ← Tâm
    //    │      ·       │
    //    │              │
    //  (0,1) ──────── (1,1)
    
    // Tính khoảng cách từ tâm
    vec2 coord = gl_PointCoord - vec2(0.5);  // Shift về tâm
    float dist = length(coord);               // Khoảng cách đến tâm
    
    // Loại bỏ pixel ngoài hình tròn
    if (dist > 0.5) {
        discard;  // Không vẽ pixel này
    }
    
    // Tạo viền mềm (soft edge) với smoothstep
    // smoothstep(edge0, edge1, x): 
    //   - x < edge0 → 0
    //   - x > edge1 → 1  
    //   - Nội suy mượt giữa 0 và 1
    float alpha = smoothstep(0.5, 0.2, dist) * vColor.a;
    
    // Output màu cuối cùng
    gl_FragColor = vec4(vColor.rgb, alpha);
}
```

#### Minh họa soft edge

```
Không có soft edge:              Có soft edge (smoothstep):

    ┌─────────────┐                   ╭─────────────╮
    │█████████████│                 ╱░░░░░█████░░░░░╲
    │█████████████│               │░░░░█████████░░░░│
    │█████████████│               │░░█████████████░░│
    │█████████████│               │░░█████████████░░│
    │█████████████│               │░░░░█████████░░░░│
    └─────────────┘                 ╲░░░░░█████░░░░░╱
                                      ╰─────────────╯
                                   
    Cạnh sắc, thô                  Cạnh mềm, mượt
    (alpha = 1 hoặc 0)             (alpha gradient)
```

---

### 4. Ma trận MVP (Model-View-Projection)

#### Tại sao cần ma trận MVP?

Để hiển thị vật thể 3D lên màn hình 2D, cần 3 phép biến đổi:

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  Tọa độ          Model         View           Projection   Màn hình │
│  Local    ────▶  Matrix  ────▶ Matrix   ────▶  Matrix  ────▶ 2D     │
│  (Object)        (World)       (Camera)        (Screen)             │
│                                                                      │
│  Ví dụ:          Đặt vật      Camera          Chiếu 3D    Pixel    │
│  (0,0,0)         vào thế      nhìn từ         lên 2D      (x,y)    │
│                  giới 3D      đâu                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### Code trong dự án

```kotlin
// 1. Projection Matrix - xác định "lens" của camera
Matrix.frustumM(
    projectionMatrix, 0,
    -ratio, ratio,     // Left, Right
    -1f, 1f,           // Bottom, Top  
    1f, 10f            // Near, Far (clipping planes)
)

// 2. View Matrix - vị trí và hướng nhìn camera
Matrix.setLookAtM(
    viewMatrix, 0,
    0f, 0f, 4f,    // Eye: Camera ở vị trí (0, 0, 4)
    0f, 0f, 0f,    // Center: Nhìn vào gốc tọa độ
    0f, 1f, 0f     // Up: Hướng lên là trục Y
)

// 3. Model Matrix - xoay vật thể (animation)
Matrix.setRotateM(
    rotationMatrix, 0,
    time * 10f,        // Góc xoay (thay đổi theo thời gian)
    0f, 1f, 0f         // Xoay quanh trục Y
)

// Kết hợp: MVP = Projection × View × Model
Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, rotationMatrix, 0)
Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
```

---

## 📷 CameraX là gì?

### Định nghĩa
**CameraX** là thư viện Jetpack giúp đơn giản hóa việc sử dụng camera trên Android. Nó xây dựng trên Camera2 API nhưng dễ sử dụng hơn nhiều.

### Tại sao dùng CameraX thay vì Camera2?

| Tiêu chí | CameraX | Camera2 |
|----------|---------|---------|
| **Độ phức tạp** | Đơn giản | Rất phức tạp |
| **Lines of code** | ~50 dòng | ~500+ dòng |
| **Xử lý lifecycle** | Tự động | Manual |
| **Tương thích thiết bị** | Tốt (thư viện lo) | Phải tự xử lý |

### Use Cases trong CameraX

```kotlin
// 1. Preview Use Case - hiển thị camera
val preview = Preview.Builder().build()
preview.setSurfaceProvider(previewView.surfaceProvider)

// 2. ImageAnalysis Use Case - phân tích frame cho AI
val imageAnalyzer = ImageAnalysis.Builder()
    .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
    .build()
    
imageAnalyzer.setAnalyzer(executor) { imageProxy ->
    // Gửi frame cho MediaPipe xử lý
    handLandmarker.detect(imageProxy)
    imageProxy.close()
}

// 3. Bind to lifecycle - tự động start/stop theo Activity
cameraProvider.bindToLifecycle(
    lifecycleOwner,
    cameraSelector,
    preview,
    imageAnalyzer
)
```

---

## 🎨 Blending trong OpenGL

### Alpha Blending là gì?

Khi vẽ các hạt chồng lên nhau, cần **blend** (pha trộn) màu để tạo hiệu ứng trong suốt:

```kotlin
GLES30.glEnable(GLES30.GL_BLEND)
GLES30.glBlendFunc(
    GLES30.GL_SRC_ALPHA,           // Source factor
    GLES30.GL_ONE_MINUS_SRC_ALPHA  // Destination factor
)
```

### Công thức blending

```
Final Color = (Source Color × SRC_ALPHA) + (Dest Color × (1 - SRC_ALPHA))

Ví dụ: Hạt đỏ (alpha=0.5) vẽ lên nền xanh:
- Source: (1, 0, 0, 0.5)  ← Đỏ, 50% trong suốt
- Dest:   (0, 1, 0, 1)    ← Xanh lá
- Final:  (0.5, 0.5, 0)   ← Vàng (pha trộn đỏ + xanh)
```

---

## 🔄 Lifecycle trong Android

### Activity Lifecycle và GLSurfaceView

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ANDROID LIFECYCLE                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  onCreate() ──▶ Camera permission ──▶ initializeComponents()        │
│       │                                      │                       │
│       ▼                                      ▼                       │
│  onStart() ◀───────────────────────── Setup GLSurfaceView          │
│       │                                      │                       │
│       ▼                                      ▼                       │
│  onResume() ──────────────────────▶ GLSurfaceView.onResume()        │
│       │                             (Bắt đầu render)                │
│       │                                                              │
│  [APP ĐANG CHẠY - Render loop 60fps]                                │
│       │                                                              │
│       ▼                                                              │
│  onPause() ───────────────────────▶ GLSurfaceView.onPause()         │
│       │                             (Dừng render)                   │
│       ▼                                                              │
│  onDestroy() ─────────────────────▶ Cleanup resources               │
│                                     (Release MediaPipe)             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Data Flow tổng hợp

```
┌─────────────────────────────────────────────────────────────────────┐
│                    COMPLETE DATA FLOW                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Camera ───▶ CameraX ───▶ ImageProxy ───▶ MediaPipe                 │
│  (Hardware)  (Library)    (Frame)         (AI Model)                │
│                                              │                       │
│                                              ▼                       │
│                                       21 Landmarks                   │
│                                       (x, y, z) × 21                │
│                                              │                       │
│                                              ▼                       │
│                                     GestureDetector                  │
│                                     - detectGesture()                │
│                                     - calculateOpenness()            │
│                                              │                       │
│                              ┌───────────────┼───────────────┐       │
│                              ▼               ▼               ▼       │
│                         HandGesture     openness        palmCenter   │
│                         (enum)          (0.0-1.0)       (x, y, z)   │
│                              │               │               │       │
│                              ▼               ▼               ▼       │
│                    ┌─────────────────────────────────────────┐       │
│                    │          ParticleRenderer               │       │
│                    │  - setHandData(palmCenter, openness)    │       │
│                    │  - Update particle positions            │       │
│                    │  - Render with OpenGL ES               │       │
│                    └─────────────────────────────────────────┘       │
│                                              │                       │
│                                              ▼                       │
│                                     ┌───────────────┐                │
│                                     │   Display     │                │
│                                     │   (Screen)    │                │
│                                     └───────────────┘                │
│                                                                      │
│  UI Thread ◀──────────────────────────────────────────────────────  │
│       │                                                              │
│       ▼                                                              │
│  updateGestureDisplay()                                              │
│  - Cập nhật icon (🖐️/✊/👋)                                          │
│  - Cập nhật text                                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📚 Thuật ngữ quan trọng

| Thuật ngữ | Giải thích |
|-----------|------------|
| **Landmark** | Điểm đặc trưng trên bàn tay (21 điểm) |
| **Normalized** | Giá trị được chuẩn hóa về khoảng 0-1 |
| **Shader** | Chương trình chạy trên GPU để render |
| **Vertex** | Đỉnh trong đồ họa 3D |
| **Fragment/Pixel** | Điểm ảnh trên màn hình |
| **Buffer** | Vùng nhớ chứa dữ liệu để gửi lên GPU |
| **MVP Matrix** | Model-View-Projection: biến đổi 3D→2D |
| **Blend** | Pha trộn màu khi vẽ chồng lên nhau |
| **Alpha** | Độ trong suốt (0=trong suốt, 1=đục) |
| **Spherical Coords** | Tọa độ cầu (r, θ, φ) |
| **Cartesian Coords** | Tọa độ Descartes (x, y, z) |

---

*Tài liệu được tạo: 25/12/2024*
