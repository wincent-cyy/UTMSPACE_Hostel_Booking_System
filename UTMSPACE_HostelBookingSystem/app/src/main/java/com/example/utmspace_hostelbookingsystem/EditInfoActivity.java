package com.example.utmspace_hostelbookingsystem;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class EditInfoActivity extends AppCompatActivity {

    private static final String TAG = "EditInfoActivity";
    private static final String SHARED_PREFS_NAME = "HostelHub";
    private static final String KEY_PROFILE_IMAGE = "profile_image_base64";

    // UI Elements
    private LinearLayout ivBack;
    private LinearLayout btnSave;
    private LinearLayout btnChangePhoto;
    private ImageView ivProfilePicture;

    // Basic Information (Read Only)
    private TextInputEditText etFullName;
    private TextInputEditText etPhoneNumber;
    private TextInputEditText etEmail;

    // Role Information (Dynamic based on user role)
    private TextInputLayout tilField1;
    private TextInputLayout tilField2;
    private TextInputLayout tilField3;
    private TextInputEditText etStudentId;   // Will be used as Field 1
    private TextInputEditText etProgramme;    // Will be used as Field 2
    private TextInputEditText etSemester;     // Will be used as Field 3

    // Technician Section
    private LinearLayout technicianSection;
    private TextInputEditText etSpecialization;
    private TextInputEditText etWorkshop;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String currentUserId;
    private String userRole = "";

    // Profile image
    private String currentProfileImageBase64 = "";
    private Uri capturedImageUri = null;
    private File capturedImageFile = null;
    private Uri gallerySelectedUri = null;

    // Permission launcher
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    // Camera launcher
    private ActivityResultLauncher<Intent> cameraLauncher;

    // Gallery launcher
    private ActivityResultLauncher<Intent> galleryLauncher;

    // Crop launcher
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = currentUser.getUid();

        initViews();
        setupInputFilters();
        setupTextWatchers();
        setupPermissionLauncher();
        setupImageLaunchers();
        setupClickListeners();
        makeBasicInfoReadOnly();
        loadUserData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        btnSave = findViewById(R.id.btnSave);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);

        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);

        // Role Information fields - using your XML IDs
        tilField1 = findViewById(R.id.tilField1);
        tilField2 = findViewById(R.id.tilField2);
        tilField3 = findViewById(R.id.tilField3);
        etStudentId = findViewById(R.id.etStudentId);
        etProgramme = findViewById(R.id.etProgramme);
        etSemester = findViewById(R.id.etSemester);

        // Technician Section
        technicianSection = findViewById(R.id.technicianSection);
        etSpecialization = findViewById(R.id.etSpecialization);
        etWorkshop = findViewById(R.id.etWorkshop);
    }

    private void setupInputFilters() {
        // Alphanumeric filter (for IDs)
        InputFilter alphanumericFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Pattern pattern = Pattern.compile("[A-Za-z0-9]*");
                if (!pattern.matcher(source).matches()) {
                    return "";
                }
                return null;
            }
        };

        // Text filter (letters, spaces, dots)
        InputFilter textFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Pattern pattern = Pattern.compile("[A-Za-z\\s\\.]*");
                if (!pattern.matcher(source).matches()) {
                    return "";
                }
                return null;
            }
        };

        // Number filter
        InputFilter numberFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Pattern pattern = Pattern.compile("[0-9]*");
                if (!pattern.matcher(source).matches()) {
                    return "";
                }
                return null;
            }
        };

        // Auto-format filter for ID (Student or Staff)
        InputFilter idFormatFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                // Get the current text + new input
                String currentText = dest.toString();
                String newText = currentText.substring(0, dstart) + source.toString() + currentText.substring(dend);

                // Limit to 9 characters total
                if (newText.length() > 9) {
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

        // Apply filters - Student/Staff ID: max 9 characters, auto uppercase
        if (etStudentId != null) {
            etStudentId.setFilters(new InputFilter[]{alphanumericFilter, idFormatFilter, new InputFilter.LengthFilter(9)});
        }
        if (etProgramme != null) {
            etProgramme.setFilters(new InputFilter[]{textFilter, new InputFilter.LengthFilter(50)});
        }
        if (etSemester != null) {
            etSemester.setFilters(new InputFilter[]{numberFilter, new InputFilter.LengthFilter(4)});
        }
    }

    private void setupTextWatchers() {
        // Student/Staff ID - Convert to uppercase
        if (etStudentId != null) {
            etStudentId.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    String input = s.toString();
                    if (!input.equals(input.toUpperCase())) {
                        s.replace(0, s.length(), input.toUpperCase());
                    }
                }
            });
        }

        // Programme/Department - Convert to uppercase
        if (etProgramme != null) {
            etProgramme.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    String input = s.toString();
                    if (!input.equals(input.toUpperCase())) {
                        s.replace(0, s.length(), input.toUpperCase());
                    }
                }
            });
        }
    }

    private void makeBasicInfoReadOnly() {
        etFullName.setFocusable(false);
        etFullName.setClickable(false);
        etFullName.setEnabled(false);
        etPhoneNumber.setFocusable(false);
        etPhoneNumber.setClickable(false);
        etPhoneNumber.setEnabled(false);
        etEmail.setFocusable(false);
        etEmail.setClickable(false);
        etEmail.setEnabled(false);
    }

    private void setupPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && capturedImageFile != null) {
                        try {
                            Uri imageUri = FileProvider.getUriForFile(this,
                                    getPackageName() + ".fileprovider", capturedImageFile);
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            if (bitmap != null) {
                                processAndDisplayImage(bitmap);
                                if (capturedImageFile.exists()) {
                                    capturedImageFile.delete();
                                }
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading camera image: " + e.getMessage());
                            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );

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
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                                if (bitmap != null) {
                                    processAndDisplayImage(bitmap);
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
            uCrop = uCrop.withMaxResultSize(200, 200);
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
        options.setCompressionQuality(50);
        options.setFreeStyleCropEnabled(false);
        options.setHideBottomControls(false);
        options.setToolbarTitle("Crop Image");

        int maroonColor = 0xFF800000;
        options.setToolbarColor(maroonColor);
        options.setStatusBarColor(maroonColor);
        options.setToolbarWidgetColor(0xFFFFFFFF);

        return options;
    }

    private void processAndDisplayImage(Bitmap originalBitmap) {
        int targetSize = 200;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, true);

        Glide.with(this)
                .load(scaledBitmap)
                .circleCrop()
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

        currentProfileImageBase64 = bitmapToShortBase64(scaledBitmap);
        Toast.makeText(this, "Photo updated! Remember to save.", Toast.LENGTH_SHORT).show();
        originalBitmap.recycle();
    }

    private String bitmapToShortBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        Log.d(TAG, "Base64 length: " + base64.length() + " characters");
        return base64;
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else if (which == 1) {
                        openGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            capturedImageFile = createImageFile();
            if (capturedImageFile != null) {
                capturedImageUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", capturedImageFile);
                Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                cameraLauncher.launch(takeIntent);
            } else {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Camera error: " + e.getMessage());
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickIntent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        btnChangePhoto.setOnClickListener(v -> showImagePickerDialog());
        ivProfilePicture.setOnClickListener(v -> showImagePickerDialog());
        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void updateUIBasedOnRole() {
        // 找到 Role Information 部分的父容器
        View roleInformationSection = findViewById(R.id.roleInformationSection);

        if (userRole.equalsIgnoreCase("student")) {
            // Student labels
            if (tilField1 != null) tilField1.setHint("Student ID");
            if (tilField2 != null) tilField2.setHint("Programme");
            if (tilField3 != null) tilField3.setHint("Semester");

            // 显示 Role Information 部分
            if (roleInformationSection != null) roleInformationSection.setVisibility(View.VISIBLE);

            // Hide technician section
            if (technicianSection != null) technicianSection.setVisibility(View.GONE);

        } else if (userRole.equalsIgnoreCase("staff")) {
            // Staff labels
            if (tilField1 != null) tilField1.setHint("Staff ID");
            if (tilField2 != null) tilField2.setHint("Department");
            if (tilField3 != null) tilField3.setHint("Year");

            // 显示 Role Information 部分
            if (roleInformationSection != null) roleInformationSection.setVisibility(View.VISIBLE);

            // Hide technician section
            if (technicianSection != null) technicianSection.setVisibility(View.GONE);

        } else if (userRole.equalsIgnoreCase("technician")) {
            // Technician - 隐藏整个 Role Information 部分
            if (roleInformationSection != null) roleInformationSection.setVisibility(View.GONE);

            // 显示 Technician 专用部分
            if (technicianSection != null) technicianSection.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserData() {
        db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");
                        String role = documentSnapshot.getString("role");

                        etFullName.setText(name != null ? name : "Not provided");
                        etPhoneNumber.setText(phone != null ? phone : "Not provided");
                        etEmail.setText(email != null ? email : currentUser.getEmail());

                        userRole = role != null ? role.toLowerCase() : "student";

                        // Load based on role
                        if (userRole.equalsIgnoreCase("student")) {
                            String studentId = documentSnapshot.getString("studentId");
                            String programme = documentSnapshot.getString("programme");
                            String semester = documentSnapshot.getString("semester");

                            etStudentId.setText(studentId != null ? studentId : "");
                            etProgramme.setText(programme != null ? programme : "");
                            etSemester.setText(semester != null ? semester : "");
                        } else if (userRole.equalsIgnoreCase("staff")) {
                            String staffId = documentSnapshot.getString("staffId");
                            String department = documentSnapshot.getString("department");
                            String year = documentSnapshot.getString("year");

                            etStudentId.setText(staffId != null ? staffId : "");
                            etProgramme.setText(department != null ? department : "");
                            etSemester.setText(year != null ? year : "");
                        } else if (userRole.equalsIgnoreCase("technician")) {
                            String specialization = documentSnapshot.getString("specialization");
                            String workshop = documentSnapshot.getString("workshop");

                            etSpecialization.setText(specialization != null ? specialization : "");
                            etWorkshop.setText(workshop != null ? workshop : "");
                        }

                        // Update UI based on role
                        updateUIBasedOnRole();

                        // Load profile image
                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            currentProfileImageBase64 = profileImageBase64;
                            try {
                                byte[] decodedBytes = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                if (bitmap != null) {
                                    Glide.with(this)
                                            .load(bitmap)
                                            .circleCrop()
                                            .into(new CustomTarget<Drawable>() {
                                                @Override
                                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                    btnChangePhoto.setBackground(resource);
                                                    ivProfilePicture.setVisibility(View.GONE);
                                                }
                                                @Override
                                                public void onLoadCleared(@Nullable Drawable placeholder) {}
                                            });
                                } else {
                                    resetToDefaultAvatar();
                                }
                            } catch (Exception e) {
                                resetToDefaultAvatar();
                            }
                        } else {
                            resetToDefaultAvatar();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data: " + e.getMessage());
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetToDefaultAvatar() {
        btnChangePhoto.setBackgroundResource(R.drawable.avatar_background);
        ivProfilePicture.setVisibility(View.VISIBLE);
        ivProfilePicture.setImageResource(R.drawable.ic_account_circle);
    }

    private boolean validateFields() {
        boolean isValid = true;

        if (userRole.equalsIgnoreCase("student")) {
            // Student validation - Format: A12AB1234 (1 letter, 2 digits, 2 letters, 4 digits)
            String studentId = etStudentId.getText().toString().trim().toUpperCase();
            if (studentId.isEmpty()) {
                tilField1.setError("Student ID is required");
                isValid = false;
            } else if (studentId.length() != 9) {
                tilField1.setError("Student ID must be exactly 9 characters");
                isValid = false;
            } else if (!Pattern.matches("^[A-Z]\\d{2}[A-Z]{2}\\d{4}$", studentId)) {
                tilField1.setError("Format: A12AB1234 (1 Letter + 2 Numbers + 2 Letters + 4 Numbers)");
                isValid = false;
            } else {
                tilField1.setError(null);
            }

            String programme = etProgramme.getText().toString().trim().toUpperCase();
            if (programme.isEmpty()) {
                tilField2.setError("Programme is required");
                isValid = false;
            } else if (!Pattern.matches("^[A-Z\\s\\.]+$", programme)) {
                tilField2.setError("Only letters and spaces allowed");
                isValid = false;
            } else {
                tilField2.setError(null);
            }

            String semester = etSemester.getText().toString().trim();
            if (semester.isEmpty()) {
                tilField3.setError("Semester is required");
                isValid = false;
            } else if (!Pattern.matches("^[0-9]+$", semester)) {
                tilField3.setError("Only numbers allowed");
                isValid = false;
            } else {
                int semesterNum = Integer.parseInt(semester);
                if (semesterNum < 1 || semesterNum > 8) {
                    tilField3.setError("Semester must be 1-8");
                    isValid = false;
                } else {
                    tilField3.setError(null);
                }
            }
        } else if (userRole.equalsIgnoreCase("staff")) {
            // Staff validation - Same format: 1 letter + 2 digits + 2 letters + 4 digits = 9 characters
            String staffId = etStudentId.getText().toString().trim().toUpperCase();
            if (staffId.isEmpty()) {
                tilField1.setError("Staff ID is required");
                isValid = false;
            } else if (staffId.length() != 9) {
                tilField1.setError("Staff ID must be exactly 9 characters");
                isValid = false;
            } else if (!Pattern.matches("^[A-Z]\\d{2}[A-Z]{2}\\d{4}$", staffId)) {
                tilField1.setError("Format: A12AB1234 (1 Letter + 2 Numbers + 2 Letters + 4 Numbers)");
                isValid = false;
            } else {
                tilField1.setError(null);
            }

            String department = etProgramme.getText().toString().trim().toUpperCase();
            if (department.isEmpty()) {
                tilField2.setError("Department is required");
                isValid = false;
            } else if (!Pattern.matches("^[A-Z\\s\\.]+$", department)) {
                tilField2.setError("Only letters and spaces allowed");
                isValid = false;
            } else {
                tilField2.setError(null);
            }

            String year = etSemester.getText().toString().trim();
            if (year.isEmpty()) {
                tilField3.setError("Year is required");
                isValid = false;
            } else if (!Pattern.matches("^[0-9]+$", year)) {
                tilField3.setError("Only numbers allowed");
                isValid = false;
            } else {
                int yearNum = Integer.parseInt(year);
                if (yearNum < 1 || yearNum > 50) {
                    tilField3.setError("Year must be 1-50");
                    isValid = false;
                } else {
                    tilField3.setError(null);
                }
            }
        } else if (userRole.equalsIgnoreCase("technician")) {
            // Technician validation
            String specialization = etSpecialization.getText().toString().trim();
            if (specialization.isEmpty()) {
                etSpecialization.setError("Specialization is required");
                isValid = false;
            }

            String workshop = etWorkshop.getText().toString().trim();
            if (workshop.isEmpty()) {
                etWorkshop.setError("Workshop location is required");
                isValid = false;
            }
        }

        return isValid;
    }

    private void saveUserData() {
        if (!validateFields()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setAlpha(0.5f);

        Map<String, Object> userMap = new HashMap<>();

        if (userRole.equalsIgnoreCase("student")) {
            userMap.put("studentId", etStudentId.getText().toString().trim().toUpperCase());
            userMap.put("programme", etProgramme.getText().toString().trim().toUpperCase());
            userMap.put("semester", etSemester.getText().toString().trim());
        } else if (userRole.equalsIgnoreCase("staff")) {
            userMap.put("staffId", etStudentId.getText().toString().trim().toUpperCase());
            userMap.put("department", etProgramme.getText().toString().trim().toUpperCase());
            userMap.put("year", etSemester.getText().toString().trim());
        } else if (userRole.equalsIgnoreCase("technician")) {
            userMap.put("specialization", etSpecialization.getText().toString().trim());
            userMap.put("workshop", etWorkshop.getText().toString().trim());
        }

        if (currentProfileImageBase64 != null && !currentProfileImageBase64.isEmpty()) {
            userMap.put("profileImageBase64", currentProfileImageBase64);
            SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putString(KEY_PROFILE_IMAGE, currentProfileImageBase64).apply();
        }

        db.collection("Users").document(currentUserId)
                .update(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setAlpha(1.0f);
                });
    }
}