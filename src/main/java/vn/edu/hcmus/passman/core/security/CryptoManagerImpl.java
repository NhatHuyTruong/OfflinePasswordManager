package vn.edu.hcmus.passman.core.security;

public class CryptoManagerImpl implements ICryptoManager {

    // Khai báo các module chuyên biệt đã được tách ra
    private final KeyGenerator keyGenerator;
    private final AesGcmEncryption aesGcmEncryption;

    public CryptoManagerImpl() {
        // Khởi tạo các module con
        this.keyGenerator = new KeyGenerator();
        this.aesGcmEncryption = new AesGcmEncryption();
    }

    @Override
    public byte[] generateSecureRandom(int length) {
        // Gọi thẳng vào class Utility dùng SecureRandom
        return SecureRandomUtil.generateRandomBytes(length);
    }

    @Override
    public byte[] deriveKey(char[] masterPassword, byte[] salt) {
        // Ủy quyền (delegate) cho class KeyGenerator xử lý Argon2
        return keyGenerator.deriveKey(masterPassword, salt);
    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws Exception {
        // Ủy quyền cho class AesGcmEncryption xử lý
        return aesGcmEncryption.encrypt(plaintext, key, iv);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) throws Exception {
        // Ủy quyền cho class AesGcmEncryption xử lý và check Auth Tag
        return aesGcmEncryption.decrypt(ciphertext, key, iv);
    }
}