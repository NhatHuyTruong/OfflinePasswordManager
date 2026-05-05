package vn.edu.hcmus.passman.core.security;

public interface ICryptoManager {
    
    /**
     * Dẫn xuất khóa mã hóa từ Master Password bằng thuật toán Argon2
     * @param masterPassword Mật khẩu người dùng nhập vào
     * @param salt Chuỗi muối ngẫu nhiên
     * @return Encryption Key (thường là 32 bytes cho AES-256)
     */
    byte[] deriveKey(char[] masterPassword, byte[] salt);

    /**
     * Mã hóa dữ liệu bằng AES-256-GCM
     * @param plaintext Dữ liệu dạng mảng byte cần mã hóa (VD: chuỗi JSON của đối tượng Vault)
     * @param key Khóa mã hóa 256-bit
     * @param iv Initialization Vector (thường 12 bytes cho GCM)
     * @return Ciphertext đã bao gồm cả Authentication Tag ở cuối
     */
    byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws Exception;

    /**
     * Giải mã dữ liệu và xác thực tính toàn vẹn (Check Auth Tag)
     * @param ciphertext Dữ liệu đã mã hóa
     * @param key Khóa giải mã
     * @param iv Initialization Vector đã dùng khi mã hóa
     * @return Plaintext ban đầu
     */
    byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) throws Exception;
    
    /**
     * Sinh các chuỗi byte ngẫu nhiên an toàn (Dùng cho Salt và IV)
     */
    byte[] generateSecureRandom(int length);
}