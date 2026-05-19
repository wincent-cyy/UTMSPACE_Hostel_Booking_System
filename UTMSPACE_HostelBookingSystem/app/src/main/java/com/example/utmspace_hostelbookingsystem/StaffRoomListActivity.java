package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
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

    private FirebaseFirestore db;
    private StaffRoomAdapter adapter;
    private List<RoomModel> masterRoomList;
    private List<RoomModel> filteredRoomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_room_list);

        db = FirebaseFirestore.getInstance();

        initViews();
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
    }

    private void setupRecyclerView() {
        masterRoomList = new ArrayList<>();
        filteredRoomList = new ArrayList<>();

        rvRoomList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StaffRoomAdapter(filteredRoomList, room -> {
            Intent intent = new Intent(StaffRoomListActivity.this, StaffRoomDetailActivity.class);
            intent.putExtra("ROOM_DOC_ID", room.getDocumentId());
            intent.putExtra("ROOM_NUMBER", room.getRoomNumber());
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
                    updateRoomCount();
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
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilters() {
        filteredRoomList.clear();
        String searchQuery = etSearchRoom.getText().toString().toLowerCase().trim();

        for (RoomModel room : masterRoomList) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    room.getRoomNumber().toLowerCase().contains(searchQuery) ||
                    room.getLocation().toLowerCase().contains(searchQuery);

            if (matchesSearch) {
                filteredRoomList.add(room);
            }
        }

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