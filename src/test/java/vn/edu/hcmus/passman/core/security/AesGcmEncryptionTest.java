package vn.edu.hcmus.passman.core.security;

import org.junit.jupiter.api.Test;
import javax.crypto.AEADBadTagException;
import static org.junit.jupiter.api.Assertions.*;

public class AesGcmEncryptionTest {

    @Test
    public void testAuthenticationTag_TamperDetection() throws Exception {
        // 1. CHUẨN BỊ MÔI TRƯỜNG
        AesGcmEncryption aes = new AesGcmEncryption();
        byte[] key = SecureRandomUtil.generateRandomBytes(32); // Khóa 256-bit
        byte[] iv = SecureRandomUtil.generateRandomBytes(12);  // IV 96-bit
        
        String originalMessage = "Đồ án Quản lý mật khẩu của Huy - Dữ liệu tuyệt mật!";
        byte[] plaintext = originalMessage.getBytes();

        // 2. MÃ HÓA
        byte[] ciphertext = aes.encrypt(plaintext, key, iv);

        // Kiểm tra độ dài: Dữ liệu mã hóa phải dài hơn bản gốc đúng 16 bytes (Auth Tag)
        assertEquals(plaintext.length + 16, ciphertext.length, 
            "Độ dài Ciphertext phải bao gồm 16 bytes Authentication Tag");

        // 3. GIẢI MÃ BÌNH THƯỜNG (Kỳ vọng: Thành công)
        byte[] decrypted = aes.decrypt(ciphertext, key, iv);
        assertEquals(originalMessage, new String(decrypted), 
            "Dữ liệu giải mã phải khớp hoàn toàn với bản gốc");

        // 4. GIẢ LẬP TẤN CÔNG: Hacker mở file vault.json và sửa đổi 1 byte dữ liệu
        // Chúng ta lật bit (flip bit) của byte đầu tiên trong mảng ciphertext
        ciphertext[0] = (byte) (ciphertext[0] ^ 0xFF); 

        // 5. GIẢI MÃ DỮ LIỆU ĐÃ BỊ SỬA (Kỳ vọng: Thất bại, văng lỗi AEADBadTagException)
        Exception exception = assertThrows(Exception.class, () -> {
            aes.decrypt(ciphertext, key, iv);
        });

        // Xác minh rằng nguyên nhân gốc (cause) của lỗi chính là sai Auth Tag
        assertTrue(exception.getCause() instanceof AEADBadTagException, 
            "Phải ném ra lỗi AEADBadTagException khi dữ liệu bị can thiệp");
            
        System.out.println("Tấn công thất bại! Lỗi bị bắt: " + exception.getMessage());
    }
}