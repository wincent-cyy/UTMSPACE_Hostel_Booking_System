package com.example.utmspace_hostelbookingsystem;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final String CHANNEL_ID = "payment_channel";

    private LinearLayout ivBack, btnPayNow;
    private TextView tvRoomName, tvRoomNumber, tvTotalAmount;
    private RadioGroup paymentMethodGroup;
    private RadioButton radioCard, radioFPX, radioEWallet, radioQR;

    private String bookingDocId, roomId, roomType, roomPrice;
    private String selectedPaymentMethod = "";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 检查当前用户
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginAndSignupActivity.class));
            finish();
            return;
        }

        // 打印用户信息确认
        Log.d(TAG, "User logged in: " + currentUser.getEmail());
        Log.d(TAG, "UID: " + currentUser.getUid());

        // Stripe init（记得换成你自己的 publishable key）
        PaymentConfiguration.init(
                getApplicationContext(),
                "pk_test_51TecLn2OMxhp6dH92Uznx2TfYwa9yNu4eV8rMGEeE9ifItjCLBA7mIynGIWo1CuWNT3S8edwmBv3yKwsvpDLYo7a00NwucyJPZ"
        );

        paymentSheet = new PaymentSheet(this, result -> {

            if (result instanceof PaymentSheetResult.Completed) {
                Toast.makeText(this, "Payment Success!", Toast.LENGTH_SHORT).show();
                updateBookingStatus(true);
            }

            if (result instanceof PaymentSheetResult.Canceled) {
                Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show();
            }

            if (result instanceof PaymentSheetResult.Failed) {
                Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show();
            }
        });

        initViews();
        getIntentData();
        setupPaymentMethodToggle();

        btnPayNow.setOnClickListener(v -> processPayment());
        ivBack.setOnClickListener(v -> finish());

        createNotificationChannel();
    }

    private void processPayment() {

        if (selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!selectedPaymentMethod.equals("Credit/Debit Card")) {
            Toast.makeText(this, "Only Card supported", Toast.LENGTH_SHORT).show();
            return;
        }

        startStripePayment();
    }

    private void startStripePayment() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_LONG).show();
            return;
        }

        btnPayNow.setEnabled(false);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Initializing payment...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // ✅ 使用 OkHttp 直接调用 HTTP 触发器
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 创建 JSON 请求体
        JSONObject json = new JSONObject();
        try {
            json.put("amount", getAmountInCents());
            Log.d(TAG, "Amount in cents: " + getAmountInCents());
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
            progressDialog.dismiss();
            btnPayNow.setEnabled(true);
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url("https://us-central1-utmspace-hostel-booking-system.cloudfunctions.net/createPaymentIntent")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Response: " + responseBody);

                runOnUiThread(() -> {
                    progressDialog.dismiss();

                    try {
                        JSONObject result = new JSONObject(responseBody);

                        if (result.has("clientSecret")) {
                            paymentIntentClientSecret = result.getString("clientSecret");
                            openPaymentSheet();
                        } else if (result.has("error")) {
                            String error = result.getString("error");
                            Log.e(TAG, "Payment error: " + error);
                            Toast.makeText(PaymentActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                            btnPayNow.setEnabled(true);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse error", e);
                        Toast.makeText(PaymentActivity.this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnPayNow.setEnabled(true);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    btnPayNow.setEnabled(true);
                    Toast.makeText(PaymentActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private int getAmountInCents() {
        // 从 roomPrice 解析金额
        try {
            String priceStr = roomPrice.replace("RM ", "").trim();
            double price = Double.parseDouble(priceStr);
            return (int) (price * 100);  // 转换为 cents
        } catch (NumberFormatException e) {
            return 12000;  // 默认 RM 120
        }
    }

    private void openPaymentSheet() {
        PaymentSheet.Configuration config =
                new PaymentSheet.Configuration("Hostel Booking");

        paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret,
                config
        );
    }

    private void updateBookingStatus(boolean success) {

        long currentTime = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("bookingStatus", "Paid");
        updates.put("paymentMethod", selectedPaymentMethod);
        updates.put("paymentTimestamp", currentTime);

        db.collection("Bookings").document(bookingDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {

                    increaseRoomOccupancy();

                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_LONG).show();

                    showNotification();

                    Intent intent = new Intent(this, ReceiptActivity.class);
                    intent.putExtra("BOOKING_DOC_ID", bookingDocId);
                    intent.putExtra("ROOM_ID", roomId);
                    intent.putExtra("ROOM_TYPE", roomType);
                    intent.putExtra("ROOM_PRICE", roomPrice);

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void increaseRoomOccupancy() {
        // 如果 roomId 是房间编号而不是文档 ID
        db.collection("Rooms")
                .whereEqualTo("roomId", roomId)  // 通过字段查询
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String docId = query.getDocuments().get(0).getId();
                        db.collection("Rooms").document(docId)
                                .update("currentOccupancy", FieldValue.increment(1));
                    }
                });
    }

    private void showNotification() {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Payment Successful")
                        .setContentText("Your payment is completed")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        nm.notify(1001, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Payment",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private void setupPaymentMethodToggle() {
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCard) {
                selectedPaymentMethod = "Credit/Debit Card";
            }
        });
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        radioCard = findViewById(R.id.radioCard);
        radioFPX = findViewById(R.id.radioFPX);
        radioEWallet = findViewById(R.id.radioEWallet);
        radioQR = findViewById(R.id.radioQR);

        btnPayNow = findViewById(R.id.btnPayNow);
    }

    private void getIntentData() {
        Intent intent = getIntent();

        bookingDocId = intent.getStringExtra("BOOKING_DOC_ID");
        roomId = intent.getStringExtra("ROOM_ID");
        roomType = intent.getStringExtra("ROOM_TYPE");
        roomPrice = intent.getStringExtra("ROOM_PRICE");

        tvRoomName.setText(roomType);
        tvRoomNumber.setText(roomId);
        tvTotalAmount.setText(roomPrice);
    }
}