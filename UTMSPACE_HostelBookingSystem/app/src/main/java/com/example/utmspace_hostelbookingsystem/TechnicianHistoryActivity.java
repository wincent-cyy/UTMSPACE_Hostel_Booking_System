package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TechnicianHistoryActivity extends AppCompatActivity {

    private static final String PREF_NAME = "TechnicianHistoryPrefs";
    private static final String KEY_HIDDEN_IDS = "hidden_completed_ids";

    // Tab 按钮
    private TextView tabInProgress;
    private TextView tabCompleted;

    private LinearLayout inProgressContainer;
    private LinearLayout completedContainer;
    private TextView tvEmptyState;
    private TextView tvCounterLabel, tvCounterValue;
    private EditText etSearchHistory;
    private ImageView ivClearSearch;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnClearHistory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    private FirebaseFirestore db;
    private List<RepairRequest> inProgressList;
    private List<RepairRequest> completedList;
    private boolean isInProgressSelected = true;
    private String currentSearchQuery = "";
    private boolean isLoading = false;

    private SharedPreferences sharedPreferences;
    private Set<String> hiddenCompletedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_history);

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        loadHiddenIds();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupSwipeRefresh();
        setupTabs();
        setupSearchFunction();
        setupNavigation();
        setupClickListeners();
        fetchRepairRequests();
    }

    private void loadHiddenIds() {
        hiddenCompletedIds.clear();
        String hiddenIdsString = sharedPreferences.getString(KEY_HIDDEN_IDS, "");
        if (!hiddenIdsString.isEmpty()) {
            String[] ids = hiddenIdsString.split(",");
            for (String id : ids) {
                if (!id.isEmpty()) {
                    hiddenCompletedIds.add(id);
                }
            }
        }
    }

    private void saveHiddenIds() {
        String hiddenIdsString = TextUtils.join(",", hiddenCompletedIds);
        sharedPreferences.edit().putString(KEY_HIDDEN_IDS, hiddenIdsString).apply();
    }

    private void initViews() {
        // Tab 按钮
        tabInProgress = findViewById(R.id.tabInProgress);
        tabCompleted = findViewById(R.id.tabCompleted);

        // 容器
        inProgressContainer = findViewById(R.id.inProgressContainer);
        completedContainer = findViewById(R.id.completedContainer);

        // 其他视图
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvCounterLabel = findViewById(R.id.tvCounterLabel);
        tvCounterValue = findViewById(R.id.tvCounterValue);
        etSearchHistory = findViewById(R.id.etSearchHistory);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);

        if (btnClearHistory != null) {
            btnClearHistory.setVisibility(View.GONE);
        }

        inProgressList = new ArrayList<>();
        completedList = new ArrayList<>();
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
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

        // Reset search
        currentSearchQuery = "";
        if (etSearchHistory != null) {
            etSearchHistory.setText("");
        }
        if (ivClearSearch != null) {
            ivClearSearch.setVisibility(View.GONE);
        }

        // Reload data
        fetchRepairRequests();

        // Stop refresh animation after data is loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1500);
    }

    private void setupTabs() {
        tabInProgress.setOnClickListener(v -> {
            if (!isInProgressSelected) {
                isInProgressSelected = true;
                updateTabStyles();
                displayInProgressOrders();
                btnClearHistory.setVisibility(View.GONE);
            }
        });

        tabCompleted.setOnClickListener(v -> {
            if (isInProgressSelected) {
                isInProgressSelected = false;
                updateTabStyles();
                displayCompletedOrders();
                btnClearHistory.setVisibility(View.VISIBLE);
            }
        });

        // 默认选中 In Progress
        updateTabStyles();
    }

    private void updateTabStyles() {
        GradientDrawable selectedBg = new GradientDrawable();
        selectedBg.setColor(Color.parseColor("#800000"));
        selectedBg.setCornerRadius(30f);

        GradientDrawable unselectedBg = new GradientDrawable();
        unselectedBg.setColor(Color.TRANSPARENT);
        unselectedBg.setCornerRadius(30f);

        if (isInProgressSelected) {
            tabInProgress.setBackground(selectedBg);
            tabInProgress.setTextColor(Color.WHITE);
            tabCompleted.setBackground(unselectedBg);
            tabCompleted.setTextColor(Color.parseColor("#A16A5E"));
        } else {
            tabCompleted.setBackground(selectedBg);
            tabCompleted.setTextColor(Color.WHITE);
            tabInProgress.setBackground(unselectedBg);
            tabInProgress.setTextColor(Color.parseColor("#A16A5E"));
        }
    }

    private void setupSearchFunction() {
        etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();

                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }

                if (isInProgressSelected) {
                    displayInProgressOrders();
                } else {
                    displayCompletedOrders();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etSearchHistory.setText("");
                currentSearchQuery = "";
                if (isInProgressSelected) {
                    displayInProgressOrders();
                } else {
                    displayCompletedOrders();
                }
            });
        }
    }

    private void fetchRepairRequests() {
        if (isLoading) return;

        isLoading = true;

        db.collection("RepairRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    inProgressList.clear();
                    completedList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequest request = createRequestFromDocument(document);
                        String status = request.getStatus();

                        if ("In Progress".equalsIgnoreCase(status)) {
                            inProgressList.add(request);
                        } else if ("Completed".equalsIgnoreCase(status)) {
                            if (!hiddenCompletedIds.contains(request.getDocumentId())) {
                                completedList.add(request);
                            }
                        }
                    }

                    // 显示当前选中的列表
                    if (isInProgressSelected) {
                        displayInProgressOrders();
                    } else {
                        displayCompletedOrders();
                    }

                    isLoading = false;

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                    isLoading = false;
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private RepairRequest createRequestFromDocument(QueryDocumentSnapshot document) {
        RepairRequest request = new RepairRequest();
        request.setDocumentId(document.getId());
        request.setRoomId(document.getString("roomId"));
        request.setRoomType(document.getString("roomType"));
        request.setIssueType(document.getString("issueType"));
        request.setPriority(document.getString("priority"));
        request.setDescription(document.getString("description"));
        request.setStatus(document.getString("status"));
        request.setName(document.getString("name"));

        Long createdAt = document.getLong("createdAt");
        request.setCreatedAt(createdAt != null ? createdAt : 0);

        request.setAvailableTime(document.getString("availableTime"));
        request.setContactPerson(document.getString("contactPerson"));
        request.setProofImage(document.getString("proofImage"));
        return request;
    }

    private void displayInProgressOrders() {
        inProgressContainer.removeAllViews();

        List<RepairRequest> filteredList = new ArrayList<>();
        for (RepairRequest request : inProgressList) {
            if (matchesSearch(request)) {
                filteredList.add(request);
            }
        }

        tvCounterLabel.setText("In Progress Requests");
        tvCounterValue.setText(String.valueOf(filteredList.size()));

        if (filteredList.isEmpty()) {
            showEmptyState(true);
            return;
        }

        showEmptyState(false);
        inProgressContainer.setVisibility(View.VISIBLE);
        completedContainer.setVisibility(View.GONE);

        for (int i = 0; i < filteredList.size(); i++) {
            RepairRequest request = filteredList.get(i);
            View orderView = createOrderItemView(request);
            inProgressContainer.addView(orderView);

            // 添加间距
            if (i < filteredList.size() - 1) {
                addDivider(inProgressContainer);
            }
        }
    }

    private void displayCompletedOrders() {
        completedContainer.removeAllViews();

        List<RepairRequest> filteredList = new ArrayList<>();
        for (RepairRequest request : completedList) {
            if (matchesSearch(request)) {
                filteredList.add(request);
            }
        }

        tvCounterLabel.setText("Completed Requests");
        tvCounterValue.setText(String.valueOf(filteredList.size()));

        if (filteredList.isEmpty()) {
            showEmptyState(true);
            return;
        }

        showEmptyState(false);
        inProgressContainer.setVisibility(View.GONE);
        completedContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < filteredList.size(); i++) {
            RepairRequest request = filteredList.get(i);
            View orderView = createOrderItemView(request);
            completedContainer.addView(orderView);

            // 添加间距
            if (i < filteredList.size() - 1) {
                addDivider(completedContainer);
            }
        }
    }

    private void addDivider(LinearLayout container) {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 12
        );
        divider.setLayoutParams(params);
        container.addView(divider);
    }

    private boolean matchesSearch(RepairRequest request) {
        if (currentSearchQuery.isEmpty()) return true;
        String roomId = request.getRoomId() != null ? request.getRoomId().toLowerCase() : "";
        return roomId.contains(currentSearchQuery);
    }

    private View createOrderItemView(RepairRequest request) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_technician_repair, null);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(16f);
        cardBg.setStroke(1, Color.parseColor("#E0E0E0"));
        itemView.setBackground(cardBg);

        itemView.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 0;
        itemView.setLayoutParams(params);

        TextView tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
        TextView tvIssueType = itemView.findViewById(R.id.tvIssueType);
        TextView tvDescription = itemView.findViewById(R.id.tvDescription);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        TextView tvPriority = itemView.findViewById(R.id.tvPriority);
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        LinearLayout btnDetails = itemView.findViewById(R.id.btnStartRepair);

        tvRoomNumber.setText(request.getRoomId() != null ? request.getRoomId() : "N/A");
        tvIssueType.setText(request.getIssueType() != null ? request.getIssueType() : "N/A");
        tvDescription.setText(request.getDescription() != null ? request.getDescription() : "No description");

        String status = request.getStatus();
        if (status != null) {
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setCornerRadius(30f);

            if (status.equalsIgnoreCase("In Progress")) {
                statusBg.setColor(Color.parseColor("#3B82F6"));
                tvStatus.setText("In Progress");
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setBackground(statusBg);
                tvStatus.setPadding(16, 8, 16, 8);
            } else if (status.equalsIgnoreCase("Completed")) {
                statusBg.setColor(Color.parseColor("#10B981"));
                tvStatus.setText("Completed");
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setBackground(statusBg);
                tvStatus.setPadding(16, 8, 16, 8);
            }
        }

        String priority = request.getPriority();
        tvPriority.setText(priority != null ? priority : "Medium");

        long createdAt = request.getCreatedAt();
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(createdAt)));
        } else {
            tvDate.setText("N/A");
        }

        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(TechnicianHistoryActivity.this, TechnicianRepairDetailActivity.class);
            intent.putExtra("REQUEST_ID", request.getDocumentId());
            intent.putExtra("roomId", request.getRoomId());
            intent.putExtra("roomType", request.getRoomType());
            intent.putExtra("issueType", request.getIssueType());
            intent.putExtra("priority", request.getPriority());
            intent.putExtra("description", request.getDescription());
            intent.putExtra("status", request.getStatus());
            intent.putExtra("name", request.getName());
            intent.putExtra("createdAt", request.getCreatedAt());
            startActivity(intent);
        });

        return itemView;
    }

    private void showEmptyState(boolean show) {
        if (show) {
            tvEmptyState.setVisibility(View.VISIBLE);
            inProgressContainer.setVisibility(View.GONE);
            completedContainer.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        if (btnClearHistory == null) return;

        btnClearHistory.setOnClickListener(v -> {
            if (!completedList.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Clear History")
                        .setMessage("This will remove all completed repair requests from the display list. Data will remain in the database.")
                        .setPositiveButton("Clear Display", (dialog, which) -> {
                            for (RepairRequest request : completedList) {
                                hiddenCompletedIds.add(request.getDocumentId());
                            }
                            saveHiddenIds();
                            completedList.clear();
                            displayCompletedOrders();
                            Toast.makeText(TechnicianHistoryActivity.this, "History display cleared", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                Toast.makeText(this, "No completed requests to clear", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_tech_history);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_tech_history) {
                return true;
            } else if (id == R.id.nav_tech_home) {
                Intent intent = new Intent(this, TechnicianDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_request) {
                Intent intent = new Intent(this, TechnicianRepairRequestActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRepairRequests();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_tech_history);
        }
    }
}