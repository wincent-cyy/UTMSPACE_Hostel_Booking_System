package com.example.utmspace_hostelbookingsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String SHARED_PREFS_NAME = "BioAuthPrefs";
    private static final String KEY_BIOMETRIC_ENABLED = "FingerprintEnabled";
    private static final String KEY_SAVED_UID = "SavedUserUid";
    private static final String KEY_NOTIFICATION_ENABLED = "NotificationEnabled";

    // UI Elements
    private LinearLayout profileAvatar;  // 改用 LinearLayout 作为头像容器
    private ImageView ivProfilePicture;
    private TextView tvUserName;
    private TextView tvUserRole;
    private TextView tvUserEmail;

    private LinearLayout btnEditProfile;
    private LinearLayout btnChangePassword;
    private LinearLayout btnFingerprint;
    private Switch switchFingerprint;
    private LinearLayout btnDeleteAccount;
    private LinearLayout btnLogout;

    private BottomNavigationView bottomNavigationView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    private String userRole = "student";
    private boolean isRoleLoaded = false;
    private boolean isUpdatingUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        fetchUserProfileData();
        loadBiometricToggleStatus();
        setupListeners();
    }

    private void initViews() {
        // Header - 使用 profileAvatar 作为头像容器
        profileAvatar = findViewById(R.id.profileAvatar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserRole = findViewById(R.id.tvUserRole);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        // Account Management Section
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Security and Privacy Section
        btnFingerprint = findViewById(R.id.btnFingerprint);
        switchFingerprint = findViewById(R.id.switchFingerprint);

        // Danger Zone Section
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        // Logout Button
        btnLogout = findViewById(R.id.btnLogout);

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // ========== 设置 Switch 颜色 ==========
        // 设置 Switch 的 Track 颜色（背景）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 未选中时的 Track 颜色
            switchFingerprint.setTrackTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#E0E0E0")));
            // 选中时的 Track 颜色
            switchFingerprint.setTrackTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#800000")));
        }

        // 设置 Switch 的 Thumb 颜色（圆形按钮）
        switchFingerprint.setThumbTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#FFFFFF")));

        if (profileAvatar == null) {
            Log.e(TAG, "profileAvatar not found in XML! Please add android:id=\"@+id/profileAvatar\"");
        }
    }

    private void loadBiometricToggleStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        boolean isEnabled = sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, false);

        isUpdatingUI = true;
        switchFingerprint.setChecked(isEnabled);
        isUpdatingUI = false;
    }

    private void updateSwitchSilently(boolean checked) {
        isUpdatingUI = true;
        switchFingerprint.setChecked(checked);
        isUpdatingUI = false;
    }

    private void setupListeners() {
        // Fingerprint Switch
        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingUI) return;

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                updateSwitchSilently(false);
                return;
            }

            String uid = currentUser.getUid();

            if (isChecked) {
                BiometricManager manager = BiometricManager.from(this);
                int result = manager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                );

                if (result == BiometricManager.BIOMETRIC_SUCCESS) {
                    authenticateBiometric(uid);
                } else if (result == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                    Toast.makeText(this, "No fingerprint hardware available", Toast.LENGTH_SHORT).show();
                    updateSwitchSilently(false);
                } else if (result == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                    Toast.makeText(this, "Fingerprint hardware not available", Toast.LENGTH_SHORT).show();
                    updateSwitchSilently(false);
                } else if (result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                    Toast.makeText(this, "No fingerprint enrolled. Please add fingerprint in device settings.", Toast.LENGTH_LONG).show();
                    updateSwitchSilently(false);
                } else {
                    Toast.makeText(this, "Biometric not available", Toast.LENGTH_SHORT).show();
                    updateSwitchSilently(false);
                }
            } else {
                sharedPreferences.edit()
                        .putBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, false)
                        .apply();
                Toast.makeText(this, "Fingerprint disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Edit Profile
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditInfoActivity.class);
            startActivity(intent);
        });

        // Change Password
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ProfilePasswordActivity.class);
            startActivity(intent);
        });

        // Delete Account
        btnDeleteAccount.setOnClickListener(v -> showDeleteDialog());

        // Logout
        btnLogout.setOnClickListener(v -> performLogout());

        // Bottom Navigation
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                return true;
            }

            if (!isRoleLoaded) {
                Toast.makeText(this, "Loading role...", Toast.LENGTH_SHORT).show();
                return false;
            }

            Intent intent = null;

            if ("staff".equals(userRole)) {
                if (itemId == R.id.nav_home) {
                    intent = new Intent(this, StaffDashboardActivity.class);
                } else if (itemId == R.id.nav_booking) {
                    intent = new Intent(this, BookingManagementActivity.class);
                } else if (itemId == R.id.nav_history) {
                    intent = new Intent(this, StaffRoomListActivity.class);
                }
            } else if ("technician".equals(userRole)) {
                if (itemId == R.id.nav_home) {
                    intent = new Intent(this, TechnicianDashboardActivity.class);
                } else if (itemId == R.id.nav_booking) {
                    intent = new Intent(this, TechnicianRepairRequestActivity.class);
                } else if (itemId == R.id.nav_history) {
                    intent = new Intent(this, TechnicianHistoryActivity.class);
                }
            } else {
                if (itemId == R.id.nav_home) {
                    intent = new Intent(this, StudentDashboardActivity.class);
                } else if (itemId == R.id.nav_booking) {
                    intent = new Intent(this, BookingsActivity.class);
                } else if (itemId == R.id.nav_history) {
                    intent = new Intent(this, HistoryActivity.class);
                }
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }

    private void authenticateBiometric(String uid) {
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, true);
                        editor.putString(KEY_SAVED_UID, uid);
                        editor.apply();

                        Log.d(TAG, "Fingerprint enabled for user: " + uid);

                        runOnUiThread(() -> {
                            updateSwitchSilently(true);
                            Toast.makeText(ProfileActivity.this, "Fingerprint enabled successfully", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        runOnUiThread(() -> {
                            updateSwitchSilently(false);
                            Toast.makeText(ProfileActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        Log.e(TAG, "Authentication error: " + errorCode + " - " + errString);
                        runOnUiThread(() -> {
                            updateSwitchSilently(false);
                            Toast.makeText(ProfileActivity.this, "Error: " + errString, Toast.LENGTH_SHORT).show();
                        });
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable Fingerprint Login")
                .setSubtitle("Verify your identity to enable fingerprint login for future sessions")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void fetchUserProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginAndSignupActivity.class));
            finish();
            return;
        }

        db.collection("Users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Get role
                        String role = doc.getString("role");
                        userRole = (role == null) ? "student" : role.trim().toLowerCase();
                        tvUserRole.setText(userRole.substring(0, 1).toUpperCase() + userRole.substring(1));

                        // Set name and email
                        String name = doc.getString("name");
                        String email = doc.getString("email");

                        tvUserName.setText(name != null && !name.isEmpty() ? name :
                                (user.getDisplayName() != null ? user.getDisplayName() : "User"));
                        tvUserEmail.setText(email != null && !email.isEmpty() ? email :
                                (user.getEmail() != null ? user.getEmail() : ""));

                        // Load profile picture
                        String base64String = doc.getString("profileImageBase64");

                        if (base64String != null && !base64String.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                if (bitmap != null && profileAvatar != null) {
                                    // 清除原有背景
                                    profileAvatar.setBackground(null);
                                    // 使用 Glide 加载圆形图片并设置为背景
                                    Glide.with(this)
                                            .load(bitmap)
                                            .circleCrop()
                                            .into(new CustomTarget<Drawable>() {
                                                @Override
                                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                    if (profileAvatar != null) {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                            profileAvatar.setBackground(resource);
                                                        } else {
                                                            profileAvatar.setBackgroundDrawable(resource);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                                }
                                            });
                                    // 隐藏 ImageView
                                    if (ivProfilePicture != null) {
                                        ivProfilePicture.setVisibility(View.GONE);
                                    }
                                } else {
                                    resetToDefaultAvatar();
                                }
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, "Base64 decode error", e);
                                resetToDefaultAvatar();
                            }
                        } else {
                            resetToDefaultAvatar();
                        }

                        isRoleLoaded = true;

                        if (bottomNavigationView != null) {
                            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                        }
                    } else {
                        isRoleLoaded = true;
                        userRole = "student";
                        tvUserRole.setText("Student");
                        resetToDefaultAvatar();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user profile details", e);
                    isRoleLoaded = true;
                    userRole = "student";
                    tvUserRole.setText("Student");
                    resetToDefaultAvatar();
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetToDefaultAvatar() {
        if (profileAvatar != null) {
            profileAvatar.setBackgroundResource(R.drawable.avatar_background);
        }
        if (ivProfilePicture != null) {
            ivProfilePicture.setVisibility(View.VISIBLE);
            ivProfilePicture.setImageResource(R.drawable.ic_account_circle);
        }
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (d, w) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_BIOMETRIC_ENABLED + "_" + uid);
            editor.remove(KEY_NOTIFICATION_ENABLED + "_" + uid);
            editor.remove(KEY_SAVED_UID);
            editor.remove("SavedEmail");
            editor.remove("SavedPassword");
            editor.apply();

            db.collection("Users").document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        user.delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                    performLogout();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to delete auth: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void performLogout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginAndSignupActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserProfileData();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        }
    }
}