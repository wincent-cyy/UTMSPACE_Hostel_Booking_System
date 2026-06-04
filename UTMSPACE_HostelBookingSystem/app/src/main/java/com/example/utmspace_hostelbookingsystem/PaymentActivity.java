package com.example.utmspace_hostelbookingsystem;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "payment_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Header
    private LinearLayout ivBack;

    // Order Summary
    private TextView tvRoomName;
    private TextView tvRoomNumber;
    private TextView tvDuration;
    private TextView tvTotalAmount;

    // Payment Method
    private RadioGroup paymentMethodGroup;
    private RadioButton radioCard, radioFPX, radioEWallet, radioQR;

    // Card Details Section
    private LinearLayout cardDetailsSection;
    private TextInputEditText etCardNumber;
    private TextInputEditText etExpiryDate;
    private TextInputEditText etCVV;
    private TextInputEditText etCardHolderName;

    // FPX Section
    private LinearLayout fpxSection;
    private Spinner spinnerBank;

    // E-Wallet Section
    private LinearLayout ewalletSection;
    private LinearLayout btnTouchNGo;
    private TextView tvSelectedWallet;

    // QR Section
    private LinearLayout qrSection;

    // Pay Button
    private LinearLayout btnPayNow;

    // Data
    private String bookingDocId;
    private String roomId;
    private String roomType;
    private String roomPrice;
    private String studentName;
    private String matricNumber;
    private String phoneNumber;
    private String checkInDate;
    private String leaseDuration;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Selected payment method
    private String selectedPaymentMethod = "";
    private String selectedWallet = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        // 创建通知渠道
        createNotificationChannel();

        initViews();
        getIntentData();
        setupPaymentMethodToggle();
        setupWalletSelection();
        setupClickListeners();
    }

    private void initViews() {
        // Header
        ivBack = findViewById(R.id.ivBack);

        // Order Summary
        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvDuration = findViewById(R.id.tvDuration);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

        // Payment Method
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        radioCard = findViewById(R.id.radioCard);
        radioFPX = findViewById(R.id.radioFPX);
        radioEWallet = findViewById(R.id.radioEWallet);
        radioQR = findViewById(R.id.radioQR);

        // Card Details
        cardDetailsSection = findViewById(R.id.cardDetailsSection);
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        etCVV = findViewById(R.id.etCVV);
        etCardHolderName = findViewById(R.id.etCardHolderName);

        // FPX
        fpxSection = findViewById(R.id.fpxSection);
        spinnerBank = findViewById(R.id.spinnerBank);

        // E-Wallet
        ewalletSection = findViewById(R.id.ewalletSection);
        btnTouchNGo = findViewById(R.id.btnTouchNGo);
        tvSelectedWallet = findViewById(R.id.tvSelectedWallet);

        // QR
        qrSection = findViewById(R.id.qrSection);

        // Pay Button
        btnPayNow = findViewById(R.id.btnPayNow);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            bookingDocId = intent.getStringExtra("BOOKING_DOC_ID");
            roomId = intent.getStringExtra("ROOM_ID");
            roomType = intent.getStringExtra("ROOM_TYPE");
            roomPrice = intent.getStringExtra("ROOM_PRICE");
            studentName = intent.getStringExtra("STUDENT_NAME");
            matricNumber = intent.getStringExtra("MATRIC_NUMBER");
            phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            checkInDate = intent.getStringExtra("CHECK_IN_DATE");
            leaseDuration = intent.getStringExtra("LEASE_DURATION");

            // Display order summary
            tvRoomName.setText(roomType != null ? roomType : "N/A");
            tvRoomNumber.setText(roomId != null ? roomId : "N/A");
            tvDuration.setText(leaseDuration != null ? leaseDuration : "1 Semester");
            tvTotalAmount.setText(roomPrice != null ? roomPrice : "RM 0");
        }
    }

    private void setupPaymentMethodToggle() {
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Hide all sections first
            cardDetailsSection.setVisibility(View.GONE);
            fpxSection.setVisibility(View.GONE);
            ewalletSection.setVisibility(View.GONE);
            qrSection.setVisibility(View.GONE);

            if (checkedId == R.id.radioCard) {
                selectedPaymentMethod = "Credit/Debit Card";
                cardDetailsSection.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radioFPX) {
                selectedPaymentMethod = "FPX (Online Banking)";
                fpxSection.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radioEWallet) {
                selectedPaymentMethod = "E-Wallet";
                ewalletSection.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radioQR) {
                selectedPaymentMethod = "QR Code (DuitNow)";
                qrSection.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupWalletSelection() {
        btnTouchNGo.setOnClickListener(v -> {
            selectedWallet = "Touch 'n Go";
            tvSelectedWallet.setText("Selected: Touch 'n Go");
            tvSelectedWallet.setVisibility(View.VISIBLE);
        });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnPayNow.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        // Validate payment method selection
        if (selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate based on selected method
        if (selectedPaymentMethod.equals("Credit/Debit Card")) {
            if (!validateCardDetails()) {
                return;
            }
        } else if (selectedPaymentMethod.equals("E-Wallet") && selectedWallet.isEmpty()) {
            Toast.makeText(this, "Please select an e-wallet", Toast.LENGTH_SHORT).show();
            return;
        }

        // 模拟支付
        simulatePayment();
    }

    /**
     * 模拟支付功能
     */
    private void simulatePayment() {
        // 创建模拟支付对话框
        AlertDialog paymentDialog = new AlertDialog.Builder(this)
                .setTitle("Processing Payment")
                .setMessage("Please wait while we process your payment...")
                .setCancelable(false)
                .create();
        paymentDialog.show();

        // 模拟网络延迟（2秒）
        new Handler().postDelayed(() -> {
            paymentDialog.dismiss();

            // 随机生成支付结果（80%成功，20%失败）
            boolean isSuccess = Math.random() < 0.8;

            if (isSuccess) {
                // 支付成功
                updateBookingStatus(true);
            } else {
                // 支付失败
                showPaymentFailedDialog();
            }
        }, 2000);
    }

    /**
     * 显示支付失败对话框
     */
    private void showPaymentFailedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Payment Failed")
                .setMessage("Your payment could not be processed. Please try again or use another payment method.")
                .setPositiveButton("Try Again", (dialog, which) -> {
                    // 重试
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * 创建通知渠道（Android 8.0+ 必需）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Payment Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Payment success notifications");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 显示支付成功通知（普通弹窗，不跳转）
     */
    private void showPaymentSuccessNotification() {
        // 创建空的 PendingIntent（点击通知不跳转，只是关闭通知）
        Intent emptyIntent = new Intent();
        emptyIntent.setAction(Intent.ACTION_MAIN);
        emptyIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                emptyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ Payment Successful")
                .setContentText("Your payment for " + roomType + " has been processed successfully.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your payment for " + roomType + " (Room: " + roomId + ") has been processed successfully. Amount: " + roomPrice))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private boolean validateCardDetails() {
        String cardNumber = etCardNumber.getText().toString().trim();
        String expiryDate = etExpiryDate.getText().toString().trim();
        String cvv = etCVV.getText().toString().trim();
        String cardHolderName = etCardHolderName.getText().toString().trim();

        if (cardNumber.isEmpty()) {
            etCardNumber.setError("Card number required");
            return false;
        }
        if (cardNumber.length() < 16) {
            etCardNumber.setError("Valid card number required (16 digits)");
            return false;
        }
        if (expiryDate.isEmpty()) {
            etExpiryDate.setError("Expiry date required");
            return false;
        }
        if (!expiryDate.matches("\\d{2}/\\d{2}")) {
            etExpiryDate.setError("Valid expiry date (MM/YY) required");
            return false;
        }
        if (cvv.isEmpty()) {
            etCVV.setError("CVV required");
            return false;
        }
        if (cvv.length() < 3) {
            etCVV.setError("Valid CVV required (3-4 digits)");
            return false;
        }
        if (cardHolderName.isEmpty()) {
            etCardHolderName.setError("Cardholder name required");
            return false;
        }
        return true;
    }

    /**
     * 更新房间的当前入住人数 +1
     */
    private void updateRoomOccupancy() {
        if (roomId == null || roomId.isEmpty()) {
            return;
        }

        db.collection("Rooms").document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Integer currentOccupancy = documentSnapshot.getLong("currentOccupancy") != null
                                ? documentSnapshot.getLong("currentOccupancy").intValue() : 0;
                        int maxCapacity = documentSnapshot.getLong("maxCapacity") != null
                                ? documentSnapshot.getLong("maxCapacity").intValue() : 1;

                        int newOccupancy = currentOccupancy + 1;

                        // 确保不超过最大容量
                        if (newOccupancy <= maxCapacity) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("currentOccupancy", newOccupancy);

                            db.collection("Rooms").document(roomId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(PaymentActivity.this, "Room occupancy updated", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(PaymentActivity.this, "Failed to update occupancy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // 静默失败
                });
    }

    private void updateBookingStatus(boolean isSuccess) {
        if (bookingDocId == null || bookingDocId.isEmpty()) {
            Toast.makeText(this, "Error: Booking ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Parse the total amount
        double amountPaid = 0;
        String amountText = tvTotalAmount.getText().toString().replace("RM ", "").trim();
        try {
            amountPaid = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            amountPaid = 0;
        }

        final double finalAmountPaid = amountPaid;
        final String finalPaymentMethod = selectedPaymentMethod;

        Map<String, Object> updates = new HashMap<>();
        updates.put("bookingStatus", "Paid");
        updates.put("paymentMethod", selectedPaymentMethod);
        updates.put("paymentTimestamp", currentTime);
        updates.put("amountPaid", amountPaid);

        db.collection("Bookings").document(bookingDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 显示成功 Toast
                    Toast.makeText(PaymentActivity.this, "Payment successful!", Toast.LENGTH_LONG).show();

                    // 更新房间入住人数 +1
                    updateRoomOccupancy();

                    // 显示系统通知（不跳转）
                    showPaymentSuccessNotification();

                    // 直接跳转到 Receipt 页面
                    Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);
                    intent.putExtra("BOOKING_DOC_ID", bookingDocId);
                    intent.putExtra("ROOM_ID", roomId);
                    intent.putExtra("ROOM_TYPE", roomType);
                    intent.putExtra("ROOM_PRICE", roomPrice);
                    intent.putExtra("STUDENT_NAME", studentName);
                    intent.putExtra("MATRIC_NUMBER", matricNumber);
                    intent.putExtra("PHONE_NUMBER", phoneNumber);
                    intent.putExtra("CHECK_IN_DATE", checkInDate);
                    intent.putExtra("LEASE_DURATION", leaseDuration);
                    intent.putExtra("PAYMENT_METHOD", finalPaymentMethod);
                    intent.putExtra("AMOUNT_PAID", finalAmountPaid);
                    intent.putExtra("PAYMENT_TIMESTAMP", currentTime);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PaymentActivity.this, "Payment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}