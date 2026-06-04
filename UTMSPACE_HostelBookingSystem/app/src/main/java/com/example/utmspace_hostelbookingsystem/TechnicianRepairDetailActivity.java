package com.example.utmspace_hostelbookingsystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TechnicianRepairDetailActivity extends AppCompatActivity {

    // 返回按钮 - XML 中是 ivBack
    private LinearLayout ivBack;

    // 状态显示 - XML 中是 tvStatus
    private TextView tvStatus;
    private TextView tvStatusIcon;
    private TextView tvRequestId;

    // 房间信息
    private TextView tvRoomNumber;
    private TextView tvRoomType;

    // 问题详情
    private TextView tvIssueType;
    private TextView tvPriority;
    private TextView tvDescription;

    // 附加信息
    private TextView tvReportedBy;
    private TextView tvReportedDate;
    private TextView tvPreferredTime;
    private TextView tvContactPerson;

    // 照片相关
    private LinearLayout proofImageCard;
    private ImageView ivProofImage;
    private LinearLayout takePhotoSection;
    private LinearLayout btnTakePhoto;
    private LinearLayout photoPreviewContainer;
    private ImageView ivPhotoPreview;
    private LinearLayout btnRemovePhoto;

    // 按钮
    private LinearLayout btnStartRepairContainer;
    private LinearLayout btnStartRepair;
    private LinearLayout btnSubmitCompletionContainer;
    private LinearLayout btnSubmitCompletion;

    // Firebase
    private FirebaseFirestore db;
    private String requestId;
    private String currentStatus;
    private String proofImageBase64 = "";
    private String currentPhotoBase64 = "";
    private boolean hasNewPhoto = false;      // 专门检查是否新拍了照片
    private boolean hasExistingPhoto = false; // 已有照片
    private Uri photoUri;
    private String currentPhotoPath;

    // Permission
    private static final int CAMERA_PERMISSION_CODE = 100;

    // Launchers
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (photoUri != null) {
                        uploadAndDisplayImage(photoUri);
                    } else if (currentPhotoPath != null) {
                        Uri uri = Uri.fromFile(new File(currentPhotoPath));
                        uploadAndDisplayImage(uri);
                    }
                } else {
                    Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_repair_detail);

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        displayData();
        setupButtons();
        setupClickListeners();
        loadSavedImage();
    }

    private void initViews() {
        // Header
        ivBack = findViewById(R.id.ivBack);

        // Status Banner
        tvStatusIcon = findViewById(R.id.tvStatusIcon);
        tvStatus = findViewById(R.id.tvStatus);
        tvRequestId = findViewById(R.id.tvRequestId);

        // Room Information
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRoomType = findViewById(R.id.tvRoomType);

        // Issue Details
        tvIssueType = findViewById(R.id.tvIssueType);
        tvPriority = findViewById(R.id.tvPriority);
        tvDescription = findViewById(R.id.tvDescription);

        // Additional Information
        tvReportedBy = findViewById(R.id.tvReportedBy);
        tvReportedDate = findViewById(R.id.tvReportedDate);
        tvPreferredTime = findViewById(R.id.tvPreferredTime);
        tvContactPerson = findViewById(R.id.tvContactPerson);

        // Photo Cards
        proofImageCard = findViewById(R.id.proofImageCard);
        ivProofImage = findViewById(R.id.ivProofImage);
        takePhotoSection = findViewById(R.id.takePhotoSection);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        photoPreviewContainer = findViewById(R.id.photoPreviewContainer);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);

        // Action Buttons
        btnStartRepairContainer = findViewById(R.id.btnStartRepairContainer);
        btnStartRepair = findViewById(R.id.btnStartRepair);
        btnSubmitCompletionContainer = findViewById(R.id.btnSubmitCompletionContainer);
        btnSubmitCompletion = findViewById(R.id.btnSubmitCompletion);
    }

    private void displayData() {
        Intent intent = getIntent();
        requestId = intent.getStringExtra("REQUEST_ID");
        currentStatus = intent.getStringExtra("STATUS");

        if (currentStatus == null) {
            currentStatus = intent.getStringExtra("status");
        }

        // Set Request ID
        tvRequestId.setText("#" + (requestId != null ? requestId.substring(0, Math.min(8, requestId.length())) : "N/A"));

        // Room Information
        String roomId = intent.getStringExtra("ROOM_ID");
        if (roomId == null) roomId = intent.getStringExtra("roomId");
        tvRoomNumber.setText(roomId != null ? roomId : "N/A");

        String roomType = intent.getStringExtra("ROOM_TYPE");
        if (roomType == null) roomType = intent.getStringExtra("roomType");
        tvRoomType.setText(roomType != null ? roomType : "N/A");

        // Issue Details
        String issueType = intent.getStringExtra("ISSUE_TYPE");
        if (issueType == null) issueType = intent.getStringExtra("issueType");
        tvIssueType.setText(issueType != null ? issueType : "N/A");

        String priority = intent.getStringExtra("PRIORITY");
        if (priority == null) priority = intent.getStringExtra("priority");
        tvPriority.setText(priority != null ? priority : "N/A");
        setPriorityColor(priority);

        String description = intent.getStringExtra("DESCRIPTION");
        if (description == null) description = intent.getStringExtra("description");
        tvDescription.setText(description != null ? description : "No description");

        // Additional Information
        String reportedBy = intent.getStringExtra("STAFF_NAME");
        if (reportedBy == null) reportedBy = intent.getStringExtra("name");
        tvReportedBy.setText(reportedBy != null ? reportedBy : "Unknown");

        long createdAt = intent.getLongExtra("CREATED_AT", 0);
        if (createdAt == 0) createdAt = intent.getLongExtra("createdAt", 0);
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            tvReportedDate.setText(sdf.format(new Date(createdAt)));
        } else {
            tvReportedDate.setText("N/A");
        }

        String preferredTime = intent.getStringExtra("PREFERRED_TIME");
        if (preferredTime == null) preferredTime = intent.getStringExtra("availableTime");
        tvPreferredTime.setText(preferredTime != null ? preferredTime : "Not specified");

        String contactPerson = intent.getStringExtra("CONTACT_PERSON");
        if (contactPerson == null) contactPerson = intent.getStringExtra("contactPerson");
        tvContactPerson.setText(contactPerson != null && !contactPerson.isEmpty() ? contactPerson : "N/A");

        // Set status display
        setStatusDisplay(currentStatus);
    }

    private void setStatusDisplay(String status) {
        if (status == null) status = "Pending";
        tvStatus.setText(status);

        switch (status.toLowerCase()) {
            case "pending":
                tvStatusIcon.setText("⏳");
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                break;
            case "in progress":
            case "in-progress":
                tvStatusIcon.setText("🔄");
                tvStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                break;
            case "completed":
                tvStatusIcon.setText("✅");
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                break;
            default:
                tvStatusIcon.setText("⏳");
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        }
    }

    private void setPriorityColor(String priority) {
        if (priority == null) return;
        switch (priority.toLowerCase()) {
            case "high":
                tvPriority.setTextColor(getColor(android.R.color.holo_red_dark));
                break;
            case "medium":
                tvPriority.setTextColor(getColor(android.R.color.holo_orange_dark));
                break;
            case "low":
                tvPriority.setTextColor(getColor(android.R.color.holo_green_dark));
                break;
        }
    }

    private void setupButtons() {
        if (currentStatus == null) return;

        switch (currentStatus.toLowerCase()) {
            case "pending":
                btnStartRepairContainer.setVisibility(View.VISIBLE);
                btnSubmitCompletionContainer.setVisibility(View.GONE);
                takePhotoSection.setVisibility(View.GONE);
                proofImageCard.setVisibility(View.GONE);
                break;

            case "in progress":
            case "in-progress":
                btnStartRepairContainer.setVisibility(View.GONE);
                btnSubmitCompletionContainer.setVisibility(View.VISIBLE);
                takePhotoSection.setVisibility(View.VISIBLE);
                proofImageCard.setVisibility(View.GONE);
                break;

            case "completed":
                btnStartRepairContainer.setVisibility(View.GONE);
                btnSubmitCompletionContainer.setVisibility(View.GONE);
                takePhotoSection.setVisibility(View.GONE);
                proofImageCard.setVisibility(View.VISIBLE);
                break;

            default:
                btnStartRepairContainer.setVisibility(View.VISIBLE);
                btnSubmitCompletionContainer.setVisibility(View.GONE);
                takePhotoSection.setVisibility(View.GONE);
                proofImageCard.setVisibility(View.GONE);
                break;
        }
    }

    private void loadSavedImage() {
        if (requestId == null) return;

        db.collection("RepairRequests").document(requestId).get()
                .addOnSuccessListener(doc -> {
                    String savedImage = doc.getString("proofImage");
                    String completionPhoto = doc.getString("completionPhoto");
                    String imageToLoad = completionPhoto != null && !completionPhoto.isEmpty() ? completionPhoto : savedImage;

                    if (imageToLoad != null && !imageToLoad.isEmpty()) {
                        proofImageBase64 = imageToLoad;
                        hasExistingPhoto = true;
                        byte[] decodedBytes = Base64.decode(imageToLoad, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        ivProofImage.setImageBitmap(bitmap);
                        proofImageCard.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnStartRepair.setOnClickListener(v -> updateStatus("In Progress"));

        btnSubmitCompletion.setOnClickListener(v -> {
            // 必须要有新拍的照片才能提交
            if (hasNewPhoto) {
                updateStatusWithPhoto("Completed");
            } else {
                Toast.makeText(this, "Please take a new photo as proof of completion", Toast.LENGTH_LONG).show();
            }
        });

        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());

        if (btnRemovePhoto != null) {
            btnRemovePhoto.setOnClickListener(v -> removePhoto());
        }
    }

    private void removePhoto() {
        currentPhotoBase64 = "";
        hasNewPhoto = false;  // 重置新照片标志
        photoPreviewContainer.setVisibility(View.GONE);
        btnTakePhoto.setVisibility(View.VISIBLE);
        ivPhotoPreview.setImageBitmap(null);
    }

    private void checkCameraPermission() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);

                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    cameraLauncher.launch(cameraIntent);
                } else {
                    Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void uploadAndDisplayImage(Uri imageUri) {
        try {
            InputStream is = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            // 使用 600x600 像素，保证完成证明清晰可见
            int targetSize = 600;

            // 保持原始宽高比
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int targetWidth, targetHeight;

            if (width > height) {
                targetWidth = targetSize;
                targetHeight = (int) ((float) height / width * targetSize);
            } else {
                targetHeight = targetSize;
                targetWidth = (int) ((float) width / height * targetSize);
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

            ivPhotoPreview.setImageBitmap(scaledBitmap);
            photoPreviewContainer.setVisibility(View.VISIBLE);
            btnTakePhoto.setVisibility(View.GONE);

            // 使用 60% 质量，比头像更清晰
            currentPhotoBase64 = bitmapToMediumBase64(scaledBitmap);
            hasNewPhoto = true;

            Log.d("TechnicianRepairDetail", "Final size: " + targetWidth + "x" + targetHeight);
            Log.d("TechnicianRepairDetail", "Base64 length: " + currentPhotoBase64.length());

            is.close();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToMediumBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 使用 60% 质量
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private String bitmapToShortBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        Log.d("TechnicianRepairDetail", "Image bytes: " + imageBytes.length + " bytes");
        return base64;
    }

    private void updateStatus(String newStatus) {
        if (requestId == null) return;

        btnStartRepair.setEnabled(false);
        btnStartRepair.setAlpha(0.5f);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("RepairRequests").document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task started successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnStartRepair.setEnabled(true);
                    btnStartRepair.setAlpha(1f);
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStatusWithPhoto(String newStatus) {
        if (requestId == null) return;

        btnSubmitCompletion.setEnabled(false);
        btnSubmitCompletion.setAlpha(0.5f);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", System.currentTimeMillis());
        updates.put("completionPhoto", currentPhotoBase64);

        db.collection("RepairRequests").document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task completed successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitCompletion.setEnabled(true);
                    btnSubmitCompletion.setAlpha(1f);
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}