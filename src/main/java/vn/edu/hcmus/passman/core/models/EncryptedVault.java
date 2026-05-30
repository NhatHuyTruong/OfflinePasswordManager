package vn.edu.hcmus.passman.core.models;

import java.util.List;

public class EncryptedVault {
    private VaultMetadata vaultMetadata;
    private MasterKeyMetadata masterKeyMetadata;
    private RecoveryMetadata recoveryMetadata;
    private String ciphertext; // Dữ liệu Vault mã hóa bằng DEK

    public EncryptedVault(VaultMetadata vaultMetadata, MasterKeyMetadata masterKeyMetadata, 
                          RecoveryMetadata recoveryMetadata, String ciphertext) {
        this.vaultMetadata = vaultMetadata;
        this.masterKeyMetadata = masterKeyMetadata;
        this.recoveryMetadata = recoveryMetadata;
        this.ciphertext = ciphertext;
    }

    public VaultMetadata getVaultMetadata() { return vaultMetadata; }
    public MasterKeyMetadata getMasterKeyMetadata() { return masterKeyMetadata; }
    public RecoveryMetadata getRecoveryMetadata() { return recoveryMetadata; }
    public String getCiphertext() { return ciphertext; }

    public static class VaultMetadata {
        private String iv; // IV dùng để mã hóa Vault (Base64)
        public VaultMetadata(String iv) { this.iv = iv; }
        public String getIv() { return iv; }
    }

    public static class MasterKeyMetadata {
        private String salt; // Base64
        private String iv;   // Base64
        private String encryptedDek; // Base64
        
        public MasterKeyMetadata(String salt, String iv, String encryptedDek) {
            this.salt = salt;
            this.iv = iv;
            this.encryptedDek = encryptedDek;
        }

        public String getSalt() { return salt; }
        public String getIv() { return iv; }
        public String getEncryptedDek() { return encryptedDek; }
    }

    public static class RecoveryMetadata {
        private int threshold;
        private List<EncryptedShare> shares;

        public RecoveryMetadata(int threshold, List<EncryptedShare> shares) {
            this.threshold = threshold;
            this.shares = shares;
        }

        public int getThreshold() { return threshold; }
        public List<EncryptedShare> getShares() { return shares; }
    }

    public static class EncryptedShare {
        private String questionText;
        private int shareIndex; // Tương ứng giá trị x trong SSS (1 đến 10)
        private String salt; // Base64 (dùng để băm câu trả lời)
        private String iv;   // Base64
        private String encryptedShareBytes; // Base64 (mảnh y đã bị mã hóa bằng câu trả lời)

        public EncryptedShare(String questionText, int shareIndex, String salt, String iv, String encryptedShareBytes) {
            this.questionText = questionText;
            this.shareIndex = shareIndex;
            this.salt = salt;
            this.iv = iv;
            this.encryptedShareBytes = encryptedShareBytes;
        }

        public String getQuestionText() { return questionText; }
        public int getShareIndex() { return shareIndex; }
        public String getSalt() { return salt; }
        public String getIv() { return iv; }
        public String getEncryptedShareBytes() { return encryptedShareBytes; }
    }
}