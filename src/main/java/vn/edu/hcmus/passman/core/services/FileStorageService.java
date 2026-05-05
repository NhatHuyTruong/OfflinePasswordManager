package vn.edu.hcmus.passman.core.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class FileStorageService implements IStorageService {

    // Thư mục lưu trữ mặc định: ~/.passman/ (Thư mục ẩn ở thư mục User của Windows/Linux/Mac)
    private final Path storageDir;
    private final Path vaultFile;
    private final Path tempFile;

    public FileStorageService() {
        String userHome = System.getProperty("user.home");
        this.storageDir = Paths.get(userHome, ".passman");
        this.vaultFile = storageDir.resolve("vault.json");
        this.tempFile = storageDir.resolve("vault.json.tmp");
    }

    @Override
    public void saveVault(String encryptedJson) throws Exception {
        // 1. Đảm bảo thư mục lưu trữ đã tồn tại
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        try {
            // 2. GHI FILE TẠM (Safe Write - Bước 1)
            // Ghi toàn bộ chuỗi JSON ra file .tmp. Nếu đang ghi mà cúp điện, file chính (vault.json) không bị ảnh hưởng.
            Files.writeString(tempFile, encryptedJson, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);

            // 3. TRÁO ĐỔI FILE (Safe Write - Bước 2)
            // Lệnh ATOMIC_MOVE yêu cầu OS đổi tên file ngay lập tức. Đảm bảo không bị gián đoạn.
            Files.move(tempFile, vaultFile, 
                StandardCopyOption.REPLACE_EXISTING, 
                StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            // Nếu có lỗi, xóa file tạm để dọn rác
            Files.deleteIfExists(tempFile);
            throw new Exception("Lỗi I/O khi lưu file Vault: " + e.getMessage(), e);
        }
    }

    @Override
    public String loadVault() throws Exception {
        if (!vaultExists()) {
            throw new Exception("File vault.json không tồn tại ở đường dẫn: " + vaultFile.toString());
        }
        return Files.readString(vaultFile);
    }

    @Override
    public boolean vaultExists() {
        return Files.exists(vaultFile);
    }
}