package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AllRoomsActivity extends AppCompatActivity {

    // UI Elements
    private LinearLayout ivBack;
    private EditText etSearchInput;
    private ImageView ivClearSearch;
    private LinearLayout ivFilterButton;
    private TextView tvRoomCount;
    private RecyclerView rvRoomList;
    private TextView tvNoResults;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<RoomModel> allRooms = new ArrayList<>();
    private List<RoomModel> filteredRooms = new ArrayList<>();
    private RoomAdapter roomAdapter;

    // Search and Filter
    private String searchKeyword = "";
    private String selectedStatus = "all";
    private String selectedBlock = "all";
    private String selectedRoomType = "all";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Filter dialog views
    private TextView btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance;
    private TextView btnBlockAll, btnBlockA, btnBlockB;
    private TextView btnRoomAll, btnRoomSingle, btnRoomDouble, btnRoomQuad;
    private TextView btnClearFilters, btnApplyFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_rooms);

        // 解决键盘弹出问题 - 键盘不会顶起布局
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupSwipeRefresh();
        setupListeners();
        setupRecyclerView();
        loadAllRooms();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etSearchInput = findViewById(R.id.etSearchRoom);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        ivFilterButton = findViewById(R.id.ivFilterButton);
        tvRoomCount = findViewById(R.id.tvRoomCount);
        rvRoomList = findViewById(R.id.rvRoomList);
        tvNoResults = findViewById(R.id.tvNoResults);
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
        // Reset filters and search
        resetFilters();
        if (etSearchInput != null) {
            etSearchInput.setText("");
        }

        // Reload rooms
        loadAllRooms();

        // Stop refresh animation after data is loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1500);
    }

    private void setupListeners() {
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        if (etSearchInput != null) {
            etSearchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }

                    searchRunnable = () -> {
                        searchKeyword = s.toString().toLowerCase().trim();
                        filterAndDisplayRooms();

                        if (!searchKeyword.isEmpty() && filteredRooms.isEmpty()) {
                            showNoSearchResultsDialog(searchKeyword);
                        }
                    };
                    searchHandler.postDelayed(searchRunnable, 500);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (ivClearSearch != null) {
                        ivClearSearch.setVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }
            });
        }

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                if (etSearchInput != null) {
                    etSearchInput.setText("");
                    searchKeyword = "";
                    filterAndDisplayRooms();
                }
            });
        }

        if (ivFilterButton != null) {
            ivFilterButton.setOnClickListener(v -> showFilterBottomSheet());
        }
    }

    private void showNoSearchResultsDialog(String keyword) {
        Toast toast = Toast.makeText(this,
                "🔍 No rooms found matching \"" + keyword + "\"",
                Toast.LENGTH_LONG);
        toast.setGravity(android.view.Gravity.BOTTOM, 0, 100);
        toast.show();
    }

    private void showFilterBottomSheet() {
        View filterView = getLayoutInflater().inflate(R.layout.dialog_filter_bottom_sheet, null);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(filterView);

        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            bottomSheetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        initFilterViews(filterView);
        updateFilterUI();
        setupFilterClickListeners();

        btnApplyFilters.setOnClickListener(v -> {
            filterAndDisplayRooms();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
        });

        btnClearFilters.setOnClickListener(v -> {
            resetFilters();
            filterAndDisplayRooms();
            updateFilterUI();
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void initFilterViews(View view) {
        btnStatusAll = view.findViewById(R.id.btnStatusAll);
        btnStatusAvailable = view.findViewById(R.id.btnStatusAvailable);
        btnStatusFull = view.findViewById(R.id.btnStatusFull);
        btnStatusMaintenance = view.findViewById(R.id.btnStatusMaintenance);
        btnBlockAll = view.findViewById(R.id.btnBlockAll);
        btnBlockA = view.findViewById(R.id.btnBlockA);
        btnBlockB = view.findViewById(R.id.btnBlockB);
        btnRoomAll = view.findViewById(R.id.btnRoomAll);
        btnRoomSingle = view.findViewById(R.id.btnRoomSingle);
        btnRoomDouble = view.findViewById(R.id.btnRoomDouble);
        btnRoomQuad = view.findViewById(R.id.btnRoomQuad);
        btnClearFilters = view.findViewById(R.id.btnClearFilters);
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters);
    }

    private void updateFilterUI() {
        updateChipStyle(btnStatusAll, selectedStatus.equals("all"));
        updateChipStyle(btnStatusAvailable, selectedStatus.equals("available"));
        updateChipStyle(btnStatusFull, selectedStatus.equals("full"));
        updateChipStyle(btnStatusMaintenance, selectedStatus.equals("maintenance"));
        updateChipStyle(btnBlockAll, selectedBlock.equals("all"));
        updateChipStyle(btnBlockA, selectedBlock.equals("Block A"));
        updateChipStyle(btnBlockB, selectedBlock.equals("Block B"));
        updateChipStyle(btnRoomAll, selectedRoomType.equals("all"));
        updateChipStyle(btnRoomSingle, selectedRoomType.equals("Single Room"));
        updateChipStyle(btnRoomDouble, selectedRoomType.equals("Double Room"));
        updateChipStyle(btnRoomQuad, selectedRoomType.equals("Quad Room"));
    }

    private void updateChipStyle(TextView chip, boolean isSelected) {
        if (chip == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.filter_chip_selected);
            chip.setTextColor(getColor(android.R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.filter_chip_unselected);
            chip.setTextColor(getColor(R.color.tabInactiveText));
        }
    }

    private void setupFilterClickListeners() {
        btnStatusAll.setOnClickListener(v -> { selectedStatus = "all"; updateFilterUI(); });
        btnStatusAvailable.setOnClickListener(v -> { selectedStatus = "available"; updateFilterUI(); });
        btnStatusFull.setOnClickListener(v -> { selectedStatus = "full"; updateFilterUI(); });
        btnStatusMaintenance.setOnClickListener(v -> { selectedStatus = "maintenance"; updateFilterUI(); });

        btnBlockAll.setOnClickListener(v -> { selectedBlock = "all"; updateFilterUI(); });
        btnBlockA.setOnClickListener(v -> { selectedBlock = "Block A"; updateFilterUI(); });
        btnBlockB.setOnClickListener(v -> { selectedBlock = "Block B"; updateFilterUI(); });

        btnRoomAll.setOnClickListener(v -> { selectedRoomType = "all"; updateFilterUI(); });
        btnRoomSingle.setOnClickListener(v -> { selectedRoomType = "Single Room"; updateFilterUI(); });
        btnRoomDouble.setOnClickListener(v -> { selectedRoomType = "Double Room"; updateFilterUI(); });
        btnRoomQuad.setOnClickListener(v -> { selectedRoomType = "Quad Room"; updateFilterUI(); });
    }

    private void resetFilters() {
        selectedStatus = "all";
        selectedBlock = "all";
        selectedRoomType = "all";
        searchKeyword = "";
        if (etSearchInput != null) {
            etSearchInput.setText("");
        }
    }

    private void setupRecyclerView() {
        roomAdapter = new RoomAdapter(filteredRooms, room -> {
            Intent intent = new Intent(AllRoomsActivity.this, RoomDetailsActivity.class);
            intent.putExtra("room_id", room.getDocumentId());
            intent.putExtra("room_type", room.getRoomType());
            startActivity(intent);
        });
        rvRoomList.setLayoutManager(new LinearLayoutManager(this));
        rvRoomList.setAdapter(roomAdapter);
    }

    private void loadAllRooms() {
        db.collection("Rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRooms.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        RoomModel room = doc.toObject(RoomModel.class);
                        room.setDocumentId(doc.getId());
                        allRooms.add(room);
                    }
                    filterAndDisplayRooms();

                    // Stop refresh if still refreshing
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void filterAndDisplayRooms() {
        filteredRooms.clear();

        if (allRooms.isEmpty()) {
            updateUIForEmptyResults();
            if (tvRoomCount != null) {
                tvRoomCount.setText("0 rooms found");
            }
            return;
        }

        for (RoomModel room : allRooms) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;
            boolean matchesBlock = true;
            boolean matchesType = true;

            if (!searchKeyword.isEmpty()) {
                String roomId = room.getRoomId() != null ? room.getRoomId().toLowerCase() : "";
                String location = room.getLocation() != null ? room.getLocation().toLowerCase() : "";
                matchesSearch = roomId.contains(searchKeyword) || location.contains(searchKeyword);
            }

            if (!selectedStatus.equals("all")) {
                String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
                matchesStatus = status.equals(selectedStatus);
            }

            if (!selectedBlock.equals("all")) {
                String location = room.getLocation() != null ? room.getLocation() : "";
                matchesBlock = location.contains(selectedBlock);
            }

            if (!selectedRoomType.equals("all")) {
                String roomType = room.getRoomType() != null ? room.getRoomType() : "";
                matchesType = roomType.equals(selectedRoomType);
            }

            if (matchesSearch && matchesStatus && matchesBlock && matchesType) {
                filteredRooms.add(room);
            }
        }

        if (roomAdapter != null) {
            roomAdapter.notifyDataSetChanged();
        }

        if (filteredRooms.isEmpty()) {
            updateUIForEmptyResults();
        } else {
            updateUIForResults();
        }

        if (tvRoomCount != null) {
            tvRoomCount.setText(filteredRooms.size() + " rooms found");
        }
    }

    private void updateUIForEmptyResults() {
        if (tvNoResults != null) {
            tvNoResults.setVisibility(View.VISIBLE);
        }
        if (rvRoomList != null) {
            rvRoomList.setVisibility(View.GONE);
        }
    }

    private void updateUIForResults() {
        if (tvNoResults != null) {
            tvNoResults.setVisibility(View.GONE);
        }
        if (rvRoomList != null) {
            rvRoomList.setVisibility(View.VISIBLE);
        }
    }
}