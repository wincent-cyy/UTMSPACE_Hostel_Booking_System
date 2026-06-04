package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StaffActionActivity extends AppCompatActivity {

    private static final String TAG = "StaffActionActivity";

    // UI Elements - 匹配 XML ID
    private LinearLayout ivBack;
    private TextView tvApplicationStatus;
    private TextView tvStudentName;
    private TextView tvStudentId;
    private TextView tvPhoneNumber;
    private TextView tvEmail;
    private TextView tvProgramme;
    private TextView tvRoomType;
    private TextView tvRoomNumber;
    private TextView tvDuration;
    private TextView tvTotalAmount;
    private LinearLayout rejectionSection;
    private EditText etRejectionReason;
    private LinearLayout btnReject;
    private LinearLayout btnApprove;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private String bookingId;
    private String userId;
    private String currentStatus;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_action);

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        getIntentData();
        loadBookingDetails();
        setupClickListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvApplicationStatus = findViewById(R.id.tvApplicationStatus);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvEmail = findViewById(R.id.tvEmail);
        tvProgramme = findViewById(R.id.tvProgramme);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvDuration = findViewById(R.id.tvDuration);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        rejectionSection = findViewById(R.id.rejectionSection);
        etRejectionReason = findViewById(R.id.etRejectionReason);
        btnReject = findViewById(R.id.btnReject);
        btnApprove = findViewById(R.id.btnApprove);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            bookingId = intent.getStringExtra("BOOKING_DOC_ID");
            userId = intent.getStringExtra("userId");
            currentStatus = intent.getStringExtra("BOOKING_STATUS");
            roomId = intent.getStringExtra("ROOM_ID");

            // 如果以上为 null，尝试其他可能的 key
            if (bookingId == null) bookingId = intent.getStringExtra("documentId");
            if (userId == null) userId = intent.getStringExtra("user_id");
            if (currentStatus == null) currentStatus = intent.getStringExtra("STATUS");
        }
    }

    private void loadBookingDetails() {
        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Booking ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("Bookings").document(bookingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayBookingData(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayBookingData(DocumentSnapshot document) {
        // 从 Booking 集合获取数据
        String bookingStatus = document.getString("bookingStatus");
        if (bookingStatus == null) bookingStatus = currentStatus != null ? currentStatus : "Pending";

        String roomIdFromDoc = document.getString("roomId");
        if (roomIdFromDoc != null) roomId = roomIdFromDoc;

        String roomTypeFromDoc = document.getString("roomType");
        String checkInDate = document.getString("checkInDate");
        String leaseDuration = document.getString("leaseDuration");
        Double price = document.getDouble("price");

        // 从 Booking 获取学生信息（快照数据）
        String studentName = document.getString("name");
        String studentMatric = document.getString("matricNumber");
        String studentPhone = document.getString("phone");
        String studentEmail = document.getString("email");
        String studentProgramme = document.getString("programme");

        // 更新 UI
        updateStatusUI(bookingStatus);

        tvStudentName.setText(studentName != null ? studentName : "N/A");
        tvStudentId.setText(studentMatric != null ? studentMatric : "N/A");
        tvPhoneNumber.setText(studentPhone != null ? studentPhone : "N/A");
        tvEmail.setText(studentEmail != null ? studentEmail : "N/A");
        tvProgramme.setText(studentProgramme != null ? studentProgramme : "N/A");

        tvRoomType.setText(roomTypeFromDoc != null ? roomTypeFromDoc : "N/A");
        tvRoomNumber.setText(roomId != null ? roomId : "N/A");
        tvDuration.setText(leaseDuration != null ? leaseDuration : "1 Semester");

        // 计算总金额
        double totalAmount = price != null ? price : 0;
        tvTotalAmount.setText(String.format("RM %.2f", totalAmount));

        // 更新按钮可见性
        updateButtonVisibility(bookingStatus);
    }

    private void updateStatusUI(String status) {
        currentStatus = status;
        tvApplicationStatus.setText(status);

        // 根据状态设置背景和文字颜色
        LinearLayout statusBanner = findViewById(R.id.statusBanner);
        if (statusBanner != null) {
            if ("Pending".equalsIgnoreCase(status)) {
                statusBanner.setBackgroundColor(getColor(R.color.pending_bg));
                tvApplicationStatus.setTextColor(getColor(R.color.pending_text));
                tvApplicationStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            } else if ("Approved".equalsIgnoreCase(status)) {
                statusBanner.setBackgroundColor(getColor(R.color.approved_bg));
                tvApplicationStatus.setTextColor(getColor(R.color.approved_text));
                tvApplicationStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            } else if ("Rejected".equalsIgnoreCase(status)) {
                statusBanner.setBackgroundColor(getColor(R.color.rejected_bg));
                tvApplicationStatus.setTextColor(getColor(R.color.rejected_text));
                tvApplicationStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            } else if ("Paid".equalsIgnoreCase(status)) {
                statusBanner.setBackgroundColor(getColor(R.color.paid_bg));
                tvApplicationStatus.setTextColor(getColor(R.color.paid_text));
            }
        }
    }

    private void updateButtonVisibility(String status) {
        if ("Pending".equalsIgnoreCase(status)) {
            btnApprove.setVisibility(View.VISIBLE);
            btnReject.setVisibility(View.VISIBLE);
            rejectionSection.setVisibility(View.GONE);
        } else {
            btnApprove.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            rejectionSection.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // 返回按钮
        ivBack.setOnClickListener(v -> finish());

        // 批准按钮
        btnApprove.setOnClickListener(v -> showApproveConfirmation());

        // 拒绝按钮 - 显示拒绝原因输入框
        btnReject.setOnClickListener(v -> {
            rejectionSection.setVisibility(View.VISIBLE);
            btnApprove.setEnabled(false);
            btnReject.setEnabled(false);
        });

        // 注意：提交拒绝需要在 rejectionSection 中添加提交按钮
        // 如果 XML 中没有提交按钮，这里添加一个简单的确认方式
        setupRejectionSubmit();
    }

    private void setupRejectionSubmit() {
        // 添加一个提交拒绝的按钮或使用 EditText 的焦点变化
        // 这里使用一个简单的方法：当用户点击其他地方时提交，但最好添加一个提交按钮

        // 方案：在 rejectionSection 中添加一个提交按钮，或者使用悬浮按钮
        // 由于 XML 中没有提交按钮，我们添加一个 TextView 作为提交按钮
        TextView btnSubmitRejection = new TextView(this);
        btnSubmitRejection.setText("Submit Rejection");
        btnSubmitRejection.setBackgroundResource(R.drawable.primary_button_background);
        btnSubmitRejection.setPadding(32, 16, 32, 16);
        btnSubmitRejection.setTextColor(getColor(android.R.color.white));
        btnSubmitRejection.setTextSize(14);
        btnSubmitRejection.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 16;
        btnSubmitRejection.setLayoutParams(params);

        btnSubmitRejection.setOnClickListener(v -> {
            String reason = etRejectionReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Please enter a rejection reason", Toast.LENGTH_SHORT).show();
            } else {
                updateBookingStatus("Rejected", reason);
            }
        });

        rejectionSection.addView(btnSubmitRejection);

        // 添加取消按钮
        TextView btnCancelRejection = new TextView(this);
        btnCancelRejection.setText("Cancel");
        btnCancelRejection.setBackgroundResource(R.drawable.outline_button_background);
        btnCancelRejection.setPadding(32, 16, 32, 16);
        btnCancelRejection.setTextColor(getColor(R.color.primaryColor));
        btnCancelRejection.setTextSize(14);
        btnCancelRejection.setGravity(android.view.Gravity.CENTER);
        btnCancelRejection.setLayoutParams(params);

        btnCancelRejection.setOnClickListener(v -> {
            rejectionSection.setVisibility(View.GONE);
            etRejectionReason.setText("");
            btnApprove.setEnabled(true);
            btnReject.setEnabled(true);
        });

        rejectionSection.addView(btnCancelRejection);
    }

    private void showApproveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Approve Application")
                .setMessage("Are you sure you want to approve this booking application?")
                .setPositiveButton("Yes, Approve", (dialog, which) -> {
                    updateBookingStatus("Approved", null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateBookingStatus(String newStatus, String rejectReason) {
        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Booking ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("bookingStatus", newStatus);
        if (rejectReason != null) {
            updates.put("rejectReason", rejectReason);
        }

        db.collection("Bookings").document(bookingId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking " + newStatus + " successfully", Toast.LENGTH_LONG).show();

                    // 如果是批准，更新房间 occupancy
                    if ("Approved".equals(newStatus) && roomId != null) {
                        updateRoomOccupancy();
                    }

                    // 发送通知
                    sendNotification(newStatus, rejectReason);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnApprove.setEnabled(true);
                    btnReject.setEnabled(true);
                });
    }

    private void updateRoomOccupancy() {
        if (roomId == null) return;

        db.collection("Rooms").document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Integer currentOccupancy = documentSnapshot.getLong("currentOccupancy") != null
                                ? documentSnapshot.getLong("currentOccupancy").intValue() : 0;
                        int maxCapacity = documentSnapshot.getLong("maxCapacity") != null
                                ? documentSnapshot.getLong("maxCapacity").intValue() : 1;

                        int newOccupancy = currentOccupancy + 1;
                        if (newOccupancy <= maxCapacity) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("currentOccupancy", newOccupancy);
                            if (newOccupancy >= maxCapacity) {
                                updates.put("status", "Full");
                            }
                            db.collection("Rooms").document(roomId).update(updates);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update room occupancy", e));
    }

    private void sendNotification(String status, String rejectReason) {
        if (userId == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Booking " + status);

        String message;
        if ("Approved".equals(status)) {
            message = "Your room booking has been approved! Please proceed to payment.";
        } else {
            message = "Your room booking has been rejected. Reason: " + rejectReason;
        }
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("Notifications").add(notification)
                .addOnSuccessListener(docRef -> Log.d(TAG, "Notification sent"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send notification", e));
    }
}