package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StaffRoomDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvRoomNumber, tvRoomType, tvMaxCapacity, tvPrice;
    private Spinner spnStatus, spnCondition;
    private EditText etCurrentOccupancy;
    private Button btnRepairRequest, btnSaveChanges;

    private FirebaseFirestore db;
    private String roomDocId;
    private String originalStatus;
    private String originalCondition;
    private int originalOccupancy;

    // Status options
    private String[] statusOptions = {"Available", "Full", "Maintenance"};

    // Condition options
    private String[] conditionOptions = {"Good", "Needs Repair", "Under Maintenance"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_room_detail);

        db = FirebaseFirestore.getInstance();

        initViews();
        getIntentData();
        setupSpinners();
        setupClickListeners();
        loadRoomData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvMaxCapacity = findViewById(R.id.tvMaxCapacity);
        tvPrice = findViewById(R.id.tvPrice);
        spnStatus = findViewById(R.id.spnStatus);
        spnCondition = findViewById(R.id.spnCondition);
        etCurrentOccupancy = findViewById(R.id.etCurrentOccupancy);
        btnRepairRequest = findViewById(R.id.btnRepairRequest);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            roomDocId = intent.getStringExtra("ROOM_DOC_ID");
            tvRoomNumber.setText(intent.getStringExtra("ROOM_ID"));
            tvRoomType.setText(intent.getStringExtra("ROOM_TYPE"));

            // ✅ 修复价格显示
            double price = intent.getDoubleExtra("ROOM_PRICE", 0);
            tvPrice.setText(String.format("RM %.2f", price));

            tvMaxCapacity.setText(String.valueOf(intent.getIntExtra("ROOM_MAX_CAPACITY", 4)));
        }
    }

    private void setupSpinners() {
        // Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnStatus.setAdapter(statusAdapter);

        // Condition Spinner
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, conditionOptions);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCondition.setAdapter(conditionAdapter);
    }

    private void loadRoomData() {
        if (roomDocId == null || roomDocId.isEmpty()) {
            Toast.makeText(this, "Error: Room ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Rooms").document(roomDocId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load status
                        String status = documentSnapshot.getString("status");
                        originalStatus = status;
                        if (status != null) {
                            for (int i = 0; i < statusOptions.length; i++) {
                                if (statusOptions[i].equalsIgnoreCase(status)) {
                                    spnStatus.setSelection(i);
                                    break;
                                }
                            }
                        }

                        // Load condition
                        String condition = documentSnapshot.getString("condition");
                        originalCondition = condition;
                        if (condition != null) {
                            for (int i = 0; i < conditionOptions.length; i++) {
                                if (conditionOptions[i].equalsIgnoreCase(condition)) {
                                    spnCondition.setSelection(i);
                                    break;
                                }
                            }
                        }

                        // Load current occupancy
                        Long occupancy = documentSnapshot.getLong("currentOccupancy");
                        originalOccupancy = occupancy != null ? occupancy.intValue() : 0;
                        etCurrentOccupancy.setText(String.valueOf(originalOccupancy));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load room data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnRepairRequest.setOnClickListener(v -> {
            Intent intent = new Intent(StaffRoomDetailActivity.this, StaffRepairRequestActivity.class);
            intent.putExtra("ROOM_ID", tvRoomNumber.getText().toString());
            intent.putExtra("ROOM_DOC_ID", roomDocId);
            startActivity(intent);
        });

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        String newStatus = spnStatus.getSelectedItem().toString();
        String newCondition = spnCondition.getSelectedItem().toString();
        String occupancyStr = etCurrentOccupancy.getText().toString().trim();

        int newOccupancy;
        try {
            newOccupancy = Integer.parseInt(occupancyStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for occupancy", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxCapacity = Integer.parseInt(tvMaxCapacity.getText().toString());

        // Validate occupancy
        if (newOccupancy < 0) {
            Toast.makeText(this, "Occupancy cannot be negative", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newOccupancy > maxCapacity) {
            new AlertDialog.Builder(this)
                    .setTitle("Capacity Exceeded")
                    .setMessage("Current occupancy (" + newOccupancy + ") exceeds max capacity (" + maxCapacity + "). Do you want to update anyway?")
                    .setPositiveButton("Yes", (dialog, which) -> updateRoomData(newStatus, newCondition, newOccupancy, maxCapacity))
                    .setNegativeButton("No", null)
                    .show();
            return;
        }

        updateRoomData(newStatus, newCondition, newOccupancy, maxCapacity);
    }

    private void updateRoomData(String newStatus, String newCondition, int newOccupancy, int maxCapacity) {
        // Calculate new room status based on occupancy
        String calculatedStatus = newStatus;

        // If status is "Available" but occupancy reaches max, change to "Full"
        if ("Available".equalsIgnoreCase(newStatus) && newOccupancy >= maxCapacity) {
            calculatedStatus = "Full";
        }
        // If status is "Full" but occupancy drops below max, change to "Available"
        else if ("Full".equalsIgnoreCase(newStatus) && newOccupancy < maxCapacity) {
            calculatedStatus = "Available";
        }
        // If status is "Maintenance", keep as is regardless of occupancy
        else if ("Maintenance".equalsIgnoreCase(newStatus)) {
            calculatedStatus = "Maintenance";
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", calculatedStatus);
        updates.put("condition", newCondition);
        updates.put("currentOccupancy", newOccupancy);
        updates.put("lastUpdated", System.currentTimeMillis());

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        db.collection("Rooms").document(roomDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(StaffRoomDetailActivity.this, "Room details updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(StaffRoomDetailActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }
}