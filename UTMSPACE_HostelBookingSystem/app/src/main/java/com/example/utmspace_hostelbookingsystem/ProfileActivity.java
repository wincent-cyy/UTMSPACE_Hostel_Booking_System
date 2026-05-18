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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.firestore.DocumentSnapshot;
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

    private MaterialButton btnLogout, btnDeleteAccount;
    private RelativeLayout settingPersonalInfo, settingChangePassword;
    private ShapeableImageView ivProfileLarge, btnEditPicture;
    private TextView tvUserName, tvUserEmail;
    private CompoundButton switchBiometric;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigation;
    private String userRole = "student";
    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) {
                        startCropActivity(sourceUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        processAndUploadFirestoreImage(resultUri);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                    Throwable cropError = UCrop.getError(result.getData());
                    Log.e(TAG, "Image crop failed: ", cropError);
                    Toast.makeText(this, "Cropping failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        fetchUserProfileData(); // Role is identified here, which also updates the Bottom Navigation menu item items
        loadBiometricToggleStatus();
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

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void loadBiometricToggleStatus() {
        if (switchBiometric != null) {
            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : sharedPreferences.getString(KEY_SAVED_UID, null);
            if (uid != null) {
                boolean isEnabled = sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, false);
                switchBiometric.setChecked(isEnabled);
            }
        }
    }

    private void startCropActivity(Uri sourceUri) {
        try {
            File cacheDir = getCacheDir();
            String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
            File destinationFile = new File(cacheDir, destinationFileName);
            Uri destinationUri = Uri.fromFile(destinationFile);

            UCrop.Options options = new UCrop.Options();
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(80);
            options.setHideBottomControls(false);
            options.setFreeStyleCropEnabled(false);

            Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(300, 300)
                    .withOptions(options)
                    .getIntent(this);

            cropLauncher.launch(cropIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error compiling crop directory setup: ", e);
            Toast.makeText(this, "Failed to initialize image cropper.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserProfileData() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : sharedPreferences.getString(KEY_SAVED_UID, null);
        if (uid == null) return;

        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            userRole = role.toLowerCase();
                            // CRITICAL FIX: Re-inflate menu components explicitly based on Staff or Student context assignments
                            updateBottomMenuStructure();
                        }

                        String name = documentSnapshot.getString("name");
                        if (name != null && tvUserName != null) {
                            tvUserName.setText(name);
                        } else if (documentSnapshot.getString("username") != null && tvUserName != null) {
                            tvUserName.setText(documentSnapshot.getString("username"));
                        }

                        String email = documentSnapshot.getString("email");
                        if (email != null && tvUserEmail != null) {
                            tvUserEmail.setText(email);
                        }

                        String base64ImageString = documentSnapshot.getString("profilePictureBase64");
                        if (base64ImageString != null && !base64ImageString.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(base64ImageString, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                if (ivProfileLarge != null && decodedByte != null) {
                                    ivProfileLarge.setImageBitmap(decodedByte);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Base64 text image decode failed", e);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch user profile details", e));
    }

    // Dynamic Menu Swapping Infrastructure to prevent role mixing bugs
    private void updateBottomMenuStructure() {
        if (bottomNavigation == null) return;

        bottomNavigation.getMenu().clear();
        if ("staff".equalsIgnoreCase(userRole)) {
            // Inflates your designated Staff design layout file
            bottomNavigation.inflateMenu(R.menu.staff_nav_menu);
        } else {
            // Default student/technician layout menu file
            bottomNavigation.inflateMenu(R.menu.student_nav_menu);
        }

        // Re-verify visual indicator anchor rules remain focused on Profile selection area
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    private void processAndUploadFirestoreImage(Uri imageUri) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : sharedPreferences.getString(KEY_SAVED_UID, null);
        if (uid == null) return;

        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            if (bitmap == null) return;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64ImageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            ivProfileLarge.setImageBitmap(bitmap);

            db.collection("Users").document(uid)
                    .update("profilePictureBase64", base64ImageString)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Save failure: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Log.e(TAG, "Image processing error: ", e);
            Toast.makeText(this, "Failed to compile chosen image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        if (switchBiometric != null) {
            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : sharedPreferences.getString(KEY_SAVED_UID, null);
                if (uid == null) {
                    Toast.makeText(this, "Please sign in normally with password first.", Toast.LENGTH_SHORT).show();
                    switchBiometric.setChecked(false);
                    return;
                }

                if (isChecked) {
                    BiometricManager biometricManager = BiometricManager.from(this);
                    switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                        case BiometricManager.BIOMETRIC_SUCCESS:
                            authenticateBiometricToToggle(true, uid);
                            break;
                        default:
                            Toast.makeText(this, "Biometric features are not set up or available on this hardware.", Toast.LENGTH_LONG).show();
                            switchBiometric.setChecked(false);
                            break;
                    }
                } else {
                    sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, false).apply();
                    Toast.makeText(this, "Fingerprint sign-in disabled.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_profile) return true;

                Intent intent = null;

                // FIXED: Processes conditional ID checks separately according to inflated role parameters
                if (itemId == R.id.nav_home || itemId == R.id.nav_staff_home) {
                    if ("staff".equalsIgnoreCase(userRole)) {
                        intent = new Intent(this, StaffDashboardActivity.class);
                    } else if ("technician".equalsIgnoreCase(userRole)) {
                        intent = new Intent(this, TechnicianDashboardActivity.class);
                    } else {
                        intent = new Intent(this, StudentDashboardActivity.class);
                    }
                }
                else if (itemId == R.id.nav_booking || itemId == R.id.nav_staff_bookings) {
                    if ("staff".equalsIgnoreCase(userRole)) {
                        intent = new Intent(this, BookingManagementActivity.class);
                    } else if ("technician".equalsIgnoreCase(userRole)) {
                        Toast.makeText(this, "Technician booking access coming soon!", Toast.LENGTH_SHORT).show();
                        return false;
                    } else {
                        intent = new Intent(this, BookingsActivity.class);
                    }
                }
                else if (itemId == R.id.nav_history) {
                    intent = new Intent(this, HistoryActivity.class);
                }
                else if (itemId == R.id.nav_rooms) {
                    Toast.makeText(this, "Room management features coming soon!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }

        if (settingPersonalInfo != null) {
            settingPersonalInfo.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, EditInfoActivity.class);
                startActivity(intent);
            });
        }

        if (settingChangePassword != null) {
            settingChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, ProfilePasswordActivity.class);
                startActivity(intent);
            });
        }

        if (btnEditPicture != null) {
            btnEditPicture.setOnClickListener(v -> {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(galleryIntent);
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
        }
    }

    private void authenticateBiometricToToggle(boolean targetState, String uid) {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(ProfileActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (switchBiometric != null) {
                    switchBiometric.setChecked(false);
                }
                Toast.makeText(ProfileActivity.this, "Cancelled: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                sharedPreferences.edit()
                        .putBoolean(KEY_BIOMETRIC_ENABLED + "_" + uid, targetState)
                        .putString(KEY_SAVED_UID, uid)
                        .apply();

                Toast.makeText(ProfileActivity.this, "Fingerprint login activated successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(ProfileActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Secure Validation")
                .setSubtitle("Confirm hardware authentication to complete setup")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your profile permanently?")
                .setPositiveButton("Delete Forever", (dialog, which) -> deleteUserAccountWithData())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_delete)
                .show();
    }

    private void deleteUserAccountWithData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session closed. Please type your password to authorize deletion.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        db.collection("Users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    sharedPreferences.edit()
                                            .remove(KEY_BIOMETRIC_ENABLED + "_" + userId)
                                            .remove(KEY_SAVED_UID)
                                            .apply();
                                    mAuth.signOut();
                                    Toast.makeText(ProfileActivity.this, "Account data wiped successfully.", Toast.LENGTH_LONG).show();
                                    navigateToLogin();
                                } else {
                                    if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                        Toast.makeText(this, "Please sign out and sign in with password to re-authenticate this action.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore Delete Error: ", e);
                    Toast.makeText(this, "Connection error.", Toast.LENGTH_LONG).show();
                });
    }

    private void performLogout() {
        try {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        } catch (Exception e) {
            Log.e(TAG, "Logout Error", e);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
    }
}