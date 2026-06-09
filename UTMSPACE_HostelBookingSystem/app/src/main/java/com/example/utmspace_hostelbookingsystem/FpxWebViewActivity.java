package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                progressBar.setVisibility(View.GONE);
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

        // 使用 AndroidX 的 OnBackPressedDispatcher 处理返回键
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void handlePaymentResult(String url) {
        updateBookingStatus();
    }

    private void updateBookingStatus() {
        long currentTime = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
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

                        transaction.update(roomRef, "currentOccupancy", newOccupancy);

                        if (newOccupancy >= maxCapacity) {
                            transaction.update(roomRef, "status", "Full");
                        } else {
                            transaction.update(roomRef, "status", "Available");
                        }

                        return null;
                    }).addOnFailureListener(e -> {
                        Map<String, Object> simpleUpdates = new HashMap<>();
                        simpleUpdates.put("currentOccupancy", com.google.firebase.firestore.FieldValue.increment(1));
                        db.collection("Rooms").document(docId).update(simpleUpdates);
                    });
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
}