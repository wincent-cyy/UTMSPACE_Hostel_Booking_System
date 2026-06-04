package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffRepairTrackingActivity extends AppCompatActivity {

    // UI Elements
    private LinearLayout ivBack;
    private EditText etSearchRepair;
    private ImageView ivClearSearch;
    private RecyclerView rvRepairList;
    private TextView tvEmptyState;
    private TextView tvRequestCount;

    // Filter Chips
    private TextView chipAll, chipPending, chipInProgress, chipCompleted;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<RepairRequest> allRequests = new ArrayList<>();
    private List<RepairRequest> filteredRequests = new ArrayList<>();
    private RepairRequestAdapter adapter;

    // Filter variables
    private String currentStatusFilter = "All";
    private String currentSearchQuery = "";

    // Search delay
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_repair_tracking);

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupRecyclerView();
        setupFilterChips();
        setupSearchFunction();
        setupClickListeners();
        loadRepairRequests();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etSearchRepair = findViewById(R.id.etSearchRepair);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        rvRepairList = findViewById(R.id.rvRepairList);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvRequestCount = findViewById(R.id.tvRequestCount);

        // Filter chips
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipInProgress = findViewById(R.id.chipInProgress);
        chipCompleted = findViewById(R.id.chipCompleted);
    }

    private void setupRecyclerView() {
        rvRepairList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RepairRequestAdapter(filteredRequests, request -> {
            Intent intent = new Intent(StaffRepairTrackingActivity.this, StaffRepairDetailActivity.class);

            // 使用统一的 key 名称（全部小写，与 Firestore 一致）
            intent.putExtra("REQUEST_ID", request.getDocumentId());
            intent.putExtra("roomId", request.getRoomId());
            intent.putExtra("roomType", request.getRoomType());
            intent.putExtra("issueType", request.getIssueType());
            intent.putExtra("priority", request.getPriority());
            intent.putExtra("description", request.getDescription());
            intent.putExtra("status", request.getStatus());
            intent.putExtra("name", request.getName());
            intent.putExtra("createdAt", request.getCreatedAt());
            intent.putExtra("availableTime", request.getAvailableTime());  // 改为小写 availableTime

            startActivity(intent);
        });
        rvRepairList.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            currentStatusFilter = "All";
            updateChipStyles(chipAll);
            applyFilters();
        });

        chipPending.setOnClickListener(v -> {
            currentStatusFilter = "Pending";
            updateChipStyles(chipPending);
            applyFilters();
        });

        chipInProgress.setOnClickListener(v -> {
            currentStatusFilter = "In Progress";
            updateChipStyles(chipInProgress);
            applyFilters();
        });

        chipCompleted.setOnClickListener(v -> {
            currentStatusFilter = "Completed";
            updateChipStyles(chipCompleted);
            applyFilters();
        });
    }

    private void updateChipStyles(TextView selectedChip) {
        // Reset all chips
        resetChipStyle(chipAll);
        resetChipStyle(chipPending);
        resetChipStyle(chipInProgress);
        resetChipStyle(chipCompleted);

        // Set selected chip style
        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void resetChipStyle(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        chip.setTextColor(getColor(R.color.tabInactiveText));
    }

    private void setupSearchFunction() {
        etSearchRepair.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                final String query = s.toString();

                // Show/hide clear button
                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }

                searchRunnable = () -> {
                    currentSearchQuery = query;
                    applyFilters();
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etSearchRepair.setText("");
                currentSearchQuery = "";
                applyFilters();
            });
        }
    }

    private void loadRepairRequests() {
        db.collection("RepairRequests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRequests.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // 添加日志查看原始数据
                        android.util.Log.d("StaffRepairTracking", "=== Document Data ===");
                        android.util.Log.d("StaffRepairTracking", "createdAt raw: " + document.getLong("createdAt"));
                        android.util.Log.d("StaffRepairTracking", "availableTime: " + document.getString("availableTime"));

                        RepairRequest request = document.toObject(RepairRequest.class);
                        request.setDocumentId(document.getId());

                        // 添加日志查看转换后的数据
                        android.util.Log.d("StaffRepairTracking", "after toObject - createdAt: " + request.getCreatedAt());
                        android.util.Log.d("StaffRepairTracking", "after toObject - availableTime: " + request.getAvailableTime());

                        allRequests.add(request);
                    }

                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        filteredRequests.clear();

        for (RepairRequest request : allRequests) {
            // Status filter
            boolean matchesStatus = true;
            switch (currentStatusFilter) {
                case "Pending":
                    matchesStatus = "Pending".equalsIgnoreCase(request.getStatus());
                    break;
                case "In Progress":
                    matchesStatus = "In Progress".equalsIgnoreCase(request.getStatus());
                    break;
                case "Completed":
                    matchesStatus = "Completed".equalsIgnoreCase(request.getStatus());
                    break;
                case "All":
                default:
                    matchesStatus = true;
                    break;
            }

            // Search filter - by room ID
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                String roomId = request.getRoomId() != null ? request.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(cleanQuery);
            }

            if (matchesStatus && matchesSearch) {
                filteredRequests.add(request);
            }
        }

        // Update adapter
        adapter.notifyDataSetChanged();

        // Update count
        tvRequestCount.setText(filteredRequests.size() + " requests");

        // Update empty state
        if (filteredRequests.isEmpty()) {
            rvRepairList.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvRepairList.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRepairRequests();
    }
}