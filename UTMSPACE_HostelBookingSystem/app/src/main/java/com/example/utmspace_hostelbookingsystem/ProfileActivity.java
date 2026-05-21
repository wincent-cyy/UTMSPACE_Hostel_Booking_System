package com.example.utmspace_hostelbookingsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executor;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String SHARED_PREFS_NAME = "BioAuthPrefs";
    private static final String KEY_BIOMETRIC_ENABLED = "FingerprintEnabled";
    private static final String KEY_SAVED_UID = "SavedUserUid";
    private static final String KEY_NOTIFICATION_ENABLED = "NotificationEnabled";

    private MaterialButton btnLogout, btnDeleteAccount;
    private RelativeLayout settingPersonalInfo, settingChangePassword;
    private ShapeableImageView ivProfileLarge, btnEditPicture;
    private TextView tvUserName, tvUserEmail;
    private CompoundButton switchBiometric;
    private SwitchCompat switchNotifications;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigation;
    private String userRole = "student";
    private SharedPreferences sharedPreferences;

    private boolean isUpdatingUI = false;
    private boolean isRoleLoaded = false;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) startCropActivity(sourceUri);
                }
            });

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) processAndUploadFirestoreImage(resultUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        fetchUserProfileData();
        loadBiometricToggleStatus();
        loadNotificationToggleStatus();
        setupListeners();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        settingPersonalInfo = findViewById(R.id.setting_change_username);
        settingChangePassword = findViewById(R.id.setting_change_password);
        ivProfileLarge = findViewById(R.id.ivProfileLarge);
        btnEditPicture = findViewById(R.id.btnEditPicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        switchBiometric = findViewById(R.id.switchBiometric);
        switchNotifications = findViewById(R.id.switchNotifications);
    }

    private void loadBiometricToggleStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        boolean isEnabled = sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, false);

        isUpdatingUI = true;
        switchBiometric.setChecked(isEnabled);
        isUpdatingUI = false;
    }

    private void loadNotificationToggleStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        boolean isEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED + "_" + uid, false);
        switchNotifications.setChecked(isEnabled);
    }

    private void updateSwitchSilently(boolean checked) {
        isUpdatingUI = true;
        switchBiometric.setChecked(checked);
        isUpdatingUI = false;
    }

    private void setupListeners() {

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
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

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) return;

            String uid = currentUser.getUid();
            sharedPreferences.edit()
                    .putBoolean(KEY_NOTIFICATION_ENABLED + "_" + uid, isChecked)
                    .apply();

            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        bottomNavigation.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();
            Intent intent = null;

            // Handle Profile selection first (common for all roles)
            if (itemId == R.id.nav_profile) {
                return true;
            }

            // Wait for role to load
            if (!isRoleLoaded) {
                Toast.makeText(this, "Loading role...", Toast.LENGTH_SHORT).show();
                return false;
            }

            // =========================
            // STAFF NAVIGATION
            // =========================
            if ("staff".equals(userRole)) {

                if (itemId == R.id.nav_staff_home) {
                    intent = new Intent(this, StaffDashboardActivity.class);

                } else if (itemId == R.id.nav_staff_bookings) {
                    intent = new Intent(this, BookingManagementActivity.class);

                } else if (itemId == R.id.nav_rooms) {
                    intent = new Intent(this, StaffRoomListActivity.class);
                }
            }

            // =========================
            // TECHNICIAN NAVIGATION
            // =========================
            else if ("technician".equals(userRole)) {

                if (itemId == R.id.nav_home) {
                    intent = new Intent(this, TechnicianDashboardActivity.class);

                } else if (itemId == R.id.nav_booking) {
                    Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
                    return false;
                } else if (itemId == R.id.nav_history) {
                    Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            // =========================
            // STUDENT NAVIGATION
            // =========================
            else {

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

        settingPersonalInfo.setOnClickListener(v ->
                startActivity(new Intent(this, EditInfoActivity.class)));

        settingChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ProfilePasswordActivity.class)));

        btnEditPicture.setOnClickListener(v ->
                galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));

        btnLogout.setOnClickListener(v -> performLogout());

        btnDeleteAccount.setOnClickListener(v -> showDeleteDialog());
    }

    private void setupBottomNavMenu() {
        if (bottomNavigation == null) return;

        bottomNavigation.getMenu().clear();

        if ("staff".equals(userRole)) {
            bottomNavigation.inflateMenu(R.menu.staff_nav_menu);
        } else if ("technician".equals(userRole)) {
            // Check if technician menu exists, otherwise use student menu
            try {
                bottomNavigation.inflateMenu(R.menu.technician_nav_menu);
            } catch (Exception e) {
                bottomNavigation.inflateMenu(R.menu.student_nav_menu);
            }
        } else {
            bottomNavigation.inflateMenu(R.menu.student_nav_menu);
        }

        // Set profile as selected (use the correct ID based on role)
        try {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        } catch (Exception e) {
            // If nav_profile doesn't exist in the menu, try to find a profile equivalent
            Log.e(TAG, "Profile menu item not found", e);
        }
    }

    private void authenticateBiometric(String uid) {
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        // 保存指纹启用状态
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

    private void startCropActivity(Uri sourceUri) {
        try {
            File file = new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg");

            UCrop uCrop = UCrop.of(sourceUri, Uri.fromFile(file))
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(512, 512);

            cropLauncher.launch(uCrop.getIntent(this));
        } catch (Exception e) {
            Log.e(TAG, "Crop error", e);
            Toast.makeText(this, "Failed to crop image", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // User not logged in, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db.collection("Users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Get role - FIXED: Only set once
                        String role = doc.getString("role");
                        userRole = (role == null) ? "student" : role.trim().toLowerCase();

                        // Set name and email
                        String name = doc.getString("name");
                        String email = doc.getString("email");

                        if (name != null && !name.isEmpty()) {
                            tvUserName.setText(name);
                        } else {
                            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
                        }

                        if (email != null && !email.isEmpty()) {
                            tvUserEmail.setText(email);
                        } else {
                            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                        }

                        // Load profile picture from Base64
                        String img = doc.getString("profilePictureBase64");
                        if (img != null && !img.isEmpty()) {
                            try {
                                byte[] bytes = Base64.decode(img, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                ivProfileLarge.setImageBitmap(bitmap);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to decode image", e);
                            }
                        }

                        // Mark role as loaded
                        isRoleLoaded = true;

                        // Setup bottom navigation menu
                        setupBottomNavMenu();
                    } else {
                        // User document doesn't exist, create one?
                        isRoleLoaded = true;
                        userRole = "student";
                        setupBottomNavMenu();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user profile details", e);
                    // Still mark as loaded with default role
                    isRoleLoaded = true;
                    userRole = "student";
                    setupBottomNavMenu();
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                });
    }

    private void processAndUploadFirestoreImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            // Compress bitmap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            // Update Firestore
            db.collection("Users")
                    .document(mAuth.getCurrentUser().getUid())
                    .update("profilePictureBase64", base64)
                    .addOnSuccessListener(aVoid -> {
                        runOnUiThread(() -> {
                            ivProfileLarge.setImageBitmap(bitmap);
                            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Failed to update picture: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    });

            is.close();

        } catch (Exception e) {
            Log.e(TAG, "Image error", e);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
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

            SharedPreferences bioPrefs = getSharedPreferences("BioAuthPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = bioPrefs.edit();
            editor.remove(KEY_BIOMETRIC_ENABLED + "_" + uid);  // 清除该用户的指纹状态
            editor.remove(KEY_NOTIFICATION_ENABLED + "_" + uid);
            editor.remove(KEY_SAVED_UID);                       // 清除保存的 UID
            editor.remove("SavedEmail");                        // 清除保存的邮箱
            editor.remove("SavedPassword");                     // 清除保存的密码
            editor.apply();

            // Delete Firestore document first
            db.collection("Users").document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Then delete Firebase Auth user
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
        // 只退出登录，不清除指纹数据
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNavigation != null) {
            bottomNavigation.post(() -> {
                try {
                    bottomNavigation.setSelectedItemId(R.id.nav_profile);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set selected item", e);
                }
            });
        }
    }
}