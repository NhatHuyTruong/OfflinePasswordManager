package vn.edu.hcmus.passman.core.models;

import java.util.UUID;

public class Account {
    private String id;
    private String service;
    private String username;
    private char[] password; // Lưu password dạng mảng char trong RAM
    private String url;
    private String note;

    /**
     * Constructor dùng khi tạo tài khoản mới
     */
    public Account(String service, String username, char[] password, String url, String note) {
        // Tự động sinh ID ngẫu nhiên (VD: "123e4567-e89b-12d3-a456-426614174000")
        this.id = UUID.randomUUID().toString(); 
        this.service = service;
        this.username = username;
        // Clone (sao chép) mảng password để tránh bị xóa nhầm khi UI gọi MemoryWiper
        this.password = password != null ? password.clone() : new char[0];
        this.url = url;
        this.note = note;
    }

    // =========================================
    // GETTERS (Dùng để hiển thị lên TableView)
    // =========================================
    public String getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getNote() {
        return note;
    }

    // =========================================
    // SETTERS (Dùng để cập nhật khi Edit)
    // =========================================
    public void setService(String service) {
        this.service = service;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(char[] password) {
        // Clone mảng mới để lưu vào RAM, ngắt tham chiếu với mảng cũ từ UI
        this.password = password != null ? password.clone() : new char[0];
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setNote(String note) {
        this.note = note;
    }
}