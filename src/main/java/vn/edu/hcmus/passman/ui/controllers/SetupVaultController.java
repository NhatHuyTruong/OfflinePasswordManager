package vn.edu.hcmus.passman.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import vn.edu.hcmus.passman.core.models.QuestionAnswer;
import vn.edu.hcmus.passman.core.services.VaultManager;
import vn.edu.hcmus.passman.core.security.MemoryWiper;

import java.util.ArrayList;
import java.util.List;

public class SetupVaultController {

    @FXML private VBox stepPasswordBox;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private VBox stepQuestionBox;
    @FXML private Label lblQuestionNumber;
    @FXML private TextField txtQuestion;
    @FXML private PasswordField txtAnswer;

    @FXML private Label lblError;
    @FXML private Button btnNext;
    @FXML private Button btnSkip;
    @FXML private Button btnAddQuestion;
    @FXML private Button btnFinish;

    private VaultManager vaultManager;
    private char[] tempMasterPassword;
    private List<QuestionAnswer> securityQuestions = new ArrayList<>();

    public void setVaultManager(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
    }

    private boolean validatePassword() {
        lblError.setText("");
        String pass = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (pass.isEmpty() || confirm.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin!");
            return false;
        }
        if (!pass.equals(confirm)) {
            lblError.setText("Mật khẩu xác nhận không khớp!");
            return false;
        }
        
        tempMasterPassword = pass.toCharArray();
        return true;
    }

    @FXML
    private void handleNext() {
        if (!validatePassword()) return;

        txtPassword.clear();
        txtConfirmPassword.clear();
        
        stepPasswordBox.setVisible(false);
        stepPasswordBox.setManaged(false);
        stepQuestionBox.setVisible(true);
        stepQuestionBox.setManaged(true);
        updateQuestionUI();
    }
    
    @FXML
    private void handleSkip() {
        if (!validatePassword()) return;
        txtPassword.clear();
        txtConfirmPassword.clear();
        securityQuestions = null; // Bỏ qua SSS
        finalizeSetup();
    }

    @FXML
    private void handleAddQuestion() {
        lblError.setText("");
        String question = txtQuestion.getText().trim();
        String answer = txtAnswer.getText().trim();

        if (question.isEmpty() || answer.isEmpty()) {
            lblError.setText("Vui lòng nhập đủ câu hỏi và câu trả lời!");
            return;
        }

        securityQuestions.add(new QuestionAnswer(question, answer.toCharArray()));
        txtQuestion.clear();
        txtAnswer.clear();
        lblError.setStyle("-fx-text-fill: green;");
        lblError.setText("Đã thêm câu hỏi thứ " + securityQuestions.size());
        
        updateQuestionUI();
    }
    
    @FXML
    private void handleFinish() {
        lblError.setText("");
        if (securityQuestions.size() < 3) {
            lblError.setText("Cần ít nhất 3 câu hỏi bảo mật để dùng tính năng này.");
            return;
        }
        finalizeSetup();
    }
    
    private void updateQuestionUI() {
        lblQuestionNumber.setText("Câu hỏi bảo mật (" + securityQuestions.size() + " câu đã thêm)");
        if (securityQuestions.size() >= 3) {
            btnFinish.setDisable(false);
        }
    }

    private void finalizeSetup() {
        try {
            vaultManager.initializeVault(tempMasterPassword, securityQuestions);
            
            lblError.setStyle("-fx-text-fill: green;");
            lblError.setText("Khởi tạo thành công! Đang vào hệ thống...");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            
            DashBoardController dashboardController = loader.getController();
            dashboardController.setVaultManager(vaultManager);

            javafx.stage.Stage stage = (javafx.stage.Stage) lblError.getScene().getWindow(); 
            stage.setScene(new javafx.scene.Scene(root, 700, 500));
            stage.setTitle("Offline Password Manager - Dashboard");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            lblError.setStyle("-fx-text-fill: red;");
            lblError.setText("Lỗi khởi tạo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (tempMasterPassword != null) {
                MemoryWiper.clear(tempMasterPassword);
            }
        }
    }
}