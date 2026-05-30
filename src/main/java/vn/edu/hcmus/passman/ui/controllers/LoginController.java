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

        char[] masterPassword = pass.toCharArray();

        try {
            vaultManager.unlockVault(masterPassword);
            
            lblError.setStyle("-fx-text-fill: green;");
            lblError.setText("Mở khóa thành công!");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            
            DashBoardController dashboardController = loader.getController();
            dashboardController.setVaultManager(vaultManager);

            javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow(); 

            stage.setScene(new javafx.scene.Scene(root, 700, 500));
            stage.setTitle("Offline Password Manager - Dashboard");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            lblError.setStyle("-fx-text-fill: red;");
            lblError.setText("Sai mật khẩu hoặc file dữ liệu bị hỏng!");
        } finally {
            MemoryWiper.clear(masterPassword);
            txtPassword.clear();
        }
    }
    
    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/recovery.fxml"));
            Parent root = loader.load();
            
            RecoveryController recoveryController = loader.getController();
            recoveryController.setVaultManager(vaultManager);

            javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow(); 
            stage.setScene(new javafx.scene.Scene(root, 700, 500));
            stage.setTitle("Offline Password Manager - Khôi phục Mật khẩu");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            lblError.setText("Lỗi chuyển trang: " + e.getMessage());
            e.printStackTrace();
        }
    }
}