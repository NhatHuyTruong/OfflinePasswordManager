package vn.edu.hcmus.passman.core.services;

public interface IStorageService {
    /**
     * Lưu chuỗi JSON (đã mã hóa) xuống bộ nhớ.
     * @param encryptedJson Chuỗi chứa EncryptedVault
     */
    void saveVault(String encryptedJson) throws Exception;

    /**
     * Đọc chuỗi JSON (đã mã hóa) từ bộ nhớ lên.
     * @return Chuỗi mã hóa nguyên bản
     */
    String loadVault() throws Exception;

    /**
     * Kiểm tra xem file Vault đã được tạo trước đó hay chưa (dùng cho luồng Login).
     */
    boolean vaultExists();
}