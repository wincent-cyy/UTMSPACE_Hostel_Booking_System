package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_repair_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        getIntentData();
        setupClickListeners();
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
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            // Request ID
            String requestId = intent.getStringExtra("REPAIR_ID");
            if (requestId == null) {
                requestId = intent.getStringExtra("REQUEST_ID");
            }
            if (requestId == null) {
                requestId = intent.getStringExtra("documentId");
            }
            tvRequestId.setText("#" + (requestId != null ?
                    (requestId.length() > 8 ? requestId.substring(0, 8) : requestId) : "N/A"));

            // Status - Priority: STATUS > status
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
            // Reported By - Priority: STAFF_NAME > staffName > STUDENT_NAME > studentName > name
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

            // Preferred Time / Available Time
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

            // Completion Proof Photo (from staff/technician when completing repair)
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

            // Click to view full screen
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
    }
}