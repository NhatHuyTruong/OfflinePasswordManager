package vn.edu.hcmus.passman.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import vn.edu.hcmus.passman.core.models.QuestionAnswer;
import vn.edu.hcmus.passman.core.services.VaultManager;
import vn.edu.hcmus.passman.core.security.MemoryWiper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecoveryController {

    @FXML private VBox questionsContainer;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmNewPassword;
    @FXML private Label lblError;
    @FXML private Button btnRecover;

    private VaultManager vaultManager;
    private List<String> selectedQuestions = new ArrayList<>();
    private List<PasswordField> answerFields = new ArrayList<>();
    private int requiredThreshold = 0;

    public void setVaultManager(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
        loadQuestions();
    }

    private void loadQuestions() {
        try {
            requiredThreshold = vaultManager.getRecoveryThreshold();
            List<String> allQuestions = vaultManager.getRecoveryQuestions();
            Collections.shuffle(allQuestions);
            
            // Lấy đúng số lượng câu hỏi bằng threshold (K)
            for (int i = 0; i < requiredThreshold && i < allQuestions.size(); i++) {
                String q = allQuestions.get(i);
                selectedQuestions.add(q);
                
                VBox box = new VBox(5);
                Label lblQ = new Label("Câu " + (i+1) + ": " + q);
                PasswordField pfAns = new PasswordField();
                pfAns.setPromptText("Nhập câu trả lời...");
                
                box.getChildren().addAll(lblQ, pfAns);
                questionsContainer.getChildren().add(box);
                
                answerFields.add(pfAns);
            }
        } catch (Exception e) {
            lblError.setText("Lỗi tải câu hỏi: " + e.getMessage());
            btnRecover.setDisable(true);
        }
    }

    @FXML
    private void handleRecover() {
        lblError.setText("");

        String newPass = txtNewPassword.getText();
        String confirm = txtConfirmNewPassword.getText();

        if (newPass.isEmpty() || confirm.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ mật khẩu mới!");
            return;
        }

        if (!newPass.equals(confirm)) {
            lblError.setText("Mật khẩu xác nhận không khớp!");
            return;
        }

        List<QuestionAnswer> recoveryAnswers = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            String q = selectedQuestions.get(i);
            String ans = answerFields.get(i).getText();
            if (ans.trim().isEmpty()) {
                lblError.setText("Vui lòng trả lời đầy đủ " + requiredThreshold + " câu hỏi!");
                return;
            }
            recoveryAnswers.add(new QuestionAnswer(q, ans.toCharArray()));
        }

        char[] newMasterPassword = newPass.toCharArray();

        try {
            vaultManager.recoverVault(recoveryAnswers, newMasterPassword);
            
            lblError.setStyle("-fx-text-fill: green;");
            lblError.setText("Khôi phục thành công! Đang chuyển về màn hình đăng nhập...");
            
            handleBack(); 
            
        } catch (Exception e) {
            lblError.setStyle("-fx-text-fill: red;");
            lblError.setText(e.getMessage());
        } finally {
            MemoryWiper.clear(newMasterPassword);
            txtNewPassword.clear();
            txtConfirmNewPassword.clear();
            for (PasswordField pf : answerFields) {
                pf.clear();
            }
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            LoginController loginController = loader.getController();
            loginController.setVaultManager(vaultManager);

            javafx.stage.Stage stage = (javafx.stage.Stage) btnRecover.getScene().getWindow(); 
            stage.setScene(new javafx.scene.Scene(root, 700, 500));
            stage.setTitle("Offline Password Manager");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
