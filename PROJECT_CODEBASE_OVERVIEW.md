# Tài liệu tổng quan mã nguồn (dành cho người mới)

Tệp này giải thích cấu trúc chính và luồng chạy của dự án, giúp bạn tìm hiểu nhanh từ cả phía JavaScript lẫn Android native.

---

## 1. Gốc dự án
- `package.json` định nghĩa tên, phiên bản và 3 script quen thuộc giúp chạy Metro (`start`), dựng Android (`android`) và dọn dẹp (`clean`).
- `index.js` chỉ đăng ký component `RnEntry` để React Native biết bắt đầu từ đâu.
- `App.js` là màn hình chính React Native: ba nút gọi đến module `NativeLauncher`, một hộp hướng dẫn và giao diện tối với các nút màu rõ ràng.

## 2. Cầu nối JavaScript ↔ Native
- `NativeLauncherModule.kt` khai báo ba hàm `openNativeMain`, `openNumberInput`, `openTextInput` để mở tương ứng `MainActivity`, `NumberInputActivity`, `TextInputActivity` từ JavaScript bên React Native.
- `NativeLauncherPackage.kt` đăng ký module đó với React (có trong thư mục `java/com/example/handparticle`).
- `MainApplication.kt` thêm gói này vào danh sách ReactPackages để không cần chỉnh gì thêm ở JavaScript.

## 3. Kiến trúc Android native
### 3.1 Entry point
- `RnEntryActivity.kt` là `ReactActivity` mặc định (ném tới App React Native) và chạy khi mở app.
- `AndroidManifest.xml` cho biết activity nào là launcher (React) còn các hoạt động khác luôn được khai báo trong chế độ giao diện phong cảnh bị khóa.

### 3.2 Hoạt động chính
- `MainActivity.kt` là khu vực "particle":
  - Hiện giao diện GLSurfaceView (thư mục `particle`), camera preview, bảng điều khiển mở rộng/thu gọn, slider số hạt.
  - Tích hợp `HandLandmarkerHelper` và `GestureDetector` để lấy tọa độ bàn tay từ MediaPipe rồi truyền vào `ParticleRenderer`.
  - Các nút chọn màu tương ứng với `ParticleColor`.
- `NumberInputActivity.kt` và `TextInputActivity.kt` dùng `PoseLandmarkerHelper` + `PoseGestureDetector` để dịch chuyển con trỏ trên bàn phím 3x3 hoặc QWERTY thông qua các cử chỉ tay kiểu mũi tên và vòng tay chữ O.

### 3.3 Thư viện hỗ trợ
- `gesture/GestureDetector.kt` xử lý các ngón tay từ dữ liệu phản hồi của MediaPipe, trả ra trạng thái mở/tụt để thay đổi số lượng hạt, đồng thời cung cấp độ mở để tạo hiệu ứng cầu.
- `hand/HandLandmarkerHelper.kt` khởi tạo MediaPipe Hand Landmarker, kết nối CameraX và báo kết quả ngược lại cho `MainActivity`.
- `particle/ParticleRenderer.kt` + `ParticleGLSurfaceView.kt` dựng màn hình OpenGL ES 3.0, cập nhật hạt dựa trên tọa độ bàn tay và màu.
- `pose/PoseGestureDetector.kt` nhận dạng hướng tay và vòng chữ O để điều hướng hoặc xác nhận trên các giao diện nhập số/chữ.
- `pose/PoseLandmarkerHelper.kt` gắn CameraX với MediaPipe Pose Landmarker, biến mỗi frame ảnh thành `PoseLandmarkerResult` định dạng, gửi lại cho các activity.

### 3.4 Tài nguyên giao diện
- `res/layout/activity_main.xml` chứa GLSurfaceView làm nền, bảng điều khiển có tooltip màu, thanh trượt và preview camera nhỏ.
- `res/layout/activity_number_input.xml` là màn hình nền trong suốt, số hiển thị to và lưới 3x3 để người dùng dễ đọc.
- `res/layout/activity_text_input.xml` tương tự nhưng mở rộng thành bàn phím QWERTY và có phím cách, xóa, backspace.
- `res/values` (styles, colors, strings) cung cấp màu đen/đen nhạt, định dạng nút.

## 4. Luồng chạy tổng quát
1. React Native khởi động `RnEntryActivity`, hiển thị `App.js` với ba nút (hand particle, nhập số, nhập chữ).
2. Khi người dùng nhấn một nút, `NativeLauncher` (qua bridge) gọi activity tương ứng.
3. `MainActivity` khởi tạo camera + MediaPipe Hand Landmarker, rồi truyền dữ liệu về `ParticleRenderer` để vẽ hạt theo cử chỉ ngón tay (xòe cưỡng, nắm lại, chọn màu/số hạt).
4. `NumberInputActivity`/`TextInputActivity` khởi tạo MediaPipe Pose Landmarker và `PoseGestureDetector` để đo hướng cánh tay, cập nhật ô số hoặc phím chữ được chọn rồi hiển thị chuỗi đã chọn.
5. Quay lại React Native, người dùng tiếp tục chọn chế độ khác.

## 5. Phát triển và đóng gói
- Mọi cấu hình Android nằm trong `android/app/build.gradle` (kotlin 1.7, CameraX, MediaPipe, Hermes được bật).
- `android/gradle.properties` + `gradlew` giữ toàn bộ hệ thống build theo chuẩn React Native.
- Web của project (nếu cần startup) thực hiện bằng `npm run android` hoặc `npm start`.

## 6. Mẹo cho người mới
- Khi debug hoạt động native, mở `android/app/src/main/java/com/example/handparticle` vì đây nơi chứa logic xem camera và cử chỉ.
- Nếu muốn thử tạo UI mới, sửa `res/layout/activity_*.xml` rồi điều chỉnh binding tương ứng.
- Thử nghiệm gesture: mở `MainActivity`, dùng cử chỉ xòe/nhắm tay để nhìn hạt mở/thu, dùng 3 nút màu trên bảng điều khiển để đổi palette.

---

Nếu cần bản đồ thư mục chi tiết hơn (ví dụ mỗi package có bao nhiêu file và nhiệm vụ cụ thể), mình có thể mở rộng tiếp.