# 🖐️ Hand Particle System

Ứng dụng Android hiển thị hệ thống hạt 3D tương tác với cử chỉ tay sử dụng MediaPipe.

## ✨ Tính năng

- **Nhận diện cử chỉ tay realtime** - Sử dụng MediaPipe Hand Landmarker
- **Hệ thống hạt 3D** - Render bằng OpenGL ES 3.0
- **Điều khiển bằng cử chỉ:**
  - 🖐️ **Xòe tay** → Hạt mở rộng ra ngoài
  - ✊ **Nắm tay** → Hạt thu gọn thành hình cầu
- **Bộ chọn màu** - 8 màu sắc đẹp mắt
- **Điều chỉnh số lượng hạt** - 100 đến 1000 hạt
- **Giao diện hiện đại** - Dark theme, glassmorphism

## 📱 Yêu cầu

- Android SDK 24+ (Android 7.0 Nougat trở lên)
- Camera trước
- OpenGL ES 3.0

## 🚀 Cách build

### Mở trong Android Studio:

1. Mở Android Studio
2. File → Open → Chọn thư mục `HandParticleApp`
3. Đợi Gradle sync hoàn tất
4. Model MediaPipe sẽ tự động được download

### Build APK:

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

APK sẽ được tạo tại: `app/build/outputs/apk/`

## 📂 Cấu trúc Project

```
HandParticleApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/handparticle/
│   │   │   ├── MainActivity.kt           # Activity chính
│   │   │   ├── hand/
│   │   │   │   └── HandLandmarkerHelper.kt  # MediaPipe wrapper
│   │   │   ├── gesture/
│   │   │   │   └── GestureDetector.kt    # Nhận diện cử chỉ
│   │   │   └── particle/
│   │   │       ├── ParticleRenderer.kt   # OpenGL renderer
│   │   │       └── ParticleGLSurfaceView.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   ├── drawable/                 # UI drawables
│   │   │   └── values/                   # Colors, strings, themes
│   │   └── assets/
│   │       └── hand_landmarker.task      # MediaPipe model (auto-download)
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## 🎨 Cách hoạt động

```
Camera → CameraX → ImageAnalysis
                        ↓
              MediaPipe Hand Landmarker
                        ↓
              21 Hand Landmarks
                        ↓
              GestureDetector
              (Detect: Open/Closed)
                        ↓
              ParticleRenderer (OpenGL)
              - Tính toán vị trí hạt
              - Scale sphere radius
              - Render với point sprites
```

## 🔧 Tùy chỉnh

### Thay đổi số lượng hạt mặc định:
```kotlin
// MainActivity.kt
private var particleCount = 500  // Thay đổi giá trị này
```

### Thêm màu mới:
```kotlin
// MainActivity.kt
enum class ParticleColor(val r: Float, val g: Float, val b: Float) {
    // Thêm màu mới ở đây
    NEW_COLOR(0.5f, 0.5f, 0.5f),
    ...
}
```

### Điều chỉnh độ nhạy gesture:
```kotlin
// GestureDetector.kt
const val FINGER_EXTENDED_THRESHOLD = 0.06f  // Giảm = nhạy hơn
const val FIST_THRESHOLD = 0.12f
```

## 📝 License

MIT License

## 🙏 Credits

- [MediaPipe](https://developers.google.com/mediapipe) - Hand Landmarker
- [CameraX](https://developer.android.com/training/camerax) - Camera API
- OpenGL ES 3.0 - 3D Rendering
