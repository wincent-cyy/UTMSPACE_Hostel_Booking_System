package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

            // IMPROVED: Dynamic badge colors and button visibilities based on status context
            if ("Pending".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEF3C7")); // Amber
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#B45309"));

                btnApproveBooking.setVisibility(View.VISIBLE);
                btnRejectBooking.setVisibility(View.VISIBLE);
            } else if ("Approved".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#D1FAE5")); // Green
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#065F46"));

                // Safe UI: Hide processing buttons since action is complete
                btnApproveBooking.setVisibility(View.GONE);
                btnRejectBooking.setVisibility(View.GONE);
            } else if ("Rejected".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2")); // Red
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#991B1B"));

                // Safe UI: Hide processing buttons since action is complete
                btnApproveBooking.setVisibility(View.GONE);
                btnRejectBooking.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, "Critical Error: No target ID received via Intent!", Toast.LENGTH_LONG).show();
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

        btnApproveBooking.setOnClickListener(v -> updateBookingStatus("Approved", null));
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
                updateBookingStatus("Rejected", reason);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateBookingStatus(String newStatus, String rejectReason) {
        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Cannot update status: Target tracking ID is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("rejectReason", rejectReason != null ? rejectReason : "");

        db.collection("Bookings").document(bookingId.trim())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(StaffActionActivity.this, "Booking successfully marked as " + newStatus, Toast.LENGTH_SHORT).show();
                    btnBack.performClick();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StaffActionActivity.this, "Firestore Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }
}