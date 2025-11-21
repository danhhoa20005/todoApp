# AppManagement (Todo App)

Ứng dụng AppManagement là một mẫu **quản lý công việc** viết bằng Kotlin cho Android. Mã nguồn minh họa đầy đủ các lớp ViewModel, Repository, Room, Firestore và điều hướng Fragment để triển khai luồng đăng nhập, tạo–đồng bộ công việc và thống kê tuần.

## Kiến trúc tổng quan
Ứng dụng tách rõ ba tầng chính:

- **Data layer**
  - `data/entity` chứa các **Room entity**: `User` (thông tin tài khoản, trạng thái đăng nhập, `remoteId` để đồng bộ Google/Firebase) và `Task` (công việc gắn user, trạng thái hoàn thành, `remoteId`, `updatedAt`).
  - `data/dao` định nghĩa **DAO** cho CRUD và truy vấn nâng cao: `UserDao` quản lý đăng nhập, đổi thông tin và tìm theo `remoteId`; `TaskDao` cung cấp lọc theo user/ngày/trạng thái, cập nhật `orderIndex`, tìm theo `remoteId`.
  - `data/db/AppDatabase` cấu hình Room (phiên bản 2) với **migration 1→2** để bổ sung cột `remote_id`/`updated_at` cho cả `users` và `tasks`.
  - `data/remote/TaskRemoteDataSource` là **adapter Firestore**: thêm/cập nhật/xóa task (document collection `tasks`), ánh xạ trường `taskDate`, `orderIndex`, `updatedAt`… và truy vấn danh sách theo `userId` (uid Firebase).
  - `data/repo` gom logic nghiệp vụ: `AccountRepository` xử lý đăng ký/login email (BCrypt hash), đăng nhập Google dựa trên `uid` và lưu `remoteId`; `TaskRepository` thao tác Room + Firestore (tạo/sửa/xóa, đổi thứ tự, đồng bộ hai chiều theo `updatedAt`, gán `remoteId` sau khi upsert).

- **ViewModel layer**
  - `data/viewmodel/SignInViewModel` điều phối đăng nhập/đăng ký và đăng nhập Google thông qua `AccountRepository`.
  - `data/viewmodel/TaskViewModel` giữ state danh sách task (toàn bộ, chưa xong, đã xong, theo ngày), tính thống kê 7 ngày và cung cấp trigger `syncTasksForCurrentUser()` để kéo dữ liệu Firestore về Room.
  - `data/viewmodel/CreateWorkViewModel` hỗ trợ form tạo/sửa công việc, truy vấn task theo `id`.

- **UI layer (Fragments)**
  - **Onboarding** (`ui/onboard/OnboardFragment`): cho phép chọn đăng nhập bằng email hoặc Google. Khi đăng nhập Google thành công, `SignInViewModel.loginWithGoogleUser` lưu user vào Room rồi gọi `TaskViewModel.syncTasksForCurrentUser()` trước khi điều hướng sang màn chính.
  - **Đăng nhập/đăng ký** (`ui/login`): màn email/password gọi `SignInViewModel.login` hoặc `register` rồi chuyển sang Home khi thành công.
  - **Trang chủ** (`ui/home/HomeFragment`): quan sát `TaskViewModel.tasksAll` để hiển thị thống kê hôm nay, biểu đồ tuần, tìm kiếm; tự `loadTasksForCurrentUser()` và `syncTasksForCurrentUser()` khi vào màn.
  - **Menu công việc** (`ui/menu`): các tab Today/List/Calendar/Done quan sát dữ liệu lọc từ `TaskViewModel` và mở form chỉnh sửa khi cần.
  - **Tạo/Chỉnh sửa task** (`ui/task`): dùng `TaskViewModel` để thêm mới, cập nhật, toggle hoàn thành; các thao tác đều chạy trong coroutine `Dispatchers.IO`.
  - **Splash/Main** (`ui/splash`, `ui/main`): khởi động app, điều hướng vào Onboard hoặc Home tùy trạng thái đăng nhập.

