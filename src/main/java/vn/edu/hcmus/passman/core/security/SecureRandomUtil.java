package vn.edu.hcmus.passman.core.security;

import java.security.SecureRandom;

public class SecureRandomUtil {

    // Khởi tạo một instance duy nhất (Singleton). 
    // SecureRandom tốn nhiều tài nguyên để khởi tạo, nhưng nó Thread-safe, 
    // nên dùng chung một biến static sẽ tối ưu hiệu năng rất nhiều.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // --- CÁC HẰNG SỐ CHUẨN MẬT MÃ ---
    
    // Salt cho Argon2: Khuyến cáo từ 16 bytes (128-bit) trở lên.
    public static final int SALT_LENGTH = 16; 
    
    // IV cho AES-GCM: TRỌNG TÂM KỸ THUẬT!
    // Trái với AES-CBC (dùng 16 bytes), NIST khuyến cáo AES-GCM BẮT BUỘC nên dùng 12 bytes (96-bit).
    // Nếu dùng kích thước khác, GCM sẽ phải tốn thêm một bước tính toán hash (GHASH) làm chậm và giảm tính an toàn.
    public static final int GCM_IV_LENGTH = 12; 

    /**
     * Sinh mảng byte ngẫu nhiên an toàn (Hàm gốc)
     * @param length số lượng byte cần sinh
     * @return mảng byte ngẫu nhiên
     */
    public static byte[] generateRandomBytes(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Sinh Salt (128-bit) dùng cho hàm KDF (Argon2)
     */
    public static byte[] generateSalt() {
        return generateRandomBytes(SALT_LENGTH);
    }

    /**
     * Sinh IV (96-bit) dùng cho quá trình mã hóa AES-256-GCM
     */
    public static byte[] generateIV() {
        return generateRandomBytes(GCM_IV_LENGTH);
    }
}