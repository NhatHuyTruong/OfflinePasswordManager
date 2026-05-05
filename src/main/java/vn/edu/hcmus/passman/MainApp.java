package vn.edu.hcmus.passman;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import vn.edu.hcmus.passman.core.security.CryptoManagerImpl;
import vn.edu.hcmus.passman.core.services.FileStorageService;
import vn.edu.hcmus.passman.core.services.VaultManager;
import vn.edu.hcmus.passman.ui.controllers.LoginController;
import vn.edu.hcmus.passman.ui.controllers.SetupVaultController;

public class MainApp extends Application {

    private VaultManager vaultManager;

    @Override
    public void init() throws Exception {
        // KHỞI TẠO BACKEND: Hàm init() chạy trước khi giao diện được vẽ ra
        // Ở đây chúng ta lắp ráp các mảnh ghép đã làm ở Giai đoạn 1 & 2
        CryptoManagerImpl cryptoManager = new CryptoManagerImpl();
        FileStorageService storageService = new FileStorageService();
        
        this.vaultManager = new VaultManager(cryptoManager, storageService);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Khởi tạo Storage Service để kiểm tra ổ cứng
        FileStorageService storageService = new FileStorageService();
        
        FXMLLoader loader;
        
        // KIỂM TRA ĐIỀU KIỆN ĐỂ ĐIỀU HƯỚNG
        if (storageService.vaultExists()) {
            // Đã có Két Sắt -> Đi đến màn hình Đăng Nhập (Unlock)
            loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setVaultManager(vaultManager);
            primaryStage.setTitle("Offline Password Manager - Đăng nhập");
            primaryStage.setScene(new Scene(root, 400, 300));
        } else {
            // Chưa có Két Sắt -> Đi đến màn hình Thiết Lập (Setup/Register)
            loader = new FXMLLoader(getClass().getResource("/fxml/setup_vault.fxml"));
            Parent root = loader.load();
            SetupVaultController controller = loader.getController();
            controller.setVaultManager(vaultManager);
            primaryStage.setTitle("Offline Password Manager - Khởi tạo");
            primaryStage.setScene(new Scene(root, 500, 450));
        }

        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}