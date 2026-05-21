package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffRoomListActivity extends AppCompatActivity {

    private RecyclerView rvRoomList;
    private EditText etSearchRoom;
    private LinearLayout emptyState;
    private TextView tvRoomCount;
    private BottomNavigationView bottomNavigation;

    // Filter chips
    private TextView chipAll, chipAvailable, chipFull, chipMaintenance;

    private FirebaseFirestore db;
    private StaffRoomAdapter adapter;
    private List<RoomModel> masterRoomList;
    private List<RoomModel> filteredRoomList;

    private String currentStatusFilter = "All";
    private String currentSearchQuery = "";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_room_list);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupFilterChips();
        setupRecyclerView();
        setupSearchFilter();
        setupNavigation();
        fetchAllRooms();
    }

    private void initViews() {
        rvRoomList = findViewById(R.id.rvRoomList);
        etSearchRoom = findViewById(R.id.etSearchRoom);
        emptyState = findViewById(R.id.emptyState);
        tvRoomCount = findViewById(R.id.tvRoomCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Initialize filter chips
        chipAll = findViewById(R.id.chipAll);
        chipAvailable = findViewById(R.id.chipAvailable);
        chipFull = findViewById(R.id.chipFull);
        chipMaintenance = findViewById(R.id.chipMaintenance);
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
        // Reset all chips to unselected
        chipAll.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipAvailable.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipAvailable.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipFull.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipFull.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipMaintenance.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipMaintenance.setTextColor(android.graphics.Color.parseColor("#0369A1"));

        // Set selected chip style
        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void setupRecyclerView() {
        masterRoomList = new ArrayList<>();
        filteredRoomList = new ArrayList<>();

        rvRoomList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StaffRoomAdapter(filteredRoomList, room -> {
            Intent intent = new Intent(StaffRoomListActivity.this, StaffRoomDetailActivity.class);
            intent.putExtra("ROOM_DOC_ID", room.getDocumentId());
            intent.putExtra("ROOM_ID", room.getRoomId());
            intent.putExtra("ROOM_TYPE", room.getRoomType());
            intent.putExtra("ROOM_STATUS", room.getStatus());
            intent.putExtra("ROOM_LOCATION", room.getLocation());
            intent.putExtra("ROOM_PRICE", room.getPrice());
            intent.putExtra("ROOM_MAX_CAPACITY", room.getMaxCapacity());
            intent.putExtra("ROOM_CURRENT_OCCUPANCY", room.getCurrentOccupancy());
            intent.putExtra("ROOM_CONDITION", room.getCondition());
            startActivity(intent);
        });

        rvRoomList.setAdapter(adapter);
    }

    private void fetchAllRooms() {
        db.collection("Rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterRoomList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RoomModel room = document.toObject(RoomModel.class);
                        room.setDocumentId(document.getId());
                        masterRoomList.add(room);
                    }

                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void applyFilters() {
        filteredRoomList.clear();

        for (RoomModel room : masterRoomList) {
            // 1. Status filter based on selected chip
            boolean matchesStatus = true;
            switch (currentStatusFilter) {
                case "Available":
                    matchesStatus = "Available".equalsIgnoreCase(room.getStatus());
                    break;
                case "Full":
                    matchesStatus = "Full".equalsIgnoreCase(room.getStatus());
                    break;
                case "Maintenance":
                    matchesStatus = "Maintenance".equalsIgnoreCase(room.getStatus()) ||
                            "Under Maintenance".equalsIgnoreCase(room.getCondition());
                    break;
                case "All":
                default:
                    matchesStatus = true;
                    break;
            }

            // 2. Search filter - only search room number
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                String roomId = room.getRoomId() != null ? room.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(cleanQuery);
            }

            if (matchesStatus && matchesSearch) {
                filteredRoomList.add(room);
            }
        }

        // Update empty state
        if (filteredRoomList.isEmpty()) {
            rvRoomList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvRoomList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
        updateRoomCount();
    }

    private void updateRoomCount() {
        if (tvRoomCount != null) {
            tvRoomCount.setText(filteredRoomList.size() + " rooms found");
        }
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_rooms);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_rooms) {
                return true;
            } else if (id == R.id.nav_staff_home) {
                startActivity(new Intent(this, StaffDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_staff_bookings) {
                startActivity(new Intent(this, BookingManagementActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllRooms();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_rooms);
        }
    }
}