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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RoomListActivity extends AppCompatActivity {

    private TextView tvRoomTypeTitle;
    private TextView tvRoomCount;
    private RecyclerView rvRoomList;
    private LinearLayout ivBack;
    private LinearLayout filterButton;
    private EditText etSearchRoom;
    private ImageView ivClearSearch;
    private TextView tvNoResults;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    private FirebaseFirestore db;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Data lists
    private List<RoomModel> completeRoomList = new ArrayList<>();
    private List<RoomModel> displayedRoomList = new ArrayList<>();
    private RoomAdapter adapter;

    // Cache
    private static List<RoomModel> cachedRooms = null;
    private static String cachedRoomType = "";
    private long lastLoadTime = 0;
    private static final long CACHE_DURATION = 60000; // 1分钟缓存
    private boolean isLoading = false;

    // Filter criteria
    private String selectedStatus = "all";     // all, available, full, maintenance
    private String selectedBlock = "all";      // all, Block A, Block B
    private String currentSearchQuery = "";
    private String currentActiveRoomType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        // 解决键盘弹出问题
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupSwipeRefresh();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        currentActiveRoomType = getIntent().getStringExtra("ROOM_TYPE");
        if (currentActiveRoomType != null && !currentActiveRoomType.isEmpty()) {
            updateTitleBasedOnRoomType(currentActiveRoomType);
            fetchRoomsFromFirebase(currentActiveRoomType);
        } else {
            Toast.makeText(this, "Invalid room type", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupListeners();
    }

    private void initViews() {
        tvRoomTypeTitle = findViewById(R.id.tvRoomTypeTitle);
        tvRoomCount = findViewById(R.id.tvRoomCount);
        rvRoomList = findViewById(R.id.rvRoomList);
        ivBack = findViewById(R.id.ivBack);
        filterButton = findViewById(R.id.filterButton);
        etSearchRoom = findViewById(R.id.etSearchRoomNumber);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        tvNoResults = findViewById(R.id.tvNoResults);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);

        // 优化 RecyclerView 性能
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setInitialPrefetchItemCount(4);
        rvRoomList.setLayoutManager(layoutManager);
        rvRoomList.setItemAnimator(null);
        rvRoomList.setHasFixedSize(true);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            // 设置刷新颜色 - 使用主题主色
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );

            // 设置进度背景颜色
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                    ContextCompat.getColor(this, R.color.cardBackground)
            );

            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshData();
            });

            // 只有当 ScrollView 滚动到顶部时才启用下拉刷新
            if (scrollView != null) {
                scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    if (swipeRefreshLayout != null && scrollView != null) {
                        // 检查 ScrollView 是否在顶部
                        boolean isAtTop = scrollView.getScrollY() == 0;
                        swipeRefreshLayout.setEnabled(isAtTop);
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
        selectedStatus = "all";
        selectedBlock = "all";
        currentSearchQuery = "";

        if (etSearchRoom != null) {
            etSearchRoom.setText("");
        }

        // 清除搜索框的清除图标
        if (ivClearSearch != null) {
            ivClearSearch.setVisibility(View.GONE);
        }

        // 强制刷新，清除缓存
        cachedRooms = null;

        // Reload rooms
        fetchRoomsFromFirebase(currentActiveRoomType);
    }

    private void setupListeners() {
        // Back button
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // Filter button
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterBottomSheet());
        }

        // Clear search
        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                if (etSearchRoom != null) {
                    etSearchRoom.setText("");
                    currentSearchQuery = "";
                    applyFilters();
                    ivClearSearch.setVisibility(View.GONE);
                }
            });
        }

        // Search input with delay
        if (etSearchRoom != null) {
            etSearchRoom.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }

                    searchRunnable = () -> {
                        currentSearchQuery = s.toString().toLowerCase().trim();
                        applyFilters();

                        if (!currentSearchQuery.isEmpty() && displayedRoomList.isEmpty()) {
                            showNoSearchResultsDialog(currentSearchQuery);
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
    }

    private void showNoSearchResultsDialog(String keyword) {
        Toast toast = Toast.makeText(this,
                "🔍 No rooms found matching \"" + keyword + "\"",
                Toast.LENGTH_LONG);
        toast.setGravity(android.view.Gravity.BOTTOM, 0, 100);
        toast.show();
    }

    private void updateTitleBasedOnRoomType(String roomType) {
        String displayType = "";

        if (roomType.toLowerCase().contains("single")) {
            displayType = "Single Rooms";
        } else if (roomType.toLowerCase().contains("double")) {
            displayType = "Double Rooms";
        } else if (roomType.toLowerCase().contains("quad")) {
            displayType = "Quad Rooms";
        }

        if (tvRoomTypeTitle != null) {
            tvRoomTypeTitle.setText(displayType);
        }
    }

    private void fetchRoomsFromFirebase(String type) {
        // 显示加载状态
        tvNoResults.setText("Loading...");
        tvNoResults.setVisibility(View.VISIBLE);
        rvRoomList.setVisibility(View.GONE);

        db.collection("Rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    completeRoomList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RoomModel room = document.toObject(RoomModel.class);
                        room.setDocumentId(document.getId());
                        // 手动过滤
                        if (type.equals(room.getRoomType())) {
                            completeRoomList.add(room);
                        }
                    }

                    if (adapter == null) {
                        adapter = new RoomAdapter(displayedRoomList, room -> {
                            Intent intent = new Intent(RoomListActivity.this, RoomDetailsActivity.class);
                            intent.putExtra("room_id", room.getRoomId());
                            startActivity(intent);
                        });
                        rvRoomList.setAdapter(adapter);
                    }

                    applyFilters();

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    tvNoResults.setText("Error: " + e.getMessage());
                    tvNoResults.setVisibility(View.VISIBLE);
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void showLoadingState() {
        if (tvNoResults != null) {
            tvNoResults.setText("Loading rooms...");
            tvNoResults.setVisibility(View.VISIBLE);
        }
        if (rvRoomList != null) {
            rvRoomList.setVisibility(View.GONE);
        }
    }

    private void hideLoadingState() {
        // 加载完成后由 updateUIAndCount 处理显示
    }

    private void showErrorState() {
        if (tvNoResults != null) {
            tvNoResults.setText("Failed to load rooms.\nPull down to retry");
            tvNoResults.setVisibility(View.VISIBLE);
        }
        if (rvRoomList != null) {
            rvRoomList.setVisibility(View.GONE);
        }
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showFilterBottomSheet() {
        View filterView = getLayoutInflater().inflate(R.layout.dialog_filter_bottom_sheet, null);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(filterView);

        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            bottomSheetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 初始化筛选控件
        TextView btnStatusAll = filterView.findViewById(R.id.btnStatusAll);
        TextView btnStatusAvailable = filterView.findViewById(R.id.btnStatusAvailable);
        TextView btnStatusFull = filterView.findViewById(R.id.btnStatusFull);
        TextView btnStatusMaintenance = filterView.findViewById(R.id.btnStatusMaintenance);
        TextView btnBlockAll = filterView.findViewById(R.id.btnBlockAll);
        TextView btnBlockA = filterView.findViewById(R.id.btnBlockA);
        TextView btnBlockB = filterView.findViewById(R.id.btnBlockB);
        TextView btnClearFilters = filterView.findViewById(R.id.btnClearFilters);
        TextView btnApplyFilters = filterView.findViewById(R.id.btnApplyFilters);

        // 隐藏房间类型选择区域
        View roomTypeSection = filterView.findViewById(R.id.roomTypeSection);
        if (roomTypeSection != null) {
            roomTypeSection.setVisibility(View.GONE);
        }

        // 临时变量存储当前选择
        String[] tempStatus = {selectedStatus};
        String[] tempBlock = {selectedBlock};

        // 更新UI样式
        updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);

        // 状态点击事件
        btnStatusAll.setOnClickListener(v -> {
            tempStatus[0] = "all";
            updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                    btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);
        });
        btnStatusAvailable.setOnClickListener(v -> {
            tempStatus[0] = "available";
            updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                    btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);
        });
        btnStatusFull.setOnClickListener(v -> {
            tempStatus[0] = "full";
            updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                    btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);
        });
        btnStatusMaintenance.setOnClickListener(v -> {
            tempStatus[0] = "maintenance";
            updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                    btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);
        });

        // 楼栋点击事件
        btnBlockAll.setOnClickListener(v -> {
            tempBlock[0] = "all";
            updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                    btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);
        });
        btnBlockA.setOnClickListener(v -> {
            tempBlock[0] = "Block A";
            updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                    btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);
        });
        btnBlockB.setOnClickListener(v -> {
            tempBlock[0] = "Block B";
            updateFilterUI(btnStatusAll, btnStatusAvailable, btnStatusFull, btnStatusMaintenance,
                    btnBlockAll, btnBlockA, btnBlockB, tempStatus[0], tempBlock[0]);
        });

        // 清除按钮
        btnClearFilters.setOnClickListener(v -> {
            selectedStatus = "all";
            selectedBlock = "all";
            applyFilters();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
        });

        // 应用按钮
        btnApplyFilters.setOnClickListener(v -> {
            selectedStatus = tempStatus[0];
            selectedBlock = tempBlock[0];
            applyFilters();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void updateFilterUI(TextView btnStatusAll, TextView btnStatusAvailable, TextView btnStatusFull,
                                TextView btnStatusMaintenance, TextView btnBlockAll, TextView btnBlockA,
                                TextView btnBlockB, String status, String block) {
        // 更新状态 chips
        updateChipStyle(btnStatusAll, status.equals("all"));
        updateChipStyle(btnStatusAvailable, status.equals("available"));
        updateChipStyle(btnStatusFull, status.equals("full"));
        updateChipStyle(btnStatusMaintenance, status.equals("maintenance"));

        // 更新楼栋 chips
        updateChipStyle(btnBlockAll, block.equals("all"));
        updateChipStyle(btnBlockA, block.equals("Block A"));
        updateChipStyle(btnBlockB, block.equals("Block B"));
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

    private void applyFilters() {
        displayedRoomList.clear();

        if (completeRoomList.isEmpty()) {
            updateUIAndCount();
            return;
        }

        for (RoomModel room : completeRoomList) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;
            boolean matchesBlock = true;

            // 搜索匹配：房间号
            if (!currentSearchQuery.isEmpty()) {
                String roomId = room.getRoomId() != null ? room.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(currentSearchQuery);
            }

            // 状态筛选
            if (!selectedStatus.equals("all")) {
                String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
                if (selectedStatus.equals("available")) {
                    matchesStatus = status.equals("available");
                } else if (selectedStatus.equals("full")) {
                    matchesStatus = status.equals("full");
                } else if (selectedStatus.equals("maintenance")) {
                    matchesStatus = status.equals("maintenance");
                }
            }

            // 楼栋筛选
            if (!selectedBlock.equals("all")) {
                String location = room.getLocation() != null ? room.getLocation() : "";
                matchesBlock = location.contains(selectedBlock);
            }

            if (matchesSearch && matchesStatus && matchesBlock) {
                displayedRoomList.add(room);
            }
        }

        updateUIAndCount();
    }

    private void updateUIAndCount() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // 更新房间数量
        if (tvRoomCount != null) {
            tvRoomCount.setText(displayedRoomList.size() + " rooms");
        }

        // 更新空状态显示
        if (tvNoResults != null) {
            if (displayedRoomList.isEmpty()) {
                tvNoResults.setText("No rooms found");
                tvNoResults.setVisibility(View.VISIBLE);
                rvRoomList.setVisibility(View.GONE);
            } else {
                tvNoResults.setVisibility(View.GONE);
                rvRoomList.setVisibility(View.VISIBLE);
            }
        }
    }
}