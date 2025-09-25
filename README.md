

# 📌 App Manager – Ứng dụng Quản lý Công Việc

Ứng dụng Android hỗ trợ người dùng quản lý công việc hằng ngày: tạo nhắc nhở, sắp xếp theo lịch, duy trì chế độ tập trung, chỉnh sửa hồ sơ cá nhân và tùy chỉnh giao diện theo sở thích.
Ứng dụng được xây dựng với kiến trúc **MVVM** hiện đại, kết hợp **Room Database** và **Material Design** để mang đến trải nghiệm mượt mà, trực quan và dễ mở rộng.

---

## 🔄 Luồng hoạt động (Activity Flow)

### 1. Intro Screen

* Chỉ hiển thị khi người dùng mở ứng dụng lần đầu tiên.
* Sau khi hoàn thành Intro → tự động chuyển sang màn hình **Login/Register**.

### 2. Authentication

* **Login**: nếu đăng nhập thành công → chuyển sang `MainActivity`.
* **Register**: nếu chưa có tài khoản, người dùng có thể đăng ký mới → sau khi thành công → chuyển sang `MainActivity`.

### 3. MainActivity (Bottom Navigation)

Ứng dụng sử dụng **Bottom Navigation** để điều hướng giữa 4 Fragment chính:

* **Home Fragment**: hiển thị danh sách các công việc của ngày hôm nay.
* **Calendar Fragment**: hiển thị công việc theo ngày/tuần/tháng mà người dùng chọn.
* **Focus Fragment**: chế độ tập trung, có thể bật **Do Not Disturb** (tắt thông báo) và hẹn giờ đếm ngược.
* **Profile Fragment**:

  * Chỉnh sửa thông tin cá nhân: tên, mật khẩu, ảnh đại diện.
  * Truy cập **App Settings**: thay đổi theme (dark/light), font chữ, kích thước chữ.
  * **Logout**: quay trở lại màn hình Login/Register.

---

## 🛠 Công nghệ sử dụng

* **Ngôn ngữ & UI**: Kotlin + XML Layout
* **Kiến trúc**: MVVM (Model – View – ViewModel)

  * **ViewModel + LiveData**: quản lý và theo dõi dữ liệu theo vòng đời.
  * **Repository**: lớp trung gian giữa ViewModel và Database.
  * **Room Database**: hỗ trợ CRUD cho Task.
* **UI/UX**: Material Design Components
* **Điều hướng**: Navigation Component + BottomNavigationView

---

## 📂 Các tài liệu bắt buộc đi kèm

* **File Figma giao diện**: thiết kế UI toàn bộ app.
* **Biểu đồ UML Database Design**: mô tả thiết kế dữ liệu.
* **Entity Relationship Diagram (ERD)**: biểu diễn quan hệ giữa các bảng trong CSDL.
* **Activity Diagram**: mô tả luồng hoạt động của ứng dụng.
* **Class Diagram**: mô tả mối quan hệ giữa các lớp trong mô hình MVVM.
* **Link Project Management Linear Board**: theo dõi tiến độ và phân chia công việc.

---

## ⚡ Tính năng chính

* **CRUD Task**: thêm, sửa, xoá, xem chi tiết công việc.
* **Archive Task**: lưu trữ các công việc đã hoàn thành.
* **Notification**:

  * Nhắc nhở khi công việc sắp đến hạn.
  * Báo khi công việc bị lỡ deadline.
* **Focus Mode**: bật chế độ tập trung (Do Not Disturb), hẹn giờ đếm ngược.
* **Profile**: chỉnh sửa thông tin cá nhân, đổi mật khẩu, đăng xuất.
* **App Settings**: thay đổi theme, font chữ, kích thước chữ.

---

## 🚀 Tính năng nâng cao (tùy chọn trong tương lai)

* Đồng bộ công việc với **Google Calendar**.
* Thêm **Widget ngoài màn hình chính**.
* Đăng nhập bằng **Google/Facebook**.
* Hỗ trợ đa ngôn ngữ (EN, VN).

---

## 📜 License

Ứng dụng phát hành theo **MIT License** – tự do sử dụng, chỉnh sửa và phát triển.


---

