package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminBookingDetailActivity extends AppCompatActivity {

    // Header
    private LinearLayout ivBack;

    // Status Banner
    private TextView tvStatusIcon;
    private TextView tvStatus;
    private TextView tvBookingId;

    // Student Information
    private TextView tvStudentName;
    private TextView tvStudentId;
    private TextView tvPhone;
    private TextView tvEmail;
    private TextView tvProgramme;

    // Room Information
    private TextView tvRoomNumber;
    private TextView tvRoomType;
    private TextView tvLocation;
    private TextView tvPrice;

    // Booking Information
    private TextView tvApplicationDate;
    private TextView tvDuration;
    private TextView tvCheckInDate;
    private TextView tvTotalAmount;

    // Rejection Reason
    private LinearLayout rejectionSection;
    private TextView tvRejectionReason;

    private FirebaseFirestore db;
    private String bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_booking_detail);

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        db = FirebaseFirestore.getInstance();

        initViews();
        getIntentData();
        setupClickListeners();
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
        tvBookingId = findViewById(R.id.tvBookingId);

        // Student Information
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvProgramme = findViewById(R.id.tvProgramme);

        // Room Information
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);

        // Booking Information
        tvApplicationDate = findViewById(R.id.tvApplicationDate);
        tvDuration = findViewById(R.id.tvDuration);
        tvCheckInDate = findViewById(R.id.tvCheckInDate);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

        // Rejection Reason
        rejectionSection = findViewById(R.id.rejectionSection);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            // Booking ID - 使用 ManagementActivity 中传递的 key
            bookingId = intent.getStringExtra("BOOKING_ID");
            if (bookingId == null) {
                bookingId = intent.getStringExtra("BOOKING_DOC_ID");
            }
            tvBookingId.setText("#" + (bookingId != null ?
                    (bookingId.length() > 8 ? bookingId.substring(0, 8) : bookingId) : "N/A"));

            // Status - 使用 ManagementActivity 中传递的 key
            String status = intent.getStringExtra("STATUS");
            if (status == null) {
                status = intent.getStringExtra("BOOKING_STATUS");
            }
            if (status == null) {
                status = intent.getStringExtra("bookingStatus");
            }
            displayStatus(status);

            // Student Information - 使用正确的 key
            String studentName = intent.getStringExtra("STUDENT_NAME");
            if (studentName == null) studentName = intent.getStringExtra("name");
            tvStudentName.setText(studentName != null && !studentName.isEmpty() ? studentName : "N/A");

            String matricNumber = intent.getStringExtra("MATRIC_NUMBER");
            if (matricNumber == null) matricNumber = intent.getStringExtra("matricNumber");
            tvStudentId.setText(matricNumber != null && !matricNumber.isEmpty() ? matricNumber : "N/A");

            // Phone - 从 ManagementActivity 传递的是 "PHONE"
            String phone = intent.getStringExtra("PHONE");
            if (phone == null) phone = intent.getStringExtra("PHONE_NUMBER");
            if (phone == null) phone = intent.getStringExtra("phone");
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "N/A");

            // Email - 从 ManagementActivity 没有传递 email，需要从其他地方获取或显示 N/A
            // 如果 UserManagementActivity 有传递 email，可以在这里获取
            String email = intent.getStringExtra("EMAIL");
            if (email == null) email = intent.getStringExtra("email");
            tvEmail.setText(email != null && !email.isEmpty() ? email : "N/A");

            // Programme - 从 ManagementActivity 没有传递 programme，显示 N/A
            String programme = intent.getStringExtra("PROGRAMME");
            if (programme == null) programme = intent.getStringExtra("programme");
            tvProgramme.setText(programme != null && !programme.isEmpty() ? programme : "N/A");

            // Room Information
            String roomId = intent.getStringExtra("ROOM_ID");
            if (roomId == null) roomId = intent.getStringExtra("roomId");
            tvRoomNumber.setText(roomId != null && !roomId.isEmpty() ? roomId : "N/A");

            String roomType = intent.getStringExtra("ROOM_TYPE");
            if (roomType == null) roomType = intent.getStringExtra("roomType");
            tvRoomType.setText(roomType != null && !roomType.isEmpty() ? roomType : "N/A");

            // Location - 从 ManagementActivity 传递的是 "LOCATION"
            String location = intent.getStringExtra("LOCATION");
            if (location == null) location = intent.getStringExtra("ROOM_LOCATION");
            if (location == null) location = intent.getStringExtra("location");
            tvLocation.setText(location != null && !location.isEmpty() ? location : "N/A");

            // Price - 从 ManagementActivity 传递的是 "PRICE"
            double price = intent.getDoubleExtra("PRICE", 0);
            if (price == 0) {
                String priceStr = intent.getStringExtra("ROOM_PRICE");
                if (priceStr != null) {
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        price = 0;
                    }
                }
            }
            String priceText = String.format("RM %.2f", price);
            tvPrice.setText(priceText);
            tvTotalAmount.setText(priceText);

            // Booking Information
            long createdAt = intent.getLongExtra("CREATED_AT", 0);
            if (createdAt == 0) {
                createdAt = intent.getLongExtra("createdAt", 0);
            }
            if (createdAt > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvApplicationDate.setText(sdf.format(new Date(createdAt)));
            } else {
                tvApplicationDate.setText("N/A");
            }

            String leaseDuration = intent.getStringExtra("LEASE_DURATION");
            if (leaseDuration == null) leaseDuration = intent.getStringExtra("leaseDuration");
            tvDuration.setText(leaseDuration != null && !leaseDuration.isEmpty() ? leaseDuration : "N/A");

            String checkInDate = intent.getStringExtra("CHECK_IN_DATE");
            if (checkInDate == null) checkInDate = intent.getStringExtra("checkInDate");
            tvCheckInDate.setText(checkInDate != null && !checkInDate.isEmpty() ? checkInDate : "N/A");

            // Rejection Reason
            String rejectReason = intent.getStringExtra("REJECT_REASON");
            if (rejectReason == null) {
                rejectReason = intent.getStringExtra("rejectReason");
            }

            String statusVal = intent.getStringExtra("STATUS");
            if (statusVal == null) {
                statusVal = intent.getStringExtra("BOOKING_STATUS");
            }
            if (statusVal == null) {
                statusVal = intent.getStringExtra("bookingStatus");
            }

            if ("Rejected".equalsIgnoreCase(statusVal) && rejectReason != null && !rejectReason.isEmpty()) {
                rejectionSection.setVisibility(View.VISIBLE);
                tvRejectionReason.setText(rejectReason);
            } else {
                rejectionSection.setVisibility(View.GONE);
            }
        }
    }

    private void displayStatus(String status) {
        if (status == null) {
            status = "Pending";
        }
        tvStatus.setText(status);

        // Set status banner color
        LinearLayout statusBanner = findViewById(R.id.statusBanner);

        switch (status.toLowerCase()) {
            case "pending":
                tvStatusIcon.setText("⏳");
                tvStatus.setTextColor(Color.parseColor("#D97706"));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(Color.parseColor("#FEF3C7"));
                }
                break;
            case "approved":
                tvStatusIcon.setText("✅");
                tvStatus.setTextColor(Color.parseColor("#15803D"));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(Color.parseColor("#DCFCE7"));
                }
                break;
            case "rejected":
                tvStatusIcon.setText("❌");
                tvStatus.setTextColor(Color.parseColor("#B91C1C"));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(Color.parseColor("#FEE2E2"));
                }
                break;
            case "paid":
                tvStatusIcon.setText("💰");
                tvStatus.setTextColor(Color.parseColor("#1E40AF"));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(Color.parseColor("#DBEAFE"));
                }
                break;
            default:
                tvStatusIcon.setText("📋");
                tvStatus.setTextColor(Color.parseColor("#D97706"));
                if (statusBanner != null) {
                    statusBanner.setBackgroundColor(Color.parseColor("#FEF3C7"));
                }
                break;
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
    }
}