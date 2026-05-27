package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TechnicianHistoryActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView rvRepairRequests;
    private TextView tvEmptyState;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnClearHistory;

    private FirebaseFirestore db;
    private TechnicianHistoryAdapter adapter;
    private List<RepairRequest> scheduledList;
    private List<RepairRequest> completedList;
    private List<RepairRequest> currentList;

    private String currentTab = "Scheduled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_history);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupTabs();
        setupBottomNavigation();
        loadRepairRequests();
        setupClickListeners();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        rvRepairRequests = findViewById(R.id.rvRepairRequests);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnClearHistory = findViewById(R.id.btnClearHistory);
    }

    private void setupRecyclerView() {
        scheduledList = new ArrayList<>();
        completedList = new ArrayList<>();
        currentList = new ArrayList<>();

        rvRepairRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianHistoryAdapter(currentList, request -> {
            Intent intent = new Intent(TechnicianHistoryActivity.this, TechnicianRepairDetailActivity.class);
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

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    currentTab = "Scheduled";
                    showScheduledList();
                } else {
                    currentTab = "Completed";
                    showCompletedList();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadRepairRequests() {
        db.collection("RepairRequests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    scheduledList.clear();
                    completedList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequest request = document.toObject(RepairRequest.class);
                        request.setDocumentId(document.getId());

                        String status = request.getStatus();
                        if ("In Progress".equalsIgnoreCase(status)) {
                            scheduledList.add(request);
                        } else if ("Completed".equalsIgnoreCase(status)) {
                            completedList.add(request);
                        }
                    }

                    if (currentTab.equals("Scheduled")) {
                        showScheduledList();
                    } else {
                        showCompletedList();
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmptyState.setText("Failed to load: " + e.getMessage());
                    tvEmptyState.setVisibility(View.VISIBLE);
                });
    }

    private void showScheduledList() {
        currentList.clear();
        currentList.addAll(scheduledList);
        adapter.notifyDataSetChanged();

        if (currentList.isEmpty()) {
            rvRepairRequests.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No scheduled tasks");
        } else {
            rvRepairRequests.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showCompletedList() {
        currentList.clear();
        currentList.addAll(completedList);
        adapter.notifyDataSetChanged();

        if (currentList.isEmpty()) {
            rvRepairRequests.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No completed tasks");
        } else {
            rvRepairRequests.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_tech_history);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_tech_history) {
                return true;
            } else if (id == R.id.nav_tech_home) {
                startActivity(new Intent(TechnicianHistoryActivity.this, TechnicianDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_request) {
                startActivity(new Intent(TechnicianHistoryActivity.this, TechnicianRepairRequestActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(TechnicianHistoryActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        btnClearHistory.setOnClickListener(v -> {
            // Clear only completed tasks (optional)
            if (currentTab.equals("Completed") && !completedList.isEmpty()) {
                // This just clears local display, not Firestore
                completedList.clear();
                showCompletedList();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRepairRequests();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_tech_history);
        }
    }
}