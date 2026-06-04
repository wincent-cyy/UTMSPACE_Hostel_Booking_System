package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminProfileActivity extends AppCompatActivity {

    private static final String TAG = "AdminProfileActivity";
    private static final String SHARED_PREFS_NAME = "HostelHub";
    private static final String KEY_PROFILE_IMAGE = "profile_image_base64";

    // Header
    private LinearLayout ivBack;

    // Profile Picture
    private LinearLayout btnChangePhoto;
    private ImageView ivProfilePicture;

    // Basic Information (Read Only)
    private TextInputEditText etFullName;
    private TextInputEditText etPhoneNumber;
    private TextInputEditText etEmail;

    // Admin Information (Editable)
    private TextInputEditText etAdminId;
    private TextInputEditText etRole;
    private TextInputEditText etDepartment;

    // Buttons
    private LinearLayout btnChangePassword;
    private LinearLayout btnLogout;
    private LinearLayout btnDeleteAccount;
    private LinearLayout btnSaveChanges;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    // Profile image
    private String currentProfileImageBase64 = "";
    private Uri gallerySelectedUri = null;

    // Gallery launcher
    private ActivityResultLauncher<Intent> galleryLauncher;

    // Crop launcher
    private ActivityResultLauncher<Intent> cropLauncher;

    // Validation patterns
    private static final Pattern ADMIN_ID_PATTERN = Pattern.compile("^[A-Z]\\d{2}[A-Z]{2}\\d{4}$");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("^[A-Za-z\\s]+$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        initViews();
        setupInputFilters();
        setupImageLaunchers();
        setupClickListeners();
        loadAdminData();
        setupTextWatchers();
    }

    /**
     * Setup status bar to be white with dark icons
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Only set status bar color to white
            getWindow().setStatusBarColor(Color.WHITE);

            // Make status bar icons dark for visibility on white background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etAdminId = findViewById(R.id.etAdminId);
        etRole = findViewById(R.id.etRole);
        etDepartment = findViewById(R.id.etDepartment);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        if (btnSaveChanges != null) {
            btnSaveChanges.setVisibility(View.GONE);
        }
    }

    private void setupInputFilters() {
        // Admin ID filter - auto uppercase, max 9 chars, only alphanumeric
        InputFilter adminIdFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                // Get current text + new input
                String currentText = dest.toString();
                String newText = currentText.substring(0, dstart) + source.toString() + currentText.substring(dend);

                // Limit to 9 characters
                if (newText.length() > 9) {
                    return "";
                }

                // Only allow letters and numbers
                Pattern pattern = Pattern.compile("[A-Za-z0-9]*");
                if (!pattern.matcher(source).matches()) {
                    return "";
                }

                // Convert to uppercase
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < source.length(); i++) {
                    char c = source.charAt(i);
                    result.append(Character.toUpperCase(c));
                }
                return result.toString();
            }
        };

        if (etAdminId != null) {
            etAdminId.setFilters(new InputFilter[]{adminIdFilter, new InputFilter.LengthFilter(9)});
        }

        // Department filter - only letters and spaces
        InputFilter departmentFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Pattern pattern = Pattern.compile("[A-Za-z\\s]*");
                if (!pattern.matcher(source).matches()) {
                    return "";
                }
                // Convert to uppercase
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < source.length(); i++) {
                    char c = source.charAt(i);
                    result.append(Character.toUpperCase(c));
                }
                return result.toString();
            }
        };

        if (etDepartment != null) {
            etDepartment.setFilters(new InputFilter[]{departmentFilter, new InputFilter.LengthFilter(50)});
        }
    }

    private void setupTextWatchers() {
        if (etAdminId != null) {
            etAdminId.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    if (btnSaveChanges != null) {
                        btnSaveChanges.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        if (etDepartment != null) {
            etDepartment.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    if (btnSaveChanges != null) {
                        btnSaveChanges.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        if (etRole != null) {
            etRole.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    if (btnSaveChanges != null) {
                        btnSaveChanges.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    private void setupImageLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            gallerySelectedUri = imageUri;
                            startCrop(imageUri);
                        }
                    }
                }
        );

        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            try {
                                // Load bitmap from URI - 保持原始尺寸用于显示
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inPreferredConfig = Bitmap.Config.RGB_565;
                                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri), null, options);

                                if (bitmap != null) {
                                    // FIXED: 不缩放图片，保持原始尺寸用于 Base64 编码
                                    // 只需要确保图片不会太大（最大 500px）
                                    int maxSize = 500;
                                    int width = bitmap.getWidth();
                                    int height = bitmap.getHeight();
                                    int finalWidth = width;
                                    int finalHeight = height;

                                    if (width > maxSize || height > maxSize) {
                                        if (width > height) {
                                            finalWidth = maxSize;
                                            finalHeight = (int) ((float) height / width * maxSize);
                                        } else {
                                            finalHeight = maxSize;
                                            finalWidth = (int) ((float) width / height * maxSize);
                                        }
                                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
                                        bitmap.recycle();
                                        bitmap = scaledBitmap;
                                    }

                                    // 直接使用 bitmap 显示圆形头像
                                    displayProfileImageWithBitmap(bitmap);

                                    // Encode to Base64
                                    currentProfileImageBase64 = bitmapToBase64(bitmap);

                                    // 显示保存按钮
                                    if (btnSaveChanges != null) {
                                        btnSaveChanges.setVisibility(View.VISIBLE);
                                    }

                                    Toast.makeText(this, "Photo updated! Remember to save.", Toast.LENGTH_SHORT).show();

                                    // Recycle bitmap
                                    bitmap.recycle();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error loading cropped image: " + e.getMessage());
                                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        Throwable error = UCrop.getError(result.getData());
                        if (error != null) {
                            Log.e(TAG, "Crop error: " + error.getMessage());
                            Toast.makeText(this, "Crop failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    // Clean up temp file
                    if (gallerySelectedUri != null && gallerySelectedUri.getPath() != null) {
                        new File(gallerySelectedUri.getPath()).delete();
                    }
                }
        );
    }

    private void startCrop(Uri sourceUri) {
        try {
            String destinationFileName = "cropped_" + System.currentTimeMillis() + ".jpg";
            File destinationFile = new File(getCacheDir(), destinationFileName);
            Uri destinationUri = Uri.fromFile(destinationFile);

            UCrop uCrop = UCrop.of(sourceUri, destinationUri);
            uCrop = uCrop.withAspectRatio(1, 1);
            uCrop = uCrop.withMaxResultSize(500, 500);  // FIXED: 增加到 500x500
            uCrop = uCrop.withOptions(getUCropOptions());

            cropLauncher.launch(uCrop.getIntent(this));
        } catch (Exception e) {
            Log.e(TAG, "Crop error: " + e.getMessage());
            Toast.makeText(this, "Cannot crop image", Toast.LENGTH_SHORT).show();
        }
    }

    private UCrop.Options getUCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(80);  // FIXED: 提高压缩质量
        options.setFreeStyleCropEnabled(false);
        options.setHideBottomControls(false);
        options.setToolbarTitle("Crop Image");

        int maroonColor = 0xFF800000;
        options.setToolbarColor(maroonColor);
        options.setStatusBarColor(maroonColor);
        options.setToolbarWidgetColor(0xFFFFFFFF);

        return options;
    }

    /**
     * FIXED: 使用 Glide 显示圆形头像到 btnChangePhoto (LinearLayout 背景)
     */
    private void displayProfileImageWithBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            resetToDefaultAvatar();
            return;
        }

        // 使用 Glide 加载圆形图片并设置为 btnChangePhoto 的背景
        Glide.with(this)
                .load(bitmap)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            btnChangePhoto.setBackground(resource);
                        } else {
                            btnChangePhoto.setBackgroundDrawable(resource);
                        }
                        ivProfilePicture.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void displayProfileImageFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null && !bitmap.isRecycled()) {
                displayProfileImageWithBitmap(bitmap);
            } else {
                resetToDefaultAvatar();
            }
        } catch (Exception e) {
            resetToDefaultAvatar();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return "";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);  // FIXED: 提高质量到 70
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        Log.d(TAG, "Base64 length: " + base64.length() + " characters");
        return base64;
    }

    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickIntent);
    }

    private void loadAdminData() {
        db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");

                        etFullName.setText(name != null ? name : "");
                        etPhoneNumber.setText(phone != null ? phone : "");
                        etEmail.setText(email != null ? email : "");

                        // Make basic info read-only
                        etFullName.setFocusable(false);
                        etFullName.setEnabled(false);
                        etPhoneNumber.setFocusable(false);
                        etPhoneNumber.setEnabled(false);
                        etEmail.setFocusable(false);
                        etEmail.setEnabled(false);

                        String role = documentSnapshot.getString("role");
                        String adminId = documentSnapshot.getString("adminId");
                        String department = documentSnapshot.getString("department");

                        etAdminId.setText(adminId != null ? adminId : "");
                        etRole.setText(role != null ? role : "Administrator");
                        etDepartment.setText(department != null ? department : "");

                        // Make admin info editable
                        etAdminId.setFocusable(true);
                        etAdminId.setEnabled(true);
                        etRole.setFocusable(true);
                        etRole.setEnabled(true);
                        etDepartment.setFocusable(true);
                        etDepartment.setEnabled(true);

                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            currentProfileImageBase64 = profileImageBase64;
                            displayProfileImageFromBase64(profileImageBase64);
                        } else {
                            resetToDefaultAvatar();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetToDefaultAvatar() {
        btnChangePhoto.setBackgroundResource(R.drawable.avatar_background);
        ivProfilePicture.setVisibility(View.VISIBLE);
        ivProfilePicture.setImageResource(R.drawable.ic_account_circle);
    }

    private boolean validateAdminInfo() {
        boolean isValid = true;

        String adminId = etAdminId.getText().toString().trim().toUpperCase();
        if (adminId.isEmpty()) {
            etAdminId.setError("Admin ID is required");
            etAdminId.requestFocus();
            isValid = false;
        } else if (adminId.length() != 9) {
            etAdminId.setError("Admin ID must be exactly 9 characters");
            isValid = false;
        } else if (!ADMIN_ID_PATTERN.matcher(adminId).matches()) {
            etAdminId.setError("Format: A12AB1234 (1 Letter + 2 Numbers + 2 Letters + 4 Numbers)");
            isValid = false;
        } else {
            etAdminId.setError(null);
        }

        String role = etRole.getText().toString().trim();
        if (role.isEmpty()) {
            etRole.setError("Role is required");
            etRole.requestFocus();
            isValid = false;
        } else {
            etRole.setError(null);
        }

        String department = etDepartment.getText().toString().trim().toUpperCase();
        if (department.isEmpty()) {
            etDepartment.setError("Department is required");
            etDepartment.requestFocus();
            isValid = false;
        } else if (!DEPARTMENT_PATTERN.matcher(department).matches()) {
            etDepartment.setError("Department can only contain letters and spaces");
            isValid = false;
        } else {
            etDepartment.setError(null);
        }

        return isValid;
    }

    private void saveAdminInfo() {
        if (!validateAdminInfo()) {
            Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setAlpha(0.5f);

        String adminId = etAdminId.getText().toString().trim().toUpperCase();
        String role = etRole.getText().toString().trim();
        String department = etDepartment.getText().toString().trim().toUpperCase();

        Map<String, Object> updates = new HashMap<>();
        updates.put("adminId", adminId);
        updates.put("role", role);
        updates.put("department", department);
        updates.put("lastUpdated", System.currentTimeMillis());

        if (currentProfileImageBase64 != null && !currentProfileImageBase64.isEmpty()) {
            updates.put("profileImageBase64", currentProfileImageBase64);
            SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putString(KEY_PROFILE_IMAGE, currentProfileImageBase64).apply();
        }

        db.collection("Users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Admin information updated successfully", Toast.LENGTH_SHORT).show();
                    if (btnSaveChanges != null) {
                        btnSaveChanges.setVisibility(View.GONE);
                    }
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setAlpha(1f);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setAlpha(1f);
                });
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your admin account? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (d, w) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = currentUserId;

        db.collection("Users").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    mAuth.getCurrentUser().delete()
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

    private void performLogout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginAndSignupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        btnChangePhoto.setOnClickListener(v -> showImagePickerDialog());
        ivProfilePicture.setOnClickListener(v -> showImagePickerDialog());

        btnChangePassword.setOnClickListener(v -> {
            String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "";
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "No email found", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> performLogout());
        btnDeleteAccount.setOnClickListener(v -> showDeleteDialog());

        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> saveAdminInfo());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
    }
}