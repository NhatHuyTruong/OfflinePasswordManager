package vn.edu.hcmus.passman.core.services;

import com.google.gson.Gson;
import com.codahale.shamir.Scheme;
import vn.edu.hcmus.passman.core.models.Account;
import vn.edu.hcmus.passman.core.models.EncryptedVault;
import vn.edu.hcmus.passman.core.models.QuestionAnswer;
import vn.edu.hcmus.passman.core.models.Vault;
import vn.edu.hcmus.passman.core.security.ICryptoManager;
import vn.edu.hcmus.passman.core.security.MemoryWiper;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

public class VaultManager {

    private final ICryptoManager cryptoManager;
    private final IStorageService storageService;
    private final Gson gson;
    
    private byte[] sessionKey; 
    private Vault currentVault;

    public VaultManager(ICryptoManager cryptoManager, IStorageService storageService) {
        this.cryptoManager = cryptoManager;
        this.storageService = storageService;
        this.gson = new Gson();
        this.currentVault = null;
    }

    public void initializeVault(char[] masterPassword, List<QuestionAnswer> securityQuestions) throws Exception {
        if (masterPassword == null || masterPassword.length == 0) {
            throw new IllegalArgumentException("Master Password không được để trống.");
        }
        
        int n = (securityQuestions == null) ? 0 : securityQuestions.size();
        if (n > 0 && n < 3) {
            throw new IllegalArgumentException("Nếu sử dụng tính năng khôi phục, cần ít nhất 3 câu hỏi.");
        }

        byte[] dek = null;
        byte[] masterKey = null;

        try {
            dek = cryptoManager.generateSecureRandom(32);

            byte[] masterSalt = cryptoManager.generateSecureRandom(16);
            masterKey = cryptoManager.deriveKey(masterPassword, masterSalt);
            byte[] masterIv = cryptoManager.generateSecureRandom(12);
            byte[] encryptedDekMaster = cryptoManager.encrypt(dek, masterKey, masterIv);
            
            EncryptedVault.MasterKeyMetadata mkMeta = new EncryptedVault.MasterKeyMetadata(
                    Base64.getEncoder().encodeToString(masterSalt),
                    Base64.getEncoder().encodeToString(masterIv),
                    Base64.getEncoder().encodeToString(encryptedDekMaster));

            EncryptedVault.RecoveryMetadata recMeta = null;

            if (n >= 3) {
                int k = (n / 2) + 1; // Nửa số câu hỏi + 1
                Scheme scheme = new Scheme(new SecureRandom(), n, k);
                Map<Integer, byte[]> shares = scheme.split(dek);
                List<EncryptedVault.EncryptedShare> encryptedSharesList = new ArrayList<>();
                
                for (int i = 0; i < n; i++) {
                    QuestionAnswer qa = securityQuestions.get(i);
                    int shareIndex = i + 1;
                    byte[] shareBytes = shares.get(shareIndex);
                    
                    byte[] shareSalt = cryptoManager.generateSecureRandom(16);
                    byte[] shareKey = cryptoManager.deriveKey(qa.getAnswer(), shareSalt); 
                    byte[] shareIv = cryptoManager.generateSecureRandom(12);
                    byte[] encryptedShareBytes = cryptoManager.encrypt(shareBytes, shareKey, shareIv);
                    
                    encryptedSharesList.add(new EncryptedVault.EncryptedShare(
                        qa.getQuestion(), shareIndex,
                        Base64.getEncoder().encodeToString(shareSalt),
                        Base64.getEncoder().encodeToString(shareIv),
                        Base64.getEncoder().encodeToString(encryptedShareBytes)
                    ));
                    
                    MemoryWiper.clear(shareKey);
                    MemoryWiper.clear(shareBytes);
                }
                recMeta = new EncryptedVault.RecoveryMetadata(k, encryptedSharesList);
            }

            this.currentVault = new Vault();
            String vaultJson = gson.toJson(this.currentVault);
            byte[] vaultIv = cryptoManager.generateSecureRandom(12);
            byte[] ciphertext = cryptoManager.encrypt(vaultJson.getBytes(StandardCharsets.UTF_8), dek, vaultIv);
            
            EncryptedVault.VaultMetadata vaultMeta = new EncryptedVault.VaultMetadata(Base64.getEncoder().encodeToString(vaultIv));
            
            EncryptedVault encryptedVault = new EncryptedVault(vaultMeta, mkMeta, recMeta, Base64.getEncoder().encodeToString(ciphertext));

            storageService.saveVault(gson.toJson(encryptedVault));

            this.sessionKey = Arrays.copyOf(dek, dek.length);

        } finally {
            MemoryWiper.clear(dek);
            MemoryWiper.clear(masterKey);
            MemoryWiper.clear(masterPassword);
            if (securityQuestions != null) {
                for(QuestionAnswer qa : securityQuestions) {
                    MemoryWiper.clear(qa.getAnswer());
                }
            }
        }
    }
    
