package vn.edu.hcmus.passman.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import vn.edu.hcmus.passman.core.models.Account;
import vn.edu.hcmus.passman.core.services.VaultManager;

public class DashBoardController {

    @FXML private TableView<Account> tableAccounts;
    @FXML private TableColumn<Account, String> colService;
    @FXML private TableColumn<Account, String> colUsername;
    @FXML private TableColumn<Account, String> colUrl;
    @FXML private TableColumn<Account, String> colNote;

    @FXML private Button btnEdit;
    @FXML private Button btnCopy;
    @FXML private Button btnDelete;

    private VaultManager vaultManager;
    private ObservableList<Account> accountList;

    public void setVaultManager(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
        loadData(); // Tải dữ liệu ngay khi vừa được tiêm VaultManager vào
    }

    @FXML
    public void initialize() {
        // Liên kết các cột của bảng với các thuộc tính của đối tượng Account
        colService.setCellValueFactory(new PropertyValueFactory<>("service"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        // Lắng nghe sự kiện click chọn 1 dòng trong bảng
        tableAccounts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            // Chỉ bật nút Sửa/Xóa khi có 1 dòng được chọn
            btnEdit.setDisable(!hasSelection);
            btnDelete.setDisable(!hasSelection);
            btnCopy.setDisable(!hasSelection);
        });
    }

    private void loadData() {
        try {
            // Lấy danh sách từ RAM (đã được giải mã)
            accountList = FXCollections.observableArrayList(vaultManager.getAllAccounts());
            tableAccounts.setItems(accountList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAdd() {
        openAccountForm(null); // Truyền null để báo cho Form biết đây là chế độ Thêm mới
    }

    @FXML
    private void handleEdit() {
        Account selected = tableAccounts.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openAccountForm(selected); // Truyền account đang chọn để báo chế độ Sửa
        }
    }

    @FXML
    private void handleCopyPassword() {
        Account selected = tableAccounts.getSelectionModel().getSelectedItem();
        if (selected != null) {
            char[] passwordChars = selected.getPassword();
            
            if (passwordChars != null && passwordChars.length > 0) {
                // Chuyển char[] thành String tạm thời để đưa vào Clipboard của HĐH
                String passwordStr = new String(passwordChars);
                
                // Gọi Clipboard của Hệ Điều Hành
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(passwordStr);
                clipboard.setContent(content);
                
                System.out.println("[UI] Đã copy mật khẩu của " + selected.getService() + " vào Clipboard!");
                
                // UX Tốt: Đổi chữ nút bấm 1 chút để báo hiệu đã copy thành công
                btnCopy.setText("Đã Copy ✓");
                
                // (Tùy chọn) Đổi lại text sau 2 giây bằng luồng phụ
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(() -> btnCopy.setText("Copy Mật Khẩu"));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    /**
     * Hàm phụ trợ mở cửa sổ Pop-up
     */
    private void openAccountForm(Account account) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/form_account.fxml"));
            Parent root = loader.load();

            FormAccountController controller = loader.getController();
            controller.setVaultManagerAndAccount(vaultManager, account);

            // Tạo một Stage (cửa sổ) mới
            Stage stage = new Stage();
            stage.setTitle(account == null ? "Thêm Tài Khoản" : "Sửa Tài Khoản");
            stage.setScene(new Scene(root));
            
            // ĐIỂM SÁNG UX: Khóa cửa sổ cha (Dashboard) lại khi Pop-up đang mở
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            // Lệnh showAndWait() sẽ bắt luồng chờ ở đây cho đến khi người dùng tắt Pop-up
            stage.showAndWait(); 
            
            // Sau khi Pop-up tắt, tải lại dữ liệu bảng (Table) để thấy cập nhật mới nhất
            loadData();
            
            // Reset trạng thái chọn của bảng (Tắt nút Sửa/Xóa đi)
            tableAccounts.getSelectionModel().clearSelection();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        Account selected = tableAccounts.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // Xóa khỏi RAM (hàm deleteAccount sẽ tự động ghi đè RAM bằng 0)
                vaultManager.deleteAccount(selected.getId());
                // Lưu file đè xuống ổ cứng
                vaultManager.saveChanges();
                // Cập nhật lại giao diện
                accountList.remove(selected);
                System.out.println("Đã xóa thành công!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLock() {
        // Chủ động khóa Vault (Xóa sạch Session Key và RAM)
        vaultManager.lockVault();
        System.out.println("[UI] Đã khóa két an toàn.");
        
        // Quay về màn hình Login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setVaultManager(vaultManager);

            Stage stage = (Stage) tableAccounts.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 300));
            stage.setTitle("Offline Password Manager - Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}