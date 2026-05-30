# Offline Password Manager

Một ứng dụng quản lý mật khẩu ngoại tuyến (offline) mã nguồn mở được phát triển bằng **JavaFX**. Dự án này đặt trọng tâm cực kỳ lớn vào bảo mật dữ liệu, kết hợp các thuật toán mã hóa hiện đại và kiến trúc quản lý khóa hai tầng (DEK) chuyên nghiệp, cùng với tính năng khôi phục mật khẩu thông minh thông qua thuật toán Shamir's Secret Sharing.

## ✨ Các tính năng nổi bật

*   **Lưu trữ ngoại tuyến an toàn:** Toàn bộ dữ liệu được lưu cục bộ trên máy tính của bạn, không có bất kỳ kết nối đám mây hay máy chủ bên ngoài nào.
*   **Kiến trúc DEK (Data Encryption Key):** Cho phép bạn thay đổi Master Password mà không cần giải mã và mã hóa lại toàn bộ kho mật khẩu.
*   **Cơ chế khôi phục bằng Shamir's Secret Sharing (SSS):** Nếu bạn quên Master Password, hệ thống cho phép khôi phục lại mật khẩu bằng cách trả lời đúng $K$ trên tổng số $N$ câu hỏi bảo mật do bạn tự thiết lập.
*   **Quản lý bộ nhớ an toàn (Memory Wiping):** Tự động xóa sạch các dữ liệu nhạy cảm (như Master Password, DEK, mật khẩu tài khoản) trên RAM ngay sau khi sử dụng xong để chống lại các mã độc quét RAM (RAM-scraping malware).
*   **Giao diện JavaFX trực quan:** Hỗ trợ Setup dạng Wizard, Đăng nhập và Dashboard quản lý thân thiện.

## 🔒 Tiêu chuẩn Bảo mật & Mật mã học

Ứng dụng sử dụng các thuật toán chuẩn công nghiệp đã được kiểm chứng:

*   **AES-256-GCM:** Thuật toán mã hóa đối xứng có xác thực (Authenticated Encryption) mạnh mẽ nhất hiện nay, dùng để mã hóa cả khóa DEK và toàn bộ nội dung của Vault.
*   **Argon2id:** Thuật toán dẫn xuất khóa (Key Derivation Function) chống brute-force và chống phần cứng chuyên dụng (ASIC/GPU), dùng để băm Master Password và câu trả lời bảo mật.
*   **Shamir's Secret Sharing (GF-256):** Thuật toán chia sẻ bí mật dùng để phân mảnh khóa DEK thành nhiều phần rời rạc (Shares). Không một mảnh vỡ đơn lẻ nào chứa bất kỳ thông tin gì về khóa gốc.
*   **SecureRandom:** Sinh IVs (Initialization Vectors), Salts và khóa DEK ngẫu nhiên đảm bảo tính không thể dự đoán.

## 🚀 Hướng dẫn cài đặt và chạy ứng dụng

### Yêu cầu hệ thống
*   **Java JDK 17** trở lên.
*   **Apache Maven** (để quản lý thư viện và build).

### Cách chạy ứng dụng

1.  Clone (hoặc tải) mã nguồn về máy tính.
2.  Mở Terminal/Command Prompt tại thư mục gốc của dự án (nơi chứa file `pom.xml`).
3.  Chạy lệnh sau để build và khởi chạy giao diện JavaFX:

```bash
mvn clean javafx:run
```

### Cách đóng gói thành file thực thi (Tùy chọn)

Để đóng gói ứng dụng thành một file `.jar` có thể chạy độc lập (Fat JAR), hãy sử dụng lệnh:

```bash
mvn clean package
```
File thực thi sẽ nằm trong thư mục `target/`.

## 📂 Kiến trúc dữ liệu

Dữ liệu của bạn được lưu mặc định tại đường dẫn `~/.passman/vault.json` với cấu trúc JSON hoàn toàn bị mã hóa, bao gồm:

*   `vaultMetadata`: Lưu trữ IV dùng để mã hóa dữ liệu kho mật khẩu.
*   `masterKeyMetadata`: Lưu trữ Salt, IV và khóa DEK đã bị mã hóa bởi Master Password.
*   `recoveryMetadata`: Lưu trữ các cấu hình của Shamir's Secret Sharing (ngưỡng khôi phục $K$) và các mảnh vỡ DEK đã bị mã hóa bởi các câu trả lời bảo mật.
*   `ciphertext`: Chuỗi Base64 chứa danh sách toàn bộ các tài khoản của bạn đã được mã hóa siêu an toàn bởi khóa DEK.

## 🤝 Giấy phép
Dự án được xây dựng phục vụ cho mục đích học tập và phát triển ứng dụng bảo mật.