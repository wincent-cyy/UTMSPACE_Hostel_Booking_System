package com.example.utmspace_hostelbookingsystem;

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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StaffRepairDetailActivity extends AppCompatActivity {

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

    // Completion Proof
    private LinearLayout proofImageCard;
    private ImageView ivProofImage;
    private TextView tvProofTitle;

    // Firebase
    private FirebaseFirestore db;
    private String requestId;
    private String currentStatus;
    private ListenerRegistration listenerRegistration;  // 使用 ListenerRegistration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_repair_detail);

        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        displayData();
        setupClickListeners();
        setupRealTimeListener();
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

        // Completion Proof - 这些 ID 需要在 XML 中添加
        proofImageCard = findViewById(R.id.proofImageCard);
        ivProofImage = findViewById(R.id.ivProofImage);
        tvProofTitle = findViewById(R.id.tvProofTitle);

        // 如果 XML 中没有这些 ID，先注释掉避免崩溃
        // 你需要在 XML 中添加这些视图
    }

    private void displayData() {
        Intent intent = getIntent();
        requestId = intent.getStringExtra("REQUEST_ID");
        currentStatus = intent.getStringExtra("STATUS");

        if (currentStatus == null) {
            currentStatus = intent.getStringExtra("status");
        }

        // Set Request ID
        String displayId = requestId != null ? requestId.substring(0, Math.min(8, requestId.length())) : "N/A";
        tvRequestId.setText("#" + displayId);

        // ========== Room Information ==========
        String roomNumber = intent.getStringExtra("roomId");
        if (roomNumber == null) {
            roomNumber = intent.getStringExtra("ROOM_ID");
        }
        tvRoomNumber.setText(roomNumber != null ? roomNumber : "N/A");

        String roomType = intent.getStringExtra("roomType");
        if (roomType == null) {
            roomType = intent.getStringExtra("ROOM_TYPE");
        }
        tvRoomType.setText(roomType != null ? roomType : "N/A");

        // ========== Issue Details ==========
        String issueType = intent.getStringExtra("issueType");
        if (issueType == null) {
            issueType = intent.getStringExtra("ISSUE_TYPE");
        }
        tvIssueType.setText(issueType != null ? issueType : "N/A");

        String priority = intent.getStringExtra("priority");
        if (priority == null) {
            priority = intent.getStringExtra("PRIORITY");
        }
        tvPriority.setText(priority != null ? priority : "N/A");
        setPriorityColor(priority);

        String description = intent.getStringExtra("description");
        if (description == null) {
            description = intent.getStringExtra("DESCRIPTION");
        }
        tvDescription.setText(description != null ? description : "No description provided");

        // ========== Additional Information ==========
        String reportedBy = intent.getStringExtra("name");
        if (reportedBy == null) {
            reportedBy = intent.getStringExtra("NAME");
        }
        if (reportedBy == null) {
            reportedBy = intent.getStringExtra("staffName");
        }
        tvReportedBy.setText(reportedBy != null ? reportedBy : "Unknown");

        long createdAt = intent.getLongExtra("createdAt", 0);
        if (createdAt == 0) {
            createdAt = intent.getLongExtra("CREATED_AT", 0);
        }
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            tvReportedDate.setText(sdf.format(new Date(createdAt)));
        } else {
            tvReportedDate.setText("N/A");
        }

        String preferredTime = intent.getStringExtra("availableTime");
        if (preferredTime == null) {
            preferredTime = intent.getStringExtra("PREFERRED_TIME");
        }
        tvPreferredTime.setText(preferredTime != null ? preferredTime : "Not specified");

        // Set status with appropriate icon and color
        setStatusDisplay(currentStatus);
    }

    private void setStatusDisplay(String status) {
        if (status == null) {
            status = "Pending";
        }
        tvStatus.setText(status);

        switch (status.toLowerCase()) {
            case "pending":
                tvStatusIcon.setText("⏳");
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                break;
            case "in progress":
            case "in-progress":
            case "in_progress":
                tvStatusIcon.setText("🔄");
                tvStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                break;
            case "completed":
                tvStatusIcon.setText("✅");
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                showCompletionProofSection();
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

    private void showCompletionProofSection() {
        if (proofImageCard != null) {
            proofImageCard.setVisibility(View.VISIBLE);
        }
    }

    private void setupRealTimeListener() {
        if (requestId == null) return;

        DocumentReference requestRef = db.collection("RepairRequests").document(requestId);

        listenerRegistration = requestRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException error) {
                if (error != null || snapshot == null) {
                    return;
                }

                if (snapshot.exists()) {
                    // 获取最新状态
                    String newStatus = snapshot.getString("status");
                    if (newStatus != null && !newStatus.equals(currentStatus)) {
                        currentStatus = newStatus;
                        runOnUiThread(() -> setStatusDisplay(currentStatus));
                    }

                    // 获取完成证明照片
                    String completionPhoto = snapshot.getString("completionPhoto");
                    if (completionPhoto != null && !completionPhoto.isEmpty()) {
                        runOnUiThread(() -> displayCompletionPhoto(completionPhoto));
                    }
                }
            }
        });
    }

    private void displayCompletionPhoto(String base64Image) {
        if (ivProofImage == null) return;

        try {
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            ivProofImage.setImageBitmap(bitmap);

            if (proofImageCard != null) {
                proofImageCard.setVisibility(View.VISIBLE);
            }

            // 点击图片放大查看
            ivProofImage.setOnClickListener(v -> showFullScreenImage(bitmap));
        } catch (Exception e) {
            // 图片加载失败，静默处理
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
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除实时监听器
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
    }
}