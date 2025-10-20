

# Ứng dụng AppManagement (Todo App)

## 🧭 Giới thiệu

Ứng dụng **AppManagement** là một dự án mẫu quản lý công việc (To-Do List) viết bằng **Kotlin** trên nền tảng **Android**.
Ứng dụng được phát triển trong khuôn khổ học tập, minh họa quy trình **đăng nhập**, **đăng ký**, **quản lý người dùng** và **tạo – theo dõi công việc hằng ngày**.
Toàn bộ mã nguồn được chú thích bằng tiếng Việt (`//` trong code hoặc `<!-- // ... -->` trong XML) giúp người học dễ dàng đọc hiểu và tùy chỉnh.

---

## 📅 Thời gian phát triển

* **Bắt đầu:** 17/9/2025
* **Hoàn thiện bản thử nghiệm:** 20/10/2025
* **Cập nhật gần nhất:** 20/10/2025
* **Tác giả:** Nguyễn Danh Hòa – sinh viên Học viện Công nghệ Bưu chính Viễn thông (PTIT)

---

## ⚙️ Tính năng chính

* Màn hình **Splash** và **Onboarding** tự động chuyển hướng dựa vào trạng thái đăng nhập.
* **Đăng ký / Đăng nhập** qua email, bảo mật bằng **BCrypt**, lưu thông tin người dùng trong **Room Database**.
* **Tạo**, **chỉnh sửa**, **xóa**, **đánh dấu hoàn thành** công việc.
* Bộ lọc hiển thị: công việc **hôm nay**, **đã hoàn thành**, hoặc **tìm kiếm theo từ khóa**.
* **Đồng hồ bấm giờ (Stopwatch)** có khả năng lưu trạng thái khi thoát ứng dụng.
* **Thống kê trực quan** bằng biểu đồ cột (BarChart) số lượng công việc hoàn thành trong tuần.
* **Giao diện tối (Dark Mode)** và chủ đề tùy chỉnh theo Material 3.

---

## 🧩 Kiến trúc & Thành phần chính

Ứng dụng được xây dựng theo **mô hình MVVM (Model – View – ViewModel)**, kết hợp **Repository Pattern** để tách biệt logic nghiệp vụ với giao diện.

### 1. **Model**

* Định nghĩa các `Entity` như `User`, `Task` tương ứng với bảng trong Room Database.
* Dùng **TypeConverter** để lưu trữ các giá trị phức tạp (ví dụ: ngày tháng, trạng thái).

### 2. **ViewModel**

* Quản lý dữ liệu theo **Lifecycle**, cập nhật UI thông qua **LiveData** và **Flow**.
* Xử lý luồng bất đồng bộ với **Kotlin Coroutines**.

### 3. **Repository**

* Là lớp trung gian giữa ViewModel và DAO, giúp truy xuất dữ liệu từ **Room** hoặc **SharedPreferences**.

### 4. **View (UI)**

* Các **Fragment** (Home, Calendar, Done, Add/Edit, Stopwatch, Settings) sử dụng **ViewBinding** và **Material Components**.
* Điều hướng bằng **Jetpack Navigation Component** với **Safe Args**.

---

## 🧠 Công nghệ và thư viện sử dụng

| Thành phần       | Mô tả                               | Phiên bản |
| ---------------- | ----------------------------------- | --------- |
| **Ngôn ngữ**     | Kotlin (Gradle Kotlin DSL)          | 1.9.x     |
| **Build system** | Android Gradle Plugin               | 8.3.x     |
| **Kiến trúc**    | MVVM, Repository Pattern            | –         |
| **CSDL**         | Room Database + TypeConverter       | 2.6.x     |
| **Lifecycle**    | ViewModel, LiveData, CoroutineScope | 2.8.x     |
| **UI**           | Fragment, RecyclerView, Material3   | 1.10.x    |
| **Điều hướng**   | Navigation Component + Safe Args    | 2.7.x     |
| **Bảo mật**      | BCrypt, EncryptedSharedPreferences  | –         |
| **Thống kê**     | MPAndroidChart / custom BarChart    | 3.1.x     |
| **Xử lý nền**    | Kotlin Coroutines (Dispatchers.IO)  | –         |

---

## 📁 Cấu trúc thư mục nổi bật

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/appmanagement/
│   │   │   ├── ui/               # Các Fragment và logic giao diện
│   │   │   ├── data/             # Entity, DAO, Repository, ViewModel
│   │   │   └── utils/            # Tiện ích chung (DateFormatter, Validator…)
│   │   ├── res/                  # Layout, drawable, values, navigation
│   │   └── AndroidManifest.xml   # Khai báo activity, quyền và meta-data
└── build.gradle.kts              # Cấu hình phụ thuộc Kotlin DSL
```

---

## 🧰 Chuẩn bị môi trường

1. Cài đặt **Android Studio Flamingo** (hoặc mới hơn).
2. Đảm bảo SDK ≥ **Android 13 (API 33)** và **JDK 17**.
3. Đồng bộ dự án bằng lệnh:

   ```bash
   ./gradlew tasks
   ```
4. (Tùy chọn) Cập nhật `google-services.json` nếu tích hợp Firebase.

---

## ▶️ Cách chạy ứng dụng

1. Mở dự án trong **Android Studio**.
2. Chọn **thiết bị ảo hoặc thật (API 33 trở lên)**.
3. Nhấn **Run (Shift + F10)** để biên dịch và cài đặt.
4. Ở lần khởi động đầu tiên: hoàn thành onboarding → đăng ký → đăng nhập → tạo công việc mẫu.

---

## 🧪 Kiểm thử cơ bản

* Chạy `./gradlew lint` để kiểm tra quy tắc mã nguồn.
* Chạy `./gradlew test` để kiểm thử đơn vị (unit test).
* Thử nghiệm luồng chính:

  * Đăng ký / Đăng nhập
  * Tạo – chỉnh sửa – xóa công việc
  * Bấm giờ và tiếp tục Stopwatch
  * Xem biểu đồ thống kê


## 🎥 Video Demo

> 👉 *Video minh họa hoạt động của ứng dụng AppManagement, bao gồm các luồng chính: đăng ký, đăng nhập, tạo – chỉnh sửa công việc, xem thống kê và sử dụng đồng hồ bấm giờ.*

[![Xem video demo trên YouTube](https://img.shields.io/badge/Xem%20Video%20Demo-FF0000?style=for-the-badge&logo=youtube&logoColor=white)]([https://youtu.be/your_video_id_here](https://youtu.be/gbg11kG4aRU))


---

## 📝 Ghi chú  
- Tất cả tệp nguồn có chú thích tiếng Việt mô tả rõ chức năng.  
- Khi triển khai thực tế, nên:
  - Cập nhật `minSdk` và `targetSdk` phù hợp.  
  - Tối ưu hóa Room (migration, index).  
  - Kích hoạt ProGuard/R8 khi xuất bản.
---


