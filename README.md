#YPS - OfflinePasswordManager
Một ứng dụng Quản lý Mật khẩu hoàn toàn ngoại tuyến (Offline-first) được xây dựng trên triết lý **Zero-Knowledge** (Không-Lưu-Vết). YPS giúp bạn quản lý hàng trăm tài khoản một cách an toàn mà chỉ cần nhớ duy nhất một Mật khẩu Chủ (Master Password). Toàn bộ dữ liệu được mã hóa bằng tiêu chuẩn quân đội và lưu trữ cục bộ trên máy tính của bạn, không có bất kỳ máy chủ hay kết nối Internet nào can thiệp.

## ✨ Tính năng nổi bật

### 🔒 Bảo mật Cấp độ Chuyên gia (Enterprise-Grade Security)
*   **Mã hóa AES-256-GCM:** Sử dụng thuật toán mã hóa đối xứng mạnh nhất hiện nay kèm theo Authentication Tag để chống lại việc giả mạo hoặc chỉnh sửa file dữ liệu trái phép (Tamper-Proof).
*   **Dẫn xuất khóa Argon2id:** Chống lại các cuộc tấn công Brute-force bằng phần cứng chuyên dụng (GPU/ASIC) thông qua cơ chế Memory-Hard Key Derivation.
*   **Bảo vệ Bộ nhớ (Secure Memory Wiping):** Tích cực dọn dẹp RAM ngay sau khi sử dụng. Các biến chứa mật khẩu (`char[]`, `byte[]`) sẽ bị ghi đè bằng dữ liệu nhiễu ngẫu nhiên (Random bytes) và dọn về 0, ngăn chặn hoàn toàn các cuộc tấn công trích xuất bộ nhớ (Memory Dumping / Cold Boot Attacks).
*   **Auto-Lock (Khóa tự động):** Tự động xóa khóa phiên (Session Key) và làm sạch RAM, đẩy người dùng về màn hình đăng nhập nếu không có tương tác chuột/phím sau một khoảng thời gian nhất định.
*   **Ghi tệp an toàn (Atomic Safe Write):** Đảm bảo file `vault.json` không bao giờ bị hỏng (corrupted) ngay cả khi ứng dụng bị tắt đột ngột hay mất điện trong quá trình lưu dữ liệu.

### 💻 Trải nghiệm Người dùng (UX)
*   Giao diện Minimalist, hiện đại được thiết kế bằng **JavaFX**.
*   Quản lý (Thêm/Sửa/Xóa) danh sách tài khoản dễ dàng qua Cửa sổ Pop-up (Modal).
*   Tính năng **Copy to Clipboard** nhanh chóng chỉ với 1 click, không hiển thị mật khẩu dưới dạng bản rõ trên bảng dữ liệu để chống nhìn trộm (Shoulder Surfing).

---

## 🛠️ Kiến trúc & Công nghệ (Tech Stack)

Dự án áp dụng chặt chẽ mô hình **Clean Architecture** và chia làm 2 phân vùng độc lập:
1.  **Core (Lõi Nghiệp vụ & Mật mã):** Hoàn toàn là Java thuần, không phụ thuộc UI. Dễ dàng Unit Test và tái sử dụng.
2.  **UI (Giao diện):** Điều khiển bởi JavaFX và liên kết với Core thông qua Dependency Injection.

*   **Ngôn ngữ:** Java 17+
*   **UI Framework:** JavaFX
*   **Quản lý dự án / Build Tool:** Maven
*   **Xử lý JSON:** Google Gson
*   **Thư viện Mật mã:** BouncyCastle (hoặc Java Cryptography Architecture - JCA)

---

## 🚀 Hướng dẫn Cài đặt và Chạy ứng dụng

### Yêu cầu hệ thống:
*   Đã cài đặt **JDK 17** (hoặc mới hơn).
*   Đã cài đặt **Apache Maven**.

### Thực thi:
Mở Terminal/Command Prompt tại thư mục gốc của dự án và chạy lệnh sau:

`mvn clean compile javafx:run`

*Lưu ý: Trong lần chạy đầu tiên, hệ thống sẽ yêu cầu bạn Khởi tạo Két Sắt (Setup Vault). Ở các lần chạy sau, ứng dụng sẽ tự động phát hiện file `vault.json` trong thư mục người dùng (`~/.passman/`) và hiển thị màn hình Mở khóa (Login).*

---

## 📂 Cấu trúc Thư mục Chính

```text
src/
├── main/
│   ├── java/vn/edu/hcmus/passman/
│   │   ├── core/              # Lõi nghiệp vụ (Không chứa code UI)
│   │   │   ├── models/        # Cấu trúc dữ liệu (Account, Vault, EncryptedVault)
│   │   │   ├── security/      # Xử lý mã hóa, Argon2, AES, MemoryWiper
│   │   │   └── services/      # Logic điều phối (VaultManager, AutoLockService)
│   │   ├── ui/                # Giao diện người dùng
│   │   │   └── controllers/   # Các JavaFX Controllers điều khiển thao tác
│   │   └── MainApp.java       # Entry point khởi chạy ứng dụng
│   │
│   └── resources/
│       └── fxml/              # File thiết kế giao diện (Setup, Login, Dashboard...)
└── test/                      # Các Unit Test đảm bảo độ tin cậy của thuật toán
```

⚠️ Khuyến cáo Bảo mật (Disclaimer):

Không có tính năng Khôi phục Mật khẩu: Đúng với nguyên tắc Zero-Knowledge, chúng tôi KHÔNG lưu trữ hay gửi Master Password của bạn đi đâu. Nếu bạn quên Master Password, toàn bộ dữ liệu của bạn sẽ bị khóa vĩnh viễn. Vui lòng ghi nhớ Master Password cẩn thận hoặc lưu trữ an toàn.

Ứng dụng sử dụng Clipboard của Hệ điều hành để sao chép mật khẩu. Để bảo mật tối đa, người dùng nên tự dọn dẹp Clipboard sau khi sử dụng (paste) xong.
