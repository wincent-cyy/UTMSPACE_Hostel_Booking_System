package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingDetailsActivity extends AppCompatActivity {

    // Header Views
    private LinearLayout ivBack;
    private TextView tvBookingStatus;
    private TextView tvBookingId;

    // Room Information Views
    private TextView tvRoomNumber;
    private TextView tvRoomType;
    private TextView tvRoomLocation;
    private TextView tvRoomPrice;

    // Student Information Views
    private TextView tvStudentName;
    private TextView tvStudentId;
    private TextView tvStudentPhone;
    private TextView tvStudentEmail;
    private TextView tvProgramme;

    // Booking Information Views
    private TextView tvApplicationDate;
    private TextView tvDuration;
    private TextView tvTotalAmount;

    // Action Buttons
    private LinearLayout btnCancelBooking;
    private LinearLayout btnPayNow;
    private LinearLayout btnContactSupport;

    // Rejection Reason Views - ADDED
    private LinearLayout rejectionReasonContainer;
    private TextView tvRejectionReason;

    // Data variables
    private String documentId;
    private String bookingStatus;
    private String rejectReason;  // ADDED
    private String roomId;
    private String roomType;
    private String roomLocation;
    private String roomPrice;
    private String studentName;
    private String studentId;
    private String phoneNumber;
    private String email;
    private String programme;
    private String checkInDate;
    private String leaseDuration;
    private Long createdAt;
    private String totalAmount;

    // Firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        getIntentData();
        populateUI();
        setupClickListeners();
        configureStatusTheme();

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });
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
        tvBookingStatus = findViewById(R.id.tvBookingStatus);
        tvBookingId = findViewById(R.id.tvBookingId);

        // Room Information
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvRoomLocation = findViewById(R.id.tvRoomLocation);
        tvRoomPrice = findViewById(R.id.tvRoomPrice);

        // Student Information
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvStudentPhone = findViewById(R.id.tvStudentPhone);
        tvStudentEmail = findViewById(R.id.tvStudentEmail);
        tvProgramme = findViewById(R.id.tvProgramme);

        // Booking Information
        tvApplicationDate = findViewById(R.id.tvApplicationDate);
        tvDuration = findViewById(R.id.tvDuration);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

        // Action Buttons
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnPayNow = findViewById(R.id.btnPayNow);
        btnContactSupport = findViewById(R.id.btnContactSupport);

        // Rejection Reason Views - ADDED
        rejectionReasonContainer = findViewById(R.id.rejectionReasonContainer);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            documentId = intent.getStringExtra("BOOKING_DOC_ID");
            bookingStatus = intent.getStringExtra("BOOKING_STATUS");
            rejectReason = intent.getStringExtra("REJECT_REASON");  // ADDED
            roomId = intent.getStringExtra("ROOM_ID");
            roomType = intent.getStringExtra("ROOM_TYPE");
            roomLocation = intent.getStringExtra("ROOM_LOCATION");
            roomPrice = intent.getStringExtra("ROOM_PRICE");
            studentName = intent.getStringExtra("STUDENT_NAME");
            studentId = intent.getStringExtra("STUDENT_ID");
            phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            email = intent.getStringExtra("EMAIL");
            programme = intent.getStringExtra("PROGRAMME");
            checkInDate = intent.getStringExtra("CHECK_IN_DATE");
            leaseDuration = intent.getStringExtra("LEASE_DURATION");
            totalAmount = intent.getStringExtra("TOTAL_AMOUNT");
            createdAt = intent.getLongExtra("CREATED_AT", 0);

            // Debug logging
            android.util.Log.d("BookingDetails", "=== Received Data ===");
            android.util.Log.d("BookingDetails", "Reject Reason: " + rejectReason);  // ADDED
            android.util.Log.d("BookingDetails", "Location: " + roomLocation);
            android.util.Log.d("BookingDetails", "Email: " + email);
            android.util.Log.d("BookingDetails", "Programme: " + programme);
            android.util.Log.d("BookingDetails", "CreatedAt: " + createdAt);
        }
    }

    private void populateUI() {
        // Booking ID
        if (documentId != null && !documentId.isEmpty()) {
            tvBookingId.setText(documentId);
        } else {
            tvBookingId.setText("N/A");
        }

        // Room Information
        if (roomId != null && !roomId.isEmpty()) tvRoomNumber.setText(roomId);
        if (roomType != null && !roomType.isEmpty()) tvRoomType.setText(roomType);
        if (roomLocation != null && !roomLocation.isEmpty()) {
            tvRoomLocation.setText(roomLocation);
        } else {
            tvRoomLocation.setText("Not specified");
        }
        if (roomPrice != null && !roomPrice.isEmpty()) {
            tvRoomPrice.setText(roomPrice);
            tvTotalAmount.setText(roomPrice);
        }

        // Student Information
        if (studentName != null && !studentName.isEmpty()) tvStudentName.setText(studentName);
        if (studentId != null && !studentId.isEmpty()) tvStudentId.setText(studentId);
        if (phoneNumber != null && !phoneNumber.isEmpty()) tvStudentPhone.setText(phoneNumber);
        if (email != null && !email.isEmpty()) {
            tvStudentEmail.setText(email);
        } else {
            tvStudentEmail.setText("Not provided");
        }
        if (programme != null && !programme.isEmpty()) {
            tvProgramme.setText(programme);
        } else {
            tvProgramme.setText("Not specified");
        }

        // Booking Information
        if (createdAt != null && createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateString = sdf.format(new Date(createdAt));
            tvApplicationDate.setText(dateString);
        } else {
            tvApplicationDate.setText("N/A");
        }

        if (leaseDuration != null && !leaseDuration.isEmpty()) tvDuration.setText(leaseDuration);
    }

    private void showRejectionReasonIfNeeded() {
        // Only show rejection reason if status is "Rejected" and reason exists
        if ("rejected".equalsIgnoreCase(bookingStatus) && rejectReason != null && !rejectReason.isEmpty()) {
            if (rejectionReasonContainer != null && tvRejectionReason != null) {
                rejectionReasonContainer.setVisibility(View.VISIBLE);
                tvRejectionReason.setText(rejectReason);
            }
        } else {
            if (rejectionReasonContainer != null) {
                rejectionReasonContainer.setVisibility(View.GONE);
            }
        }
    }

    private void configureStatusTheme() {
        if (bookingStatus == null) {
            bookingStatus = "Pending";
        }

        tvBookingStatus.setText(bookingStatus);

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(30f);
        tvBookingStatus.setPadding(24, 12, 24, 12);

        switch (bookingStatus.toLowerCase()) {
            case "pending":
                statusBg.setColor(Color.parseColor("#FEF3C7"));
                tvBookingStatus.setBackground(statusBg);
                tvBookingStatus.setTextColor(Color.parseColor("#D97706"));
                tvRoomPrice.setTextColor(Color.parseColor("#D97706"));
                tvTotalAmount.setTextColor(Color.parseColor("#D97706"));

                btnCancelBooking.setVisibility(View.VISIBLE);
                btnPayNow.setVisibility(View.GONE);
                break;

            case "approved":
                statusBg.setColor(Color.parseColor("#DCFCE7"));
                tvBookingStatus.setBackground(statusBg);
                tvBookingStatus.setTextColor(Color.parseColor("#15803D"));
                tvRoomPrice.setTextColor(Color.parseColor("#15803D"));
                tvTotalAmount.setTextColor(Color.parseColor("#15803D"));

                btnCancelBooking.setVisibility(View.GONE);
                btnPayNow.setVisibility(View.VISIBLE);
                break;

            case "paid":
                statusBg.setColor(Color.parseColor("#DBEAFE"));
                tvBookingStatus.setBackground(statusBg);
                tvBookingStatus.setTextColor(Color.parseColor("#1E40AF"));
                tvRoomPrice.setTextColor(Color.parseColor("#1E40AF"));
                tvTotalAmount.setTextColor(Color.parseColor("#1E40AF"));

                btnCancelBooking.setVisibility(View.GONE);
                btnPayNow.setVisibility(View.GONE);
                break;

            case "rejected":
                statusBg.setColor(Color.parseColor("#FEE2E2"));
                tvBookingStatus.setBackground(statusBg);
                tvBookingStatus.setTextColor(Color.parseColor("#B91C1C"));
                tvRoomPrice.setTextColor(Color.parseColor("#B91C1C"));
                tvTotalAmount.setTextColor(Color.parseColor("#B91C1C"));

                btnCancelBooking.setVisibility(View.GONE);
                btnPayNow.setVisibility(View.GONE);

                // Show rejection reason
                showRejectionReasonIfNeeded();
                break;

            default:
                statusBg.setColor(Color.parseColor("#F3F4F6"));
                tvBookingStatus.setBackground(statusBg);
                tvBookingStatus.setTextColor(Color.parseColor("#374151"));
                break;
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> navigateBack());

        btnCancelBooking.setOnClickListener(v -> showCancellationDialog());

        btnPayNow.setOnClickListener(v -> navigateToPayment());

        btnContactSupport.setOnClickListener(v -> showContactSupportDialog());
    }

    private void navigateBack() {
        Intent intent;
        if ("Pending".equalsIgnoreCase(bookingStatus)) {
            intent = new Intent(BookingDetailsActivity.this, BookingsActivity.class);
        } else {
            intent = new Intent(BookingDetailsActivity.this, HistoryActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showCancellationDialog() {
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Cannot cancel: Invalid booking ID", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel Application")
                .setMessage("Are you sure you want to cancel this booking application? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    cancelBooking();
                })
                .setNegativeButton("No, Keep", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void cancelBooking() {
        btnCancelBooking.setEnabled(false);
        Toast.makeText(this, "Cancelling booking...", Toast.LENGTH_SHORT).show();

        db.collection("Bookings").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_LONG).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    btnCancelBooking.setEnabled(true);
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToPayment() {
        Intent intent = new Intent(BookingDetailsActivity.this, PaymentActivity.class);
        intent.putExtra("BOOKING_DOC_ID", documentId);
        intent.putExtra("ROOM_ID", roomId);
        intent.putExtra("ROOM_TYPE", roomType);
        intent.putExtra("ROOM_PRICE", roomPrice);
        intent.putExtra("STUDENT_NAME", studentName);
        intent.putExtra("STUDENT_ID", studentId);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        intent.putExtra("CHECK_IN_DATE", checkInDate);
        intent.putExtra("LEASE_DURATION", leaseDuration);
        startActivity(intent);
    }

    private void showContactSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Contact Support")
                .setMessage("📧 Email: hostelhub@utm.my\n📞 Phone: 03-5556-9012\n\n🕐 Office Hours: Monday-Friday, 9am - 5pm")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
    }
}