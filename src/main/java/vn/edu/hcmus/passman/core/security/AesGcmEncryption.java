package vn.edu.hcmus.passman.core.security;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmEncryption {

    // Chuẩn thuật toán AES, chế độ GCM, không cần Padding vì GCM hoạt động như Stream Cipher
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    
    // Độ dài của Authentication Tag (128 bit = 16 bytes). 
    // Đây là mức an toàn tối đa cho GCM.
    private static final int TAG_LENGTH_BIT = 128;

    /**
     * Mã hóa dữ liệu (Plaintext -> Ciphertext + Auth Tag)
     * 
     * @param plaintext Dữ liệu cần mã hóa (VD: chuỗi JSON)
     * @param key       Khóa AES-256 (phải đúng 32 bytes, lấy từ Argon2)
     * @param iv        Initialization Vector (phải đúng 12 bytes)
     * @return Mảng byte chứa dữ liệu đã mã hóa + 16 bytes Auth Tag ở cuối
     */
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws Exception {
        // 1. Kiểm tra kích thước đầu vào cực kỳ nghiêm ngặt
        if (key.length != 32) {
            throw new IllegalArgumentException("Khóa AES-256 bắt buộc phải dài 32 bytes.");
        }
        if (iv.length != 12) {
            throw new IllegalArgumentException("IV cho GCM bắt buộc phải dài 12 bytes.");
        }

        // 2. Khởi tạo cấu hình mã hóa
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        // 3. Thực thi mã hóa
        // Đặc điểm của Java AES-GCM: Auth Tag sẽ tự động được nối vào cuối mảng ciphertext này.
        return cipher.doFinal(plaintext);
    }

    /**
     * Giải mã dữ liệu và tự động kiểm tra tính toàn vẹn (Auth Tag)
     * 
     * @param ciphertext Dữ liệu đã mã hóa (bao gồm cả Auth Tag ở cuối)
     * @param key        Khóa AES-256
     * @param iv         IV đã dùng lúc mã hóa
     * @return Plaintext ban đầu
     * @throws Exception Bắn lỗi nếu sai mật khẩu hoặc file bị hacker chỉnh sửa
     */
    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        try {
            // 4. Thực thi giải mã
            // Java sẽ tự động tách 16 bytes cuối của ciphertext ra để check Auth Tag.
            return cipher.doFinal(ciphertext);
            
        } catch (AEADBadTagException e) {
            // Lỗi này ném ra khi:
            // - Nhập sai Master Password (dẫn đến Key sai)
            // - Hoặc file vault.json bị ai đó cố tình sửa đổi 1 ký tự (mất tính toàn vẹn)
            throw new Exception("Mở khóa thất bại! Sai mật khẩu hoặc dữ liệu đã bị can thiệp trái phép.", e);
        }
    }
}