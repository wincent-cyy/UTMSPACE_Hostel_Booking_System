package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffRoomListActivity extends AppCompatActivity {

    private static final String TAG = "StaffRoomList";

    // UI Elements
    private RecyclerView rvRoomList;
    private EditText etSearchRoom;
    private ImageView ivClearSearch;
    private LinearLayout emptyState;
    private TextView tvRoomCount;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Filter chips
    private TextView chipAll, chipAvailable, chipFull, chipMaintenance;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<RoomModel> allRooms = new ArrayList<>();
    private List<RoomModel> filteredRooms = new ArrayList<>();
    private StaffRoomAdapter adapter;

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
        setContentView(R.layout.activity_staff_room_list);

        // 设置软键盘模式 - 防止导航栏上移
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupSwipeRefresh();
        setupFilterChips();
        setupRecyclerView();
        setupSearchFunction();
        setupNavigation();
        fetchAllRooms();
    }

    private void initViews() {
        rvRoomList = findViewById(R.id.rvRoomList);
        etSearchRoom = findViewById(R.id.etSearchRoom);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        emptyState = findViewById(R.id.emptyState);
        tvRoomCount = findViewById(R.id.tvRoomCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Filter chips
        chipAll = findViewById(R.id.chipAll);
        chipAvailable = findViewById(R.id.chipAvailable);
        chipFull = findViewById(R.id.chipFull);
        chipMaintenance = findViewById(R.id.chipMaintenance);

        // 设置搜索框焦点监听
        etSearchRoom.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                rvRoomList.postDelayed(() -> rvRoomList.smoothScrollToPosition(0), 100);
            }
        });
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
        }
    }

    private void refreshData() {
        if (isLoading) {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        // Reset filters
        currentStatusFilter = "All";
        currentSearchQuery = "";

        if (etSearchRoom != null) {
            etSearchRoom.setText("");
        }
        if (ivClearSearch != null) {
            ivClearSearch.setVisibility(View.GONE);
        }

        // Update chips UI
        updateChipStyles(chipAll);

        // Reload rooms
        fetchAllRooms();
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
        resetChipStyle(chipAll);
        resetChipStyle(chipAvailable);
        resetChipStyle(chipFull);
        resetChipStyle(chipMaintenance);

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void resetChipStyle(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        chip.setTextColor(getColor(R.color.tabInactiveText));
    }

    private void setupRecyclerView() {
        rvRoomList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StaffRoomAdapter(filteredRooms, room -> {
            Intent intent = new Intent(StaffRoomListActivity.this, StaffRoomDetailActivity.class);
            intent.putExtra("ROOM_DOC_ID", room.getDocumentId());
            intent.putExtra("ROOM_ID", room.getRoomId());
            intent.putExtra("ROOM_TYPE", room.getRoomType());
            intent.putExtra("ROOM_STATUS", room.getStatus());
            intent.putExtra("ROOM_LOCATION", room.getLocation());
            intent.putExtra("ROOM_PRICE", room.getPrice());
            intent.putExtra("ROOM_MAX_CAPACITY", room.getMaxCapacity());
            intent.putExtra("ROOM_CURRENT_OCCUPANCY", room.getCurrentOccupancy());
            startActivity(intent);
        });
        rvRoomList.setAdapter(adapter);
    }

    private void fetchAllRooms() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping");
            return;
        }

        isLoading = true;
        Log.d(TAG, "Loading rooms from Firestore");

        // 显示加载状态
        showLoadingState();

        long startTime = System.currentTimeMillis();

        db.collection("Rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, "Query completed in: " + (endTime - startTime) + " ms");
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " rooms");

                    allRooms.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RoomModel room = document.toObject(RoomModel.class);
                        room.setDocumentId(document.getId());
                        allRooms.add(room);
                    }

                    applyFilters();

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load rooms: " + e.getMessage());
                    Toast.makeText(this, "Failed to load rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    isLoading = false;

                    // 显示错误状态
                    showErrorState();
                });
    }

    private void showLoadingState() {
        runOnUiThread(() -> {
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                TextView emptyText = emptyState.findViewById(R.id.tvEmptyTitle);
                if (emptyText != null) {
                    emptyText.setText("Loading rooms...");
                }
            }
            if (rvRoomList != null) {
                rvRoomList.setVisibility(View.GONE);
            }
        });
    }

    private void showErrorState() {
        runOnUiThread(() -> {
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                TextView emptyText = emptyState.findViewById(R.id.tvEmptyTitle);
                if (emptyText != null) {
                    emptyText.setText("Failed to load rooms\nPull down to retry");
                }
            }
            if (rvRoomList != null) {
                rvRoomList.setVisibility(View.GONE);
            }
        });
    }

    private void setupSearchFunction() {
        etSearchRoom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString();

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
                etSearchRoom.setText("");
                currentSearchQuery = "";
                applyFilters();
            });
        }
    }

    private void applyFilters() {
        filteredRooms.clear();

        for (RoomModel room : allRooms) {
            // 1. Status filter
            boolean matchesStatus = true;
            switch (currentStatusFilter) {
                case "Available":
                    matchesStatus = "Available".equalsIgnoreCase(room.getStatus());
                    break;
                case "Full":
                    matchesStatus = "Full".equalsIgnoreCase(room.getStatus());
                    break;
                case "Maintenance":
                    matchesStatus = "Maintenance".equalsIgnoreCase(room.getStatus());
                    break;
                case "All":
                default:
                    matchesStatus = true;
                    break;
            }

            // 2. Search filter - by room number only
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                String roomId = room.getRoomId() != null ? room.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(cleanQuery);
            }

            if (matchesStatus && matchesSearch) {
                filteredRooms.add(room);
            }
        }

        // Update UI
        runOnUiThread(() -> {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            updateRoomCount();
            updateEmptyState();
        });
    }

    private void updateRoomCount() {
        if (tvRoomCount != null) {
            tvRoomCount.setText(filteredRooms.size() + " rooms");
        }
    }

    private void updateEmptyState() {
        if (filteredRooms.isEmpty()) {
            if (rvRoomList != null) {
                rvRoomList.setVisibility(View.GONE);
            }
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                TextView emptyText = emptyState.findViewById(R.id.tvEmptyTitle);
                if (emptyText != null) {
                    if (allRooms.isEmpty() && !isLoading) {
                        emptyText.setText("No rooms available");
                    } else {
                        emptyText.setText("No rooms match your filters");
                    }
                }
            }
        } else {
            if (rvRoomList != null) {
                rvRoomList.setVisibility(View.VISIBLE);
            }
            if (emptyState != null) {
                emptyState.setVisibility(View.GONE);
            }
        }
    }

    private void setupNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_rooms);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_rooms) {
                return true;
            } else if (id == R.id.nav_staff_home) {
                Intent intent = new Intent(this, StaffDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_staff_bookings) {
                Intent intent = new Intent(this, BookingManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
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