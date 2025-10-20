# Ứng dụng AppManagement (Todo App)

## Giới thiệu
Ứng dụng AppManagement là một mẫu ứng dụng quản lý công việc viết bằng Kotlin cho nền tảng Android. Dự án được xây dựng nhằm minh họa các luồng đăng nhập, đăng ký, quản lý hồ sơ người dùng cũng như tạo và theo dõi danh sách công việc hằng ngày. Mã nguồn đã được bổ sung chú thích bằng tiếng Việt để giúp việc đọc hiểu cấu trúc dự án trở nên trực quan hơn.

## Tính năng chính
- Quy trình onboarding, splash và tự động chuyển hướng dựa trên trạng thái đăng nhập.
- Đăng ký, đăng nhập bằng email, lưu mật khẩu dạng BCrypt và duy trì trạng thái người dùng trong cơ sở dữ liệu.
- Tạo, chỉnh sửa, sắp xếp và đánh dấu hoàn thành cho công việc với dữ liệu lưu bằng Room.
- Bộ lọc công việc theo ngày, danh sách hôm nay, danh sách đã hoàn thành và chức năng tìm kiếm theo từ khóa.
- Đồng hồ bấm giờ có lưu các mốc thời gian và khả năng tiếp tục khi quay lại màn hình.
- Giao diện thống kê với biểu đồ cột thể hiện số lượng công việc hoàn thành trong tuần.

## Công nghệ và thư viện
- **Ngôn ngữ:** Kotlin với Gradle Kotlin DSL.
- **UI:** Fragment, ViewBinding, Material Components.
- **Điều hướng:** Jetpack Navigation Component và Safe Args.
- **Lưu trữ:** Room Database, LiveData và ViewModel.
- **Bảo mật:** BCrypt để băm mật khẩu, SQLCipher/EncryptedSharedPreferences đã khai báo trong phụ thuộc.
- **Khác:** Coroutine (Dispatchers.IO) cho xử lý bất đồng bộ, Handler cho đồng hồ bấm giờ.

## Cấu trúc thư mục nổi bật
- `app/src/main/java/com/example/appmanagement/ui` – Chứa các Fragment hiển thị giao diện chính và logic tương tác.
- `app/src/main/java/com/example/appmanagement/data` – Bao gồm entity, DAO, repository và ViewModel phục vụ dữ liệu.
- `app/src/main/res` – Tập hợp layout, drawable, navigation và tài nguyên cấu hình.
- `app/src/main/AndroidManifest.xml` – Khai báo activity, quyền và meta-data của ứng dụng.

## Chuẩn bị môi trường
1. Cài đặt **Android Studio Flamingo** (hoặc mới hơn) với Android SDK 36 và công cụ build tương ứng.
2. Đồng bộ dự án bằng Gradle Wrapper (`./gradlew tasks`) để tải các phụ thuộc cần thiết.
3. Nếu cần sử dụng dịch vụ Firebase/Facebook, cập nhật lại tệp `google-services.json` và các khóa ứng dụng tương ứng.

## Cách chạy ứng dụng
1. Mở dự án trong Android Studio.
2. Chọn một thiết bị ảo (API 33+) hoặc thiết bị thật có kích thước màn hình tối thiểu tương đương.
3. Nhấn **Run** (Shift + F10) để biên dịch và cài đặt ứng dụng.
4. Ở lần chạy đầu tiên, thực hiện quy trình onboarding → nhập email → đăng ký để tạo tài khoản mẫu.

## Kiểm thử cơ bản
- Chạy `./gradlew lint` để kiểm tra quy tắc mã nguồn.
- Sử dụng `./gradlew test` để chạy các bài kiểm thử đơn vị (nếu có trong thư mục `app/src/test`).
- Kiểm tra thủ công các luồng chính: đăng ký, đăng nhập, tạo công việc, chỉnh sửa, đánh dấu hoàn thành và sử dụng đồng hồ bấm giờ.

## Ghi chú
- Mọi file nguồn đã được bổ sung chú thích tiếng Việt bằng ký hiệu `//` (hoặc `<!-- // ... -->` với XML) nhằm mô tả rõ mục đích và chức năng từng thành phần mà không ảnh hưởng đến logic hoạt động.
- Khi triển khai thực tế, nên cấu hình lại giới hạn minSdk/targetSdk và tối ưu hóa dữ liệu sao lưu theo nhu cầu ứng dụng của bạn.