    public void unlockVault(char[] masterPassword) throws Exception {
        if (masterPassword == null || masterPassword.length == 0) {
            throw new IllegalArgumentException("Vui lòng nhập Master Password.");
        }
        if (!storageService.vaultExists()) {
            throw new Exception("Không tìm thấy dữ liệu Vault. Vui lòng khởi tạo Vault trước.");
        }

        byte[] masterKey = null;
        byte[] dek = null;
        byte[] plaintextBytes = null;

        try {
            String encryptedJson = storageService.loadVault();
            EncryptedVault encryptedVault = gson.fromJson(encryptedJson, EncryptedVault.class);
            EncryptedVault.MasterKeyMetadata mkMeta = encryptedVault.getMasterKeyMetadata();
            EncryptedVault.VaultMetadata vaultMeta = encryptedVault.getVaultMetadata();

            byte[] masterSalt = Base64.getDecoder().decode(mkMeta.getSalt());
            byte[] masterIv = Base64.getDecoder().decode(mkMeta.getIv());
            byte[] encryptedDekMaster = Base64.getDecoder().decode(mkMeta.getEncryptedDek());

            masterKey = cryptoManager.deriveKey(masterPassword, masterSalt);
            dek = cryptoManager.decrypt(encryptedDekMaster, masterKey, masterIv);

            byte[] vaultIv = Base64.getDecoder().decode(vaultMeta.getIv());
            byte[] ciphertext = Base64.getDecoder().decode(encryptedVault.getCiphertext());
            
            plaintextBytes = cryptoManager.decrypt(ciphertext, dek, vaultIv);
            String vaultJson = new String(plaintextBytes, StandardCharsets.UTF_8);
            this.currentVault = gson.fromJson(vaultJson, Vault.class);

            this.sessionKey = Arrays.copyOf(dek, dek.length);

        } catch (Exception e) {
            throw new Exception("Mở khóa thất bại! Mật khẩu không đúng hoặc tệp dữ liệu đã bị can thiệp.", e);
        } finally {
            MemoryWiper.clear(masterKey);
            MemoryWiper.clear(dek);
            MemoryWiper.clear(plaintextBytes);
            MemoryWiper.clear(masterPassword);
        }
    }

