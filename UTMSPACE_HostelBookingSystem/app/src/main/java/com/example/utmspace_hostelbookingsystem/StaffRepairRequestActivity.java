package com.example.utmspace_hostelbookingsystem;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StaffRepairRequestActivity extends AppCompatActivity {

    // UI Elements
    private LinearLayout ivBack;
    private TextInputEditText etRoomNumber;
    private TextInputEditText etRoomType;
    private TextInputEditText etIssueType;
    private TextInputEditText etPriority;
    private TextInputEditText etDescription;
    private TextInputEditText etContactPerson;
    private TextInputEditText etAvailableTime;
    private LinearLayout btnCancel;
    private LinearLayout btnSubmit;

    // Photo Elements
    private LinearLayout btnTakePhoto;
    private LinearLayout photoPreviewContainer;
    private ImageView ivPhotoPreview;
    private LinearLayout btnRemovePhoto;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Photo variables
    private String currentPhotoBase64 = "";
    private String currentPhotoPath = "";
    private Uri photoUri;

    // Options
    private final String[] issueTypeOptions = {"Plumbing", "Electrical", "Furniture", "Air Conditioning", "Other"};
    private final String[] priorityOptions = {"Low", "Medium", "High", "Emergency"};

    // Camera permission request code
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_repair_request);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        setupDropdowns();
        setupClickListeners();
        loadRoomInfo();
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
        etRoomNumber = findViewById(R.id.etRoomNumber);
        etRoomType = findViewById(R.id.etRoomType);
        etIssueType = findViewById(R.id.etIssueType);
        etPriority = findViewById(R.id.etPriority);
        etDescription = findViewById(R.id.etDescription);
        etContactPerson = findViewById(R.id.etContactPerson);
        etAvailableTime = findViewById(R.id.etAvailableTime);
        btnCancel = findViewById(R.id.btnCancel);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Photo views
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        photoPreviewContainer = findViewById(R.id.photoPreviewContainer);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
    }

    private void setupDropdowns() {
        // Issue Type Dropdown
        etIssueType.setFocusable(false);
        etIssueType.setClickable(true);
        etIssueType.setOnClickListener(v -> showIssueTypePicker());

        // Priority Dropdown
        etPriority.setFocusable(false);
        etPriority.setClickable(true);
        etPriority.setOnClickListener(v -> showPriorityPicker());

        // Available Time Date Picker
        etAvailableTime.setFocusable(false);
        etAvailableTime.setClickable(true);
        etAvailableTime.setOnClickListener(v -> showDatePicker());
    }

    private void showIssueTypePicker() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Issue Type")
                .setItems(issueTypeOptions, (dialog, which) -> {
                    etIssueType.setText(issueTypeOptions[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPriorityPicker() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Priority")
                .setItems(priorityOptions, (dialog, which) -> {
                    etPriority.setText(priorityOptions[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    etAvailableTime.setText(date);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void loadRoomInfo() {
        Intent intent = getIntent();
        if (intent != null) {
            final String roomId = intent.getStringExtra("ROOM_ID");
            final String roomDocId = intent.getStringExtra("ROOM_DOC_ID");

            if (roomId != null) {
                etRoomNumber.setText(roomId);
            }

            if (roomDocId != null && !roomDocId.isEmpty()) {
                db.collection("Rooms").document(roomDocId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String roomType = documentSnapshot.getString("roomType");
                                if (roomType != null) {
                                    etRoomType.setText(roomType);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {});
            }
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitRepairRequest());

        // Photo button click listener
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());

        // Remove photo button click listener
        btnRemovePhoto.setOnClickListener(v -> removePhoto());
    }

    // ========== Camera Functions ==========

    private void checkCameraPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                if (photoUri != null) {
                    // 使用与 EditInfoActivity 相同的方式加载图片
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                    if (bitmap != null) {
                        // 保持原始比例，不要强制正方形
                        displayPhotoAndConvertToBase64(bitmap);
                    }
                } else if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    if (bitmap != null) {
                        displayPhotoAndConvertToBase64(bitmap);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayPhotoAndConvertToBase64(Bitmap bitmap) {
        // 保持原始宽高比，最大尺寸 800px
        int maxSize = 800;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float aspectRatio = (float) width / (float) height;
        int targetWidth, targetHeight;

        if (width > height) {
            targetWidth = maxSize;
            targetHeight = (int) (maxSize / aspectRatio);
        } else {
            targetHeight = maxSize;
            targetWidth = (int) (maxSize * aspectRatio);
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

        // 显示预览
        ivPhotoPreview.setImageBitmap(scaledBitmap);
        photoPreviewContainer.setVisibility(View.VISIBLE);
        btnTakePhoto.setVisibility(View.GONE);

        // 使用与 EditInfoActivity 相同的压缩质量 (40)
        currentPhotoBase64 = bitmapToBase64(scaledBitmap);

        Log.d("StaffRepairRequest", "Photo size: " + targetWidth + "x" + targetHeight);
        Log.d("StaffRepairRequest", "Base64 length: " + currentPhotoBase64.length());
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 使用与 EditInfoActivity 相同的质量 (40)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private void removePhoto() {
        currentPhotoBase64 = "";
        currentPhotoPath = "";
        photoUri = null;
        photoPreviewContainer.setVisibility(View.GONE);
        btnTakePhoto.setVisibility(View.VISIBLE);
        ivPhotoPreview.setImageBitmap(null);
    }

    // ========== Validation ==========

    private boolean isDateValid(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return false;
        }
        try {
            String[] parts = dateStr.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, day, 0, 0, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);
            Calendar todayCalendar = Calendar.getInstance();
            todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
            todayCalendar.set(Calendar.MINUTE, 0);
            todayCalendar.set(Calendar.SECOND, 0);
            todayCalendar.set(Calendar.MILLISECOND, 0);
            return !selectedCalendar.before(todayCalendar);
        } catch (Exception e) {
            return false;
        }
    }

    // ========== Submit ==========

    private void submitRepairRequest() {
        final String roomId = etRoomNumber.getText().toString().trim();
        final String roomType = etRoomType.getText().toString().trim();
        final String issueType = etIssueType.getText().toString().trim();
        final String priority = etPriority.getText().toString().trim();
        final String description = etDescription.getText().toString().trim();
        final String contactPerson = etContactPerson.getText().toString().trim();
        final String availableTime = etAvailableTime.getText().toString().trim();

        if (roomId.isEmpty()) {
            etRoomNumber.setError("Room number is required");
            return;
        }
        if (issueType.isEmpty()) {
            Toast.makeText(this, "Please select issue type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (priority.isEmpty()) {
            Toast.makeText(this, "Please select priority level", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            etDescription.setError("Please describe the issue");
            return;
        }
        if (availableTime.isEmpty()) {
            Toast.makeText(this, "Please select preferred date/time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isDateValid(availableTime)) {
            Toast.makeText(this, "Please select a valid date (today or future date only)", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.5f);

        final String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        final String[] staffName = {"Staff"};

        if (!userId.isEmpty()) {
            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                staffName[0] = name;
                            }
                        }
                        saveRepairRequest(roomId, roomType, issueType, priority,
                                description, contactPerson, availableTime,
                                staffName[0], userId);
                    })
                    .addOnFailureListener(e -> {
                        saveRepairRequest(roomId, roomType, issueType, priority,
                                description, contactPerson, availableTime,
                                staffName[0], userId);
                    });
        } else {
            saveRepairRequest(roomId, roomType, issueType, priority,
                    description, contactPerson, availableTime,
                    staffName[0], userId);
        }
    }

    private void saveRepairRequest(String roomId, String roomType, String issueType,
                                   String priority, String description, String contactPerson,
                                   String availableTime, String name, String userId) {

        Map<String, Object> repairRequest = new HashMap<>();
        repairRequest.put("roomId", roomId);
        repairRequest.put("roomType", roomType);
        repairRequest.put("issueType", issueType);
        repairRequest.put("priority", priority);
        repairRequest.put("description", description);
        repairRequest.put("contactPerson", contactPerson.isEmpty() ? "" : contactPerson);
        repairRequest.put("availableTime", availableTime.isEmpty() ? "" : availableTime);
        repairRequest.put("status", "Pending");
        repairRequest.put("uid", userId);
        repairRequest.put("name", name);
        repairRequest.put("createdAt", System.currentTimeMillis());
        repairRequest.put("updatedAt", System.currentTimeMillis());

        // 添加照片 Base64（如果存在）
        if (currentPhotoBase64 != null && !currentPhotoBase64.isEmpty()) {
            repairRequest.put("proofImage", currentPhotoBase64);
        }

        db.collection("RepairRequests")
                .add(repairRequest)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(StaffRepairRequestActivity.this,
                            "Repair request submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setAlpha(1.0f);
                    Toast.makeText(StaffRepairRequestActivity.this,
                            "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
    }
}