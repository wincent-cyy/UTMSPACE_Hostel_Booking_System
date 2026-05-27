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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TechnicianRepairRequestActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvRepairRequests;
    private LinearLayout emptyState;
    private TextView tvRequestCount;
    private BottomNavigationView bottomNavigation;

    private FirebaseFirestore db;
    private TechnicianRepairAdapter adapter;
    private List<RepairRequest> allRequestsList;
    private List<RepairRequest> filteredList;

    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_repair_request);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupSearchFilter();
        loadRepairRequests();
        setupBottomNavigation();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        rvRepairRequests = findViewById(R.id.rvRepairRequests);
        emptyState = findViewById(R.id.emptyState);
        tvRequestCount = findViewById(R.id.tvRequestCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupRecyclerView() {
        allRequestsList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvRepairRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianRepairAdapter(filteredList, request -> {
            Intent intent = new Intent(TechnicianRepairRequestActivity.this, TechnicianRepairDetailActivity.class);
            intent.putExtra("REQUEST_ID", request.getDocumentId());
            intent.putExtra("ROOM_ID", request.getRoomId());
            intent.putExtra("ITEM_NAME", request.getItemName());
            intent.putExtra("URGENCY", request.getUrgency());
            intent.putExtra("DESCRIPTION", request.getDescription());
            intent.putExtra("STATUS", request.getStatus());
            intent.putExtra("STAFF_NAME", request.getStaffName());
            intent.putExtra("CREATED_AT", request.getCreatedAt());
            startActivity(intent);
        });
        rvRepairRequests.setAdapter(adapter);
    }

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadRepairRequests() {
        db.collection("RepairRequests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRequestsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequest request = document.toObject(RepairRequest.class);
                        request.setDocumentId(document.getId());
                        allRequestsList.add(request);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        filteredList.clear();

        for (RepairRequest request : allRequestsList) {
            String status = request.getStatus();
            boolean matchesStatus = status != null && status.equalsIgnoreCase("Pending");

            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                String roomId = request.getRoomId() != null ? request.getRoomId().toLowerCase() : "";
                String itemName = request.getItemName() != null ? request.getItemName().toLowerCase() : "";
                matchesSearch = roomId.contains(cleanQuery) || itemName.contains(cleanQuery);
            }

            if (matchesStatus && matchesSearch) {
                filteredList.add(request);
            }
        }

        if (filteredList.isEmpty()) {
            rvRepairRequests.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvRepairRequests.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        tvRequestCount.setText(filteredList.size() + " requests");
        adapter.notifyDataSetChanged();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_request);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_request) {
                return true;
            } else if (id == R.id.nav_tech_home) {
                startActivity(new Intent(TechnicianRepairRequestActivity.this, TechnicianDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_tech_history) {
                startActivity(new Intent(TechnicianRepairRequestActivity.this, TechnicianHistoryActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(TechnicianRepairRequestActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRepairRequests();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_request);
        }
    }
}