    public void recoverVault(List<QuestionAnswer> recoveryAnswers, char[] newMasterPassword) throws Exception {
        if (!storageService.vaultExists()) {
            throw new Exception("Không tìm thấy dữ liệu Vault.");
        }

        String encryptedJson = storageService.loadVault();
        EncryptedVault encryptedVault = gson.fromJson(encryptedJson, EncryptedVault.class);
        EncryptedVault.RecoveryMetadata recMeta = encryptedVault.getRecoveryMetadata();
        
        if (recMeta == null) {
            throw new Exception("Vault này không được thiết lập tính năng khôi phục mật khẩu.");
        }

        int n = recMeta.getShares().size();
        int k = recMeta.getThreshold();

        Map<Integer, byte[]> recoveredShares = new HashMap<>();

        try {
            for (QuestionAnswer qa : recoveryAnswers) {
                EncryptedVault.EncryptedShare targetShare = null;
                for (EncryptedVault.EncryptedShare share : recMeta.getShares()) {
                    if (share.getQuestionText().equals(qa.getQuestion())) {
                        targetShare = share;
                        break;
                    }
                }
                
                if (targetShare == null) continue;

                byte[] shareSalt = Base64.getDecoder().decode(targetShare.getSalt());
                byte[] shareIv = Base64.getDecoder().decode(targetShare.getIv());
                byte[] encryptedShareBytes = Base64.getDecoder().decode(targetShare.getEncryptedShareBytes());
                
                byte[] shareKey = null;
                byte[] sharePlaintext = null;
                try {
                    shareKey = cryptoManager.deriveKey(qa.getAnswer(), shareSalt);
                    sharePlaintext = cryptoManager.decrypt(encryptedShareBytes, shareKey, shareIv);
                    recoveredShares.put(targetShare.getShareIndex(), sharePlaintext);
                } catch (Exception ignored) {
                } finally {
                    MemoryWiper.clear(shareKey);
                }
            }

            if (recoveredShares.size() < k) {
                throw new Exception("Khôi phục thất bại. Cần ít nhất " + k + " câu trả lời đúng (bạn đúng " + recoveredShares.size() + "/" + k + ").");
            }

            Scheme scheme = new Scheme(new SecureRandom(), n, k);
            byte[] dek = scheme.join(recoveredShares);

            byte[] masterSalt = cryptoManager.generateSecureRandom(16);
            byte[] masterKey = cryptoManager.deriveKey(newMasterPassword, masterSalt);
            byte[] masterIv = cryptoManager.generateSecureRandom(12);
            byte[] encryptedDekMaster = cryptoManager.encrypt(dek, masterKey, masterIv);
            
            EncryptedVault.MasterKeyMetadata mkMeta = new EncryptedVault.MasterKeyMetadata(
                    Base64.getEncoder().encodeToString(masterSalt),
                    Base64.getEncoder().encodeToString(masterIv),
                    Base64.getEncoder().encodeToString(encryptedDekMaster));

            EncryptedVault updatedVault = new EncryptedVault(
                encryptedVault.getVaultMetadata(), 
                mkMeta, 
                encryptedVault.getRecoveryMetadata(), 
                encryptedVault.getCiphertext()
            );
            
            storageService.saveVault(gson.toJson(updatedVault));
            
            MemoryWiper.clear(dek);
            MemoryWiper.clear(masterKey);

        } finally {
            for (byte[] shareBytes : recoveredShares.values()) {
                MemoryWiper.clear(shareBytes);
            }
            for (QuestionAnswer qa : recoveryAnswers) {
                MemoryWiper.clear(qa.getAnswer());
            }
            MemoryWiper.clear(newMasterPassword);
        }
    }

    public void saveChanges() throws Exception {
        ensureVaultLoaded();

        try {
            String vaultJson = gson.toJson(this.currentVault);
            byte[] plaintextBytes = vaultJson.getBytes(StandardCharsets.UTF_8);

            byte[] newVaultIv = cryptoManager.generateSecureRandom(12);
            byte[] ciphertext = cryptoManager.encrypt(plaintextBytes, this.sessionKey, newVaultIv);
            
            String encryptedJson = storageService.loadVault();
            EncryptedVault oldVault = gson.fromJson(encryptedJson, EncryptedVault.class);

            EncryptedVault.VaultMetadata vaultMeta = new EncryptedVault.VaultMetadata(Base64.getEncoder().encodeToString(newVaultIv));
            
            EncryptedVault encryptedVault = new EncryptedVault(
                vaultMeta, 
                oldVault.getMasterKeyMetadata(), 
                oldVault.getRecoveryMetadata(), 
                Base64.getEncoder().encodeToString(ciphertext)
            );

            storageService.saveVault(gson.toJson(encryptedVault));
            MemoryWiper.clear(plaintextBytes);
        } catch (Exception e) {
            throw new Exception("Lưu thay đổi thất bại: " + e.getMessage(), e);
        }
    }

