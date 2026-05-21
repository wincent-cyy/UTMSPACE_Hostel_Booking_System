package com.example.utmspace_hostelbookingsystem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;
import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnSendResetLink;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    // Password validation pattern: at least 7 characters, must contain both letters and numbers
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{7,}$");

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

        // 4. Add auto lowercase for email input
        setupEmailAutoLowercase();

        // 5. 發送重置郵件按鈕點擊事件
        btnSendResetLink.setOnClickListener(v -> resetPassword());

        // 6. 返回登入頁面：直接調用 finish() 銷毀當前頁面，回到上一個頁面 (Login)
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void setupEmailAutoLowercase() {
        etResetEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Convert to lowercase whenever user types
                if (s != null) {
                    String input = s.toString();
                    String lowerCaseInput = input.toLowerCase(Locale.ROOT);

                    // Only update if text is actually different (avoid infinite loop)
                    if (!input.equals(lowerCaseInput)) {
                        // Remove listener temporarily to avoid recursion
                        etResetEmail.removeTextChangedListener(this);
                        etResetEmail.setText(lowerCaseInput);
                        // Move cursor to the end
                        etResetEmail.setSelection(lowerCaseInput.length());
                        etResetEmail.addTextChangedListener(this);
                    }
                }
            }
        });
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
                        // 提示用戶檢查郵箱，並顯示密碼要求
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset link sent to: " + email + "\n\nNote: New password must be at least 7 characters and contain both letters and numbers",
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

    // Public method to validate password (can be called from other activities like ResetPasswordActivity)
    public static boolean isPasswordValid(String password) {
        if (TextUtils.isEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    // Get password requirements as a string
    public static String getPasswordRequirements() {
        return "Password must be at least 7 characters and contain both letters and numbers";
    }
}