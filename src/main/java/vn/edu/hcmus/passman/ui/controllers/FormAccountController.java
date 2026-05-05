package vn.edu.hcmus.passman.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import vn.edu.hcmus.passman.core.models.Account;
import vn.edu.hcmus.passman.core.services.VaultManager;
import vn.edu.hcmus.passman.core.security.MemoryWiper;

public class FormAccountController {

    @FXML private Label lblTitle;
    @FXML private TextField txtService;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblPasswordHint;
    @FXML private TextField txtUrl;
    @FXML private TextArea txtNote;
    @FXML private Label lblError;

    private VaultManager vaultManager;
    private Account editingAccount; // Nếu null -> Chế độ Thêm mới, nếu có giá trị -> Chế độ Sửa

    /**
     * Hàm khởi tạo dữ liệu do Dashboard truyền sang
     */
    public void setVaultManagerAndAccount(VaultManager vaultManager, Account account) {
        this.vaultManager = vaultManager;
        this.editingAccount = account;

        // Nếu là chế độ Sửa (Edit Mode)
        if (this.editingAccount != null) {
            lblTitle.setText("Sửa Tài Khoản: " + account.getService());
            txtService.setText(account.getService());
            txtUsername.setText(account.getUsername());
            txtUrl.setText(account.getUrl());
            txtNote.setText(account.getNote());
            
            // Hiển thị gợi ý: Không nhập pass thì giữ nguyên pass cũ
            lblPasswordHint.setVisible(true);
            lblPasswordHint.setManaged(true);
        }
    }

    @FXML
    private void handleSave() {
        lblError.setText("");
        
        String service = txtService.getText().trim();
        String username = txtUsername.getText().trim();
        String pass = txtPassword.getText();
        String url = txtUrl.getText().trim();
        String note = txtNote.getText().trim();

        // Validate cơ bản
        if (service.isEmpty()) {
            lblError.setText("Tên dịch vụ không được để trống!");
            return;
        }

        char[] passwordChars = null;
        try {
            if (editingAccount == null) {
                // --- CHẾ ĐỘ THÊM MỚI ---
                if (pass.isEmpty()) {
                    lblError.setText("Vui lòng nhập mật khẩu!");
                    return;
                }
                passwordChars = pass.toCharArray();
                vaultManager.addAccount(service, username, passwordChars, url, note);
                
            } else {
                // --- CHẾ ĐỘ SỬA ---
                // Nếu người dùng có nhập mật khẩu mới thì cập nhật, không thì truyền mảng rỗng
                if (!pass.isEmpty()) {
                    passwordChars = pass.toCharArray();
                } else {
                    passwordChars = new char[0]; 
                }
                
                vaultManager.updateAccount(editingAccount.getId(), service, username, passwordChars, url, note);
                
                // Cập nhật lại UI cho object trong RAM (vì TableView bind trực tiếp vào object này)
                editingAccount.setService(service);
                editingAccount.setUsername(username);
                editingAccount.setUrl(url);
                editingAccount.setNote(note);
            }

            // [QUAN TRỌNG] Lưu thay đổi xuống ổ cứng (Safe Write)
            vaultManager.saveChanges();
            System.out.println("[UI] Đã lưu dữ liệu thành công xuống ổ cứng.");

            // Đóng cửa sổ
            closeWindow();

        } catch (Exception e) {
            lblError.setText("Lỗi lưu dữ liệu: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Dọn dẹp RAM cực kỳ khắt khe
            if (passwordChars != null && passwordChars.length > 0) {
                MemoryWiper.clear(passwordChars);
            }
            txtPassword.clear();
        }
    }

    @FXML
    private void handleCancel() {
        txtPassword.clear(); // Xóa pass rác trên UI nếu có
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtService.getScene().getWindow();
        stage.close();
    }
}