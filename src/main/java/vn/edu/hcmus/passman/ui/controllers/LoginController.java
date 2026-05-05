package vn.edu.hcmus.passman.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import vn.edu.hcmus.passman.core.services.VaultManager;
import vn.edu.hcmus.passman.core.security.MemoryWiper;

public class LoginController {

    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    private VaultManager vaultManager;

    public void setVaultManager(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
    }

    @FXML
    private void handleLogin() {
        lblError.setText("");
        String pass = txtPassword.getText();

        if (pass.isEmpty()) {
            lblError.setText("Vui lòng nhập mật khẩu!");
            return;
        }

        // ĐIỂM CHỐT BẢO MẬT: Đổi sang char[] ngay lập tức
        char[] masterPassword = pass.toCharArray();

        try {
            // Nút thắt quan trọng nhất: Gọi hàm Unlock
            // Hàm này sẽ ném lỗi AEADBadTagException nếu nhập sai mật khẩu
            vaultManager.unlockVault(masterPassword);
            
            lblError.setStyle("-fx-text-fill: green;");
            lblError.setText("Mở khóa thành công!");
            System.out.println("[UI] Giải mã Vault thành công. Đang vào Dashboard...");
            
            // Chuyển sang màn hình Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            
            DashBoardController dashboardController = loader.getController();
            dashboardController.setVaultManager(vaultManager);

            // Lấy Stage (cửa sổ hiện tại) từ nút bấm
            javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow(); 
            // Lưu ý: Trong SetupVaultController, thay btnLogin bằng btnCreate

            stage.setScene(new javafx.scene.Scene(root, 700, 500));
            stage.setTitle("Offline Password Manager - Dashboard");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            lblError.setStyle("-fx-text-fill: red;");
            lblError.setText("Sai mật khẩu hoặc file dữ liệu bị hỏng!");
            // System.out.println("Lỗi chi tiết: " + e.getMessage()); // Có thể bật lên để debug
        } finally {
            // Dọn dẹp RAM cực kỳ nghiêm ngặt
            MemoryWiper.clear(masterPassword);
            txtPassword.clear();
        }
    }
}