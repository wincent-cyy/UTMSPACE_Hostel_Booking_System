package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StaffRepairDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvRoomNumber, tvStatus, tvItemName, tvUrgency, tvDescription, tvStaffName, tvDate;
    private ImageView ivProofImage;  // ✅ 新增

    private FirebaseFirestore db;
    private String requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_repair_detail);

        db = FirebaseFirestore.getInstance();

        initViews();
        displayData();
        loadProofImage();  // ✅ 新增：加载图片
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvItemName = findViewById(R.id.tvItemName);
        tvUrgency = findViewById(R.id.tvUrgency);
        tvDescription = findViewById(R.id.tvDescription);
        tvStaffName = findViewById(R.id.tvStaffName);
        tvDate = findViewById(R.id.tvDate);
        ivProofImage = findViewById(R.id.ivProofImage);  // ✅ 新增
    }

    private void displayData() {
        Intent intent = getIntent();
        requestId = intent.getStringExtra("REQUEST_ID");

        tvRoomNumber.setText("Room " + intent.getStringExtra("ROOM_ID"));
        tvItemName.setText(intent.getStringExtra("ITEM_NAME"));
        tvUrgency.setText(intent.getStringExtra("URGENCY"));
        tvDescription.setText(intent.getStringExtra("DESCRIPTION"));
        tvStaffName.setText("Reported by: " + intent.getStringExtra("STAFF_NAME"));

        String status = intent.getStringExtra("STATUS");
        tvStatus.setText(status);

        long createdAt = intent.getLongExtra("CREATED_AT", 0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(createdAt)));
    }

    // ✅ 新增：加载证明图片
    private void loadProofImage() {
        if (requestId == null) return;

        db.collection("RepairRequests").document(requestId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String proofImageBase64 = documentSnapshot.getString("proofImage");
                        if (proofImageBase64 != null && !proofImageBase64.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(proofImageBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                ivProofImage.setImageBitmap(bitmap);
                                ivProofImage.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // 加载失败，不显示图片
                });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}