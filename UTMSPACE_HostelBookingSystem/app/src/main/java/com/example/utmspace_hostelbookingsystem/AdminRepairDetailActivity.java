package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminRepairDetailActivity extends AppCompatActivity {

    // Header
    private LinearLayout ivBack;

    // Status Banner
    private TextView tvStatusIcon;
    private TextView tvStatus;
    private TextView tvRequestId;

    // Room Information
    private TextView tvRoomNumber;
    private TextView tvRoomType;

    // Issue Details
    private TextView tvIssueType;
    private TextView tvPriority;
    private TextView tvDescription;

    // Additional Information
    private TextView tvReportedBy;
    private TextView tvReportedDate;
    private TextView tvPreferredTime;
    private TextView tvContactPerson;

    // Completion Proof
    private LinearLayout proofImageCard;
    private ImageView ivProofImage;

    // Danger Zone
    private LinearLayout dangerZone;
    private LinearLayout btnDeleteRequest;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String repairRequestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_repair_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupStatusBar();

        initViews();
        getIntentData();
        setupClickListeners();
        checkUserRoleAndShowDeleteButton();
    }

    /**
     * Setup status bar to be white with dark icons
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }
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

        // Completion Proof
        proofImageCard = findViewById(R.id.proofImageCard);
        ivProofImage = findViewById(R.id.ivProofImage);

        // Danger Zone
        dangerZone = findViewById(R.id.dangerZone);
        btnDeleteRequest = findViewById(R.id.btnDeleteRequest);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            // Request ID - 保存用于删除
            repairRequestId = intent.getStringExtra("REPAIR_ID");
            if (repairRequestId == null || repairRequestId.isEmpty()) {
                repairRequestId = intent.getStringExtra("REQUEST_ID");
            }
            if (repairRequestId == null || repairRequestId.isEmpty()) {
                repairRequestId = intent.getStringExtra("documentId");
            }

            tvRequestId.setText("#" + (repairRequestId != null ?
                    (repairRequestId.length() > 8 ? repairRequestId.substring(0, 8) : repairRequestId) : "N/A"));

            // Status
            String status = intent.getStringExtra("STATUS");
            if (status == null || status.isEmpty()) {
                status = intent.getStringExtra("status");
            }
            displayStatus(status);

            // Room Information
            String roomId = intent.getStringExtra("ROOM_ID");
            if (roomId == null || roomId.isEmpty()) {
                roomId = intent.getStringExtra("roomId");
            }
            tvRoomNumber.setText(roomId != null && !roomId.isEmpty() ? roomId : "N/A");

            String roomType = intent.getStringExtra("ROOM_TYPE");
            if (roomType == null || roomType.isEmpty()) {
                roomType = intent.getStringExtra("roomType");
            }
            tvRoomType.setText(roomType != null && !roomType.isEmpty() ? roomType : "N/A");

            // Issue Details
            String issueType = intent.getStringExtra("ISSUE_TYPE");
            if (issueType == null || issueType.isEmpty()) {
                issueType = intent.getStringExtra("issueType");
            }
            if (issueType == null || issueType.isEmpty()) {
                issueType = intent.getStringExtra("ITEM_NAME");
            }
            if (issueType == null || issueType.isEmpty()) {
                issueType = intent.getStringExtra("itemName");
            }
            tvIssueType.setText(issueType != null && !issueType.isEmpty() ? issueType : "N/A");

            String priority = intent.getStringExtra("PRIORITY");
            if (priority == null || priority.isEmpty()) {
                priority = intent.getStringExtra("priority");
            }
            if (priority == null || priority.isEmpty()) {
                priority = intent.getStringExtra("URGENCY");
            }
            if (priority == null || priority.isEmpty()) {
                priority = intent.getStringExtra("urgency");
            }
            tvPriority.setText(priority != null && !priority.isEmpty() ? priority : "Medium");
            setPriorityColor(priority);

            String description = intent.getStringExtra("DESCRIPTION");
            if (description == null || description.isEmpty()) {
                description = intent.getStringExtra("description");
            }
            tvDescription.setText(description != null && !description.isEmpty() ? description : "No description provided");

            // Additional Information
            String reportedBy = intent.getStringExtra("STAFF_NAME");
            if (reportedBy == null || reportedBy.isEmpty()) {
                reportedBy = intent.getStringExtra("staffName");
            }
            if (reportedBy == null || reportedBy.isEmpty()) {
                reportedBy = intent.getStringExtra("STUDENT_NAME");
            }
            if (reportedBy == null || reportedBy.isEmpty()) {
                reportedBy = intent.getStringExtra("studentName");
            }
            if (reportedBy == null || reportedBy.isEmpty()) {
                reportedBy = intent.getStringExtra("name");
            }
            tvReportedBy.setText(reportedBy != null && !reportedBy.isEmpty() ? reportedBy : "N/A");

            // Reported Date
            long createdAt = intent.getLongExtra("CREATED_AT", 0);
            if (createdAt == 0) {
                createdAt = intent.getLongExtra("createdAt", 0);
            }
            if (createdAt > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
                tvReportedDate.setText(sdf.format(new Date(createdAt)));
            } else {
                tvReportedDate.setText("N/A");
            }

            // Preferred Time
            String preferredTime = intent.getStringExtra("PREFERRED_TIME");
            if (preferredTime == null || preferredTime.isEmpty()) {
                preferredTime = intent.getStringExtra("availableTime");
            }
            if (preferredTime == null || preferredTime.isEmpty()) {
                preferredTime = intent.getStringExtra("AVAILABLE_TIME");
            }
            tvPreferredTime.setText(preferredTime != null && !preferredTime.isEmpty() ? preferredTime : "N/A");

            // Contact Person
            String contactPerson = intent.getStringExtra("CONTACT_PERSON");
            if (contactPerson == null || contactPerson.isEmpty()) {
                contactPerson = intent.getStringExtra("contactPerson");
            }
            if (contactPerson == null || contactPerson.isEmpty()) {
                contactPerson = intent.getStringExtra("STAFF_NAME");
            }
            tvContactPerson.setText(contactPerson != null && !contactPerson.isEmpty() ? contactPerson : "N/A");

            // Completion Proof Photo
            String completionPhoto = intent.getStringExtra("COMPLETION_PHOTO");
            if (completionPhoto == null || completionPhoto.isEmpty()) {
                completionPhoto = intent.getStringExtra("completionPhoto");
            }
            if (completionPhoto != null && !completionPhoto.isEmpty()) {
                displayCompletionProofImage(completionPhoto);
            } else {
                proofImageCard.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 检查当前用户角色，决定是否显示删除按钮
     */
    private void checkUserRoleAndShowDeleteButton() {
        if (mAuth.getCurrentUser() == null) {
            if (dangerZone != null) {
                dangerZone.setVisibility(View.GONE);
            }
            return;
        }

        String currentUid = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(currentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        // 只有 Admin 才能看到删除按钮
                        if ("Admin".equalsIgnoreCase(role) && dangerZone != null) {
                            dangerZone.setVisibility(View.VISIBLE);
                        } else {
                            dangerZone.setVisibility(View.GONE);
                        }
                    } else {
                        dangerZone.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    dangerZone.setVisibility(View.GONE);
                });
    }

    private void displayStatus(String status) {
        if (status == null || status.isEmpty()) {
            status = "Pending";
        }
        tvStatus.setText(status);

        LinearLayout statusBanner = findViewById(R.id.statusBanner);

        switch (status.toLowerCase()) {
            case "pending":
                tvStatusIcon.setText("⏳");
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(getColor(android.R.color.holo_orange_light));
                }
                break;
            case "in progress":
            case "in-progress":
                tvStatusIcon.setText("🔄");
                tvStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(getColor(android.R.color.holo_blue_light));
                }
                break;
            case "completed":
                tvStatusIcon.setText("✅");
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(getColor(android.R.color.holo_green_light));
                }
                break;
            default:
                tvStatusIcon.setText("🔧");
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                break;
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
            default:
                tvPriority.setTextColor(getColor(android.R.color.darker_gray));
                break;
        }
    }

    private void displayCompletionProofImage(String base64Image) {
        try {
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            ivProofImage.setImageBitmap(bitmap);
            proofImageCard.setVisibility(View.VISIBLE);

            ivProofImage.setOnClickListener(v -> showFullScreenImage(bitmap));
        } catch (Exception e) {
            proofImageCard.setVisibility(View.GONE);
        }
    }

    private void showFullScreenImage(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_full_image, null);
        ImageView fullImageView = dialogView.findViewById(R.id.fullImageView);
        fullImageView.setImageBitmap(bitmap);

        builder.setView(dialogView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        fullImageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        // 只保留 Danger Zone 的删除按钮
        if (btnDeleteRequest != null) {
            btnDeleteRequest.setOnClickListener(v -> showDeleteConfirmation());
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Repair Request")
                .setMessage("Are you sure you want to delete this repair request? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteRepairRequest())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 删除维修请求
     */
    private void deleteRepairRequest() {
        if (repairRequestId == null || repairRequestId.isEmpty()) {
            Toast.makeText(this, "Repair request ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 禁用删除按钮，防止重复点击
        if (btnDeleteRequest != null) {
            btnDeleteRequest.setEnabled(false);
            btnDeleteRequest.setAlpha(0.5f);
        }

        // 直接删除文档
        db.collection("RepairRequests").document(repairRequestId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Repair request deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (btnDeleteRequest != null) {
                        btnDeleteRequest.setEnabled(true);
                        btnDeleteRequest.setAlpha(1f);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupStatusBar();
    }
}