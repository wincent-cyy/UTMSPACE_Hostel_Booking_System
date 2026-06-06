package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StaffRoomDetailActivity extends AppCompatActivity {

    // UI Elements - 匹配 XML ID
    private LinearLayout ivBack;
    private TextInputEditText etRoomNumber;
    private TextInputEditText etRoomType;
    private TextInputEditText etLocation;
    private TextInputEditText etPrice;
    private TextInputEditText etStatus;
    private TextInputEditText etMaxCapacity;
    private EditText etCurrentOccupancy;  // 改为可编辑的 EditText
    private LinearLayout btnSendRepairRequest;
    private LinearLayout btnCancel;
    private LinearLayout btnSave;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private String roomDocId;
    private String originalStatus;
    private int maxCapacity;

    // Status options for dropdown
    private String[] statusOptions = {"Available", "Full", "Maintenance"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_room_detail);

        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        getIntentData();
        setupClickListeners();
        setupStatusDropdown();
        loadRoomData();
    }

    /**
     * Setup status bar to be white with dark icons
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Only set status bar color to white
            getWindow().setStatusBarColor(Color.WHITE);

            // Make status bar icons dark for visibility on white background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etRoomNumber = findViewById(R.id.etRoomNumber);
        etRoomType = findViewById(R.id.etRoomType);
        etLocation = findViewById(R.id.etLocation);
        etPrice = findViewById(R.id.etPrice);
        etStatus = findViewById(R.id.etStatus);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etCurrentOccupancy = findViewById(R.id.etCurrentOccupancy);
        btnSendRepairRequest = findViewById(R.id.btnSendRepairRequest);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);

        // 设置 Max Capacity 为不可编辑（只读）
        if (etMaxCapacity != null) {
            etMaxCapacity.setFocusable(false);
            etMaxCapacity.setClickable(false);
            etMaxCapacity.setEnabled(false);
        }

        // Current Occupancy 可编辑，但限制只能输入数字
        if (etCurrentOccupancy != null) {
            etCurrentOccupancy.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        } else {
            android.util.Log.e("StaffRoomDetail", "etCurrentOccupancy is null! Please check XML layout.");
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            roomDocId = intent.getStringExtra("ROOM_DOC_ID");

            // 显示基本信息
            String roomId = intent.getStringExtra("ROOM_ID");
            String roomType = intent.getStringExtra("ROOM_TYPE");
            String location = intent.getStringExtra("ROOM_LOCATION");
            double price = intent.getDoubleExtra("ROOM_PRICE", 0);
            maxCapacity = intent.getIntExtra("ROOM_MAX_CAPACITY", 1);
            int currentOccupancy = intent.getIntExtra("ROOM_CURRENT_OCCUPANCY", 0);
            String status = intent.getStringExtra("ROOM_STATUS");

            if (etRoomNumber != null) etRoomNumber.setText(roomId != null ? roomId : "N/A");
            if (etRoomType != null) etRoomType.setText(roomType != null ? roomType : "N/A");
            if (etLocation != null) etLocation.setText(location != null ? location : "Not specified");
            if (etPrice != null) etPrice.setText(String.format("%.0f", price));
            if (etMaxCapacity != null) etMaxCapacity.setText(String.valueOf(maxCapacity));
            if (etCurrentOccupancy != null) etCurrentOccupancy.setText(String.valueOf(currentOccupancy));

            if (status != null && etStatus != null) {
                originalStatus = status;
                etStatus.setText(status);
                updateStatusAppearance(status);
            }
        }
    }

    private void setupStatusDropdown() {
        if (etStatus == null) return;

        etStatus.setFocusable(false);
        etStatus.setClickable(true);

        etStatus.setOnClickListener(v -> showStatusPickerDialog());
    }

    private void showStatusPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Status");

        builder.setItems(statusOptions, (dialog, which) -> {
            String selectedStatus = statusOptions[which];
            if (etStatus != null) {
                etStatus.setText(selectedStatus);
                updateStatusAppearance(selectedStatus);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateStatusAppearance(String status) {
        if (etStatus == null) return;

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(30f);

        if ("Available".equalsIgnoreCase(status)) {
            bg.setColor(Color.parseColor("#DCFCE7"));
            etStatus.setTextColor(Color.parseColor("#15803D"));
        } else if ("Full".equalsIgnoreCase(status)) {
            bg.setColor(Color.parseColor("#FEE2E2"));
            etStatus.setTextColor(Color.parseColor("#B91C1C"));
        } else if ("Maintenance".equalsIgnoreCase(status)) {
            bg.setColor(Color.parseColor("#FEF3C7"));
            etStatus.setTextColor(Color.parseColor("#D97706"));
        }

        etStatus.setBackground(bg);
        etStatus.setPadding(24, 12, 24, 12);
    }

    private void loadRoomData() {
        if (roomDocId == null || roomDocId.isEmpty()) {
            Toast.makeText(this, "Room ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Rooms").document(roomDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String roomNumber = documentSnapshot.getString("roomId");
                        String roomType = documentSnapshot.getString("roomType");
                        String location = documentSnapshot.getString("location");
                        Double price = documentSnapshot.getDouble("price");
                        String status = documentSnapshot.getString("status");
                        Integer maxCap = documentSnapshot.getLong("maxCapacity") != null
                                ? documentSnapshot.getLong("maxCapacity").intValue() : 1;
                        Integer currentOcc = documentSnapshot.getLong("currentOccupancy") != null
                                ? documentSnapshot.getLong("currentOccupancy").intValue() : 0;

                        if (roomNumber != null && etRoomNumber != null) etRoomNumber.setText(roomNumber);
                        if (roomType != null && etRoomType != null) etRoomType.setText(roomType);
                        if (location != null && etLocation != null) etLocation.setText(location);
                        if (price != null && etPrice != null) etPrice.setText(String.format("%.0f", price));
                        if (status != null && etStatus != null) {
                            originalStatus = status;
                            etStatus.setText(status);
                            updateStatusAppearance(status);
                        }
                        if (maxCap != null && etMaxCapacity != null) {
                            maxCapacity = maxCap;
                            etMaxCapacity.setText(String.valueOf(maxCapacity));
                        }
                        if (currentOcc != null && etCurrentOccupancy != null) {
                            etCurrentOccupancy.setText(String.valueOf(currentOcc));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load room data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        // 返回按钮
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // 发送维修请求
        if (btnSendRepairRequest != null) {
            btnSendRepairRequest.setOnClickListener(v -> {
                String roomNumber = etRoomNumber != null ? etRoomNumber.getText().toString() : "";
                Intent intent = new Intent(StaffRoomDetailActivity.this, StaffRepairRequestActivity.class);
                intent.putExtra("ROOM_ID", roomNumber);
                intent.putExtra("ROOM_DOC_ID", roomDocId);
                startActivity(intent);
            });
        }

        // 取消按钮
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        // 保存按钮
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveChanges());
        }
    }

    private void saveChanges() {
        // 获取用户输入 - 添加 null 检查
        String roomNumber = etRoomNumber != null ? etRoomNumber.getText().toString().trim() : "";
        String roomType = etRoomType != null ? etRoomType.getText().toString().trim() : "";
        String location = etLocation != null ? etLocation.getText().toString().trim() : "";
        String priceStr = etPrice != null ? etPrice.getText().toString().trim() : "";
        String status = etStatus != null ? etStatus.getText().toString().trim() : "";
        String occupancyStr = etCurrentOccupancy != null ? etCurrentOccupancy.getText().toString().trim() : "";

        // 验证输入
        if (TextUtils.isEmpty(roomNumber)) {
            if (etRoomNumber != null) etRoomNumber.setError("Room number is required");
            return;
        }

        if (TextUtils.isEmpty(roomType)) {
            if (etRoomType != null) etRoomType.setError("Room type is required");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) {
                if (etPrice != null) etPrice.setError("Price cannot be negative");
                return;
            }
        } catch (NumberFormatException e) {
            if (etPrice != null) etPrice.setError("Valid price is required");
            return;
        }

        int currentOccupancy;
        try {
            currentOccupancy = Integer.parseInt(occupancyStr);
            if (currentOccupancy < 0) {
                if (etCurrentOccupancy != null) etCurrentOccupancy.setError("Occupancy cannot be negative");
                return;
            }
            if (currentOccupancy > maxCapacity) {
                if (etCurrentOccupancy != null) {
                    etCurrentOccupancy.setError("Occupancy cannot exceed max capacity (" + maxCapacity + ")");
                }
                return;
            }
        } catch (NumberFormatException e) {
            if (etCurrentOccupancy != null) etCurrentOccupancy.setError("Valid occupancy is required");
            return;
        }

        // ========== 修复状态逻辑 ==========
        String finalStatus;

        // 如果用户选择的是 Maintenance，保持 Maintenance，不受 occupancy 影响
        if ("Maintenance".equalsIgnoreCase(status)) {
            finalStatus = "Maintenance";
        } else {
            // 只有非 Maintenance 状态才根据 occupancy 自动调整
            if (currentOccupancy <= 0) {
                finalStatus = "Available";
            } else if (currentOccupancy >= maxCapacity) {
                finalStatus = "Full";
            } else {
                finalStatus = "Available";
            }
        }

        // 显示保存中
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setAlpha(0.5f);
        }

        // 更新 Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("roomId", roomNumber);
        updates.put("roomType", roomType);
        updates.put("location", location);
        updates.put("price", price);
        updates.put("status", finalStatus);
        updates.put("maxCapacity", maxCapacity);
        updates.put("currentOccupancy", currentOccupancy);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection("Rooms").document(roomDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(StaffRoomDetailActivity.this, "Room details updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                        btnSave.setAlpha(1.0f);
                    }
                    Toast.makeText(StaffRoomDetailActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
    }
}