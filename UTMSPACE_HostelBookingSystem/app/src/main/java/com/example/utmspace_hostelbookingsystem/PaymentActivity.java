package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

public class PaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnPayNow;
    private TextView tvTotalMain;

    // References for the Cards and RadioButtons
    private MaterialCardView cardDebit, cardBank, cardWallet;
    private RadioButton rbDebit, rbBank, rbWallet;

    // Data parameters received from HistoryActivity
    private String bookingDocId;
    private String roomId;
    private String roomType;
    private String roomPrice;
    private String studentName;
    private String matricNumber;
    private String phoneNumber;
    private String checkInDate;
    private String leaseDuration;

    private String selectedMethod = "";

    // IMPROVED: Added FirebaseFirestore instance reference
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        initViews();
        getIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPayNow = findViewById(R.id.btnPayNow);
        tvTotalMain = findViewById(R.id.tvTotalMain);

        // Find RadioButtons
        rbDebit = findViewById(R.id.rbCard);
        rbBank = findViewById(R.id.rbBank);
        rbWallet = findViewById(R.id.rbWallet);

        // Find the CardViews (Parent of the Parent of the RadioButton)
        cardDebit = (MaterialCardView) rbDebit.getParent().getParent();
        cardBank = (MaterialCardView) rbBank.getParent().getParent();
        cardWallet = (MaterialCardView) rbWallet.getParent().getParent();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            // Key mappings matched exactly with HistoryActivity specifications
            bookingDocId = intent.getStringExtra("BOOKING_DOC_ID");
            roomId = intent.getStringExtra("ROOM_ID");
            roomType = intent.getStringExtra("ROOM_TYPE");
            roomPrice = intent.getStringExtra("ROOM_PRICE");

            studentName = intent.getStringExtra("STUDENT_NAME");
            matricNumber = intent.getStringExtra("MATRIC_NUMBER");
            phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            checkInDate = intent.getStringExtra("CHECK_IN_DATE");
            leaseDuration = intent.getStringExtra("LEASE_DURATION");

            // Display price cleanly inside target text field boundary
            if (roomPrice != null) {
                // Strip away "/ semester" formatting extensions automatically if present
                String displayPrice = roomPrice;
                if (displayPrice.contains("/")) {
                    displayPrice = displayPrice.split("/")[0].trim();
                }
                tvTotalMain.setText(displayPrice);
            } else {
                tvTotalMain.setText("RM 0.00");
            }
        }
    }

    private void setupListeners() {
        // 1. Back Arrow navigation
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, HistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 2. Custom Radio Logic: Handle Card Container Click Actions
        cardDebit.setOnClickListener(v -> updateRadioSelection(rbDebit, "Credit / Debit Card"));
        cardBank.setOnClickListener(v -> updateRadioSelection(rbBank, "Online Banking (FPX)"));
        cardWallet.setOnClickListener(v -> updateRadioSelection(rbWallet, "E-Wallet"));

        // 3. Custom Radio Logic: Handle RadioButton Direct Click Actions
        rbDebit.setOnClickListener(v -> updateRadioSelection(rbDebit, "Credit / Debit Card"));
        rbBank.setOnClickListener(v -> updateRadioSelection(rbBank, "Online Banking (FPX)"));
        rbWallet.setOnClickListener(v -> updateRadioSelection(rbWallet, "E-Wallet"));

        // 4. Pay Now Execution Path Button
        btnPayNow.setOnClickListener(v -> {
            if (selectedMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method to proceed.", Toast.LENGTH_SHORT).show();
            } else if (bookingDocId == null || bookingDocId.isEmpty()) {
                Toast.makeText(this, "Error: Invalid booking reference identifier.", Toast.LENGTH_SHORT).show();
            } else {
                // Disable button to prevent double-clicks during server processing
                btnPayNow.setEnabled(false);

                // IMPROVED: Update booking document status directly to "Paid" in Firestore
                db.collection("Bookings").document(bookingDocId)
                        .update("status", "Paid")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(PaymentActivity.this, "Payment successful!", Toast.LENGTH_SHORT).show();

                            // Route explicitly forward into Receipt screen view framework
                            Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);

                            // Pack full dynamic payload data parameters safely
                            intent.putExtra("PAYMENT_METHOD", selectedMethod);
                            intent.putExtra("BOOKING_DOC_ID", bookingDocId);
                            intent.putExtra("BOOKING_STATUS", "Paid"); // Updated status payload
                            intent.putExtra("ROOM_ID", roomId != null ? roomId : "N/A");
                            intent.putExtra("ROOM_TYPE", roomType != null ? roomType : "N/A");
                            intent.putExtra("ROOM_PRICE", roomPrice != null ? roomPrice : "0.00");

                            intent.putExtra("STUDENT_NAME", studentName);
                            intent.putExtra("MATRIC_NUMBER", matricNumber);
                            intent.putExtra("PHONE_NUMBER", phoneNumber);
                            intent.putExtra("CHECK_IN_DATE", checkInDate);
                            intent.putExtra("LEASE_DURATION", leaseDuration);

                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            // Re-enable button on error path
                            btnPayNow.setEnabled(true);
                            Toast.makeText(PaymentActivity.this, "Transaction failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    /**
     * Clear active selected items and toggle only the target RadioButton
     */
    private void updateRadioSelection(RadioButton targetRadio, String method) {
        rbDebit.setChecked(false);
        rbBank.setChecked(false);
        rbWallet.setChecked(false);

        targetRadio.setChecked(true);
        selectedMethod = method;
    }
}