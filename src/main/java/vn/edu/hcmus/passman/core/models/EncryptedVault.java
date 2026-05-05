package vn.edu.hcmus.passman.core.models;

public class EncryptedVault {
    private CryptoMetadata crypto;
    private String ciphertext; 
    public EncryptedVault(CryptoMetadata crypto, String ciphertext) {
        this.crypto = crypto;
        this.ciphertext = ciphertext;
    }

    public CryptoMetadata getCrypto() { return crypto; }
    public String getCiphertext() { return ciphertext; }

    // Inner class chứa siêu dữ liệu cho quá trình giải mã
    public static class CryptoMetadata {
        private String salt; // Base64
        private String iv;   // Base64

        public CryptoMetadata(String salt, String iv) {
            this.salt = salt;
            this.iv = iv;
        }

        public String getSalt() { return salt; }
        public String getIv() { return iv; }
    }
}