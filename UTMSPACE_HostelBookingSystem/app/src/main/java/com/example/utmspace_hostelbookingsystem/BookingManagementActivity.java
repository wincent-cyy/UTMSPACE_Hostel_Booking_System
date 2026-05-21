package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookingManagementActivity extends AppCompatActivity {

    // UI Structural Elements
    private EditText etStaffSearch;
    private RecyclerView rvStaffBookings;
    private LinearLayout staffEmptyState;
    private BottomNavigationView bottomNavigation;

    // Database Architecture & Lists
    private FirebaseFirestore db;
    private BookingAdapter adapter;
    private List<Booking> masterAllBookingsList;
    private List<Booking> filteredBookingsList;

    private String selectedStatus = "All";
    private String selectedRoomType = "All";  // ✅ 新增
    private String currentSearchQuery = "";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_management);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupSearchFilter();
        setupNavigation();

        fetchAllBookingsFromFirestore();
    }

    private void initViews() {
        etStaffSearch = findViewById(R.id.etStaffSearch);
        rvStaffBookings = findViewById(R.id.rvStaffBookings);
        staffEmptyState = findViewById(R.id.staffEmptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        View btnFilter = findViewById(R.id.btnListFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        }
    }

    private void setupRecyclerView() {
        masterAllBookingsList = new ArrayList<>();
        filteredBookingsList = new ArrayList<>();

        rvStaffBookings.setLayoutManager(new LinearLayoutManager(this));

        // ✅ 修正: 使用新的字段名
        adapter = new BookingAdapter(filteredBookingsList, booking -> {
            Intent intent = new Intent(BookingManagementActivity.this, StaffActionActivity.class);

            // ✅ 修正: 使用新的 getter 方法
            intent.putExtra("BOOKING_DOC_ID", booking.getBookingId());
            intent.putExtra("BOOKING_STATUS", booking.getBookingStatus());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_PRICE", booking.getDisplayPrice());

            // ✅ 修正: 从 Users 继承的字段
            intent.putExtra("STUDENT_NAME", booking.getName());
            intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
            intent.putExtra("PHONE_NUMBER", booking.getPhone());
            intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
            intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());

            // ✅ 修正: 外键 uid
            intent.putExtra("userId", booking.getUid());

            startActivity(intent);
        }, true);

        rvStaffBookings.setAdapter(adapter);
    }

    private void fetchAllBookingsFromFirestore() {
        db.collection("Bookings")
                .whereIn("bookingStatus", Arrays.asList("Pending", "Approved", "Rejected", "Paid"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterAllBookingsList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        booking.setBookingId(document.getId());
                        masterAllBookingsList.add(booking);
                    }

                    applyFilters();  // ✅ 改为调用 applyFilters()
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookingManagementActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSearchFilter() {
        // 禁止自动大写
        etStaffSearch.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        // 限制只能输入字母、数字和连字符
        InputFilter roomNumberFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '-') {
                    return "";
                }
            }
            return null;
        };
        etStaffSearch.setFilters(new InputFilter[]{roomNumberFilter, new InputFilter.LengthFilter(10)});

        etStaffSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 取消之前的延迟任务
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // 保存当前搜索词
                String query = s.toString();

                // 创建新的延迟任务（500ms 后执行）
                searchRunnable = () -> {
                    currentSearchQuery = query;
                    applyFilters();

                    // 验证房间号格式（只在用户停止输入后检查）
                    if (!query.isEmpty()) {
                        String cleanQuery = query.toLowerCase().trim();
                        boolean isValidRoomFormat = cleanQuery.matches("^[A-Za-z]-?\\d+$") || cleanQuery.matches("^[A-Za-z]\\d+$");

                        if (!isValidRoomFormat) {
                            Toast.makeText(BookingManagementActivity.this,
                                    "Please enter a valid Room Number (e.g., A-101, A101)",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                // 延迟 500ms 执行
                searchHandler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_staff_bookings);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_staff_bookings) {
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_staff_home) {
                intent = new Intent(this, StaffDashboardActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            } else if (itemId == R.id.nav_rooms) {
                intent = new Intent(this, StaffRoomListActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_staff_filter, null);
        bottomSheetDialog.setContentView(sheetView);

        // Status buttons
        TextView btnStatusAll = sheetView.findViewById(R.id.btnStatusAll);
        TextView btnStatusPending = sheetView.findViewById(R.id.btnStatusPending);
        TextView btnStatusApproved = sheetView.findViewById(R.id.btnStatusApproved);
        TextView btnStatusRejected = sheetView.findViewById(R.id.btnStatusRejected);
        TextView btnStatusPaid = sheetView.findViewById(R.id.btnStatusPaid);

        // Room Type buttons
        TextView btnRoomAll = sheetView.findViewById(R.id.btnRoomAll);
        TextView btnRoomSingle = sheetView.findViewById(R.id.btnRoomSingle);
        TextView btnRoomDouble = sheetView.findViewById(R.id.btnRoomDouble);
        TextView btnRoomQuad = sheetView.findViewById(R.id.btnRoomQuad);

        MaterialButton btnClear = sheetView.findViewById(R.id.btnClearFilters);
        MaterialButton btnApply = sheetView.findViewById(R.id.btnApplyFilters);

        // Track temporary selections
        final String[] tempStatus = {selectedStatus};
        final String[] tempRoomType = {selectedRoomType};

        // ✅ 使用原来的颜色设置方式
        Runnable updateUI = () -> {
            // 1. Reset and update Status chips
            btnStatusAll.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnStatusAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
            btnStatusPending.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnStatusPending.setTextColor(android.graphics.Color.parseColor("#0369A1"));
            btnStatusApproved.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnStatusApproved.setTextColor(android.graphics.Color.parseColor("#0369A1"));
            btnStatusRejected.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnStatusRejected.setTextColor(android.graphics.Color.parseColor("#0369A1"));
            btnStatusPaid.setBackgroundResource(R.drawable.filter_chip_unselected);  // ✅ 添加
            btnStatusPaid.setTextColor(android.graphics.Color.parseColor("#0369A1"));

            if (tempStatus[0].equals("Pending")) {
                btnStatusPending.setBackgroundResource(R.drawable.filter_chip_selected);
                btnStatusPending.setTextColor(android.graphics.Color.WHITE);
            } else if (tempStatus[0].equals("Approved")) {
                btnStatusApproved.setBackgroundResource(R.drawable.filter_chip_selected);
                btnStatusApproved.setTextColor(android.graphics.Color.WHITE);
            } else if (tempStatus[0].equals("Rejected")) {
                btnStatusRejected.setBackgroundResource(R.drawable.filter_chip_selected);
                btnStatusRejected.setTextColor(android.graphics.Color.WHITE);
            } else if (tempStatus[0].equals("Paid")) {  // ✅ 添加
                btnStatusPaid.setBackgroundResource(R.drawable.filter_chip_selected);
                btnStatusPaid.setTextColor(android.graphics.Color.WHITE);
            } else if (tempStatus[0].equals("All")) {
                btnStatusAll.setBackgroundResource(R.drawable.filter_chip_selected);
                btnStatusAll.setTextColor(android.graphics.Color.WHITE);
            }

            // 2. Reset and update Room Type chips
            btnRoomAll.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnRoomAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
            btnRoomSingle.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnRoomSingle.setTextColor(android.graphics.Color.parseColor("#0369A1"));
            btnRoomDouble.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnRoomDouble.setTextColor(android.graphics.Color.parseColor("#0369A1"));
            btnRoomQuad.setBackgroundResource(R.drawable.filter_chip_unselected);
            btnRoomQuad.setTextColor(android.graphics.Color.parseColor("#0369A1"));

            if (tempRoomType[0].equals("Single")) {
                btnRoomSingle.setBackgroundResource(R.drawable.filter_chip_selected);
                btnRoomSingle.setTextColor(android.graphics.Color.WHITE);
            } else if (tempRoomType[0].equals("Double")) {
                btnRoomDouble.setBackgroundResource(R.drawable.filter_chip_selected);
                btnRoomDouble.setTextColor(android.graphics.Color.WHITE);
            } else if (tempRoomType[0].equals("Quad")) {
                btnRoomQuad.setBackgroundResource(R.drawable.filter_chip_selected);
                btnRoomQuad.setTextColor(android.graphics.Color.WHITE);
            } else if (tempRoomType[0].equals("All")) {
                btnRoomAll.setBackgroundResource(R.drawable.filter_chip_selected);
                btnRoomAll.setTextColor(android.graphics.Color.WHITE);
            }
        };

        // Execute baseline selection state rendering
        updateUI.run();

        // Status click listeners
        btnStatusAll.setOnClickListener(v -> { tempStatus[0] = "All"; updateUI.run(); });
        btnStatusPending.setOnClickListener(v -> { tempStatus[0] = "Pending"; updateUI.run(); });
        btnStatusApproved.setOnClickListener(v -> { tempStatus[0] = "Approved"; updateUI.run(); });
        btnStatusRejected.setOnClickListener(v -> { tempStatus[0] = "Rejected"; updateUI.run(); });
        btnStatusPaid.setOnClickListener(v -> { tempStatus[0] = "Paid"; updateUI.run(); });

        // Room Type click listeners
        btnRoomAll.setOnClickListener(v -> { tempRoomType[0] = "All"; updateUI.run(); });
        btnRoomSingle.setOnClickListener(v -> { tempRoomType[0] = "Single"; updateUI.run(); });
        btnRoomDouble.setOnClickListener(v -> { tempRoomType[0] = "Double"; updateUI.run(); });
        btnRoomQuad.setOnClickListener(v -> { tempRoomType[0] = "Quad"; updateUI.run(); });

        // Apply button
        btnApply.setOnClickListener(v -> {
            selectedStatus = tempStatus[0];
            selectedRoomType = tempRoomType[0];
            applyFilters();
            bottomSheetDialog.dismiss();
        });

        // Clear button
        btnClear.setOnClickListener(v -> {
            selectedStatus = "All";
            selectedRoomType = "All";
            currentSearchQuery = "";
            etStaffSearch.setText("");
            applyFilters();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void applyFilters() {
        filteredBookingsList.clear();

        for (Booking booking : masterAllBookingsList) {
            // Status filter
            boolean matchesStatus = true;
            if (!"All".equals(selectedStatus)) {
                String status = booking.getBookingStatus();
                matchesStatus = status != null && status.equalsIgnoreCase(selectedStatus);
            }

            // Room Type filter
            boolean matchesRoomType = true;
            if (!"All".equals(selectedRoomType)) {
                String roomType = booking.getRoomType();
                matchesRoomType = roomType != null && roomType.toLowerCase().contains(selectedRoomType.toLowerCase());
            }

            // Search filter
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();

                // 验证输入是否为有效的房间号格式
                boolean isValidRoomFormat = cleanQuery.matches("^[A-Za-z]-?\\d+$") || cleanQuery.matches("^[A-Za-z]\\d+$");

                if (!isValidRoomFormat) {
                    matchesSearch = false;
                } else {
                    String roomId = booking.getRoomId() != null ? booking.getRoomId().toLowerCase() : "";
                    matchesSearch = roomId.contains(cleanQuery);
                }
            }

            if (matchesStatus && matchesRoomType && matchesSearch) {
                filteredBookingsList.add(booking);
            }
        }

        // ✅ 只在搜索完成且没有结果时显示一次提示
        if (filteredBookingsList.isEmpty() && !currentSearchQuery.isEmpty()) {
            String cleanQuery = currentSearchQuery.toLowerCase().trim();
            boolean isValidRoomFormat = cleanQuery.matches("^[A-Za-z]-?\\d+$") || cleanQuery.matches("^[A-Za-z]\\d+$");
            if (isValidRoomFormat) {
                Toast.makeText(this, "No room found with number: " + currentSearchQuery, Toast.LENGTH_SHORT).show();
            }
        }

        // Update empty state
        if (filteredBookingsList.isEmpty()) {
            rvStaffBookings.setVisibility(View.GONE);
            staffEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvStaffBookings.setVisibility(View.VISIBLE);
            staffEmptyState.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllBookingsFromFirestore();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_staff_bookings);
        }
    }
}