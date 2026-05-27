package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffRepairTrackingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etSearch;
    private RecyclerView rvRepairRequests;
    private LinearLayout emptyState;
    private TextView tvRequestCount;

    private TextView chipAll, chipPending, chipScheduled, chipCompleted;

    private FirebaseFirestore db;
    private RepairRequestAdapter adapter;
    private List<RepairRequest> allRequestsList;
    private List<RepairRequest> filteredList;

    private String currentStatusFilter = "All";
    private String currentSearchQuery = "";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_repair_tracking);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupFilterChips();
        setupSearchFilter();
        loadRepairRequests();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        rvRepairRequests = findViewById(R.id.rvRepairRequests);
        emptyState = findViewById(R.id.emptyState);
        tvRequestCount = findViewById(R.id.tvRequestCount);

        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipScheduled = findViewById(R.id.chipScheduled);
        chipCompleted = findViewById(R.id.chipCompleted);
    }

    private void setupRecyclerView() {
        allRequestsList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvRepairRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RepairRequestAdapter(filteredList, request -> {
            Intent intent = new Intent(StaffRepairTrackingActivity.this, StaffRepairDetailActivity.class);
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

        chipScheduled.setOnClickListener(v -> {
            currentStatusFilter = "Scheduled";
            updateChipStyles(chipScheduled);
            applyFilters();
        });

        chipCompleted.setOnClickListener(v -> {
            currentStatusFilter = "Completed";
            updateChipStyles(chipCompleted);
            applyFilters();
        });
    }

    private void updateChipStyles(TextView selectedChip) {
        chipAll.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipPending.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipPending.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipScheduled.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipScheduled.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipCompleted.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipCompleted.setTextColor(android.graphics.Color.parseColor("#0369A1"));

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void setupSearchFilter() {
        // 设置回车键为搜索按钮
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    currentSearchQuery = query;
                    applyFilters();

                    // 检查搜索结果
                    checkSearchResults();
                } else {
                    currentSearchQuery = "";
                    applyFilters();
                }
                return true;
            }
            return false;
        });

        // 实时搜索（可选，保留延迟搜索）
        etSearch.addTextChangedListener(new TextWatcher() {
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

                    // 检查搜索结果（只在非空搜索时检查）
                    if (!query.isEmpty()) {
                        checkSearchResults();
                    }
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkSearchResults() {
        // 延迟一下，等待搜索结果更新
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (filteredList.isEmpty() && !currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                // 检查是否有匹配的搜索词
                boolean hasAnyMatch = false;
                for (RepairRequest request : allRequestsList) {
                    String roomId = request.getRoomId() != null ? request.getRoomId().toLowerCase() : "";
                    String itemName = request.getItemName() != null ? request.getItemName().toLowerCase() : "";
                    if (roomId.contains(cleanQuery) || itemName.contains(cleanQuery)) {
                        hasAnyMatch = true;
                        break;
                    }
                }

                if (!hasAnyMatch) {
                    Toast.makeText(StaffRepairTrackingActivity.this,
                            "No results found for: " + currentSearchQuery,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }, 100);
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
            boolean matchesStatus = true;
            if (!"All".equals(currentStatusFilter)) {
                String status = request.getStatus();
                matchesStatus = status != null && status.equalsIgnoreCase(currentStatusFilter);
            }

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

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(StaffRepairTrackingActivity.this, StaffDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRepairRequests();
    }
}