    public void lockVault() {
        MemoryWiper.clear(this.sessionKey);
        this.sessionKey = null;

        if (this.currentVault != null && this.currentVault.getAccounts() != null) {
            for (Account acc : this.currentVault.getAccounts()) {
                MemoryWiper.clear(acc.getPassword());
            }
        }
        this.currentVault = null;
    }

    private void ensureVaultLoaded() throws Exception {
        if (this.currentVault == null || this.sessionKey == null) {
            throw new Exception("Lỗi truy cập: Vault chưa được mở (Locked).");
        }
    }

    public java.util.List<Account> getAllAccounts() throws Exception {
        ensureVaultLoaded();
        return this.currentVault.getAccounts();
    }

    public void addAccount(String service, String username, char[] password, String url, String note) throws Exception {
        ensureVaultLoaded();
        Account newAccount = new Account(service, username, password, url, note);
        this.currentVault.addAccount(newAccount);
        MemoryWiper.clear(password);
    }

    public void updateAccount(String id, String newService, String newUsername, char[] newPassword, String newUrl, String newNote) throws Exception {
        ensureVaultLoaded();
        for (Account acc : this.currentVault.getAccounts()) {
            if (acc.getId().equals(id)) {
                if (newPassword != null && newPassword.length > 0) {
                    MemoryWiper.clear(acc.getPassword());
                    acc.setPassword(newPassword);
                    MemoryWiper.clear(newPassword);
                }
                acc.setService(newService);
                acc.setUsername(newUsername);
                acc.setUrl(newUrl);
                acc.setNote(newNote);
                return;
            }
        }
        throw new Exception("Không tìm thấy tài khoản với ID: " + id);
    }

    public void deleteAccount(String id) throws Exception {
        ensureVaultLoaded();
        Account targetAccount = null;
        for (Account acc : this.currentVault.getAccounts()) {
            if (acc.getId().equals(id)) {
                targetAccount = acc;
                break;
            }
        }
        if (targetAccount != null) {
            MemoryWiper.clear(targetAccount.getPassword());
            this.currentVault.removeAccount(id);
        } else {
            throw new Exception("Xóa thất bại: Không tìm thấy tài khoản.");
        }
    }

    public boolean isVaultLoaded() {
        return currentVault != null;
    }

    public Vault getCurrentVault() {
        return currentVault;
    }
    
    public int getRecoveryThreshold() throws Exception {
        if (!storageService.vaultExists()) throw new Exception("Vault chưa được tạo.");
        String encryptedJson = storageService.loadVault();
        EncryptedVault encryptedVault = gson.fromJson(encryptedJson, EncryptedVault.class);
        if (encryptedVault.getRecoveryMetadata() == null) {
            throw new Exception("Vault này không cài đặt tính năng khôi phục.");
        }
        return encryptedVault.getRecoveryMetadata().getThreshold();
    }

    public List<String> getRecoveryQuestions() throws Exception {
        if (!storageService.vaultExists()) {
            throw new Exception("Vault chưa được tạo.");
        }
        String encryptedJson = storageService.loadVault();
        EncryptedVault encryptedVault = gson.fromJson(encryptedJson, EncryptedVault.class);
        if (encryptedVault.getRecoveryMetadata() == null) {
            throw new Exception("Vault này không cài đặt tính năng khôi phục.");
        }
        List<String> questions = new ArrayList<>();
        for (EncryptedVault.EncryptedShare share : encryptedVault.getRecoveryMetadata().getShares()) {
            questions.add(share.getQuestionText());
        }
        return questions;
    }
}