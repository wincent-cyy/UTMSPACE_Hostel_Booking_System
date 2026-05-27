package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private ShapeableImageView ivProfileImage;
    private TextView tvAdminName, tvAdminEmail, tvAdminRole, tvMemberSince;
    private TextView tvTotalBookings, tvTotalRooms, tvTotalUsers;
    private EditText etAdminName, etAdminEmail;
    private Button btnEditProfile, btnSaveChanges, btnChangePassword, btnLogout, btnChangePicture;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isEditMode = false;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadAdminData();
        loadUserStats();
        setupClickListeners();
    }

    private void initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminEmail = findViewById(R.id.tvAdminEmail);
        tvAdminRole = findViewById(R.id.tvAdminRole);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvTotalRooms = findViewById(R.id.tvTotalRooms);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        etAdminName = findViewById(R.id.etAdminName);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);
        btnChangePicture = findViewById(R.id.btnChangePicture);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadAdminData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId != null) {
            progressBar.setVisibility(View.VISIBLE);

            db.collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        progressBar.setVisibility(View.GONE);

                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String role = documentSnapshot.getString("role");
                            Long createdAt = documentSnapshot.getLong("createdAt");
                            String profilePictureBase64 = documentSnapshot.getString("profilePictureBase64");

                            if (name != null) {
                                tvAdminName.setText(name);
                                etAdminName.setText(name);
                            }
                            if (email != null) {
                                tvAdminEmail.setText(email);
                                etAdminEmail.setText(email);
                            }
                            tvAdminRole.setText(role != null ? role : "Administrator");
                            if (createdAt != null) {
                                tvMemberSince.setText("Member since: " + formatDate(createdAt));
                            }

                            // Load profile image from Base64
                            loadImageFromBase64(profilePictureBase64);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadImageFromBase64(String base64String) {
        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
                Glide.with(this)
                        .load(imageBytes)
                        .circleCrop()
                        .placeholder(R.drawable.profile_pic)
                        .error(R.drawable.profile_pic)
                        .into(ivProfileImage);
            } catch (Exception e) {
                ivProfileImage.setImageResource(R.drawable.profile_pic);
            }
        } else {
            ivProfileImage.setImageResource(R.drawable.profile_pic);
        }
    }

    private void loadUserStats() {
        db.collection("Bookings").get().addOnSuccessListener(task -> tvTotalBookings.setText(String.valueOf(task.size())));
        db.collection("Rooms").get().addOnSuccessListener(task -> tvTotalRooms.setText(String.valueOf(task.size())));
        db.collection("Users").get().addOnSuccessListener(task -> tvTotalUsers.setText(String.valueOf(task.size())));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // 点击图片直接选择并自动保存
        btnChangePicture.setOnClickListener(v -> showImagePickerDialog());
        ivProfileImage.setOnClickListener(v -> showImagePickerDialog());

        btnEditProfile.setOnClickListener(v -> {
            isEditMode = true;
            tvAdminName.setVisibility(View.GONE);
            tvAdminEmail.setVisibility(View.GONE);
            etAdminName.setVisibility(View.VISIBLE);
            etAdminEmail.setVisibility(View.VISIBLE);
            btnChangePicture.setVisibility(View.VISIBLE);
            btnEditProfile.setVisibility(View.GONE);
            btnSaveChanges.setVisibility(View.VISIBLE);
            btnChangePassword.setVisibility(View.GONE);
        });

        btnSaveChanges.setOnClickListener(v -> saveNameAndEmail()); // 只保存名字和邮箱

        btnChangePassword.setOnClickListener(v -> {
            String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "";
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show());
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && requestCode == PICK_IMAGE_REQUEST) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                // 直接转换并自动保存到 Firestore
                uploadAndSaveImageToFirestore(imageUri);
            }
        }
    }

    // 自动上传并保存图片到 Firestore
    private void uploadAndSaveImageToFirestore(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // 转换图片为 Base64
        String base64Image = convertUriToBase64(imageUri);
        if (base64Image == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示图片
        loadImageFromBase64(base64Image);

        // 自动保存到 Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePictureBase64", base64Image);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection("Users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                    Log.d("AdminProfile", "Image saved to Firestore automatically");

                    // 更新 Dashboard 的图片
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updatedImageBase64", base64Image);
                    setResult(RESULT_OK, resultIntent);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("AdminProfile", "Failed to save image", e);
                    Toast.makeText(this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 只保存名字和邮箱
    private void saveNameAndEmail() {
        String newName = etAdminName.getText().toString().trim();
        String newEmail = etAdminEmail.getText().toString().trim();

        if (newName.isEmpty()) {
            etAdminName.setError("Name cannot be empty");
            return;
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            progressBar.setVisibility(View.VISIBLE);

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("email", newEmail);
            updates.put("lastUpdated", System.currentTimeMillis());

            db.collection("Users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("updatedName", newName);
                        setResult(RESULT_OK, resultIntent);

                        isEditMode = false;
                        tvAdminName.setText(newName);
                        tvAdminEmail.setText(newEmail);
                        tvAdminName.setVisibility(View.VISIBLE);
                        tvAdminEmail.setVisibility(View.VISIBLE);
                        etAdminName.setVisibility(View.GONE);
                        etAdminEmail.setVisibility(View.GONE);
                        btnChangePicture.setVisibility(View.GONE);
                        btnEditProfile.setVisibility(View.VISIBLE);
                        btnSaveChanges.setVisibility(View.GONE);
                        btnChangePassword.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            inputStream.close();

            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            int maxSize = 300;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min((float) maxSize / width, (float) maxSize / height);

            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            android.graphics.Bitmap resizedBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

            ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, compressedStream);
            byte[] compressedBytes = compressedStream.toByteArray();

            return Base64.encodeToString(compressedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}