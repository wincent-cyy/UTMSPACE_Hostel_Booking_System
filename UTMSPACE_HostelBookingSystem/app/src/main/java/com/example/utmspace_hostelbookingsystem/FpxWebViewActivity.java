package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class FpxWebViewActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String paymentIntentId;
    private String bookingDocId;
    private String roomId;
    private String roomType;
    private String roomPrice;
    private String studentName;
    private String phoneNumber;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fpx_web_view);

        db = FirebaseFirestore.getInstance();

        String url = getIntent().getStringExtra("URL");
        paymentIntentId = getIntent().getStringExtra("PAYMENT_INTENT_ID");
        bookingDocId = getIntent().getStringExtra("BOOKING_DOC_ID");
        roomId = getIntent().getStringExtra("ROOM_ID");
        roomType = getIntent().getStringExtra("ROOM_TYPE");
        roomPrice = getIntent().getStringExtra("ROOM_PRICE");
        studentName = getIntent().getStringExtra("STUDENT_NAME");
        phoneNumber = getIntent().getStringExtra("PHONE_NUMBER");

        setupStatusBar();

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(android.webkit.WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                progressBar.setVisibility(android.view.View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
                if (url.startsWith("hostelhub://payment-result")) {
                    handlePaymentResult(url);
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });

        webView.loadUrl(url);
    }

    private void handlePaymentResult(String url) {
        // 支付成功，更新订单状态
        updateBookingStatus();
    }

    private void updateBookingStatus() {
        long currentTime = System.currentTimeMillis();

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("bookingStatus", "Paid");
        updates.put("paymentMethod", "FPX (Online Banking)");
        updates.put("paymentTimestamp", currentTime);

        db.collection("Bookings").document(bookingDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    increaseRoomOccupancy();

                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(this, ReceiptActivity.class);
                    intent.putExtra("BOOKING_DOC_ID", bookingDocId);
                    intent.putExtra("ROOM_ID", roomId);
                    intent.putExtra("ROOM_TYPE", roomType);
                    intent.putExtra("ROOM_PRICE", roomPrice);
                    intent.putExtra("STUDENT_NAME", studentName);
                    intent.putExtra("PHONE_NUMBER", phoneNumber);
                    intent.putExtra("PAYMENT_METHOD", "FPX (Online Banking)");
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update booking", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void increaseRoomOccupancy() {
        db.collection("Rooms")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String docId = query.getDocuments().get(0).getId();
                        db.collection("Rooms").document(docId)
                                .update("currentOccupancy", com.google.firebase.firestore.FieldValue.increment(1));
                    }
                });
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}