- **Tiện ích & bảo mật**
  - `util/Security` cung cấp hàm hash/verify BCrypt cho mật khẩu local.
  - `uitls/WeekChart` (và các extension ngày giờ) hỗ trợ tính toán + vẽ biểu đồ tuần.
  - `notifications` chứa code hiển thị thông báo nhắc việc khi cần.

## Luồng hoạt động chính
1. **Khởi chạy ứng dụng**: Splash kiểm tra user đang đăng nhập (Room `is_logged_in`). Nếu có, chuyển thẳng tới Home; nếu không, mở Onboarding.
2. **Đăng nhập/Đăng ký email**: Form gọi `SignInViewModel`, repository xác thực mật khẩu hoặc tạo user mới (hash BCrypt), set cờ `is_logged_in` trong Room.
3. **Đăng nhập Google**: Onboarding khởi chạy Google Sign-In → Firebase Auth trả `FirebaseUser` → `AccountRepository.loginWithGoogleAccount` tìm/tạo `User` với `remoteId = uid` và đánh dấu đăng nhập → TaskViewModel đồng bộ task với Firestore theo `remoteId`.
4. **Quản lý công việc**:
   - Thêm/Sửa/Xóa/Đổi thứ tự/Toggle hoàn thành: `TaskRepository` ghi Room, cập nhật `updatedAt` và (nếu user có `remoteId`) upsert/xóa trên Firestore; khi Firestore tạo document mới, `remoteId` được lưu lại vào Room.
   - Lọc & thống kê: TaskViewModel xuất LiveData theo user hiện tại; biểu đồ tuần và thống kê hôm nay tính toán dựa trên danh sách `tasksAll`.
5. **Đồng bộ đa thiết bị**: `syncFromRemote(User)` tải mọi task Firestore của `remoteId` hiện tại; mỗi task so sánh `updatedAt` với bản local để quyết định update/giữ nguyên, đảm bảo “bản mới nhất thắng” khi dùng cùng Gmail trên nhiều thiết bị.

## Thiết lập & chạy
1. **Yêu cầu**: Android Studio Hedgehog/Flamingo+, JDK 17, Android SDK 33+.
2. **Firebase**: đặt tệp `google-services.json` hợp lệ dưới `app/` để Google Sign-In + Firestore hoạt động; bật Authentication (Google) và Cloud Firestore trên console.
3. **Đồng bộ phụ thuộc**: chạy `./gradlew tasks` hoặc Sync trong Android Studio.
4. **Chạy ứng dụng**: chọn thiết bị ảo/thật (API 33+), bấm Run. Lần đầu mở app đi qua Onboard → đăng ký email hoặc đăng nhập Google → tạo công việc → thử đồng bộ bằng cách đăng nhập cùng Gmail ở thiết bị khác và gọi lại sync (Home tự gọi khi mở).

## Kiểm thử nhanh
- `./gradlew lint` – kiểm tra style và issue thường gặp.
- `./gradlew test` – chạy test đơn vị (nếu có).
- Kiểm thử thủ công: tạo task, đánh dấu hoàn thành, đổi thứ tự, đăng nhập Google trên hai thiết bị/AVD để xác nhận đồng bộ Firestore.

## Cấu trúc thư mục rút gọn
- `app/src/main/java/com/example/appmanagement/data/entity` – Entity `User`, `Task`.
- `app/src/main/java/com/example/appmanagement/data/dao` – DAO Room.
- `app/src/main/java/com/example/appmanagement/data/db/AppDatabase.kt` – cấu hình Room + migration.
- `app/src/main/java/com/example/appmanagement/data/remote/TaskRemoteDataSource.kt` – API Firestore.
- `app/src/main/java/com/example/appmanagement/data/repo` – Repository tài khoản và công việc.
- `app/src/main/java/com/example/appmanagement/data/viewmodel` – ViewModel cho đăng nhập, công việc.
- `app/src/main/java/com/example/appmanagement/ui` – Fragment UI (onboarding, login, home, menu, task, splash, main).
- `app/src/main/java/com/example/appmanagement/util` & `uitls` – tiện ích chung.
- `app/src/main/java/com/example/appmanagement/notifications` – logic thông báo.

