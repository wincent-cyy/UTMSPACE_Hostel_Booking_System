package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class AdminEditRoomActivity extends AppCompatActivity {

    private LinearLayout ivBack;
    private LinearLayout btnSave;
    private LinearLayout btnDeleteRoom;
    private LinearLayout dangerZoneLayout;

    private TextInputEditText etRoomNumber;
    private TextInputEditText etRoomType;
    private TextInputEditText etLocation;
    private TextInputEditText etPrice;
    private TextInputEditText etStatus;
    private TextInputEditText etMaxCapacity;
    private TextInputEditText etCurrentOccupancy;

    private FirebaseFirestore db;
    private String roomDocId;
    private boolean isViewOnly;

    // Options
    private final String[] roomTypeOptions = {"Single Room", "Double Room", "Quad Room"};
    private final String[] statusOptions = {"Available", "Full", "Maintenance"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_room);

        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        getIntentData();
        setupDropdowns();
        setupClickListeners();
        setupEditMode();
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
        btnSave = findViewById(R.id.btnSave);
        btnDeleteRoom = findViewById(R.id.btnDeleteRoom);

        etRoomNumber = findViewById(R.id.etRoomNumber);
        etRoomType = findViewById(R.id.etRoomType);
        etLocation = findViewById(R.id.etLocation);
        etPrice = findViewById(R.id.etPrice);
        etStatus = findViewById(R.id.etStatus);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etCurrentOccupancy = findViewById(R.id.etCurrentOccupancy);
        dangerZoneLayout = findViewById(R.id.dangerZoneLayout);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            // Get document ID for Firestore operations
            roomDocId = intent.getStringExtra("ROOM_DOCUMENT_ID");

            // Get view only mode
            isViewOnly = intent.getBooleanExtra("VIEW_ONLY", false);

            // Set values from intent
            String roomId = intent.getStringExtra("ROOM_ID");
            String roomType = intent.getStringExtra("ROOM_TYPE");
            String location = intent.getStringExtra("LOCATION");
            double price = intent.getDoubleExtra("PRICE", 0);
            int maxCapacity = intent.getIntExtra("MAX_CAPACITY", 1);
            int currentOccupancy = intent.getIntExtra("CURRENT_OCCUPANCY", 0);
            String status = intent.getStringExtra("STATUS");

            etRoomNumber.setText(roomId != null ? roomId : "N/A");
            etRoomType.setText(roomType != null ? roomType : "");
            etLocation.setText(location != null ? location : "");
            etPrice.setText(String.format("%.2f", price));
            etMaxCapacity.setText(String.valueOf(maxCapacity));
            etCurrentOccupancy.setText(String.valueOf(currentOccupancy));
            etStatus.setText(status != null ? status : "Available");
        }
    }

    private void setupDropdowns() {
        // Room Type - dropdown picker
        etRoomType.setFocusable(false);
        etRoomType.setClickable(true);
        etRoomType.setOnClickListener(v -> {
            if (!isViewOnly) showRoomTypePicker();
        });

        // Location - now a regular text field, no dropdown
        // No need to set any special properties, it's editable by default

        // Status - dropdown picker
        etStatus.setFocusable(false);
        etStatus.setClickable(true);
        etStatus.setOnClickListener(v -> {
            if (!isViewOnly) showStatusPicker();
        });
    }

    private void setupEditMode() {
        if (isViewOnly) {
            // View only mode - disable all fields and hide danger zone
            enableFields(false);
            btnSave.setVisibility(View.GONE);

            // 隐藏整个 Danger Zone 区域
            if (dangerZoneLayout != null) {
                dangerZoneLayout.setVisibility(View.GONE);
            }
        } else {
            // Edit mode - enable fields and show danger zone
            enableFields(true);
            btnSave.setVisibility(View.VISIBLE);

            // 显示整个 Danger Zone 区域
            if (dangerZoneLayout != null) {
                dangerZoneLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void enableFields(boolean enable) {
        etRoomNumber.setEnabled(enable);
        etRoomType.setEnabled(enable);
        etLocation.setEnabled(enable);
        etPrice.setEnabled(enable);
        etStatus.setEnabled(enable);
        etMaxCapacity.setEnabled(enable);
        etCurrentOccupancy.setEnabled(enable);

        // Room number should never be editable
        etRoomNumber.setEnabled(false);
    }

    private void showRoomTypePicker() {
        new AlertDialog.Builder(this)
                .setTitle("Select Room Type")
                .setItems(roomTypeOptions, (dialog, which) -> {
                    String selectedType = roomTypeOptions[which];
                    etRoomType.setText(selectedType);

                    // Auto-set max capacity based on room type
                    if (selectedType.equals("Single Room")) {
                        etMaxCapacity.setText("1");
                        try {
                            int currentOcc = Integer.parseInt(etCurrentOccupancy.getText().toString());
                            if (currentOcc > 1) {
                                etCurrentOccupancy.setText("0");
                            }
                        } catch (NumberFormatException e) {
                            etCurrentOccupancy.setText("0");
                        }
                    } else if (selectedType.equals("Double Room")) {
                        etMaxCapacity.setText("2");
                        try {
                            int currentOcc = Integer.parseInt(etCurrentOccupancy.getText().toString());
                            if (currentOcc > 2) {
                                etCurrentOccupancy.setText("0");
                            }
                        } catch (NumberFormatException e) {
                            etCurrentOccupancy.setText("0");
                        }
                    } else if (selectedType.equals("Quad Room")) {
                        etMaxCapacity.setText("4");
                        try {
                            int currentOcc = Integer.parseInt(etCurrentOccupancy.getText().toString());
                            if (currentOcc > 4) {
                                etCurrentOccupancy.setText("0");
                            }
                        } catch (NumberFormatException e) {
                            etCurrentOccupancy.setText("0");
                        }
                    }
                })
                .show();
    }

    private void showStatusPicker() {
        new AlertDialog.Builder(this)
                .setTitle("Select Status")
                .setItems(statusOptions, (dialog, which) -> {
                    etStatus.setText(statusOptions[which]);
                })
                .show();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveRoomData());
        btnDeleteRoom.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void saveRoomData() {
        if (!validateFields()) {
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setAlpha(0.5f);

        final String roomNumber = etRoomNumber.getText().toString().trim();
        final String roomType = etRoomType.getText().toString().trim();
        final String location = etLocation.getText().toString().trim();
        final String priceStr = etPrice.getText().toString().trim();
        final String status = etStatus.getText().toString().trim();
        final String maxCapacityStr = etMaxCapacity.getText().toString().trim();
        final String currentOccupancyStr = etCurrentOccupancy.getText().toString().trim();

        final double price = Double.parseDouble(priceStr);
        final int maxCapacity = Integer.parseInt(maxCapacityStr);
        final int currentOccupancy = Integer.parseInt(currentOccupancyStr);

        // Auto-update status based on occupancy if not maintenance
        String finalStatus = status;
        if (!"Maintenance".equalsIgnoreCase(status)) {
            if (currentOccupancy <= 0) {
                finalStatus = "Available";
            } else if (currentOccupancy >= maxCapacity) {
                finalStatus = "Full";
            } else {
                finalStatus = "Available";
            }
        }

        // Update Rooms collection
        Map<String, Object> updates = new HashMap<>();
        updates.put("roomId", roomNumber);
        updates.put("roomType", roomType);
        updates.put("location", location);
        updates.put("price", price);
        updates.put("status", finalStatus);
        updates.put("maxCapacity", maxCapacity);
        updates.put("currentOccupancy", currentOccupancy);
        updates.put("updatedAt", System.currentTimeMillis());

        final String finalStatus1 = finalStatus;

        db.collection("Rooms").document(roomDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    updateRelatedCollections(roomNumber, roomType, price, finalStatus1);
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setAlpha(1f);
                    Toast.makeText(this, "Failed to update room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRelatedCollections(final String roomNumber, final String roomType, final double price, final String status) {
        final int[] pendingUpdates = {2};
        final boolean[] hasError = {false};

        // Update Bookings collection - 使用 roomNumber (即 "A-101")
        db.collection("Bookings")
                .whereEqualTo("roomId", roomNumber)  // 改成 roomNumber
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> bookingUpdates = new HashMap<>();
                        bookingUpdates.put("roomType", roomType);
                        bookingUpdates.put("price", price);
                        document.getReference().update(bookingUpdates);
                    }
                    checkAndFinish(pendingUpdates, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinish(pendingUpdates, hasError);
                });

        // Update RepairRequests collection - 使用 roomNumber
        db.collection("RepairRequests")
                .whereEqualTo("roomId", roomNumber)  // 改成 roomNumber
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> repairUpdates = new HashMap<>();
                        repairUpdates.put("roomType", roomType);
                        document.getReference().update(repairUpdates);
                    }
                    checkAndFinish(pendingUpdates, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinish(pendingUpdates, hasError);
                });
    }

    private void checkAndFinish(int[] pendingUpdates, boolean[] hasError) {
        pendingUpdates[0]--;
        if (pendingUpdates[0] == 0) {
            btnSave.setEnabled(true);
            btnSave.setAlpha(1f);

            if (hasError[0]) {
                Toast.makeText(this, "Room updated with some errors. Please check data consistency.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Room updated successfully!", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        if (TextUtils.isEmpty(etRoomNumber.getText())) {
            etRoomNumber.setError("Room number is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(etRoomType.getText())) {
            Toast.makeText(this, "Please select room type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(etLocation.getText())) {
            etLocation.setError("Location is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Price is required");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(etPrice.getText().toString().trim());
                if (price < 0) {
                    etPrice.setError("Price cannot be negative");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etPrice.setError("Invalid price format");
                isValid = false;
            }
        }

        if (TextUtils.isEmpty(etMaxCapacity.getText())) {
            etMaxCapacity.setError("Max capacity is required");
            isValid = false;
        } else {
            try {
                int capacity = Integer.parseInt(etMaxCapacity.getText().toString().trim());
                if (capacity < 1) {
                    etMaxCapacity.setError("Capacity must be at least 1");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etMaxCapacity.setError("Invalid number");
                isValid = false;
            }
        }

        if (TextUtils.isEmpty(etCurrentOccupancy.getText())) {
            etCurrentOccupancy.setError("Current occupancy is required");
            isValid = false;
        } else {
            try {
                int occupancy = Integer.parseInt(etCurrentOccupancy.getText().toString().trim());
                int maxCapacity = Integer.parseInt(etMaxCapacity.getText().toString().trim());
                if (occupancy < 0) {
                    etCurrentOccupancy.setError("Occupancy cannot be negative");
                    isValid = false;
                } else if (occupancy > maxCapacity) {
                    etCurrentOccupancy.setError("Occupancy cannot exceed max capacity");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etCurrentOccupancy.setError("Invalid number");
                isValid = false;
            }
        }

        return isValid;
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Room")
                .setMessage("Are you sure you want to delete this room? This action cannot be undone.\n\nNote: This will also affect all associated bookings and repair requests.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteRoom())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRoom() {
        btnDeleteRoom.setEnabled(false);
        btnDeleteRoom.setAlpha(0.5f);

        final int[] pendingDeletions = {3};
        final boolean[] hasError = {false};

        final String roomNumber = etRoomNumber.getText().toString().trim();

        // Update Bookings - 使用 roomNumber
        db.collection("Bookings")
                .whereEqualTo("roomId", roomNumber)  // 改成 roomNumber
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("roomStatus", "Deleted");
                        updates.put("roomId", "N/A");
                        updates.put("roomType", "Deleted Room");
                        document.getReference().update(updates);
                    }
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });

        // Update RepairRequests - 使用 roomNumber
        db.collection("RepairRequests")
                .whereEqualTo("roomId", roomNumber)  // 改成 roomNumber
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("roomId", "N/A");
                        updates.put("roomType", "Deleted Room");
                        document.getReference().update(updates);
                    }
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });

        // Delete Room
        db.collection("Rooms").document(roomDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });
    }

    private void checkAndFinishDeletion(int[] pendingDeletions, boolean[] hasError) {
        pendingDeletions[0]--;
        if (pendingDeletions[0] == 0) {
            if (hasError[0]) {
                Toast.makeText(this, "Room partially deleted. Please check related data.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Room deleted successfully", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
    }
}