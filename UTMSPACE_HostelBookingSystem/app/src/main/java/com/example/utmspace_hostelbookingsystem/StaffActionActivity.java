package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StaffActionActivity extends AppCompatActivity {

    // UI Elements
    private ImageButton btnBack;
    private TextView tvStudentName, tvMatricNumber, tvPhoneNumber, tvStatusBadge, tvRoomId, tvRoomType, tvCheckInDate, tvLeaseDuration, tvRoomPrice;
    private MaterialButton btnContactStudent, btnRejectBooking, btnApproveBooking;

    // Data Variables
    private String bookingId;
    private String studentId;
    private String studentPhone;

    // Firebase Component
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_action);

        db = FirebaseFirestore.getInstance();

        initViews();
        getIncomingIntentData();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        tvStudentName = findViewById(R.id.tvDetailStudentName);
        tvMatricNumber = findViewById(R.id.tvDetailMatricNumber);
        tvPhoneNumber = findViewById(R.id.tvDetailPhoneNumber);
        btnContactStudent = findViewById(R.id.btnContactStudent);
        tvRoomId = findViewById(R.id.tvDetailRoomId);
        tvRoomType = findViewById(R.id.tvDetailRoomType);
        tvCheckInDate = findViewById(R.id.tvDetailCheckInDate);
        tvLeaseDuration = findViewById(R.id.tvDetailLeaseDuration);
        tvRoomPrice = findViewById(R.id.tvDetailRoomPrice);
        btnRejectBooking = findViewById(R.id.btnRejectBooking);
        btnApproveBooking = findViewById(R.id.btnApproveBooking);
    }

    private void getIncomingIntentData() {
        if (getIntent().hasExtra("BOOKING_DOC_ID")) {
            bookingId = getIntent().getStringExtra("BOOKING_DOC_ID");
        } else if (getIntent().hasExtra("BOOKING_ID")) {
            bookingId = getIntent().getStringExtra("BOOKING_ID");
        } else if (getIntent().hasExtra("documentId")) {
            bookingId = getIntent().getStringExtra("documentId");
        }

        if (bookingId != null) {
            studentId = getIntent().getStringExtra("userId");

            tvStudentName.setText(getIntent().getStringExtra("STUDENT_NAME"));
            tvMatricNumber.setText(getIntent().getStringExtra("MATRIC_NUMBER"));

            studentPhone = getIntent().getStringExtra("PHONE_NUMBER");
            tvPhoneNumber.setText(studentPhone);

            String status = getIntent().getStringExtra("BOOKING_STATUS");
            if (status == null) status = getIntent().getStringExtra("STATUS");
            if (status == null) status = "Pending";

            tvStatusBadge.setText(status);

            tvRoomId.setText(getIntent().getStringExtra("ROOM_ID"));
            tvRoomType.setText(getIntent().getStringExtra("ROOM_TYPE"));
            tvCheckInDate.setText(getIntent().getStringExtra("CHECK_IN_DATE"));
            tvLeaseDuration.setText(getIntent().getStringExtra("LEASE_DURATION"));
            tvRoomPrice.setText(getIntent().getStringExtra("ROOM_PRICE"));

            // Dynamic badge colors and button visibilities based on status context
            if ("Pending".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEF3C7"));
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#B45309"));
                btnApproveBooking.setVisibility(View.VISIBLE);
                btnRejectBooking.setVisibility(View.VISIBLE);
            } else if ("Approved".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#D1FAE5"));
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#065F46"));
                btnApproveBooking.setVisibility(View.GONE);
                btnRejectBooking.setVisibility(View.GONE);
            } else if ("Rejected".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"));
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#991B1B"));
                btnApproveBooking.setVisibility(View.GONE);
                btnRejectBooking.setVisibility(View.GONE);
            } else if ("Paid".equalsIgnoreCase(status)) {  // ✅ 添加 Paid 状态
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#DBEAFE")); // 蓝色
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#1E40AF"));
                btnApproveBooking.setVisibility(View.GONE);
                btnRejectBooking.setVisibility(View.GONE);
            }
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(StaffActionActivity.this, BookingManagementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnContactStudent.setOnClickListener(v -> {
            if (studentPhone != null && !studentPhone.trim().isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + studentPhone.trim()));
                startActivity(dialIntent);
            } else {
                Toast.makeText(this, "Phone number is invalid or unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        btnApproveBooking.setOnClickListener(v -> updateBookingStatusOnly("Approved", null));
        btnRejectBooking.setOnClickListener(v -> showRejectionDialog());
    }

    private void showRejectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reject Application");
        builder.setMessage("Please provide a reason for rejecting this room booking:");

        final EditText input = new EditText(this);
        input.setHint("e.g., Selected room type is fully booked");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Submit Reject", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(StaffActionActivity.this, "Rejection reason cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                updateBookingStatusOnly("Rejected", reason);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ✅ 简化：只更新预订状态，不修改房间 occupancy
    private void updateBookingStatusOnly(String newStatus, String rejectReason) {
        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Cannot update status: Target tracking ID is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("bookingStatus", newStatus);
        updates.put("rejectReason", rejectReason != null ? rejectReason : "");

        db.collection("Bookings").document(bookingId.trim())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(StaffActionActivity.this, "Booking successfully marked as " + newStatus, Toast.LENGTH_SHORT).show();

                    // ✅ 发送通知给学生
                    String studentName = tvStudentName.getText().toString();
                    String title = "Booking " + newStatus;
                    String message;
                    if ("Approved".equals(newStatus)) {
                        message = "Dear " + studentName + ", your room booking has been approved! Please proceed to payment.";
                    } else {
                        message = "Dear " + studentName + ", your room booking has been rejected. Reason: " + rejectReason;
                    }
                    sendNotificationToUser(studentId, title, message);

                    btnBack.performClick();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StaffActionActivity.this, "Firestore Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // 发送通知给指定用户
    private void sendNotificationToUser(String userId, String title, String message) {
        // 检查用户是否开启了通知
        SharedPreferences prefs = getSharedPreferences("BioAuthPrefs", MODE_PRIVATE);
        boolean isNotificationEnabled = prefs.getBoolean("NotificationEnabled_" + userId, true);

        if (!isNotificationEnabled) {
            return; // 用户关闭了通知，不发送
        }

        // 发送通知到 Firestore
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("Notifications").add(notification)
                .addOnSuccessListener(doc -> {
                    Log.d("NOTI", "SUCCESS: " + doc.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("NOTI", "FAILED", e);
                });
    }

    private void sendApprovalNotification(String userId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Application Approved");
        notification.put("message", "Your hostel application has been approved.");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("Notifications").add(notification);
    }

    private void sendRejectionNotification(String userId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Application Rejected");
        notification.put("message", "Your hostel application has been rejected.");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("Notifications").add(notification);
    }
}