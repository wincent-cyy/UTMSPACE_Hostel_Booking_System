package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomManagementActivity extends AppCompatActivity {

    // UI Elements
    private EditText etSearchRoom;
    private RecyclerView rvRoomList;
    private LinearLayout emptyState;
    private TextView tvRoomCount;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddRoom;

    private TextView chipAll, chipAvailable, chipFull, chipMaintenance;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private AdminRoomAdapter adapter;
    private List<RoomModel> allRoomsList;
    private List<RoomModel> filteredList;

    private String currentStatusFilter = "All";
    private String currentSearchQuery = "";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_management);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupFilterChips();
        setupSearchFilter();
        loadRooms();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        etSearchRoom = findViewById(R.id.etSearchRoom);
        rvRoomList = findViewById(R.id.rvRoomList);
        emptyState = findViewById(R.id.emptyState);
        tvRoomCount = findViewById(R.id.tvRoomCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddRoom = findViewById(R.id.fabAddRoom);

        chipAll = findViewById(R.id.chipAll);
        chipAvailable = findViewById(R.id.chipAvailable);
        chipFull = findViewById(R.id.chipFull);
        chipMaintenance = findViewById(R.id.chipMaintenance);
    }

    private void setupRecyclerView() {
        allRoomsList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvRoomList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminRoomAdapter(filteredList, new AdminRoomAdapter.OnRoomActionListener() {
            @Override
            public void onEdit(RoomModel room) {
                showEditRoomDialog(room);
            }

            @Override
            public void onDelete(RoomModel room) {
                showDeleteConfirmDialog(room);
            }
        });
        rvRoomList.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            currentStatusFilter = "All";
            updateChipStyles(chipAll);
            applyFilters();
        });

        chipAvailable.setOnClickListener(v -> {
            currentStatusFilter = "Available";
            updateChipStyles(chipAvailable);
            applyFilters();
        });

        chipFull.setOnClickListener(v -> {
            currentStatusFilter = "Full";
            updateChipStyles(chipFull);
            applyFilters();
        });

        chipMaintenance.setOnClickListener(v -> {
            currentStatusFilter = "Maintenance";
            updateChipStyles(chipMaintenance);
            applyFilters();
        });
    }

    private void updateChipStyles(TextView selectedChip) {
        chipAll.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipAvailable.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipAvailable.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipFull.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipFull.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipMaintenance.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipMaintenance.setTextColor(android.graphics.Color.parseColor("#0369A1"));

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void setupSearchFilter() {
        etSearchRoom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString();
                searchRunnable = () -> {
                    currentSearchQuery = query;
                    applyFilters();
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadRooms() {
        db.collection("Rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRoomsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RoomModel room = document.toObject(RoomModel.class);
                        room.setDocumentId(document.getId());  // Store the actual document ID

                        // Ensure currentOccupancy has a default value if missing
                        if (!document.contains("currentOccupancy")) {
                            room.setCurrentOccupancy(0);
                        }

                        allRoomsList.add(room);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        filteredList.clear();

        for (RoomModel room : allRoomsList) {
            boolean matchesStatus = true;
            if (!"All".equals(currentStatusFilter)) {
                String status = room.getStatus();
                matchesStatus = status != null && status.equalsIgnoreCase(currentStatusFilter);
            }

            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                String roomId = room.getRoomId() != null ? room.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(cleanQuery);
            }

            if (matchesStatus && matchesSearch) {
                filteredList.add(room);
            }
        }

        if (filteredList.isEmpty()) {
            rvRoomList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvRoomList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        tvRoomCount.setText(filteredList.size() + " rooms");
        adapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
    }

    private void showAddRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_room_form, null);

        EditText etRoomId = dialogView.findViewById(R.id.etRoomId);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etMaxCapacity = dialogView.findViewById(R.id.etMaxCapacity);
        EditText etCurrentOccupancy = dialogView.findViewById(R.id.etCurrentOccupancy);
        Spinner spnRoomType = dialogView.findViewById(R.id.spnRoomType);
        Spinner spnStatus = dialogView.findViewById(R.id.spnStatus);

        // Setup Room Type Spinner with default selection
        String[] roomTypes = {"Single Room", "Double Room", "Quad Room"};
        ArrayAdapter<String> roomTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roomTypes);
        roomTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnRoomType.setAdapter(roomTypeAdapter);
        spnRoomType.setSelection(0); // Set "Single Room" as default

        // Setup Status Spinner with default selection
        String[] statusOptions = {"Available", "Full", "Maintenance"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnStatus.setAdapter(statusAdapter);
        spnStatus.setSelection(0); // Set "Available" as default

        // Set default value for current occupancy
        etCurrentOccupancy.setText("0");

        builder.setTitle("Add New Room")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String roomId = etRoomId.getText().toString().trim();
                    String roomType = spnRoomType.getSelectedItem().toString();
                    String location = etLocation.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    String capacityStr = etMaxCapacity.getText().toString().trim();
                    String currentOccupancyStr = etCurrentOccupancy.getText().toString().trim();
                    String status = spnStatus.getSelectedItem().toString();

                    // Validation
                    if (roomId.isEmpty()) {
                        etRoomId.setError("Room number required");
                        return;
                    }
                    if (location.isEmpty()) {
                        etLocation.setError("Location required");
                        return;
                    }
                    if (priceStr.isEmpty()) {
                        etPrice.setError("Price required");
                        return;
                    }
                    if (capacityStr.isEmpty()) {
                        etMaxCapacity.setError("Max capacity required");
                        return;
                    }
                    if (currentOccupancyStr.isEmpty()) {
                        etCurrentOccupancy.setError("Current occupancy required");
                        return;
                    }

                    try {
                        double price = Double.parseDouble(priceStr);
                        int maxCapacity = Integer.parseInt(capacityStr);
                        int currentOccupancy = Integer.parseInt(currentOccupancyStr);

                        // Validate current occupancy doesn't exceed max capacity
                        if (currentOccupancy > maxCapacity) {
                            etCurrentOccupancy.setError("Current occupancy cannot exceed max capacity");
                            return;
                        }

                        addRoom(roomId, roomType, location, price, maxCapacity, currentOccupancy, status);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showEditRoomDialog(RoomModel room) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_room_form, null);

        EditText etRoomId = dialogView.findViewById(R.id.etRoomId);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etMaxCapacity = dialogView.findViewById(R.id.etMaxCapacity);
        EditText etCurrentOccupancy = dialogView.findViewById(R.id.etCurrentOccupancy);
        Spinner spnRoomType = dialogView.findViewById(R.id.spnRoomType);
        Spinner spnStatus = dialogView.findViewById(R.id.spnStatus);

        // Setup spinners
        String[] roomTypes = {"Single Room", "Double Room", "Quad Room"};
        ArrayAdapter<String> roomTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roomTypes);
        roomTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnRoomType.setAdapter(roomTypeAdapter);

        String[] statusOptions = {"Available", "Full", "Maintenance"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnStatus.setAdapter(statusAdapter);

        // Set existing values
        etRoomId.setText(room.getRoomId());

        // Set spinner selections
        int roomTypePosition = roomTypeAdapter.getPosition(room.getRoomType());
        if (roomTypePosition >= 0) spnRoomType.setSelection(roomTypePosition);

        etLocation.setText(room.getLocation());
        etPrice.setText(String.valueOf(room.getPrice()));
        etMaxCapacity.setText(String.valueOf(room.getMaxCapacity()));
        etCurrentOccupancy.setText(String.valueOf(room.getCurrentOccupancy()));

        int statusPosition = statusAdapter.getPosition(room.getStatus());
        if (statusPosition >= 0) spnStatus.setSelection(statusPosition);

        // Make room ID read-only
        etRoomId.setEnabled(false);

        // If status is "Full", show warning but still allow editing current occupancy
        if ("Full".equals(room.getStatus())) {
            etCurrentOccupancy.setEnabled(true);
            Toast.makeText(this, "Room is full. You can update current occupancy manually.", Toast.LENGTH_LONG).show();
        }

        builder.setTitle("Edit Room")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String roomType = spnRoomType.getSelectedItem().toString();
                    String location = etLocation.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    String capacityStr = etMaxCapacity.getText().toString().trim();
                    String currentOccupancyStr = etCurrentOccupancy.getText().toString().trim();
                    String status = spnStatus.getSelectedItem().toString();

                    // Validation
                    if (location.isEmpty()) {
                        etLocation.setError("Location required");
                        return;
                    }
                    if (priceStr.isEmpty()) {
                        etPrice.setError("Price required");
                        return;
                    }
                    if (capacityStr.isEmpty()) {
                        etMaxCapacity.setError("Max capacity required");
                        return;
                    }
                    if (currentOccupancyStr.isEmpty()) {
                        etCurrentOccupancy.setError("Current occupancy required");
                        return;
                    }

                    try {
                        double price = Double.parseDouble(priceStr);
                        int maxCapacity = Integer.parseInt(capacityStr);
                        int currentOccupancy = Integer.parseInt(currentOccupancyStr);

                        // Validate current occupancy doesn't exceed max capacity
                        if (currentOccupancy > maxCapacity) {
                            etCurrentOccupancy.setError("Current occupancy cannot exceed max capacity");
                            return;
                        }

                        // Auto-update status based on occupancy if needed
                        String finalStatus = status;
                        if (currentOccupancy == 0) {
                            finalStatus = "Available";
                        } else if (currentOccupancy >= maxCapacity) {
                            finalStatus = "Full";
                        }

                        updateRoom(room.getDocumentId(), roomType, location, price, maxCapacity, currentOccupancy, finalStatus);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateRoom(String documentId, String roomType, String location, double price, int maxCapacity, int currentOccupancy, String status) {
        if (isProcessing) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;

        Map<String, Object> updates = new HashMap<>();
        updates.put("roomType", roomType);
        updates.put("location", location);
        updates.put("price", price);
        updates.put("maxCapacity", maxCapacity);
        updates.put("currentOccupancy", currentOccupancy);
        updates.put("status", status);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection("Rooms").document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    isProcessing = false;
                    Toast.makeText(this, "Room updated successfully", Toast.LENGTH_SHORT).show();
                    loadRooms();
                })
                .addOnFailureListener(e -> {
                    isProcessing = false;
                    Toast.makeText(this, "Failed to update room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmDialog(RoomModel room) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Room")
                .setMessage("Are you sure you want to delete room " + room.getRoomId() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteRoom(room.getDocumentId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRoom(String documentId) {
        db.collection("Rooms").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Room deleted successfully", Toast.LENGTH_SHORT).show();
                    loadRooms();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addRoom(String roomId, String roomType, String location, double price, int maxCapacity, int currentOccupancy, String status) {
        if (isProcessing) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;

        // Use roomId as the document ID
        DocumentReference roomRef = db.collection("Rooms").document(roomId);

        roomRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        isProcessing = false;
                        Toast.makeText(this, "Room number already exists!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> roomData = new HashMap<>();
                    roomData.put("roomId", roomId);
                    roomData.put("roomType", roomType);
                    roomData.put("location", location);
                    roomData.put("price", price);
                    roomData.put("maxCapacity", maxCapacity);
                    roomData.put("currentOccupancy", currentOccupancy);
                    roomData.put("status", status);
                    roomData.put("condition", "Good");
                    roomData.put("lastUpdated", System.currentTimeMillis());

                    // Use roomId as the document name
                    roomRef.set(roomData)
                            .addOnSuccessListener(aVoid -> {
                                isProcessing = false;
                                Toast.makeText(this, "Room added successfully", Toast.LENGTH_SHORT).show();
                                loadRooms();
                            })
                            .addOnFailureListener(e -> {
                                isProcessing = false;
                                Toast.makeText(this, "Failed to add room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    isProcessing = false;
                    Toast.makeText(this, "Failed to check room existence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_rooms);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_rooms) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, UserManagementActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_management) {
                startActivity(new Intent(this, ManagementActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRooms();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_rooms);
        }
    }
}