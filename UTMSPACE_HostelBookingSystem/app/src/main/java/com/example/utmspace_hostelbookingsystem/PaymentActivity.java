package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;

public class PaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnPayNow;
    private TextView tvTotalMain, tvDisplayName, tvDisplayMatric, tvDisplayPhone;

    private MaterialCardView cardDebit, cardBank, cardWallet;
    private RadioButton rbDebit, rbBank, rbWallet;

    private String bookingDocId, roomType, roomPrice;
    private String selectedMethod = "";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        getIntentData();
        fetchUserInfo();
        fetchBookingDetails();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPayNow = findViewById(R.id.btnPayNow);
        tvTotalMain = findViewById(R.id.tvTotalMain);

        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvDisplayMatric = findViewById(R.id.tvDisplayMatric);
        tvDisplayPhone = findViewById(R.id.tvDisplayPhone);

        rbDebit = findViewById(R.id.rbCard);
        rbBank = findViewById(R.id.rbBank);
        rbWallet = findViewById(R.id.rbWallet);

        cardDebit = (MaterialCardView) rbDebit.getParent().getParent();
        cardBank = (MaterialCardView) rbBank.getParent().getParent();
        cardWallet = (MaterialCardView) rbWallet.getParent().getParent();
    }

    private void fetchUserInfo() {
        if (bookingDocId != null && !bookingDocId.isEmpty()) {
            db.collection("Bookings").document(bookingDocId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    String name = doc.getString("studentName");
                    String matric = doc.getString("matricNumber");
                    String phone = doc.getString("phoneNumber");

                    if (name != null && !name.isEmpty()) tvDisplayName.setText(name);
                    if (matric != null && !matric.isEmpty()) tvDisplayMatric.setText(matric);
                    if (phone != null && !phone.isEmpty()) tvDisplayPhone.setText(phone);
                } else {
                    tvDisplayName.setText("Student Name");
                    tvDisplayMatric.setText("Matric Number");
                    tvDisplayPhone.setText("Phone Number");
                }
            });
        }
    }

    private void fetchBookingDetails() {
        if (bookingDocId != null && !bookingDocId.isEmpty()) {
            db.collection("Bookings").document(bookingDocId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    String status = doc.getString("status");

                    if (status != null && status.equals("Paid")) {
                        Toast.makeText(this, "This booking has already been paid!", Toast.LENGTH_LONG).show();
                        btnPayNow.setEnabled(false);
                        btnPayNow.setText("Already Paid");
                    }
                }
            });
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            bookingDocId = intent.getStringExtra("BOOKING_DOC_ID");
            roomType = intent.getStringExtra("ROOM_TYPE");
            roomPrice = intent.getStringExtra("ROOM_PRICE");

            if (roomPrice != null && !roomPrice.isEmpty()) {
                if (roomPrice.contains("/")) {
                    String[] parts = roomPrice.split("/");
                    tvTotalMain.setText(parts[0].trim());
                } else {
                    tvTotalMain.setText(roomPrice);
                }
            } else {
                tvTotalMain.setText("RM 0.00");
            }

            // Debug log
            android.util.Log.d("PaymentActivity", "Booking ID: " + bookingDocId);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        cardDebit.setOnClickListener(v -> updateRadioSelection(rbDebit, "Credit / Debit Card"));
        cardBank.setOnClickListener(v -> updateRadioSelection(rbBank, "Online Banking (FPX)"));
        cardWallet.setOnClickListener(v -> updateRadioSelection(rbWallet, "E-Wallet"));

        rbDebit.setOnClickListener(v -> updateRadioSelection(rbDebit, "Credit / Debit Card"));
        rbBank.setOnClickListener(v -> updateRadioSelection(rbBank, "Online Banking (FPX)"));
        rbWallet.setOnClickListener(v -> updateRadioSelection(rbWallet, "E-Wallet"));

        btnPayNow.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        // Validation checks
        if (selectedMethod.isEmpty()) {
            Toast.makeText(this, "Please select a payment method.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bookingDocId == null || bookingDocId.isEmpty()) {
            Toast.makeText(this, "Error: Booking information missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent double click
        btnPayNow.setEnabled(false);
        btnPayNow.setText("Processing...");

        DocumentReference bookingRef = db.collection("Bookings").document(bookingDocId);

        // First, get the booking to find the actual room number
        bookingRef.get().addOnSuccessListener(bookingDoc -> {
            if (!bookingDoc.exists()) {
                resetPayButton();
                Toast.makeText(this, "Error: Booking not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentStatus = bookingDoc.getString("status");
            if (currentStatus != null && currentStatus.equals("Paid")) {
                resetPayButton();
                Toast.makeText(this, "This booking has already been paid!", Toast.LENGTH_LONG).show();
                return;
            }

            // Get the room number from booking (field is "roomId" in your collection)
            String roomNumberFromBooking = bookingDoc.getString("roomId");
            if (roomNumberFromBooking == null || roomNumberFromBooking.isEmpty()) {
                resetPayButton();
                Toast.makeText(this, "Error: Room number not found in booking.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Clean the room number: Remove "Room " prefix if present
            String cleanRoomNumber = roomNumberFromBooking.replace("Room ", "").replace("room ", "").trim();

            android.util.Log.d("PaymentActivity", "Original room number from booking: " + roomNumberFromBooking);
            android.util.Log.d("PaymentActivity", "Cleaned room number: " + cleanRoomNumber);

            // Find the room in Rooms collection by roomNumber field (using cleaned number)
            db.collection("Rooms")
                    .whereEqualTo("roomNumber", cleanRoomNumber)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            resetPayButton();
                            Toast.makeText(this, "Error: Room not found. Room number: " + cleanRoomNumber, Toast.LENGTH_LONG).show();
                            return;
                        }

                        DocumentSnapshot roomDoc = querySnapshot.getDocuments().get(0);
                        String roomDocId = roomDoc.getId();

                        // Get current occupancy and max capacity
                        Long currentOccupancyLong = roomDoc.getLong("currentOccupancy");
                        Long maxCapacityLong = roomDoc.getLong("maxCapacity");

                        int currentOccupancy = (currentOccupancyLong != null) ? currentOccupancyLong.intValue() : 0;
                        int maxCapacity = (maxCapacityLong != null) ? maxCapacityLong.intValue() : 2;

                        android.util.Log.d("PaymentActivity", "Current occupancy: " + currentOccupancy + ", Max capacity: " + maxCapacity);

                        // Check if room is available
                        if (currentOccupancy >= maxCapacity) {
                            resetPayButton();
                            Toast.makeText(this, "Sorry, this room is already fully booked! Occupancy: " + currentOccupancy + "/" + maxCapacity, Toast.LENGTH_LONG).show();
                            return;
                        }

                        int newOccupancy = currentOccupancy + 1;
                        String newRoomStatus = (newOccupancy >= maxCapacity) ? "Full" : "Available";

                        // Use WriteBatch for atomic operations
                        WriteBatch batch = db.batch();

                        // Update booking status to Paid
                        batch.update(bookingRef, "status", "Paid");
                        batch.update(bookingRef, "paymentMethod", selectedMethod);
                        batch.update(bookingRef, "paymentTimestamp", System.currentTimeMillis());

                        // Update room occupancy and status
                        DocumentReference roomRef = db.collection("Rooms").document(roomDocId);
                        batch.update(roomRef, "currentOccupancy", newOccupancy);
                        batch.update(roomRef, "status", newRoomStatus);

                        // Commit the batch
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    // Navigate to receipt activity
                                    Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);
                                    intent.putExtra("BOOKING_DOC_ID", bookingDocId);
                                    intent.putExtra("PAYMENT_METHOD", selectedMethod);
                                    intent.putExtra("ROOM_ID", roomDocId);
                                    intent.putExtra("ROOM_PRICE", tvTotalMain.getText().toString());
                                    intent.putExtra("MATRIC_NUMBER", tvDisplayMatric.getText().toString());
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    resetPayButton();
                                    Toast.makeText(PaymentActivity.this,
                                            "Payment failed: " + getErrorMessage(e), Toast.LENGTH_LONG).show();
                                    android.util.Log.e("PaymentActivity", "Payment error", e);
                                });

                    })
                    .addOnFailureListener(e -> {
                        resetPayButton();
                        Toast.makeText(this, "Failed to find room: " + getErrorMessage(e), Toast.LENGTH_LONG).show();
                        android.util.Log.e("PaymentActivity", "Room search error", e);
                    });

        }).addOnFailureListener(e -> {
            resetPayButton();
            Toast.makeText(this, "Failed to verify booking: " + getErrorMessage(e), Toast.LENGTH_LONG).show();
            android.util.Log.e("PaymentActivity", "Booking verification error", e);
        });
    }

    private void resetPayButton() {
        btnPayNow.setEnabled(true);
        btnPayNow.setText("Pay Now");
    }

    private String getErrorMessage(Exception e) {
        String error = e.getMessage();
        if (error == null || error.isEmpty()) {
            return "Network error. Please check your connection.";
        }

        if (error.contains("already been paid")) {
            return error;
        } else if (error.contains("fully booked")) {
            return error;
        } else if (error.contains("PERMISSION_DENIED")) {
            return "Permission denied. Please check your authentication.";
        } else if (error.contains("NOT_FOUND")) {
            return "Data not found in database.";
        } else if (error.contains("UNAVAILABLE")) {
            return "Service unavailable. Please try again later.";
        } else if (error.contains("DEADLINE_EXCEEDED")) {
            return "Request timeout. Please check your internet connection.";
        }

        return error;
    }

    private void updateRadioSelection(RadioButton targetRadio, String method) {
        rbDebit.setChecked(false);
        rbBank.setChecked(false);
        rbWallet.setChecked(false);
        targetRadio.setChecked(true);
        selectedMethod = method;
    }
}