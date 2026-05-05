package vn.edu.hcmus.passman.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import vn.edu.hcmus.passman.core.services.VaultManager;
import vn.edu.hcmus.passman.core.security.MemoryWiper;

public class SetupVaultController {

    // Liên kết với các thẻ FXML thông qua fx:id
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblError;
    @FXML private Button btnCreate;

    // Tham chiếu đến VaultManager từ tầng Core
    private VaultManager vaultManager;

    // Hàm này sẽ được MainApp gọi để truyền (inject) VaultManager vào
    public void setVaultManager(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
    }

    /**
     * Sự kiện khi người dùng bấm nút "Tạo Vault"
     */
    @FXML
    private void handleCreateVault() {
        // 1. Xóa thông báo lỗi cũ
        lblError.setText("");

        String pass = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        // 2. Validate dữ liệu đầu vào
        if (pass.isEmpty() || confirm.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!pass.equals(confirm)) {
            lblError.setText("Mật khẩu xác nhận không khớp!");
            return;
        }

        // 3. Chuyển String sang char[] để đưa xuống Core xử lý
        char[] masterPassword = pass.toCharArray();

        try {
            // 4. Gọi luồng khởi tạo đã viết ở tuần trước
            vaultManager.initializeVault(masterPassword);
            
            System.out.println("[UI] Tạo Vault thành công!");
            lblError.setStyle("-fx-text-fill: green;");
            lblError.setText("Khởi tạo thành công! Đang vào hệ thống...");
            
            // Chuyển sang màn hình Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            
            DashBoardController dashboardController = loader.getController();
            dashboardController.setVaultManager(vaultManager);

            // Lấy Stage (cửa sổ hiện tại) từ nút bấm
            javafx.stage.Stage stage = (javafx.stage.Stage) btnCreate.getScene().getWindow(); 

            stage.setScene(new javafx.scene.Scene(root, 700, 500));
            stage.setTitle("Offline Password Manager - Dashboard");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            lblError.setStyle("-fx-text-fill: red;");
            lblError.setText("Lỗi khởi tạo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // ĐIỂM CHỐT BẢO MẬT UI:
            // Xóa mảng char[] ngay sau khi truyền xuống Core
            MemoryWiper.clear(masterPassword);
            
            // Xóa trắng ô Text trên màn hình để chống nhìn trộm (Shoulder Surfing)
            // và ép Garbage Collector dọn dẹp biến String cũ
            txtPassword.clear();
            txtConfirmPassword.clear();
        }
    }
}