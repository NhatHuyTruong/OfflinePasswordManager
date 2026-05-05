package vn.edu.hcmus.passman.core.services;

public class MockStorageService implements IStorageService {
    
    // Đóng vai trò như một "ổ cứng ảo"
    private String fakeFileSystem = null; 

    @Override
    public void saveVault(String encryptedJson) throws Exception {
        if (encryptedJson == null || encryptedJson.trim().isEmpty()) {
            throw new Exception("Dữ liệu lưu trữ không hợp lệ (Rỗng).");
        }
        
        this.fakeFileSystem = encryptedJson;
        
        // In log ra console để dễ debug
        System.out.println("[MOCK STORAGE] Đã lưu file thành công.");
        System.out.println("[MOCK STORAGE] Dung lượng ảo: " + encryptedJson.length() + " ký tự.");
    }

    @Override
    public String loadVault() throws Exception {
        if (!vaultExists()) {
            throw new Exception("[MOCK STORAGE] Lỗi: File vault chưa tồn tại hoặc đã bị xóa!");
        }
        
        System.out.println("[MOCK STORAGE] Đã đọc file từ bộ nhớ ảo.");
        return fakeFileSystem;
    }

    @Override
    public boolean vaultExists() {
        return fakeFileSystem != null && !fakeFileSystem.isEmpty();
    }
    
    // (Tùy chọn) Hàm hỗ trợ riêng cho Mock để reset trạng thái khi chạy Unit Test
    public void clearMockDisk() {
        this.fakeFileSystem = null;
        System.out.println("[MOCK STORAGE] Đã format lại ổ cứng ảo.");
    }
}