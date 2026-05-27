package com.example.utmspace_hostelbookingsystem;

import android.Manifest;
import android.content.ContentValues;
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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    private ImageButton btnBack;
    private TextView tvStatusBadge, tvRoomNumber, tvItemName, tvDescription, tvUrgency, tvReportedBy, tvProofTitle;
    private Button btnTakeTask, btnMarkCompleted, btnBackToList, btnUploadPhoto;
    private ImageView ivProofImage;

    private FirebaseFirestore db;
    private String requestId;
    private String currentStatus;
    private String proofImageBase64 = "";
    private boolean hasPhoto = false;
    private Uri photoUri;
    private String currentPhotoPath;

    // Permission request codes
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    // Camera launcher
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

    // Gallery launcher
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadAndDisplayImage(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_repair_detail);

        db = FirebaseFirestore.getInstance();

        initViews();
        displayData();
        setupButtons();
        setupClickListeners();
        loadSavedImage();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvItemName = findViewById(R.id.tvItemName);
        tvDescription = findViewById(R.id.tvDescription);
        tvUrgency = findViewById(R.id.tvUrgency);
        tvReportedBy = findViewById(R.id.tvReportedBy);
        tvProofTitle = findViewById(R.id.tvProofTitle);
        btnTakeTask = findViewById(R.id.btnTakeTask);
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted);
        btnBackToList = findViewById(R.id.btnBackToList);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        ivProofImage = findViewById(R.id.ivProofImage);
    }

    private void displayData() {
        Intent intent = getIntent();
        requestId = intent.getStringExtra("REQUEST_ID");
        currentStatus = intent.getStringExtra("STATUS");

        tvRoomNumber.setText("Room " + intent.getStringExtra("ROOM_ID"));
        tvItemName.setText(intent.getStringExtra("ITEM_NAME"));
        tvDescription.setText(intent.getStringExtra("DESCRIPTION"));
        tvUrgency.setText(intent.getStringExtra("URGENCY"));
        tvReportedBy.setText(intent.getStringExtra("STAFF_NAME"));

        if (currentStatus != null) {
            switch (currentStatus) {
                case "Pending":
                    tvStatusBadge.setBackgroundResource(R.drawable.status_badge_pending);
                    tvStatusBadge.setText("Pending");
                    break;
                case "In Progress":
                    tvStatusBadge.setBackgroundResource(R.drawable.status_badge_scheduled);
                    tvStatusBadge.setText("In Progress");
                    break;
                case "Completed":
                    tvStatusBadge.setBackgroundResource(R.drawable.status_badge_completed);
                    tvStatusBadge.setText("Completed");
                    break;
                default:
                    tvStatusBadge.setBackgroundResource(R.drawable.status_badge_pending);
                    tvStatusBadge.setText("Pending");
                    break;
            }
        }
    }

    private void setupButtons() {
        if (currentStatus != null) {
            switch (currentStatus) {
                case "Pending":
                    btnTakeTask.setVisibility(View.VISIBLE);
                    btnMarkCompleted.setVisibility(View.GONE);
                    btnBackToList.setVisibility(View.GONE);
                    tvProofTitle.setVisibility(View.GONE);
                    btnUploadPhoto.setVisibility(View.GONE);
                    ivProofImage.setVisibility(View.GONE);
                    break;
                case "In Progress":
                    btnTakeTask.setVisibility(View.GONE);
                    btnMarkCompleted.setVisibility(View.VISIBLE);
                    btnBackToList.setVisibility(View.GONE);
                    tvProofTitle.setVisibility(View.VISIBLE);
                    btnUploadPhoto.setVisibility(View.VISIBLE);
                    break;
                case "Completed":
                    btnTakeTask.setVisibility(View.GONE);
                    btnMarkCompleted.setVisibility(View.GONE);
                    btnBackToList.setVisibility(View.VISIBLE);
                    tvProofTitle.setVisibility(View.VISIBLE);
                    btnUploadPhoto.setVisibility(View.GONE);
                    break;
                default:
                    btnTakeTask.setVisibility(View.VISIBLE);
                    btnMarkCompleted.setVisibility(View.GONE);
                    btnBackToList.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private void loadSavedImage() {
        if (requestId == null) return;

        db.collection("RepairRequests").document(requestId).get()
                .addOnSuccessListener(doc -> {
                    String savedImage = doc.getString("proofImage");
                    if (savedImage != null && !savedImage.isEmpty()) {
                        proofImageBase64 = savedImage;
                        hasPhoto = true;
                        byte[] decodedBytes = Base64.decode(savedImage, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        ivProofImage.setImageBitmap(bitmap);
                        ivProofImage.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        checkStoragePermission();
                    }
                })
                .show();
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

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need storage permission for media
            openGallery();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            } else {
                openGallery();
            }
        } else {
            openGallery();
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
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            // Create a file to store the image
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

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void uploadAndDisplayImage(Uri imageUri) {
        try {
            InputStream is = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            // Resize bitmap to avoid太大了
            int maxWidth = 1024;
            int maxHeight = 1024;
            if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * ratio);
                int newHeight = Math.round(bitmap.getHeight() * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            // Compress image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            proofImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            hasPhoto = true;

            // Display image
            ivProofImage.setImageBitmap(bitmap);
            ivProofImage.setVisibility(View.VISIBLE);

            // Save to Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("proofImage", proofImageBase64);

            db.collection("RepairRequests").document(requestId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            is.close();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnTakeTask.setOnClickListener(v -> updateStatus("In Progress"));

        btnMarkCompleted.setOnClickListener(v -> {
            if (hasPhoto) {
                updateStatus("Completed");
            } else {
                Toast.makeText(this, "Please upload proof photo before completing the task", Toast.LENGTH_LONG).show();
            }
        });

        btnBackToList.setOnClickListener(v -> {
            Intent intent = new Intent(TechnicianRepairDetailActivity.this, TechnicianRepairRequestActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnUploadPhoto.setOnClickListener(v -> showImagePickerDialog());
    }

    private void updateStatus(String newStatus) {
        if (requestId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("RepairRequests").document(requestId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    String message;
                    if (newStatus.equals("In Progress")) {
                        message = "Task started successfully!";
                    } else {
                        message = "Task marked as completed!";
                    }
                    Toast.makeText(TechnicianRepairDetailActivity.this, message, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(TechnicianRepairDetailActivity.this, TechnicianRepairRequestActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TechnicianRepairDetailActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}