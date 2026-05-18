package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class BookingDetailsActivity extends AppCompatActivity {

    // Voucher Display Fields
    private ImageButton btnBack;
    private TextView tvDetailsTitle, tvStatusTag, tvRoomId, tvRoomType, tvPrice;
    private TextView tvStudentName, tvMatric, tvPhoneNumber, tvCheckIn, tvLeaseDuration;

    // Rejection Layout Structural Elements
    private LinearLayout layoutRejectReason;
    private TextView tvRejectReasonContent;

    // Bottom Action Bar Containers & Buttons
    private LinearLayout layoutCompletedActions;
    private Button btnCancelBooking, btnReturn, btnShare;

    // Runtime Allocation Parameters
    private String documentId;
    private String bookingStatus;
    private String rejectReason;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        // Initialize Cloud Database Node
        db = FirebaseFirestore.getInstance();

        // 1. Structural Binding Init
        initViews();

        // 2. Map Inbound Application Intent Bundle Metrics
        getIntentData();

        // 3. Inject Dynamic Aesthetic Themes depending on Verification State
        configureVoucherThemeEngine();

        // 4. Bind Activity Event Interceptors
        setupClickListeners();

        // Handle System Back Button Press using modern Dispatcher Lifecycle
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleSmartBackNavigation();
            }
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvDetailsTitle = findViewById(R.id.tvDetailsTitle);
        tvStatusTag = findViewById(R.id.tvStatusTag);
        tvRoomId = findViewById(R.id.tvRoomId);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvPrice = findViewById(R.id.tvPrice);

        tvStudentName = findViewById(R.id.tvStudentName);
        tvMatric = findViewById(R.id.tvMatric);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvCheckIn = findViewById(R.id.tvCheckIn);
        tvLeaseDuration = findViewById(R.id.tvLeaseDuration);

        // Map Rejection Field Pairs safely from layout references
        layoutRejectReason = findViewById(R.id.layoutRejectReason);
        tvRejectReasonContent = findViewById(R.id.tvRejectReasonContent);

        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        layoutCompletedActions = findViewById(R.id.layoutCompletedActions);
        btnReturn = findViewById(R.id.btnReturn);
        btnShare = findViewById(R.id.btnShare);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            documentId = intent.getStringExtra("BOOKING_DOC_ID");
            bookingStatus = intent.getStringExtra("BOOKING_STATUS");
            rejectReason = intent.getStringExtra("REJECT_REASON");

            if (intent.getStringExtra("ROOM_ID") != null) tvRoomId.setText(intent.getStringExtra("ROOM_ID"));
            if (intent.getStringExtra("ROOM_TYPE") != null) tvRoomType.setText(intent.getStringExtra("ROOM_TYPE"));

            // Strip away "/ semester" formatting extensions automatically
            if (intent.getStringExtra("ROOM_PRICE") != null) {
                String rawPrice = intent.getStringExtra("ROOM_PRICE");
                if (rawPrice.contains("/")) {
                    rawPrice = rawPrice.split("/")[0].trim();
                }
                tvPrice.setText(rawPrice);
            }

            if (intent.getStringExtra("STUDENT_NAME") != null) tvStudentName.setText(intent.getStringExtra("STUDENT_NAME"));
            if (intent.getStringExtra("MATRIC_NUMBER") != null) tvMatric.setText(intent.getStringExtra("MATRIC_NUMBER"));
            if (intent.getStringExtra("PHONE_NUMBER") != null) tvPhoneNumber.setText(intent.getStringExtra("PHONE_NUMBER"));
            if (intent.getStringExtra("CHECK_IN_DATE") != null) tvCheckIn.setText(intent.getStringExtra("CHECK_IN_DATE"));
            if (intent.getStringExtra("LEASE_DURATION") != null) tvLeaseDuration.setText(intent.getStringExtra("LEASE_DURATION"));
        }
    }

    private void configureVoucherThemeEngine() {
        if (bookingStatus == null) {
            bookingStatus = "Pending";
        }

        tvStatusTag.setText(bookingStatus.toUpperCase().trim());
        GradientDrawable statusBgShape = (GradientDrawable) tvStatusTag.getBackground();

        if (layoutRejectReason != null) {
            layoutRejectReason.setVisibility(View.GONE);
        }

        switch (bookingStatus.toLowerCase().trim()) {
            case "pending":
                if (statusBgShape != null) statusBgShape.setColor(Color.parseColor("#F59E0B")); // Amber
                tvPrice.setTextColor(Color.parseColor("#F59E0B"));

                btnCancelBooking.setVisibility(View.VISIBLE);
                layoutCompletedActions.setVisibility(View.GONE);
                break;

            case "approved":
                if (statusBgShape != null) statusBgShape.setColor(Color.parseColor("#6366F1")); // Indigo Blue for action state
                tvPrice.setTextColor(Color.parseColor("#6366F1"));

                btnCancelBooking.setVisibility(View.GONE);
                layoutCompletedActions.setVisibility(View.VISIBLE);

                // IMPROVEMENT: Transform share button into a dynamic payment pathway link
                btnShare.setText("Proceed to Payment");
                btnShare.setBackgroundColor(Color.parseColor("#6366F1"));
                break;

            case "paid":
                if (statusBgShape != null) statusBgShape.setColor(Color.parseColor("#10B981")); // Emerald Green
                tvPrice.setTextColor(Color.parseColor("#10B981"));

                btnCancelBooking.setVisibility(View.GONE);
                layoutCompletedActions.setVisibility(View.VISIBLE);
                btnShare.setText("Share Receipt");
                btnShare.setBackgroundColor(Color.parseColor("#10B981")); // Match color with state
                break;

            case "rejected":
                if (statusBgShape != null) statusBgShape.setColor(Color.parseColor("#EF4444")); // Crimson Red
                tvPrice.setTextColor(Color.parseColor("#EF4444"));

                btnCancelBooking.setVisibility(View.GONE);
                layoutCompletedActions.setVisibility(View.VISIBLE);
                btnShare.setText("Share Status");

                if (layoutRejectReason != null && tvRejectReasonContent != null) {
                    layoutRejectReason.setVisibility(View.VISIBLE);
                    if (rejectReason != null && !rejectReason.trim().isEmpty()) {
                        tvRejectReasonContent.setText(rejectReason.trim());
                    } else {
                        tvRejectReasonContent.setText("No explicit custom feedback was written by the administrator.");
                    }
                }
                break;
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> handleSmartBackNavigation());
        btnReturn.setOnClickListener(v -> handleSmartBackNavigation());
        btnCancelBooking.setOnClickListener(v -> showCancellationDialog());

        // IMPROVEMENT: Smart Action click dispatcher depending on current database status configuration state
        btnShare.setOnClickListener(v -> {
            if ("approved".equalsIgnoreCase(bookingStatus.trim())) {
                navigateToPaymentGateway();
            } else {
                executeShareVoucherSheet();
            }
        });
    }

    private void navigateToPaymentGateway() {
        Intent paymentIntent = new Intent(BookingDetailsActivity.this, PaymentActivity.class);

        // Pack all variables matching requirements of PaymentActivity
        paymentIntent.putExtra("BOOKING_DOC_ID", documentId);
        paymentIntent.putExtra("ROOM_ID", tvRoomId.getText().toString());
        paymentIntent.putExtra("ROOM_TYPE", tvRoomType.getText().toString());
        paymentIntent.putExtra("ROOM_PRICE", tvPrice.getText().toString());

        paymentIntent.putExtra("STUDENT_NAME", tvStudentName.getText().toString());
        paymentIntent.putExtra("MATRIC_NUMBER", tvMatric.getText().toString());
        paymentIntent.putExtra("PHONE_NUMBER", tvPhoneNumber.getText().toString());
        paymentIntent.putExtra("CHECK_IN_DATE", tvCheckIn.getText().toString());
        paymentIntent.putExtra("LEASE_DURATION", tvLeaseDuration.getText().toString());

        startActivity(paymentIntent);
    }

    private void handleSmartBackNavigation() {
        if (bookingStatus == null) bookingStatus = "Pending";

        Intent targetIntent;
        if (bookingStatus.equalsIgnoreCase("Pending")) {
            targetIntent = new Intent(BookingDetailsActivity.this, BookingsActivity.class);
        } else {
            targetIntent = new Intent(BookingDetailsActivity.this, HistoryActivity.class);
        }

        targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(targetIntent);
        finish();
    }

    private void showCancellationDialog() {
        Log.d("FIRESTORE_DELETE_DEBUG", "Evaluating documentId: " + documentId);

        if (documentId == null || documentId.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Extraction Error")
                    .setMessage("Cannot verify this booking instance on the server. The database document key was missing.")
                    .setPositiveButton("Go Back", (d, w) -> finish())
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel My Application")
                .setMessage("Are you absolutely sure you want to retract this hostel accommodation request? This data cannot be recovered.")
                .setPositiveButton("Confirm Cancellation", (dialog, which) -> {

                    btnCancelBooking.setEnabled(false);
                    Toast.makeText(this, "Deleting from Firestore server...", Toast.LENGTH_SHORT).show();

                    db.collection("Bookings").document(documentId.trim())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(BookingDetailsActivity.this, "Booking canceled successfully.", Toast.LENGTH_LONG).show();

                                Intent intent = new Intent(BookingDetailsActivity.this, BookingsActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnCancelBooking.setEnabled(true);

                                new AlertDialog.Builder(BookingDetailsActivity.this)
                                        .setTitle("Firebase Transaction Exception")
                                        .setMessage("Details: " + e.getLocalizedMessage() + "\n\nChecked Collection Key Path: Bookings/" + documentId.trim())
                                        .setPositiveButton("Acknowledge", null)
                                        .show();
                            });
                })
                .setNegativeButton("Keep Reservation", null)
                .show();
    }

    private void executeShareVoucherSheet() {
        String roomId = tvRoomId.getText().toString();
        String roomType = tvRoomType.getText().toString();
        String currentStatus = tvStatusTag.getText().toString();
        String residentName = tvStudentName.getText().toString();

        String shareBodyText = "🛏️ UTM Space Hostel Booking Voucher Record Receipt:\n\n" +
                "• Status Profile: [" + currentStatus + "]\n" +
                "• Resident: " + residentName + "\n" +
                "• Room Identifier Tag: " + roomId + "\n" +
                "• Layout Tier Type: " + roomType + "\n";

        if ("REJECTED".equalsIgnoreCase(currentStatus) && rejectReason != null && !rejectReason.isEmpty()) {
            shareBodyText += "• Reason for Rejection: " + rejectReason + "\n";
        }

        shareBodyText += "\nGenerated via official UTM Space Allocation Management Architecture System client terminal.";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "UTM Space Booking Voucher Log Summary Copy");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBodyText);

        startActivity(Intent.createChooser(shareIntent, "Share Booking Record Summary Via:"));
    }
}