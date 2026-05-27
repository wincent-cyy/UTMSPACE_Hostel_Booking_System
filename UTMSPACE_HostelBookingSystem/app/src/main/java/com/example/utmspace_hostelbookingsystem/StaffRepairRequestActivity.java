package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StaffRepairRequestActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnSubmitRequest;
    private TextInputEditText etItemName;
    private TextInputEditText etDescription;
    private AutoCompleteTextView autoCompleteUrgency;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentItemName = "";
    private String currentUrgency = "";
    private String currentDescription = "";

    private String[] urgencyOptions = {"Low", "Medium", "High", "Emergency"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_repair_request);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupUrgencyDropdown();
        setupClickListeners();
    }

    private void initViews() {
        // 添加 null 检查，如果找不到就打印日志
        btnBack = findViewById(R.id.btnBack);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        etItemName = findViewById(R.id.etItemName);
        etDescription = findViewById(R.id.etDescription);
        autoCompleteUrgency = findViewById(R.id.autoCompleteUrgency);

        // 调试日志
        if (btnBack == null) {
            android.util.Log.e("RepairRequest", "btnBack is null! Check XML ID");
        }
        if (btnSubmitRequest == null) {
            android.util.Log.e("RepairRequest", "btnSubmitRequest is null! Check XML ID");
        }
    }

    private void setupUrgencyDropdown() {
        if (autoCompleteUrgency != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, urgencyOptions);
            autoCompleteUrgency.setAdapter(adapter);
            autoCompleteUrgency.setThreshold(1);
        }
    }

    private void setupClickListeners() {
        // 添加 null 检查
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        } else {
            Toast.makeText(this, "Back button not found", Toast.LENGTH_SHORT).show();
        }

        if (btnSubmitRequest != null) {
            btnSubmitRequest.setOnClickListener(v -> submitRepairRequest());
        }
    }

    private void submitRepairRequest() {
        if (etItemName != null) {
            currentItemName = etItemName.getText().toString().trim();
        }

        if (autoCompleteUrgency != null) {
            currentUrgency = autoCompleteUrgency.getText().toString().trim();
        }

        if (etDescription != null) {
            currentDescription = etDescription.getText().toString().trim();
        }

        if (currentItemName.isEmpty()) {
            if (etItemName != null) {
                etItemName.setError("Please enter the affected item name");
                etItemName.requestFocus();
            }
            return;
        }

        if (currentUrgency.isEmpty()) {
            Toast.makeText(this, "Please select urgency level", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentDescription.isEmpty()) {
            if (etDescription != null) {
                etDescription.setError("Please describe the problem");
                etDescription.requestFocus();
            }
            return;
        }

        btnSubmitRequest.setEnabled(false);
        btnSubmitRequest.setText("Submitting...");

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (!userId.isEmpty()) {
            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String staffName = "Staff";
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                staffName = name;
                            }
                        }
                        saveRepairRequest(staffName, userId);
                    })
                    .addOnFailureListener(e -> {
                        saveRepairRequest("Staff", userId);
                    });
        } else {
            saveRepairRequest("Staff", userId);
        }
    }

    private void saveRepairRequest(String staffName, String userId) {
        String roomId = getIntent().getStringExtra("ROOM_ID");
        // 删除 roomDocId

        Map<String, Object> repairRequest = new HashMap<>();
        repairRequest.put("itemName", currentItemName);
        repairRequest.put("urgency", currentUrgency);
        repairRequest.put("description", currentDescription);
        repairRequest.put("status", "Pending");
        repairRequest.put("roomId", roomId);
        repairRequest.put("uid", userId);           // ✅ 改为 uid
        repairRequest.put("staffName", staffName);
        repairRequest.put("createdAt", System.currentTimeMillis());
        repairRequest.put("updatedAt", System.currentTimeMillis());

        db.collection("RepairRequests")
                .add(repairRequest)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(StaffRepairRequestActivity.this,
                            "Repair request submitted successfully!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(StaffRepairRequestActivity.this, StaffRoomListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitRequest.setEnabled(true);
                    btnSubmitRequest.setText("Submit Request");
                    Toast.makeText(StaffRepairRequestActivity.this,
                            "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}