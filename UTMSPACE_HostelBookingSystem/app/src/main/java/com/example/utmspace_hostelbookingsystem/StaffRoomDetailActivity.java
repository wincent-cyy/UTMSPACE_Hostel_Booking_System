package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StaffRoomDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvRoomNumber, tvRoomType;
    private Spinner spnStatus, spnCondition;
    private EditText etCurrentOccupancy;
    private Button btnSaveChanges;

    private FirebaseFirestore db;
    private String roomDocId;
    private int maxCapacity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_room_detail);

        db = FirebaseFirestore.getInstance();

        initViews();
        getIntentData();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRoomType = findViewById(R.id.tvRoomType);
        spnStatus = findViewById(R.id.spnStatus);
        spnCondition = findViewById(R.id.spnCondition);
        etCurrentOccupancy = findViewById(R.id.etCurrentOccupancy);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void getIntentData() {
        roomDocId = getIntent().getStringExtra("ROOM_DOC_ID");
        tvRoomNumber.setText(getIntent().getStringExtra("ROOM_NUMBER"));
        tvRoomType.setText(getIntent().getStringExtra("ROOM_TYPE"));
        maxCapacity = getIntent().getIntExtra("ROOM_MAX_CAPACITY", 4);

        String currentStatus = getIntent().getStringExtra("ROOM_STATUS");
        String currentCondition = getIntent().getStringExtra("ROOM_CONDITION");
        int currentOccupancy = getIntent().getIntExtra("ROOM_CURRENT_OCCUPANCY", 0);

        // Setup Status Spinner
        String[] statusOptions = {"Available", "Full"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnStatus.setAdapter(statusAdapter);

        if (currentStatus != null) {
            int pos = currentStatus.equalsIgnoreCase("Full") ? 1 : 0;
            spnStatus.setSelection(pos);
        }

        // Setup Condition Spinner
        String[] conditionOptions = {"Good", "Needs Repair", "Under Maintenance"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, conditionOptions);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCondition.setAdapter(conditionAdapter);

        if (currentCondition != null) {
            int pos = 0;
            if (currentCondition.equalsIgnoreCase("Needs Repair")) pos = 1;
            else if (currentCondition.equalsIgnoreCase("Under Maintenance")) pos = 2;
            spnCondition.setSelection(pos);
        }

        etCurrentOccupancy.setText(String.valueOf(currentOccupancy));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> {
            String newStatus = spnStatus.getSelectedItem().toString();
            String newCondition = spnCondition.getSelectedItem().toString();
            String occupancyStr = etCurrentOccupancy.getText().toString().trim();

            if (occupancyStr.isEmpty()) {
                etCurrentOccupancy.setError("Please enter current occupancy");
                return;
            }

            int newOccupancy = Integer.parseInt(occupancyStr);
            if (newOccupancy > maxCapacity) {
                etCurrentOccupancy.setError("Occupancy cannot exceed max capacity (" + maxCapacity + ")");
                return;
            }

            // Auto-update status based on occupancy if user selected "Available/Full" incorrectly
            if (newOccupancy >= maxCapacity) {
                newStatus = "Full";
            } else if (newOccupancy == 0) {
                newStatus = "Available";
            }

            updateRoomInFirestore(newStatus, newCondition, newOccupancy);
        });
    }

    private void updateRoomInFirestore(String status, String condition, int occupancy) {
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("condition", condition);
        updates.put("currentOccupancy", occupancy);
        updates.put("lastUpdated", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date()));

        db.collection("Rooms").document(roomDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Room updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}