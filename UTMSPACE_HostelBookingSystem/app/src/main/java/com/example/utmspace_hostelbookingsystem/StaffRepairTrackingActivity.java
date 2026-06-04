package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffRepairTrackingActivity extends AppCompatActivity {

    private static final String TAG = "StaffRepairTracking";

    // UI Elements
    private LinearLayout ivBack;
    private EditText etSearchRepair;
    private ImageView ivClearSearch;
    private RecyclerView rvRepairList;
    private TextView tvEmptyState;
    private TextView tvRequestCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

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

    // Loading state
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_repair_tracking);

        // 解决键盘弹出问题
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        setupFilterChips();
        setupSearchFunction();
        setupClickListeners();
        loadRepairRequests();
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
        etSearchRepair = findViewById(R.id.etSearchRepair);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        rvRepairList = findViewById(R.id.rvRepairList);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvRequestCount = findViewById(R.id.tvRequestCount);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);

        // Filter chips
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipInProgress = findViewById(R.id.chipInProgress);
        chipCompleted = findViewById(R.id.chipCompleted);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                    ContextCompat.getColor(this, R.color.cardBackground)
            );

            swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d(TAG, "Swipe refresh triggered");
                refreshData();
            });

            // 只有当 ScrollView 滚动到顶部时才启用下拉刷新
            if (scrollView != null) {
                scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    if (swipeRefreshLayout != null && scrollView != null) {
                        swipeRefreshLayout.setEnabled(scrollView.getScrollY() == 0);
                    }
                });
            }
        }
    }

    private void refreshData() {
        if (isLoading) {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        // Reset filters and search
        currentStatusFilter = "All";
        currentSearchQuery = "";

        if (etSearchRepair != null) {
            etSearchRepair.setText("");
        }
        if (ivClearSearch != null) {
            ivClearSearch.setVisibility(View.GONE);
        }

        // Update chips UI
        updateChipStyles(chipAll);

        // Reload data
        loadRepairRequests();
    }

    private void setupRecyclerView() {
        rvRepairList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RepairRequestAdapter(filteredRequests, request -> {
            Intent intent = new Intent(StaffRepairTrackingActivity.this, StaffRepairDetailActivity.class);

            intent.putExtra("REQUEST_ID", request.getDocumentId());
            intent.putExtra("roomId", request.getRoomId());
            intent.putExtra("roomType", request.getRoomType());
            intent.putExtra("issueType", request.getIssueType());
            intent.putExtra("priority", request.getPriority());
            intent.putExtra("description", request.getDescription());
            intent.putExtra("status", request.getStatus());
            intent.putExtra("name", request.getName());
            intent.putExtra("createdAt", request.getCreatedAt());
            intent.putExtra("availableTime", request.getAvailableTime());
            intent.putExtra("contactPerson", request.getContactPerson());
            intent.putExtra("completionPhoto", request.getCompletionPhoto());

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
        resetChipStyle(chipAll);
        resetChipStyle(chipPending);
        resetChipStyle(chipInProgress);
        resetChipStyle(chipCompleted);

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

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etSearchRepair.setText("");
                currentSearchQuery = "";
                applyFilters();
            });
        }
    }

    private void loadRepairRequests() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping");
            return;
        }

        isLoading = true;
        Log.d(TAG, "Loading repair requests from Firestore");

        // 显示加载状态
        showLoadingState();

        long startTime = System.currentTimeMillis();

        db.collection("RepairRequests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, "Query completed in: " + (endTime - startTime) + " ms");
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " requests");

                    allRequests.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequest request = document.toObject(RepairRequest.class);
                        request.setDocumentId(document.getId());
                        allRequests.add(request);
                    }

                    applyFilters();

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load requests: " + e.getMessage());
                    Toast.makeText(this, "Failed to load requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    isLoading = false;

                    showErrorState();
                });
    }

    private void showLoadingState() {
        runOnUiThread(() -> {
            if (tvEmptyState != null) {
                tvEmptyState.setText("Loading requests...");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
            if (rvRepairList != null) {
                rvRepairList.setVisibility(View.GONE);
            }
        });
    }

    private void showErrorState() {
        runOnUiThread(() -> {
            if (tvEmptyState != null) {
                tvEmptyState.setText("Failed to load requests\nPull down to retry");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
            if (rvRepairList != null) {
                rvRepairList.setVisibility(View.GONE);
            }
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

        // Update UI
        runOnUiThread(() -> {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            tvRequestCount.setText(filteredRequests.size() + " requests");

            if (filteredRequests.isEmpty()) {
                if (rvRepairList != null) {
                    rvRepairList.setVisibility(View.GONE);
                }
                if (tvEmptyState != null) {
                    if (allRequests.isEmpty() && !isLoading) {
                        tvEmptyState.setText("No repair requests available");
                    } else {
                        tvEmptyState.setText("No requests match your filters");
                    }
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                if (rvRepairList != null) {
                    rvRepairList.setVisibility(View.VISIBLE);
                }
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
        loadRepairRequests();
    }
}