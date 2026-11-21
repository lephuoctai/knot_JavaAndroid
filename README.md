# Knot Android

Ứng dụng Android Java lưu kỷ niệm theo hành trình, kết nối API cloud dùng chung. Snapshot này chỉ chứa source ứng dụng và cấu hình Gradle; server, migration/backend, dữ liệu runtime, test và báo cáo triển khai được giữ ngoài repo Android.

## Build

- Android Studio/Android SDK tương thích compileSdk 36; minSdk 29 (Android 10).
- Dùng Gradle Wrapper đi kèm; Gradle 8.13. Android Gradle Plugin và dependency được khóa trong `gradle/libs.versions.toml`/build files. Dùng JDK tương thích phiên bản AGP này.
- Mở project bằng Android Studio, chọn SDK path ở máy mới rồi Sync Gradle.
- Windows: `./gradlew.bat :app:assembleDebug`.
- macOS/Linux: `./gradlew :app:assembleDebug`.
- APK build ở `app/build/outputs/apk/debug/app-debug.apk`, không đưa APK lên Git.

API mặc định là Supabase HTTPS đã cấu hình trong `app/build.gradle.kts`; có thể override bằng Gradle property `knotApiUrl`. Không cần khởi động local server để dùng cấu hình mặc định. File `local.properties` do máy phát triển tạo và không được commit. Không đặt service-role/backend secret trong cấu hình ứng dụng.

## Hành vi hiện tại

Email là nhãn phân biệt tài khoản, không mật khẩu/OTP. Có thể tạo/tham gia hành trình, tối đa hai email; thêm ảnh/ngày/nội dung, xem timeline và Reload thủ công. Bản đã tải có thể mở offline. Đổi hành trình/đăng xuất gỡ thành viên sau hai cảnh báo; người cuối rời khóa hành trình và backend dọn dữ liệu bất đồng bộ.

Giữ applicationId và khóa ký ổn định khi phát hành bản cập nhật. Repo không chứa khóa ký, credential, cache, dữ liệu thiết bị hoặc môi trường cá nhân. Source không đồng nghĩa với việc đã nghiệm thu phát hành store; các bằng chứng/test nội bộ được lưu riêng trong workspace SP018.
