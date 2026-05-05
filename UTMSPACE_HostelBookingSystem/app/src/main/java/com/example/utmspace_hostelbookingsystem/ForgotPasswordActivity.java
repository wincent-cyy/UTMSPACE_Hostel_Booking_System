package com.example.utmspace_hostelbookingsystem;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnSendResetLink;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // 1. 初始化 Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. 初始化 UI 控件
        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // 3. 初始化進度條
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending reset link...");
        progressDialog.setCancelable(false);

        // 4. 發送重置郵件按鈕點擊事件
        btnSendResetLink.setOnClickListener(v -> resetPassword());

        // 5. 返回登入頁面：直接調用 finish() 銷毀當前頁面，回到上一個頁面 (Login)
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void resetPassword() {
        // 獲取輸入並強制轉換為小寫 (Always small letter)
        String email = etResetEmail.getText().toString().trim().toLowerCase(Locale.ROOT);

        // 驗證輸入是否為空
        if (TextUtils.isEmpty(email)) {
            etResetEmail.setError("Email is required");
            etResetEmail.requestFocus();
            return;
        }

        // 驗證 Email 格式是否正確
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("Please enter a valid email address");
            etResetEmail.requestFocus();
            return;
        }

        progressDialog.show();

        // Firebase 核心功能：向指定 Email 發送重置密碼連結
        // 用戶點擊郵件中的連結設置新密碼後，Firebase 會自動更新後台數據
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // 提示用戶檢查郵箱
                        Toast.makeText(ForgotPasswordActivity.this,
                                "A reset link has been sent to: " + email + ". Please check your inbox.",
                                Toast.LENGTH_LONG).show();

                        // 成功發送後，結束當前頁面返回登入頁
                        finish();
                    } else {
                        // 處理錯誤 (例如：該 Email 未被註冊)
                        String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}