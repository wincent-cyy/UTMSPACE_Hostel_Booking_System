package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.core.splashscreen.SplashScreen;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

public class LoginAndSignupActivity extends AppCompatActivity {

    private static final String TAG = "LoginSignupActivity";
    private static final String SHARED_PREFS_NAME = "BioAuthPrefs";
    private static final String KEY_BIOMETRIC_ENABLED = "FingerprintEnabled";
    private static final String KEY_SAVED_UID = "SavedUserUid";
    private static final String KEY_SAVED_EMAIL = "SavedEmail";
    private static final String KEY_SAVED_PASSWORD = "SavedPassword";
    private static final String KEY_REMEMBER_ME = "RememberMeChecked";
    private String lastUnverifiedEmail = "";

    // UI Components
    private LinearLayout loginForm;
    private LinearLayout signupForm;
    private TextView tabLoginBtn;
    private TextView tabSignupBtn;

    // Login Fields
    private EditText loginEmail;
    private EditText loginPassword;
    private CheckBox rememberMe;
    private TextView openForgotPageBtn;

    // Signup Fields
    private EditText signupName;
    private EditText signupPhone;
    private EditText signupEmail;
    private Spinner signupRole;
    private EditText signupPassword;
    private EditText signupConfirmPwd;
    private CheckBox agreeTerms;
    private TextView openTermsPageBtn;
    // Resend Verification
    private TextView resendVerificationBtn;
    // Password visibility toggles
    private ImageView loginTogglePassword;
    private ImageView signupTogglePassword;
    private ImageView signupToggleConfirmPwd;

    // Role dropdown clickable container
    private LinearLayout roleContainer;
    private ImageView roleDropdownArrow;

    // Buttons
    private Button doLoginBtn;
    private Button doSignupBtn;

    // Quick Access Buttons (Login Page)
    private LinearLayout quickMapBtn;
    private LinearLayout quickEmergencyBtn;

    // Contact Support Button (Signup Page)
    private LinearLayout contactSupportBtn;

    // Animations
    private Animation slideInLeft;
    private Animation slideOutRight;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPreferences;

    // Handlers
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private boolean isProcessing = false;
    private boolean isAnimating = false;
    private LinearLayout resendVerificationContainer;

