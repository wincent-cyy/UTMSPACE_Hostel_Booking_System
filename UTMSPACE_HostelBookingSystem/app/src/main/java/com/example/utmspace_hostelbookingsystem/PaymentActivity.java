package com.example.utmspace_hostelbookingsystem;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnPayNow;
    private TextView tvTotalMain, tvInstallmentDetail, tvDisplayName, tvDisplayMatric, tvDisplayPhone;
    private TextView tvFullPaymentAmount, tvInstallment3Amount, tvInstallment6Amount;

    private RadioGroup radioGroupPayment, radioGroupInstallment;
    private RadioButton rbCard, rbBank, rbWallet;
    private RadioButton rbFullPayment, rbInstallment3, rbInstallment6;

    private MaterialCardView cardDebit, cardBank, cardWallet;

    private String bookingDocId, roomId, roomType;
    private String selectedMethod = "";
    private String selectedInstallment = "Full";
    private double originalPrice = 0;
    private double finalPrice = 0;

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
        setupListeners();
        setupInstallmentOptions();
        setupCardClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPayNow = findViewById(R.id.btnPayNow);
        tvTotalMain = findViewById(R.id.tvTotalMain);
        tvInstallmentDetail = findViewById(R.id.tvInstallmentDetail);

        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvDisplayMatric = findViewById(R.id.tvDisplayMatric);
        tvDisplayPhone = findViewById(R.id.tvDisplayPhone);

        tvFullPaymentAmount = findViewById(R.id.tvFullPaymentAmount);
        tvInstallment3Amount = findViewById(R.id.tvInstallment3Amount);
        tvInstallment6Amount = findViewById(R.id.tvInstallment6Amount);

        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        radioGroupInstallment = findViewById(R.id.radioGroupInstallment);

        rbCard = findViewById(R.id.rbCard);
        rbBank = findViewById(R.id.rbBank);
        rbWallet = findViewById(R.id.rbWallet);

        rbFullPayment = findViewById(R.id.rbFullPayment);
        rbInstallment3 = findViewById(R.id.rbInstallment3);
        rbInstallment6 = findViewById(R.id.rbInstallment6);

        cardDebit = findViewById(R.id.cardDebit);
        cardBank = findViewById(R.id.cardBank);
        cardWallet = findViewById(R.id.cardWallet);

        if (rbFullPayment != null) {
            rbFullPayment.setChecked(true);
        }
    }

    private void setupCardClickListeners() {
        if (cardDebit != null) cardDebit.setOnClickListener(v -> rbCard.performClick());
        if (cardBank != null) cardBank.setOnClickListener(v -> rbBank.performClick());
        if (cardWallet != null) cardWallet.setOnClickListener(v -> rbWallet.performClick());
    }

    private void setupInstallmentOptions() {
        if (radioGroupInstallment != null) {
            radioGroupInstallment.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbFullPayment) {
                    selectedInstallment = "Full";
                    finalPrice = originalPrice;
                    updatePriceDisplay();
                } else if (checkedId == R.id.rbInstallment3) {
                    selectedInstallment = "3 Months";
                    finalPrice = originalPrice;
                    updatePriceDisplay();
                } else if (checkedId == R.id.rbInstallment6) {
                    selectedInstallment = "6 Months";
                    finalPrice = originalPrice;
                    updatePriceDisplay();
                }
            });
        }
    }

    private void updatePriceDisplay() {
        // 更新总价显示
        tvTotalMain.setText(String.format("RM %.2f", finalPrice));

        // 更新分期详情显示
        if ("Full".equals(selectedInstallment)) {
            tvInstallmentDetail.setVisibility(View.GONE);
        } else {
            int months = Integer.parseInt(selectedInstallment.split(" ")[0]);
            double monthlyPayment = finalPrice / months;
            tvInstallmentDetail.setVisibility(View.VISIBLE);
            tvInstallmentDetail.setText(String.format("(%d months x RM %.2f)", months, monthlyPayment));
        }

        // 更新选项金额显示
        tvFullPaymentAmount.setText(String.format("Total: RM %.2f", originalPrice));
        tvInstallment3Amount.setText(String.format("RM %.2f / month (Total: RM %.2f)", originalPrice / 3, originalPrice));
        tvInstallment6Amount.setText(String.format("RM %.2f / month (Total: RM %.2f)", originalPrice / 6, originalPrice));
    }

    private void fetchUserInfo() {
        if (bookingDocId != null && !bookingDocId.isEmpty()) {
            db.collection("Bookings").document(bookingDocId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    String name = doc.getString("name");
                    String matric = doc.getString("matricNumber");
                    String phone = doc.getString("phone");

                    if (name != null) tvDisplayName.setText(name);
                    if (matric != null) tvDisplayMatric.setText(matric);
                    if (phone != null) tvDisplayPhone.setText(phone);
                }
            });
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            bookingDocId = intent.getStringExtra("BOOKING_DOC_ID");
            roomId = intent.getStringExtra("ROOM_ID");
            roomType = intent.getStringExtra("ROOM_TYPE");
            String priceStr = intent.getStringExtra("ROOM_PRICE");

            if (priceStr != null) {
                String numericPrice = priceStr.replaceAll("[^0-9.]", "");
                try {
                    originalPrice = Double.parseDouble(numericPrice);
                    finalPrice = originalPrice;
                    tvTotalMain.setText(String.format("RM %.2f", finalPrice));
                    updatePriceDisplay();
                } catch (NumberFormatException e) {
                    tvTotalMain.setText(priceStr);
                }
            } else {
                tvTotalMain.setText("RM 0.00");
            }
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // 付款方式互斥逻辑
        rbCard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedMethod = "Credit / Debit Card";
                rbBank.setChecked(false);
                rbWallet.setChecked(false);
            }
        });

        rbBank.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedMethod = "Online Banking (FPX)";
                rbCard.setChecked(false);
                rbWallet.setChecked(false);
            }
        });

        rbWallet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedMethod = "E-Wallet";
                rbCard.setChecked(false);
                rbBank.setChecked(false);
            }
        });

        // 分期计划互斥逻辑
        rbFullPayment.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedInstallment = "Full";
                finalPrice = originalPrice;
                updatePriceDisplay();
                rbInstallment3.setChecked(false);
                rbInstallment6.setChecked(false);
            }
        });

        rbInstallment3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedInstallment = "3 Months";
                finalPrice = originalPrice;
                updatePriceDisplay();
                rbFullPayment.setChecked(false);
                rbInstallment6.setChecked(false);
            }
        });

        rbInstallment6.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedInstallment = "6 Months";
                finalPrice = originalPrice;
                updatePriceDisplay();
                rbFullPayment.setChecked(false);
                rbInstallment3.setChecked(false);
            }
        });

        btnPayNow.setOnClickListener(v -> {
            if (selectedMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method.", Toast.LENGTH_SHORT).show();
            } else if (bookingDocId != null && roomId != null) {
                showPaymentConfirmationDialog();
            }
        });
    }

    private void showPaymentConfirmationDialog() {
        String installmentInfo = "";
        if (!"Full".equals(selectedInstallment)) {
            int months = Integer.parseInt(selectedInstallment.split(" ")[0]);
            double monthlyPayment = finalPrice / months;
            installmentInfo = String.format("\n\nPayment Plan: %s\nMonthly: RM %.2f", selectedInstallment, monthlyPayment);
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setMessage(String.format(
                        "Payment Details:\n" +
                                "Method: %s\n" +
                                "Amount: RM %.2f%s\n\n" +
                                "This is a demo payment. No actual charge will be made.",
                        selectedMethod, finalPrice, installmentInfo))
                .setPositiveButton("Confirm Payment", (dialog, which) -> processPayment())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processPayment() {
        btnPayNow.setEnabled(false);
        btnPayNow.setText("Processing...");

        DocumentReference bookingRef = db.collection("Bookings").document(bookingDocId);

        bookingRef.get().addOnSuccessListener(bookingDoc -> {
            if (!bookingDoc.exists()) {
                resetPayButton();
                Toast.makeText(this, "Error: Booking not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            String roomIdFromBooking = bookingDoc.getString("roomId");

            db.collection("Rooms")
                    .whereEqualTo("roomId", roomIdFromBooking)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            resetPayButton();
                            Toast.makeText(this, "Error: Room not found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentSnapshot roomDoc = querySnapshot.getDocuments().get(0);
                        String roomDocId = roomDoc.getId();

                        Long currentOccupancyLong = roomDoc.getLong("currentOccupancy");
                        Long maxCapacityLong = roomDoc.getLong("maxCapacity");

                        int currentOccupancy = (currentOccupancyLong != null) ? currentOccupancyLong.intValue() : 0;
                        int maxCapacity = (maxCapacityLong != null) ? maxCapacityLong.intValue() : 4;

                        if (currentOccupancy >= maxCapacity) {
                            resetPayButton();
                            Toast.makeText(this, "Sorry, this room is already fully booked!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        int newOccupancy = currentOccupancy + 1;
                        String newRoomStatus = (newOccupancy >= maxCapacity) ? "Full" : "Available";

                        WriteBatch batch = db.batch();

                        batch.update(bookingRef, "bookingStatus", "Paid");
                        batch.update(bookingRef, "paymentMethod", selectedMethod);
                        batch.update(bookingRef, "installmentPlan", selectedInstallment);
                        batch.update(bookingRef, "amountPaid", finalPrice);
                        batch.update(bookingRef, "paymentTimestamp", System.currentTimeMillis());

                        DocumentReference roomRef = db.collection("Rooms").document(roomDocId);
                        batch.update(roomRef, "currentOccupancy", newOccupancy);
                        batch.update(roomRef, "status", newRoomStatus);

                        // ✅ 只有一个 batch.commit()
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    showLocalNotification(bookingDoc.getString("uid"));
                                    // 发送支付成功通知
                                    sendPaymentSuccessNotification(bookingDoc.getString("uid"));
                                    showPaymentSuccessDialog();
                                })
                                .addOnFailureListener(e -> {
                                    resetPayButton();
                                    Toast.makeText(PaymentActivity.this,
                                            "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        resetPayButton();
                        Toast.makeText(this, "Failed to find room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            resetPayButton();
            Toast.makeText(this, "Failed to verify booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void sendPaymentSuccessNotification(String userId) {
        // ✅ 添加开关检查
        SharedPreferences prefs = getSharedPreferences("BioAuthPrefs", MODE_PRIVATE);
        boolean isNotificationEnabled = prefs.getBoolean("NotificationEnabled_" + userId, false);

        if (!isNotificationEnabled) {
            return; // 用户关闭了通知，不发送
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "Payment Successful");
        notification.put("message", "Your payment has been processed successfully. Receipt is ready.");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("Notifications").add(notification);
    }

    private void showPaymentSuccessDialog() {
        String installmentInfo = "";
        if (!"Full".equals(selectedInstallment)) {
            int months = Integer.parseInt(selectedInstallment.split(" ")[0]);
            double monthlyPayment = finalPrice / months;
            installmentInfo = String.format("\nPayment Plan: %s\nMonthly: RM %.2f", selectedInstallment, monthlyPayment);
        }

        new AlertDialog.Builder(this)
                .setTitle("Payment Successful!")
                .setMessage(String.format(
                        "Your payment has been processed successfully.\n\n" +
                                "Amount: RM %.2f\n" +
                                "Payment Method: %s%s",
                        finalPrice, selectedMethod, installmentInfo))
                .setPositiveButton("View Receipt", (dialog, which) -> {
                    Intent intent = new Intent(PaymentActivity.this, ReceiptActivity.class);
                    intent.putExtra("BOOKING_DOC_ID", bookingDocId);
                    intent.putExtra("PAYMENT_METHOD", selectedMethod);
                    intent.putExtra("ROOM_ID", roomId);
                    intent.putExtra("AMOUNT_PAID", finalPrice);
                    intent.putExtra("INSTALLMENT_PLAN", selectedInstallment);
                    intent.putExtra("MATRIC_NUMBER", tvDisplayMatric.getText().toString());
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showLocalNotification(String userId) {
        // 检查通知开关
        SharedPreferences prefs = getSharedPreferences("BioAuthPrefs", MODE_PRIVATE);
        boolean isNotificationEnabled = prefs.getBoolean("NotificationEnabled_" + userId, false);

        if (!isNotificationEnabled) {
            return; // 用户关闭了通知，不弹出
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "utm_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Payment Successful")
                .setContentText("Your payment has been completed successfully.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1001, builder.build());
    }

    private void resetPayButton() {
        btnPayNow.setEnabled(true);
        btnPayNow.setText("Confirm and Pay");
    }
}