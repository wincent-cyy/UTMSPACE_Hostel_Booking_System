package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TechnicianRepairRequestActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageView ivClearSearch;
    private RecyclerView rvRepairRequests;
    private LinearLayout emptyState;
    private TextView tvRequestCount;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private TechnicianRepairAdapter adapter;
    private List<RepairRequest> allRequestsList;
    private List<RepairRequest> filteredList;

    private String currentSearchQuery = "";
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_repair_request);

        // 解决键盘弹出问题 - 防止导航栏上移
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        setupSearchFilter();
        setupBottomNavigation();
        loadRepairRequests();

        // FIXED: 监听键盘显示/隐藏，自动隐藏导航栏
        setupKeyboardListener();
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

    /**
     * FIXED: 监听键盘显示/隐藏，当键盘弹出时隐藏底部导航栏
     */
    private void setupKeyboardListener() {
        if (etSearch == null) return;

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 键盘弹出时隐藏底部导航栏
                if (bottomNavigation != null) {
                    bottomNavigation.setVisibility(View.GONE);
                }
            } else {
                // 键盘隐藏时显示底部导航栏
                if (bottomNavigation != null) {
                    bottomNavigation.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        rvRepairRequests = findViewById(R.id.rvRepairList);
        emptyState = findViewById(R.id.emptyState);
        tvRequestCount = findViewById(R.id.tvRequestCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshData();
            });
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
        if (etSearch != null) {
            etSearch.setText("");
        }
        if (ivClearSearch != null) {
            ivClearSearch.setVisibility(View.GONE);
        }

        // Clear focus from search to hide keyboard and show navigation
        if (etSearch != null) {
            etSearch.clearFocus();
        }

        // Reload data
        loadRepairRequests();

        // Stop refresh animation after data is loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1500);
    }

    private void setupRecyclerView() {
        allRequestsList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvRepairRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianRepairAdapter(filteredList, request -> {
            Intent intent = new Intent(TechnicianRepairRequestActivity.this, TechnicianRepairDetailActivity.class);
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
            intent.putExtra("proofImage", request.getProofImage());
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
                currentSearchQuery = s.toString().toLowerCase().trim();

                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }

                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                currentSearchQuery = "";
                applyFilters();
                // 清除焦点以隐藏键盘和显示导航栏
                etSearch.clearFocus();
                if (bottomNavigation != null) {
                    bottomNavigation.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void loadRepairRequests() {
        if (isLoading) return;

        isLoading = true;

        db.collection("RepairRequests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRequestsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequest request = new RepairRequest();
                        request.setDocumentId(document.getId());
                        request.setRoomId(document.getString("roomId"));
                        request.setRoomType(document.getString("roomType"));
                        request.setIssueType(document.getString("issueType"));
                        request.setPriority(document.getString("priority"));
                        request.setDescription(document.getString("description"));
                        request.setStatus(document.getString("status"));
                        request.setName(document.getString("name"));
                        request.setCreatedAt(document.getLong("createdAt") != null ? document.getLong("createdAt") : 0);
                        request.setAvailableTime(document.getString("availableTime"));
                        request.setContactPerson(document.getString("contactPerson"));
                        request.setProofImage(document.getString("proofImage"));

                        allRequestsList.add(request);
                    }
                    applyFilters();
                    isLoading = false;

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isLoading = false;
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void applyFilters() {
        filteredList.clear();

        for (RepairRequest request : allRequestsList) {
            String status = request.getStatus();
            // 只显示 Pending 状态的请求
            boolean matchesStatus = status != null && status.equalsIgnoreCase("Pending");

            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String roomId = request.getRoomId() != null ? request.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(currentSearchQuery);
            }

            if (matchesStatus && matchesSearch) {
                filteredList.add(request);
            }
        }

        // Update adapter
        adapter.updateList(filteredList);

        // Update count
        tvRequestCount.setText(filteredList.size() + " requests");

        // Update empty state
        if (filteredList.isEmpty()) {
            rvRepairRequests.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvRepairRequests.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_request);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_request) {
                return true;
            } else if (id == R.id.nav_tech_home) {
                Intent intent = new Intent(TechnicianRepairRequestActivity.this, TechnicianDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_tech_history) {
                Intent intent = new Intent(TechnicianRepairRequestActivity.this, TechnicianHistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(TechnicianRepairRequestActivity.this, ProfileActivity.class);
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
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
        loadRepairRequests();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_request);
        }
    }
}