    // Terms Launcher
    private ActivityResultLauncher<Intent> termsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_and_signup);

        // Initialize Animations
        initAnimations();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        // IMPROVED: Set status bar and navigation bar to white for full background coverage
        setupSystemBars();

        // Initialize Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Initialize Views
        initViews();

        // FORCE CheckBox colors to RED - 强制红色
        forceCheckBoxRed();

        // Setup Spinner
        setupSpinner();

        // Setup Email Auto Lowercase
        setupEmailAutoLowercase();

        // Setup Terms Launcher
        setupTermsLauncher();

        // Setup Click Listeners
        setupClickListeners();

        // Check for biometric authentication
        checkAndTriggerBiometricAuth();

        // Set default tab to Login
        setTabSelected(true, false);

        // Load saved Remember Me state
        loadRememberMeState();
    }

    /**
     * IMPROVED: Setup system bars (status bar and navigation bar) to match white background
     * This ensures the entire screen has consistent white color
     */
    private void setupSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set status bar and navigation bar to white
            getWindow().setStatusBarColor(Color.WHITE);
            getWindow().setNavigationBarColor(Color.WHITE);

            // Make status bar icons dark (for white background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |  // Dark status bar icons
                                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR // Dark navigation bar icons (API 27+)
                );
            }

            // For Android 10+ ensure edge-to-edge experience
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getWindow().setDecorFitsSystemWindows(false);
            }
        }
    }

    private void initAnimations() {
        slideInLeft = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideOutRight = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        slideInLeft.setDuration(300);
        slideOutRight.setDuration(300);
    }

    private void initViews() {
        // Tab containers
        loginForm = findViewById(R.id.loginForm);
        signupForm = findViewById(R.id.signupForm);
        tabLoginBtn = findViewById(R.id.tabLoginBtn);
        tabSignupBtn = findViewById(R.id.tabSignupBtn);

        // Login fields
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        rememberMe = findViewById(R.id.rememberMe);
        openForgotPageBtn = findViewById(R.id.openForgotPageBtn);

        // Password toggle icons
        loginTogglePassword = findViewById(R.id.loginTogglePassword);
        signupTogglePassword = findViewById(R.id.signupTogglePassword);
        signupToggleConfirmPwd = findViewById(R.id.signupToggleConfirmPwd);

        // Role dropdown clickable elements
        roleContainer = findViewById(R.id.roleContainer);
        roleDropdownArrow = findViewById(R.id.roleDropdownArrow);
        resendVerificationContainer = findViewById(R.id.resendVerificationContainer);

        // Signup fields
        signupName = findViewById(R.id.signupName);
        signupPhone = findViewById(R.id.signupPhone);
        signupEmail = findViewById(R.id.signupEmail);
        signupRole = findViewById(R.id.signupRole);
        signupPassword = findViewById(R.id.signupPassword);
        signupConfirmPwd = findViewById(R.id.signupConfirmPwd);
        agreeTerms = findViewById(R.id.agreeTerms);
        openTermsPageBtn = findViewById(R.id.openTermsPageBtn);

        // Buttons
        doLoginBtn = findViewById(R.id.doLoginBtn);
        doSignupBtn = findViewById(R.id.doSignupBtn);

        // Quick Access Buttons
        quickMapBtn = findViewById(R.id.quickMapBtn);
        quickEmergencyBtn = findViewById(R.id.quickEmergencyBtn);

        // Contact Support Button
        contactSupportBtn = findViewById(R.id.contactSupportBtn);
    }

    /**
     * FORCE CheckBox to #800000 with WHITE check mark visible
     */
    private void forceCheckBoxRed() {
        int maroonColor = Color.parseColor("#800000");

        // Fix Remember Me CheckBox
        if (rememberMe != null) {
            // 先清除任何已有的tint
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                rememberMe.setButtonTintList(null);
            }

            // 创建 checked/unchecked 状态的颜色
            android.content.res.ColorStateList tintList = new android.content.res.ColorStateList(
                    new int[][]{
                            new int[]{-android.R.attr.state_checked},  // unchecked state
                            new int[]{android.R.attr.state_checked}     // checked state
                    },
                    new int[]{
                            maroonColor,  // unchecked - 边框颜色
                            maroonColor   // checked - 背景颜色
                    }
            );
            rememberMe.setButtonTintList(tintList);
            rememberMe.jumpDrawablesToCurrentState();
        }

        // Fix Terms CheckBox - 使用完全相同的颜色
        if (agreeTerms != null) {
            // 先清除任何已有的tint
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                agreeTerms.setButtonTintList(null);
            }

            android.content.res.ColorStateList tintList = new android.content.res.ColorStateList(
                    new int[][]{
                            new int[]{-android.R.attr.state_checked},
                            new int[]{android.R.attr.state_checked}
                    },
                    new int[]{
                            maroonColor,  // 使用相同的 #800000
                            maroonColor   // 使用相同的 #800000
                    }
            );
            agreeTerms.setButtonTintList(tintList);
            agreeTerms.jumpDrawablesToCurrentState();
        }
    }

    private void setupSpinner() {
        List<String> roles = new ArrayList<>();
        roles.add("Select your role");
        roles.add("Student");
        roles.add("Staff");
        roles.add("Technician");
        roles.add("Admin");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        signupRole.setAdapter(adapter);
    }

    private void setupEmailAutoLowercase() {
        TextWatcher lowercaseWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.equals(input.toLowerCase(Locale.ROOT))) {
                    String lowercased = input.toLowerCase(Locale.ROOT);
                    s.replace(0, s.length(), lowercased);
                }
            }
        };
        loginEmail.addTextChangedListener(lowercaseWatcher);
        signupEmail.addTextChangedListener(lowercaseWatcher);
    }

    private void setupTermsLauncher() {
        termsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        agreeTerms.setChecked(true);
                        // Reapply red color
                        if (agreeTerms.getButtonDrawable() != null) {
                            agreeTerms.getButtonDrawable().setColorFilter(Color.parseColor("#D32F2F"), PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        // Tab switching
        tabLoginBtn.setOnClickListener(v -> {
            if (!isAnimating && loginForm.getVisibility() != View.VISIBLE) {
                setTabSelected(true, true);
            }
        });

        tabSignupBtn.setOnClickListener(v -> {
            if (!isAnimating && signupForm.getVisibility() != View.VISIBLE) {
                setTabSelected(false, true);
            }
        });

        // Login
        doLoginBtn.setOnClickListener(v -> loginUser());
        openForgotPageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginAndSignupActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Signup
        doSignupBtn.setOnClickListener(v -> registerUser());
        openTermsPageBtn.setOnClickListener(v -> {
            try {
                termsLauncher.launch(new Intent(this, TermsActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Terms page not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Name filter
        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (Character.isLetter(character) || Character.isSpaceChar(character)) {
                    filtered.append(Character.toUpperCase(character));
                }
            }
            return filtered.toString();
        };
        signupName.setFilters(new InputFilter[]{nameFilter, new InputFilter.LengthFilter(50)});
        signupPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});

        // Feature Listeners
        quickMapBtn.setOnClickListener(v -> showDormitoryMap());
        quickEmergencyBtn.setOnClickListener(v -> showEmergencyContacts());

        // Contact Support Button - 发送邮件
        contactSupportBtn.setOnClickListener(v -> contactSupport());

        // Setup password toggle functionality
        setupPasswordToggle(loginPassword, loginTogglePassword);
        setupPasswordToggle(signupPassword, signupTogglePassword);
        setupPasswordToggle(signupConfirmPwd, signupToggleConfirmPwd);

        // Setup role dropdown clickable (make arrow and container open spinner)
        setupRoleDropdownClickable();

        if (resendVerificationContainer != null) {
            resendVerificationContainer.setOnClickListener(v -> resendVerificationEmail());
        }
    }

    private void showResendVerificationButton(String email) {
        lastUnverifiedEmail = email;
        // 显示容器，而不是单独的 TextView
        if (resendVerificationContainer != null) {
            resendVerificationContainer.setVisibility(View.VISIBLE);
        }
        // 隐藏旧的单独按钮（如果存在）
        if (resendVerificationBtn != null) {
            resendVerificationBtn.setVisibility(View.GONE);
        }
    }

    private void hideResendVerificationButton() {
        if (resendVerificationContainer != null) {
            resendVerificationContainer.setVisibility(View.GONE);
        }
        if (resendVerificationBtn != null) {
            resendVerificationBtn.setVisibility(View.GONE);
        }
        lastUnverifiedEmail = "";
    }

    private void resendVerificationEmail() {
        if (lastUnverifiedEmail == null || lastUnverifiedEmail.isEmpty()) {
            Toast.makeText(this, "Please enter your email and try logging in first", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示倒计时对话框
        showResendConfirmationDialog();
    }

    private void showResendConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_resend_verification, null);
        builder.setView(dialogView);

        TextView tvEmail = dialogView.findViewById(R.id.tvResendEmail);
        TextView tvCountdown = dialogView.findViewById(R.id.tvCountdown);
        Button btnResend = dialogView.findViewById(R.id.btnResend);
        Button btnChangeEmail = dialogView.findViewById(R.id.btnChangeEmail);

        tvEmail.setText(lastUnverifiedEmail);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // 倒计时逻辑
        Handler countdownHandler = new Handler(Looper.getMainLooper());
        int[] secondsLeft = {30}; // 30秒倒计时
        boolean[] canResend = {false};

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (secondsLeft[0] > 0) {
                    tvCountdown.setText("You can resend in " + secondsLeft[0] + " seconds");
                    secondsLeft[0]--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    tvCountdown.setText("You can now resend the verification email");
                    btnResend.setEnabled(true);
                    btnResend.setAlpha(1.0f);
                    canResend[0] = true;
                }
            }
        };

        // 初始状态：按钮禁用
        btnResend.setEnabled(false);
        btnResend.setAlpha(0.5f);
        countdownHandler.post(countdownRunnable);

        // Resend 按钮点击
        btnResend.setOnClickListener(v -> {
            if (!canResend[0]) {
                Toast.makeText(this, "Please wait " + secondsLeft[0] + " seconds", Toast.LENGTH_SHORT).show();
                return;
            }

            performResendVerification(lastUnverifiedEmail, dialog);
        });

        // Change Email 按钮 - 允许用户更改邮箱
        btnChangeEmail.setOnClickListener(v -> {
            dialog.dismiss();
            showChangeEmailDialog();
        });

        dialog.show();
    }

    private void performResendVerification(String email, AlertDialog dialog) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Sending verification email...");
        progress.setCancelable(false);
        progress.show();

        // 关键修复：先尝试登录（密码正确但未验证的用户）
        // 注意：这里需要用户输入的密码，但我们已经有了 lastUnverifiedEmail
        // 我们需要从 loginPassword 获取密码
        String password = loginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(password)) {
            progress.dismiss();
            Toast.makeText(this, "Please enter your password first", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && !user.isEmailVerified()) {
                            // 用户已登录但未验证，可以重新发送验证邮件
                            user.sendEmailVerification()
                                    .addOnSuccessListener(aVoid -> {
                                        progress.dismiss();
                                        dialog.dismiss();
                                        Toast.makeText(this, "Verification email resent! Please check your inbox.", Toast.LENGTH_LONG).show();

                                        // 登出，让用户重新登录
                                        mAuth.signOut();
                                    })
                                    .addOnFailureListener(e -> {
                                        progress.dismiss();
                                        Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            progress.dismiss();
                            dialog.dismiss();
                            Toast.makeText(this, "Your email is already verified! Please login.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progress.dismiss();
                        // 登录失败，可能是密码错误
                        new AlertDialog.Builder(this)
                                .setTitle("Cannot Resend")
                                .setMessage("Unable to resend verification email.\n\n" +
                                        "Possible reasons:\n" +
                                        "1. Wrong password - Please check your password\n" +
                                        "2. Email not registered - Please sign up first\n\n" +
                                        "Would you like to try a different email?")
                                .setPositiveButton("Try Different Email", (d, which) -> {
                                    dialog.dismiss();
                                    showChangeEmailDialog();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });
    }

    private void sendVerificationEmailDirect(String email, ProgressDialog progress, AlertDialog dialog) {
        // 使用 Firebase 的 createUserWithEmailAndPassword 会创建新用户，不好
        // 更好的方法：使用 FirebaseAuth 发送验证邮件的 API
        // 实际上 Firebase 没有直接发送验证邮件到已存在邮箱的 API
        // 解决方案：提示用户通过忘记密码重置或重新注册

        progress.dismiss();
        dialog.dismiss();

        // 显示选项对话框
        new AlertDialog.Builder(this)
                .setTitle("Verification Options")
                .setMessage("What would you like to do?")
                .setPositiveButton("Forgot Password?", (d, which) -> {
                    // 跳转到 Forgot Password
                    Intent intent = new Intent(this, ForgotPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                })
                .setNegativeButton("Try Different Email", (d, which) -> {
                    showChangeEmailDialog();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showChangeEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
        builder.setView(dialogView);

        EditText etNewEmail = dialogView.findViewById(R.id.etNewEmail);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitEmail);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEmail);

        AlertDialog dialog = builder.create();

        btnSubmit.setOnClickListener(v -> {
            String newEmail = etNewEmail.getText().toString().trim().toLowerCase();
            if (TextUtils.isEmpty(newEmail) || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                etNewEmail.setError("Valid email required");
                return;
            }

            dialog.dismiss();

            // 显示检查进度
            ProgressDialog progress = new ProgressDialog(this);
            progress.setMessage("Checking email...");
            progress.setCancelable(false);
            progress.show();

            // 直接查询 Firestore Users 集合
            db.collection("Users")
                    .whereEqualTo("email", newEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        progress.dismiss();

                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            // 邮箱在 Firestore 中存在 = 已注册
                            DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                            String registeredRole = userDoc.getString("role");

                            new AlertDialog.Builder(this)
                                    .setTitle("Email Already Registered")
                                    .setMessage("The email " + newEmail + " is already registered as " + registeredRole + ".\n\n" +
                                            "Please login with your existing account instead.")
                                    .setPositiveButton("Go to Login", (d, which) -> {
                                        setTabSelected(true, true);
                                        loginEmail.setText(newEmail);
                                        loginPassword.requestFocus();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            // 邮箱在 Firestore 中不存在
                            new AlertDialog.Builder(this)
                                    .setTitle("Account Not Found")
                                    .setMessage("No account found with email: " + newEmail + "\n\nWould you like to create a new account with this email?")
                                    .setPositiveButton("Create Account", (d, which) -> {
                                        setTabSelected(false, true);
                                        signupEmail.setText(newEmail);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progress.dismiss();
                        Log.e(TAG, "Firestore query failed: " + e.getMessage());

                        // 查询失败，让用户自己选择
                        new AlertDialog.Builder(this)
                                .setTitle("Unable to Check Email")
                                .setMessage("Network error. Please check your connection and try again.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Setup password visibility toggle for EditText
     * Logic:
     * - When eye is CLOSED (ic_eye_close) → password is HIDDEN → click to SHOW password → change to OPEN eye
     * - When eye is OPEN (ic_eye_open) → password is VISIBLE → click to HIDE password → change to CLOSED eye
     */
    private void setupPasswordToggle(EditText editText, ImageView toggleIcon) {
        if (toggleIcon == null) return;

        toggleIcon.setOnClickListener(v -> {
            // Check current input type
            if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Currently HIDDEN (closed eye) → change to VISIBLE (open eye)
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleIcon.setImageResource(R.drawable.ic_eye_open);  // Open eye = password visible
            } else {
                // Currently VISIBLE (open eye) → change to HIDDEN (closed eye)
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleIcon.setImageResource(R.drawable.ic_eye_close); // Closed eye = password hidden
            }
            // Move cursor to end
            editText.setSelection(editText.getText().length());
        });
    }

    /**
     * Setup role dropdown - make the entire container and arrow clickable to open spinner
     */
    private void setupRoleDropdownClickable() {
        if (roleContainer == null || roleDropdownArrow == null || signupRole == null) return;

        View.OnClickListener openSpinnerListener = v -> signupRole.performClick();

        roleContainer.setOnClickListener(openSpinnerListener);
        roleDropdownArrow.setOnClickListener(openSpinnerListener);
    }

    private void setTabSelected(boolean isLoginSelected, boolean animate) {
        if (isAnimating) return;

        isAnimating = true;

        tabLoginBtn.setSelected(isLoginSelected);
        tabSignupBtn.setSelected(!isLoginSelected);

        if (isLoginSelected) {
            tabLoginBtn.setTextColor(getColor(android.R.color.white));
            tabSignupBtn.setTextColor(getColor(R.color.tabInactiveText));

            if (animate) {
                signupForm.startAnimation(slideOutRight);
                signupForm.setVisibility(View.GONE);
                loginForm.setVisibility(View.VISIBLE);
                loginForm.startAnimation(slideInLeft);
            } else {
                signupForm.setVisibility(View.GONE);
                loginForm.setVisibility(View.VISIBLE);
            }
        } else {
            tabLoginBtn.setTextColor(getColor(R.color.tabInactiveText));
            tabSignupBtn.setTextColor(getColor(android.R.color.white));

            if (animate) {
                loginForm.startAnimation(slideOutRight);
                loginForm.setVisibility(View.GONE);
                signupForm.setVisibility(View.VISIBLE);
                signupForm.startAnimation(slideInLeft);
            } else {
                loginForm.setVisibility(View.GONE);
                signupForm.setVisibility(View.VISIBLE);
            }
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> isAnimating = false, 350);
    }

    private void showDormitoryMap() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dormitory Map");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        ImageView mapImage = new ImageView(this);

        try {
            mapImage.setImageResource(R.drawable.dorm_map);
            mapImage.setAdjustViewBounds(true);
            mapImage.setMaxHeight(800);
            mapImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            layout.addView(mapImage);
        } catch (Exception e) {
            TextView errorText = new TextView(this);
            errorText.setText("❌ Map image not found\n\nPlease add dorm_map.png to res/drawable/");
            errorText.setTextSize(14);
            errorText.setTextColor(Color.RED);
            errorText.setGravity(android.view.Gravity.CENTER);
            errorText.setPadding(20, 50, 20, 50);
            layout.addView(errorText);
            Log.e(TAG, "Map image not found: " + e.getMessage());
        }

        builder.setView(layout);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showEmergencyContacts() {
        String[] contacts = {
                "Security Guard - 03-5556 1234",
                "Clinic / Medical - 999",
                "Maintenance - 03-5556 5678",
                "Student Affairs - 03-5556 9012",
                "Fire Department - 994"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Emergency Contacts");
        builder.setItems(contacts, (dialog, which) -> {
            String phoneNumber = "";
            switch (which) {
                case 0:
                    phoneNumber = "tel:0355561234";
                    break;
                case 1:
                    phoneNumber = "tel:999";
                    break;
                case 2:
                    phoneNumber = "tel:0355565678";
                    break;
                case 3:
                    phoneNumber = "tel:0355569012";
                    break;
                case 4:
                    phoneNumber = "tel:994";
                    break;
            }

            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse(phoneNumber));
            startActivity(callIntent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Contact Support - Send email to support
     */
    private void contactSupport() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:hostelhub@utm.my"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Hostel Hub Support Request");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Dear Support Team,\n\nI need help with:\n\n\n\nBest regards,");

        try {
            startActivity(emailIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No email app found. Please email us at: hostelhub@utm.my", Toast.LENGTH_LONG).show();
        }
    }

    private void loadRememberMeState() {
        boolean rememberMeSaved = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        String savedEmail = sharedPreferences.getString(KEY_SAVED_EMAIL, "");
        String savedPassword = sharedPreferences.getString(KEY_SAVED_PASSWORD, "");

        if (rememberMeSaved && !savedEmail.isEmpty()) {
            rememberMe.setChecked(true);
            // Reapply red color
            if (rememberMe.getButtonDrawable() != null) {
                rememberMe.getButtonDrawable().setColorFilter(Color.parseColor("#D32F2F"), PorterDuff.Mode.SRC_IN);
            }
            loginEmail.setText(savedEmail);
            loginPassword.setText(savedPassword);
        }
    }

    // ==================== BIOMETRIC METHODS ====================

    private void checkAndTriggerBiometricAuth() {
        String savedUid = sharedPreferences.getString(KEY_SAVED_UID, null);
        String savedEmail = sharedPreferences.getString(KEY_SAVED_EMAIL, "");
        String savedPassword = sharedPreferences.getString(KEY_SAVED_PASSWORD, "");

        if (savedUid != null && !savedEmail.isEmpty() && !savedPassword.isEmpty()) {
            boolean rememberMeChecked = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
            boolean isBioEnabled = sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED + "_" + savedUid, false);

            if (rememberMeChecked && isBioEnabled) {
                BiometricManager biometricManager = BiometricManager.from(this);
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                    showBiometricPrompt(savedUid);
                }
            }
        }
    }

    private void showBiometricPrompt(String uid) {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginAndSignupActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.d(TAG, "Biometric prompt skipped or closed: " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                if (isFinishing() || isDestroyed()) return;
                progressDialog.setMessage("Auto logging in...");
                progressDialog.show();

                String savedEmail = sharedPreferences.getString(KEY_SAVED_EMAIL, "");
                String savedPassword = sharedPreferences.getString(KEY_SAVED_PASSWORD, "");

                if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                    performLogin(savedEmail, savedPassword, true);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(LoginAndSignupActivity.this, "No saved credentials. Please login manually.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginAndSignupActivity.this, "Fingerprint verification failed.", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Hostel System Login")
                .setSubtitle("Scan fingerprint to securely continue to your dashboard")
                .setNegativeButtonText("Use Password Instead")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // ==================== LOGIN METHODS ====================

    private void loginUser() {
        String email = loginEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = loginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            loginEmail.setError("Email is required");
            loginEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Password is required");
            loginPassword.requestFocus();
            return;
        }

        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }

        performLogin(email, password, false);
    }

    private void performLogin(String email, String password, boolean isBiometricLogin) {
        progressDialog.setMessage("Logging in...");
        progressDialog.show();

        timeoutRunnable = () -> {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                Toast.makeText(this, "Login timeout. Please check connection.", Toast.LENGTH_LONG).show();
            }
            isProcessing = false;
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15000);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            boolean rememberMeChecked = rememberMe.isChecked();

                            if (rememberMeChecked || isBiometricLogin) {
                                saveLoginCredentials(email, password, user.getUid(), rememberMeChecked);
                            } else {
                                clearSavedCredentials();
                            }

                            checkUserRole(user.getUid());
                        } else {
                            progressDialog.dismiss();
                            mAuth.signOut();

                            // ADD THIS: Show resend verification button
                            showResendVerificationButton(email);

                            Toast.makeText(LoginAndSignupActivity.this,
                                    "Please verify your email before logging in.\n\nCheck your inbox or click 'Resend' to get a new verification email.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progressDialog.dismiss();
                        Log.e(TAG, "Auth failed: " + task.getException());
                        Toast.makeText(LoginAndSignupActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveLoginCredentials(String email, String password, String uid, boolean rememberMeChecked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_SAVED_EMAIL, email);
        editor.putString(KEY_SAVED_PASSWORD, password);
        editor.putString(KEY_SAVED_UID, uid);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMeChecked);

        if (rememberMeChecked) {
            editor.putBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, true);
            Log.d(TAG, "Fingerprint auto-enabled for user: " + uid);
            Toast.makeText(this, "✓ Fingerprint login enabled", Toast.LENGTH_SHORT).show();
        } else {
            editor.putBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, false);
        }

        editor.apply();
    }

    private void clearSavedCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_SAVED_EMAIL);
        editor.remove(KEY_SAVED_PASSWORD);
        editor.remove(KEY_SAVED_UID);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }

    private void checkUserRole(String uid) {
        db.collection("Users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("role");
                            if (role != null) {
                                navigateToDashboard(role);
                            } else {
                                Toast.makeText(LoginAndSignupActivity.this, "Role not assigned to profile", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginAndSignupActivity.this, "User details not found in database.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginAndSignupActivity.this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        String structuralRole = role != null ? role.trim().toLowerCase(Locale.ROOT) : "";

        switch (structuralRole) {
            case "student":
                intent = new Intent(LoginAndSignupActivity.this, StudentDashboardActivity.class);
                break;
            case "staff":
                intent = new Intent(LoginAndSignupActivity.this, StaffDashboardActivity.class);
                break;
            case "technician":
                intent = new Intent(LoginAndSignupActivity.this, TechnicianDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(LoginAndSignupActivity.this, AdminDashboardActivity.class);
                break;
            default:
                Toast.makeText(this, "Unknown role assigned: " + role, Toast.LENGTH_SHORT).show();
                return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ==================== SIGNUP METHODS ====================

    private void registerUser() {
        if (isProcessing) return;

        String name = signupName.getText().toString().trim();
        String phone = signupPhone.getText().toString().trim();
        String email = signupEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String role = signupRole.getSelectedItem().toString();
        String pass = signupPassword.getText().toString().trim();
        String confirmPass = signupConfirmPwd.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            setFieldError(signupName, "Full Name is required");
            return;
        }
        if (phone.length() < 10 || phone.length() > 11) {
            setFieldError(signupPhone, "Enter a valid phone number (10-11 digits)");
            return;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setFieldError(signupEmail, "Valid email required");
            return;
        }
        if (role.equals("Select your role")) {
            Toast.makeText(this, "Please select a valid role", Toast.LENGTH_SHORT).show();
            return;
        }

        Pattern passPattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{7,}$");
        if (!passPattern.matcher(pass).matches()) {
            setFieldError(signupPassword, "Must be 7+ chars with letters & numbers");
            return;
        }
        if (!pass.equals(confirmPass)) {
            setFieldError(signupConfirmPwd, "Passwords do not match");
            return;
        }
        if (!agreeTerms.isChecked()) {
            Toast.makeText(this, "Please agree to Terms and Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        startSignupProcess(email, pass, name, phone, role);
    }

    private void setFieldError(EditText textField, String errorMsg) {
        ForegroundColorSpan fcs = new ForegroundColorSpan(Color.WHITE);
        SpannableStringBuilder ssb = new SpannableStringBuilder(errorMsg);
        ssb.setSpan(fcs, 0, errorMsg.length(), 0);
        textField.setError(ssb);
        textField.requestFocus();
    }

    private void startSignupProcess(String email, String pass, String name, String phone, String role) {
        isProcessing = true;
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        timeoutRunnable = () -> {
            if (isProcessing) {
                stopTimeoutAndResetUI();
                Toast.makeText(this, "Network Timeout. Please check connection.", Toast.LENGTH_SHORT).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 25000);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove(KEY_SAVED_UID);
                            editor.remove(KEY_SAVED_EMAIL);
                            editor.remove(KEY_SAVED_PASSWORD);
                            editor.apply();

                            user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    saveUserToFirestore(user.getUid(), name, phone, email, role);
                                } else {
                                    stopTimeoutAndResetUI();
                                    Toast.makeText(LoginAndSignupActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        stopTimeoutAndResetUI();
                        Toast.makeText(LoginAndSignupActivity.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String phone, String email, String role) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("phone", phone);
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("uid", uid);
        userMap.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("Users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    stopTimeoutAndResetUI();
                    Toast.makeText(LoginAndSignupActivity.this, "Success! Please check your email to verify.", Toast.LENGTH_LONG).show();
                    mAuth.signOut();

                    setTabSelected(true, true);
                    loginEmail.setText(email);
                    loginPassword.setText("");
                    signupName.setText("");
                    signupPhone.setText("");
                    signupEmail.setText("");
                    signupPassword.setText("");
                    signupConfirmPwd.setText("");
                    agreeTerms.setChecked(false);
                    signupRole.setSelection(0);
                    rememberMe.setChecked(false);
                })
                .addOnFailureListener(e -> {
                    stopTimeoutAndResetUI();
                    Log.e(TAG, "Firestore Error: " + e.getMessage());
                    Toast.makeText(LoginAndSignupActivity.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void stopTimeoutAndResetUI() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        isProcessing = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}