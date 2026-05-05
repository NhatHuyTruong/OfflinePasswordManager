package vn.edu.hcmus.passman.core.services;

import com.google.gson.Gson;

import vn.edu.hcmus.passman.core.models.Account;
import vn.edu.hcmus.passman.core.models.EncryptedVault;
import vn.edu.hcmus.passman.core.models.Vault;
import vn.edu.hcmus.passman.core.security.ICryptoManager;
import vn.edu.hcmus.passman.core.security.MemoryWiper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class VaultManager {

    private final ICryptoManager cryptoManager;
    private final IStorageService storageService;
    private final Gson gson;
    private byte[] sessionKey;
    private byte[] currentSalt;

    // Bộ nhớ RAM chứa Vault đang được mở (Sẽ được dùng nhiều ở các thao tác CRUD sau này)
    private Vault currentVault;

    // Dependency Injection: Nhận các module từ bên ngoài truyền vào
    public VaultManager(ICryptoManager cryptoManager, IStorageService storageService) {
        this.cryptoManager = cryptoManager;
        this.storageService = storageService;
        this.gson = new Gson();
        this.currentVault = null;
    }

    /**
     * Quy trình 1: Khởi tạo Vault mới hoàn toàn
     * @param masterPassword Mật khẩu gốc do người dùng nhập (char[])
     */
    public void initializeVault(char[] masterPassword) throws Exception {
        if (masterPassword == null || masterPassword.length == 0) {
            throw new IllegalArgumentException("Master Password không được để trống.");
        }

        byte[] key = null;
        byte[] plaintextBytes = null;
        byte[] ciphertext = null;

        try {
            // Bước 1: Sinh Salt (16 bytes) và Dẫn xuất Khóa AES (32 bytes) bằng Argon2
            byte[] salt = cryptoManager.generateSecureRandom(16);
            key = cryptoManager.deriveKey(masterPassword, salt);

            // Bước 2: Tạo Vault rỗng và chuyển thành chuỗi JSON
            this.currentVault = new Vault();
            String vaultJson = gson.toJson(this.currentVault);
            plaintextBytes = vaultJson.getBytes(StandardCharsets.UTF_8);

            // Bước 3: Sinh IV (12 bytes) và Mã hóa Vault JSON bằng AES-256-GCM
            byte[] iv = cryptoManager.generateSecureRandom(12);
            ciphertext = cryptoManager.encrypt(plaintextBytes, key, iv);

            // Bước 4: Đóng gói dữ liệu để chuẩn bị lưu file
            // LƯU Ý: Phải chuyển byte[] sang Base64 thì mới lưu dạng text (JSON) được an toàn
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext);

            EncryptedVault.CryptoMetadata metadata = new EncryptedVault.CryptoMetadata(saltBase64, ivBase64);
            EncryptedVault encryptedVault = new EncryptedVault(metadata, ciphertextBase64);

            // Bước 5: Serialize object EncryptedVault thành JSON và lưu xuống bộ nhớ (IStorageService)
            String finalJsonToSave = gson.toJson(encryptedVault);
            storageService.saveVault(finalJsonToSave);

        } finally {
            // ĐIỂM CHỐT BẢO MẬT BỘ NHỚ:
            // Dù quá trình khởi tạo thành công hay bị lỗi (crash giữa chừng),
            // khối finally luôn được chạy để dọn dẹp các byte nhạy cảm khỏi RAM.
            MemoryWiper.clear(key);
            MemoryWiper.clear(plaintextBytes);
            
            // Xóa Master Password khỏi RAM (UI truyền xuống, Core dùng xong xóa ngay)
            MemoryWiper.clear(masterPassword); 
        }
    }
    
    /**
     * Quy trình 2: Mở khóa Vault đã có sẵn
     * @param masterPassword Mật khẩu gốc do người dùng nhập để mở khóa
     */
    public void unlockVault(char[] masterPassword) throws Exception {
        if (masterPassword == null || masterPassword.length == 0) {
            throw new IllegalArgumentException("Vui lòng nhập Master Password.");
        }

        // Bước 1: Kiểm tra xem file vault.json có tồn tại không
        if (!storageService.vaultExists()) {
            throw new Exception("Không tìm thấy dữ liệu Vault. Vui lòng khởi tạo Vault trước.");
        }

        byte[] key = null;
        byte[] plaintextBytes = null;

        try {
            // Bước 2: Đọc chuỗi JSON từ ổ cứng lên
            String encryptedJson = storageService.loadVault();

            // Bước 3: Deserialize JSON thành đối tượng EncryptedVault
            EncryptedVault encryptedVault = gson.fromJson(encryptedJson, EncryptedVault.class);
            EncryptedVault.CryptoMetadata metadata = encryptedVault.getCrypto();

            // Bước 4: Giải mã Base64 để lấy lại mảng byte gốc của Salt, IV và Ciphertext
            byte[] salt = Base64.getDecoder().decode(metadata.getSalt());
            byte[] iv = Base64.getDecoder().decode(metadata.getIv());
            byte[] ciphertext = Base64.getDecoder().decode(encryptedVault.getCiphertext());

            // Bước 5: Chạy Argon2id cùng với Salt (từ file) để sinh ra Encryption Key
            key = cryptoManager.deriveKey(masterPassword, salt);

            // Bước 6: Đưa vào AES-256-GCM để giải mã. 
            // NẾU SAI MẬT KHẨU HOẶC FILE BỊ SỬA: Hàm decrypt sẽ ném AEADBadTagException ngay lập tức!
            plaintextBytes = cryptoManager.decrypt(ciphertext, key, iv);

            // Bước 7: Quá trình giải mã thành công -> Parse JSON trả về RAM
            String vaultJson = new String(plaintextBytes, StandardCharsets.UTF_8);
            this.currentVault = gson.fromJson(vaultJson, Vault.class);

            // Bước 8: LƯU SESSION KEY & SALT ĐỂ DÙNG KHI LƯU FILE
            this.sessionKey = Arrays.copyOf(key, key.length);
            this.currentSalt = Arrays.copyOf(salt, salt.length);

        } catch (Exception e) {
            // Bắt mọi lỗi (bao gồm lỗi do Authentication Tag) và ném ra thông báo chung 
            // theo nguyên tắc bảo mật: Không tiết lộ cho user biết chính xác lỗi do sai pass hay hỏng file.
            throw new Exception("Mở khóa thất bại! Mật khẩu không đúng hoặc tệp dữ liệu đã bị can thiệp.", e);
        } finally {
            // ĐIỂM CHỐT BẢO MẬT BỘ NHỚ:
            // Tuyệt đối không để Key giải mã và bản rõ (plaintext) nằm lại trong RAM
            MemoryWiper.clear(key);
            MemoryWiper.clear(plaintextBytes);
            
            // Xóa Master Password do UI truyền xuống
            MemoryWiper.clear(masterPassword);
        }
    }

    /**
     * [THÊM MỚI] Lưu dữ liệu từ RAM xuống ổ cứng (Serialize & Safe Write)
     */
    public void saveChanges() throws Exception {
        ensureVaultLoaded();

        if (this.sessionKey == null || this.currentSalt == null) {
            throw new Exception("Lỗi hệ thống: Không tìm thấy Session Key để mã hóa.");
        }

        try {
            // 1. Object (RAM) -> JSON (Plaintext)
            String vaultJson = gson.toJson(this.currentVault);
            byte[] plaintextBytes = vaultJson.getBytes(StandardCharsets.UTF_8);

            // 2. Sinh IV hoàn toàn MỚI cho lần mã hóa này (KHÔNG BAO GIỜ DÙNG LẠI IV CŨ)
            byte[] newIv = cryptoManager.generateSecureRandom(12);

            // 3. Mã hóa dữ liệu bằng Session Key đang có trong RAM
            byte[] ciphertext = cryptoManager.encrypt(plaintextBytes, this.sessionKey, newIv);

            // 4. Encode sang Base64 và tạo đối tượng EncryptedVault
            String saltBase64 = Base64.getEncoder().encodeToString(this.currentSalt);
            String ivBase64 = Base64.getEncoder().encodeToString(newIv);
            String ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext);

            EncryptedVault.CryptoMetadata metadata = new EncryptedVault.CryptoMetadata(saltBase64, ivBase64);
            EncryptedVault encryptedVault = new EncryptedVault(metadata, ciphertextBase64);

            // 5. Serialize EncryptedVault -> JSON String và gọi Safe Write
            String finalJsonToSave = gson.toJson(encryptedVault);
            storageService.saveVault(finalJsonToSave);

            // Dọn RAM bản rõ
            MemoryWiper.clear(plaintextBytes);

        } catch (Exception e) {
            throw new Exception("Quá trình mã hóa và lưu file thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * [THÊM MỚI] Chủ động khóa Vault và dọn dẹp TOÀN BỘ RAM
     */
    public void lockVault() {
        // 1. Xóa Session Key
        MemoryWiper.clear(this.sessionKey);
        this.sessionKey = null;
        
        // 2. Xóa Salt
        MemoryWiper.clear(this.currentSalt);
        this.currentSalt = null;

        // 3. Xóa dữ liệu từng tài khoản trong Vault
        if (this.currentVault != null && this.currentVault.getAccounts() != null) {
            for (vn.edu.hcmus.passman.core.models.Account acc : this.currentVault.getAccounts()) {
                MemoryWiper.clear(acc.getPassword());
            }
        }
        
        // 4. Hủy object Vault
        this.currentVault = null;
    }

    // =====================================================================
    // CÁC THAO TÁC CRUD TRONG RAM
    // =====================================================================

    /**
     * Hàm kiểm tra an toàn: Đảm bảo Vault đã được mở trước khi thao tác
     */
    private void ensureVaultLoaded() throws Exception {
        if (this.currentVault == null) {
            throw new Exception("Lỗi truy cập: Vault chưa được mở (Locked).");
        }
    }

    /**
     * [READ] Lấy danh sách toàn bộ tài khoản
     */
    public java.util.List<Account> getAllAccounts() throws Exception {
        ensureVaultLoaded();
        return this.currentVault.getAccounts();
    }

    /**
     * [CREATE] Thêm một tài khoản mới vào Vault
     * @param password Mật khẩu được truyền dưới dạng char[] từ UI
     */
    public void addAccount(String service, String username, char[] password, String url, String note) throws Exception {
        ensureVaultLoaded();
        
        Account newAccount = new Account(service, username, password, url, note);
        this.currentVault.addAccount(newAccount);
        
        // Lưu ý: Dọn dẹp mảng char[] đầu vào từ UI ngay sau khi copy vào Account object
        MemoryWiper.clear(password);
    }

    /**
     * [UPDATE] Cập nhật thông tin tài khoản hiện có
     */
    public void updateAccount(String id, String newService, String newUsername, char[] newPassword, String newUrl, String newNote) throws Exception {
        ensureVaultLoaded();

        for (Account acc : this.currentVault.getAccounts()) {
            if (acc.getId().equals(id)) {
                // Nếu người dùng có nhập mật khẩu mới
                if (newPassword != null && newPassword.length > 0) {
                    // ĐIỂM CHỐT BẢO MẬT: Phải xóa trắng mật khẩu CŨ trong RAM trước khi ghi đè
                    MemoryWiper.clear(acc.getPassword());
                    acc.setPassword(newPassword);
                    
                    // Dọn dẹp mảng đầu vào từ UI
                    MemoryWiper.clear(newPassword);
                }
                
                // Cập nhật các trường thông thường (không nhạy cảm) bằng reflection hoặc setter
                // Vì ở Account.java ta chưa viết đủ setter, bạn có thể bổ sung các setter cơ bản bên class Account.
                // Ví dụ (cần thêm setter trong Account.java):
                // acc.setService(newService);
                // acc.setUsername(newUsername);
                // acc.setUrl(newUrl);
                // acc.setNote(newNote);
                return;
            }
        }
        throw new Exception("Không tìm thấy tài khoản với ID: " + id);
    }

    /**
     * [DELETE] Xóa một tài khoản khỏi Vault
     */
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
            // ĐIỂM CHỐT BẢO MẬT BỘ NHỚ: 
            // Nếu chỉ gọi removeAccount(), object Account vẫn trôi nổi trong RAM chờ GC dọn dẹp.
            // Phải thủ công ghi đè toàn bộ số 0 lên mảng char[] chứa mật khẩu trước khi gỡ tham chiếu.
            MemoryWiper.clear(targetAccount.getPassword());
            
            // Sau khi đã xóa dữ liệu nhạy cảm, mới gỡ bỏ object khỏi danh sách
            this.currentVault.removeAccount(id);
        } else {
            throw new Exception("Xóa thất bại: Không tìm thấy tài khoản.");
        }
    }

    // Các hàm phụ trợ
    public boolean isVaultLoaded() {
        return currentVault != null;
    }

    public Vault getCurrentVault() {
        return currentVault;
    }
}