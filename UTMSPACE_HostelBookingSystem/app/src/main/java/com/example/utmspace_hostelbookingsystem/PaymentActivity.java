package com.example.utmspace_hostelbookingsystem;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final String CHANNEL_ID = "payment_channel";

    // Stripe 密钥
    private static final String STRIPE_PK_TEST = "pk_test_51TecLn2OMxhp6dH92Uznx2TfYwa9yNu4eV8rMGEeE9ifItjCLBA7mIynGIWo1CuWNT3S8edwmBv3yKwsvpDLYo7a00NwucyJPZ";
    private static final String STRIPE_PK_LIVE = "pk_live_51TecLKRrulifJ7fOSKUyFcYMQsKS0x8MFMMyyQFnEo6Lp2POgoHphZWMTS8SDEDomAbt8ptwO9WiNKNBCmAvsFeI00AxD2TUSF";

    private static final boolean TEST_GRABPAY_MODE = true;

    private LinearLayout ivBack, btnPayNow;
    private TextView tvRoomName, tvRoomNumber, tvTotalAmount;
    private RadioGroup paymentMethodGroup;
    private RadioButton radioCard, radioFPX, radioEWallet;

    private String bookingDocId, roomId, roomType, roomPrice;
    private String studentName, phoneNumber;
    private String selectedPaymentMethod = "";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;

    private String currentFpxBankCode = "";
    private boolean isPaymentSheetInitialized = false;

    // 保存当前模式，用于 PaymentSheet 回调
    private String currentPaymentMode = "test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginAndSignupActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "User logged in: " + currentUser.getEmail());
        Log.d(TAG, "UID: " + currentUser.getUid());

        // 初始化测试模式
        PaymentConfiguration.init(getApplicationContext(), STRIPE_PK_TEST);
        initPaymentSheet();

        initViews();
        getIntentData();
        setupPaymentMethodToggle();

        btnPayNow.setOnClickListener(v -> processPayment());
        ivBack.setOnClickListener(v -> finish());

        createNotificationChannel();
    }

    private void initPaymentSheet() {
        if (!isPaymentSheetInitialized) {
            try {
                paymentSheet = new PaymentSheet(this, result -> {
                    if (result instanceof PaymentSheetResult.Completed) {
                        Log.d(TAG, "PaymentSheet: Completed successfully");
                        updateBookingStatus(true);
                    } else if (result instanceof PaymentSheetResult.Canceled) {
                        Log.d(TAG, "PaymentSheet: Cancelled by user");
                        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show();
                        btnPayNow.setEnabled(true);
                    } else if (result instanceof PaymentSheetResult.Failed) {
                        PaymentSheetResult.Failed failed = (PaymentSheetResult.Failed) result;
                        Log.e(TAG, "PaymentSheet Failed: " + failed.getError().getMessage());
                        Toast.makeText(this, "Payment Failed: " + failed.getError().getMessage(), Toast.LENGTH_LONG).show();
                        btnPayNow.setEnabled(true);
                    }
                });
                isPaymentSheetInitialized = true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize PaymentSheet: " + e.getMessage());
                Toast.makeText(this, "Payment system error. Please try again.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void reconfigureStripe(String publishableKey) {
        PaymentConfiguration.init(getApplicationContext(), publishableKey);
    }

    private void processPayment() {
        if (selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPaymentMethod.equals("Credit/Debit Card")) {
            currentPaymentMode = "test";
            reconfigureStripe(STRIPE_PK_TEST);
            startCardPayment();
        } else if (selectedPaymentMethod.equals("FPX (Online Banking)")) {
            currentPaymentMode = "test";
            reconfigureStripe(STRIPE_PK_TEST);
            showFpxBankSelectionDialog();
        } else if (selectedPaymentMethod.equals("Grab Pay")) {
            currentPaymentMode = "live";
            reconfigureStripe(STRIPE_PK_LIVE);
            startLiveTouchNGoPayment();
        } else {
            Toast.makeText(this, "This payment method is coming soon", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== 信用卡支付（测试模式） ====================

    private void startCardPayment() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_LONG).show();
            return;
        }

        btnPayNow.setEnabled(false);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Initializing payment...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        JSONObject json = new JSONObject();
        try {
            json.put("amount", getAmountInCents());
            json.put("paymentMethodType", "card");
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
                .url("https://us-central1-utmspace-hostel-booking-system.cloudfunctions.net/createPaymentIntentTest")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Card Payment Response: " + responseBody);

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

    // ==================== FPX 支付（测试模式） ====================

    private void showFpxBankSelectionDialog() {
        BankInfo[] banks = {
                new BankInfo("Maybank", "maybank2u", R.drawable.ic_maybank),
                new BankInfo("CIMB Clicks", "cimb_clicks", R.drawable.ic_cimb),
                new BankInfo("Public Bank", "public_bank", R.drawable.ic_public_bank),
                new BankInfo("RHB Bank", "rhb", R.drawable.ic_rhb),
                new BankInfo("Hong Leong Bank", "hong_leong_bank", R.drawable.ic_hong_leong),
                new BankInfo("AmBank", "ambank", R.drawable.ic_ambank),
                new BankInfo("HSBC", "hsbc", R.drawable.ic_hsbc),
                new BankInfo("Affin Bank", "affin_bank", R.drawable.ic_affin)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_bank_selector, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        LinearLayout bankContainer = dialogView.findViewById(R.id.bankContainer);

        for (BankInfo bank : banks) {
            View bankItem = getLayoutInflater().inflate(R.layout.item_bank, null);

            ImageView ivBankIcon = bankItem.findViewById(R.id.ivBankIcon);
            TextView tvBankName = bankItem.findViewById(R.id.tvBankName);
            TextView tvBankDesc = bankItem.findViewById(R.id.tvBankDesc);

            if (bank.iconRes != 0) {
                ivBankIcon.setImageResource(bank.iconRes);
            } else {
                ivBankIcon.setImageResource(R.drawable.ic_bank_placeholder);
            }

            tvBankName.setText(bank.name);
            tvBankDesc.setText("Online Banking");

            bankItem.setOnClickListener(v -> {
                dialog.dismiss();
                currentFpxBankCode = bank.code;
                startFpxPayment();
            });

            bankContainer.addView(bankItem);
        }

        dialog.show();
    }

    private void startFpxPayment() {
        btnPayNow.setEnabled(false);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Initializing FPX payment...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        JSONObject json = new JSONObject();
        try {
            json.put("amount", getAmountInCents());
            json.put("paymentMethodType", "fpx");
            json.put("fpxBank", currentFpxBankCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url("https://us-central1-utmspace-hostel-booking-system.cloudfunctions.net/createPaymentIntentTest")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "FPX Create Response: " + responseBody);

                try {
                    JSONObject result = new JSONObject(responseBody);
                    String clientSecret = result.getString("clientSecret");
                    String paymentIntentId = result.getString("paymentIntentId");

                    createFpxPaymentMethod(paymentIntentId, clientSecret, progressDialog);

                } catch (JSONException e) {
                    handleError(progressDialog, "Parse error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                handleError(progressDialog, "Network error: " + e.getMessage());
            }
        });
    }

    private void createFpxPaymentMethod(String paymentIntentId,
                                        String clientSecret, ProgressDialog progressDialog) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        RequestBody body = new FormBody.Builder()
                .add("type", "fpx")
                .add("fpx[bank]", currentFpxBankCode)
                .build();

        Request request = new Request.Builder()
                .url("https://api.stripe.com/v1/payment_methods")
                .addHeader("Authorization", "Bearer sk_test_51TecLn2OMxhp6dH9yzzh5IfIRHFh2lbNHHBMwtYMl9tFse2Cui7eM7SZRS4mEB5NdvIUDj5VWizcZGI9kLBLjhEA00PjHp6Uof")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "PaymentMethod Response: " + responseBody);

                try {
                    JSONObject result = new JSONObject(responseBody);
                    String paymentMethodId = result.getString("id");

                    confirmFpxPayment(paymentIntentId, paymentMethodId, progressDialog);

                } catch (JSONException e) {
                    handleError(progressDialog, "Failed to create payment method");
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                handleError(progressDialog, "Network error: " + e.getMessage());
            }
        });
    }

    private void confirmFpxPayment(String paymentIntentId, String paymentMethodId,
                                   ProgressDialog progressDialog) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        JSONObject json = new JSONObject();
        try {
            json.put("paymentIntentId", paymentIntentId);
            json.put("paymentMethodId", paymentMethodId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url("https://us-central1-utmspace-hostel-booking-system.cloudfunctions.net/confirmFpxPayment")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Confirm Response: " + responseBody);

                try {
                    JSONObject result = new JSONObject(responseBody);
                    String redirectUrl = result.optString("redirectUrl");

                    if (!redirectUrl.isEmpty()) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            openFpxWebView(redirectUrl, paymentIntentId);
                        });
                    } else {
                        handleError(progressDialog, "No redirect URL received");
                    }

                } catch (JSONException e) {
                    handleError(progressDialog, "Confirm error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                handleError(progressDialog, "Network error: " + e.getMessage());
            }
        });
    }

    private void openFpxWebView(String url, String paymentIntentId) {
        Intent intent = new Intent(this, FpxWebViewActivity.class);
        intent.putExtra("URL", url);
        intent.putExtra("PAYMENT_INTENT_ID", paymentIntentId);
        intent.putExtra("BOOKING_DOC_ID", bookingDocId);
        intent.putExtra("ROOM_ID", roomId);
        intent.putExtra("ROOM_TYPE", roomType);
        intent.putExtra("ROOM_PRICE", roomPrice);
        intent.putExtra("STUDENT_NAME", studentName);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        startActivityForResult(intent, 1001);  // 使用 requestCode = 1001
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // FPX 回调 (requestCode = 1001)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            updateBookingStatus(true);
        }
        // GrabPay 回调 (requestCode = 1002)
        else if (requestCode == 1002 && resultCode == RESULT_OK) {
            updateBookingStatus(true);
        }
        else if (requestCode == 1002 && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show();
            btnPayNow.setEnabled(true);
        }
    }

    // ==================== GrabPay 支付（使用 Checkout Session + WebView） ====================

    private void startLiveTouchNGoPayment() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_LONG).show();
            return;
        }

        btnPayNow.setEnabled(false);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Initializing GrabPay...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        JSONObject json = new JSONObject();
        try {
            int amount;
            if (TEST_GRABPAY_MODE) {
                amount = 100;  // RM 1.00 测试
                Log.d(TAG, "TEST MODE - Using RM 1.00 for GrabPay");
            } else {
                amount = getAmountInCents();
                Log.d(TAG, "PRODUCTION MODE - Amount in cents: " + amount);
            }
            json.put("amount", amount);
            json.put("successUrl", "https://yourdomain.com/success");
            json.put("cancelUrl", "https://yourdomain.com/cancel");
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

        // ✅ 使用新的 Checkout Session 函数
        Request request = new Request.Builder()
                .url("https://us-central1-utmspace-hostel-booking-system.cloudfunctions.net/createGrabPayCheckout")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "GrabPay Checkout Response: " + responseBody);

                try {
                    JSONObject result = new JSONObject(responseBody);

                    if (result.has("checkoutUrl")) {
                        String checkoutUrl = result.getString("checkoutUrl");
                        Log.d(TAG, "Checkout URL: " + checkoutUrl);

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            openGrabPayWebView(checkoutUrl);
                        });
                    } else if (result.has("error")) {
                        String error = result.getString("error");
                        Log.e(TAG, "Payment error: " + error);
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(PaymentActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                            btnPayNow.setEnabled(true);
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parse error", e);
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(PaymentActivity.this, "Parse error", Toast.LENGTH_SHORT).show();
                        btnPayNow.setEnabled(true);
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(PaymentActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnPayNow.setEnabled(true);
                });
            }
        });
    }

    private void openGrabPayWebView(String url) {
        Intent intent = new Intent(this, GrabPayWebViewActivity.class);
        intent.putExtra("PAYMENT_URL", url);
        startActivityForResult(intent, 1002);
    }

    private void openGrabPayWebView(String url, String clientSecret) {
        Intent intent = new Intent(this, GrabPayWebViewActivity.class);
        intent.putExtra("PAYMENT_URL", url);
        intent.putExtra("CLIENT_SECRET", clientSecret);
        startActivityForResult(intent, 1002);
    }

    // ==================== 通用方法 ====================

    private int getAmountInCents() {
        try {
            String priceStr = roomPrice.replace("RM ", "").trim();
            double price = Double.parseDouble(priceStr);
            return (int) (price * 100);
        } catch (NumberFormatException e) {
            return 12000;
        }
    }

    private void openPaymentSheet() {
        // 不需要手动指定 paymentMethodTypes
        // Stripe 会自动根据 PaymentIntent 中设置的 payment_method_types 来显示支付方式
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Hostel Booking")
                .build();

        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
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
                    intent.putExtra("STUDENT_NAME", studentName);
                    intent.putExtra("PHONE_NUMBER", phoneNumber);
                    intent.putExtra("PAYMENT_METHOD", selectedPaymentMethod);
                    intent.putExtra("AMOUNT_PAID", getAmountInCents() / 100.0);
                    intent.putExtra("PAYMENT_TIMESTAMP", currentTime);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPayNow.setEnabled(true);
                });
    }

    private void increaseRoomOccupancy() {
        db.collection("Rooms")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;

                    String docId = query.getDocuments().get(0).getId();
                    DocumentReference roomRef = db.collection("Rooms").document(docId);

                    db.runTransaction(transaction -> {
                        DocumentSnapshot snapshot = transaction.get(roomRef);

                        int currentOccupancy = snapshot.getLong("currentOccupancy") != null ?
                                snapshot.getLong("currentOccupancy").intValue() : 0;
                        int maxCapacity = snapshot.getLong("maxCapacity") != null ?
                                snapshot.getLong("maxCapacity").intValue() : 1;

                        int newOccupancy = currentOccupancy + 1;
                        boolean shouldBeFull = newOccupancy >= maxCapacity;

                        // 用 Toast 显示判断结果
                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "Current: " + currentOccupancy +
                                            ", Max: " + maxCapacity +
                                            ", New: " + newOccupancy +
                                            ", Should be Full: " + shouldBeFull,
                                    Toast.LENGTH_LONG).show();
                        });

                        transaction.update(roomRef, "currentOccupancy", newOccupancy);

                        if (shouldBeFull) {
                            transaction.update(roomRef, "status", "Full");
                            runOnUiThread(() -> Toast.makeText(this, "Setting status to FULL", Toast.LENGTH_SHORT).show());
                        } else {
                            transaction.update(roomRef, "status", "Available");
                            runOnUiThread(() -> Toast.makeText(this, "Setting status to Available", Toast.LENGTH_SHORT).show());
                        }

                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        runOnUiThread(() -> Toast.makeText(this, "Transaction SUCCESS", Toast.LENGTH_SHORT).show());
                    }).addOnFailureListener(e -> {
                        runOnUiThread(() -> Toast.makeText(this, "Transaction FAILED: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    });
                });
    }

    private void handleError(ProgressDialog dialog, String message) {
        runOnUiThread(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            btnPayNow.setEnabled(true);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Payment Successful")
                .setContentText("Your payment is completed")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1001, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Payment", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private void setupPaymentMethodToggle() {
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCard) {
                selectedPaymentMethod = "Credit/Debit Card";
            } else if (checkedId == R.id.radioFPX) {
                selectedPaymentMethod = "FPX (Online Banking)";
            } else if (checkedId == R.id.radioEWallet) {
                selectedPaymentMethod = "Grab Pay";
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
        btnPayNow = findViewById(R.id.btnPayNow);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        bookingDocId = intent.getStringExtra("BOOKING_DOC_ID");
        roomId = intent.getStringExtra("ROOM_ID");
        roomType = intent.getStringExtra("ROOM_TYPE");
        roomPrice = intent.getStringExtra("ROOM_PRICE");
        studentName = intent.getStringExtra("STUDENT_NAME");
        phoneNumber = intent.getStringExtra("PHONE_NUMBER");

        tvRoomName.setText(roomType);
        tvRoomNumber.setText(roomId);
        tvTotalAmount.setText(roomPrice);
    }

    private static class BankInfo {
        String name;
        String code;
        int iconRes;
        BankInfo(String name, String code, int iconRes) {
            this.name = name;
            this.code = code;
            this.iconRes = iconRes;
        }
    }
}