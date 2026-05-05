package vn.edu.hcmus.passman.core.security;

import java.security.SecureRandom;
import java.util.Arrays;

public class MemoryWiper {

    // Sử dụng SecureRandom thay vì Random để tạo nhiễu mật mã học
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * GHI ĐÈ MẢNG CHAR (Dùng cho Master Password / Plaintext Password)
     * Kỹ thuật: Ghi đè bằng ký tự ngẫu nhiên -> Ghi đè bằng ký tự null.
     */
    public static void clear(char[] sensitiveData) {
        if (sensitiveData != null && sensitiveData.length > 0) {
            // Bước 1: Ghi đè bằng dữ liệu ngẫu nhiên (chống dư ảnh trên RAM vật lý)
            for (int i = 0; i < sensitiveData.length; i++) {
                // Ép kiểu một số nguyên ngẫu nhiên thành ký tự char
                sensitiveData[i] = (char) SECURE_RANDOM.nextInt(Character.MAX_VALUE);
            }
            
            // Bước 2: Đưa toàn bộ về ký tự null (\0) để đồng bộ hóa
            Arrays.fill(sensitiveData, '\0');
        }
    }

    /**
     * GHI ĐÈ MẢNG BYTE (Dùng cho Encryption Key, Salt, IV, Ciphertext)
     * Kỹ thuật: Ghi đè bằng Random Bytes -> Ghi đè bằng 0.
     */
    public static void clear(byte[] sensitiveData) {
        if (sensitiveData != null && sensitiveData.length > 0) {
            // Bước 1: Ghi đè bằng byte ngẫu nhiên
            byte[] randomNoise = new byte[sensitiveData.length];
            SECURE_RANDOM.nextBytes(randomNoise);
            
            // System.arraycopy thao tác ở mức thấp (native), cực kỳ nhanh và tối ưu
            System.arraycopy(randomNoise, 0, sensitiveData, 0, sensitiveData.length);
            
            // Bước 2: Đưa toàn bộ về 0 (Zero-out)
            Arrays.fill(sensitiveData, (byte) 0);
        }
    }
}