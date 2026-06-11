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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingManagementActivity extends AppCompatActivity {

    // UI Elements
    private EditText etSearchBooking;
    private ImageView ivClearSearch;
    private LinearLayout btnFilter;
    private TextView tvBookingCount;
    private RecyclerView rvBookings;
    private TextView tvEmptyState;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    private BookingAdapter adapter;

    // Filter variables
    private String selectedStatus = "All";
    private String selectedRoomType = "All";
    private String currentSearchQuery = "";

    // Search delay
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_management);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        db = FirebaseFirestore.getInstance();

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        setupSearchFunction();
        setupListeners();
        setupNavigation();

        fetchAllBookings();
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
        etSearchBooking = findViewById(R.id.etSearchBooking);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        btnFilter = findViewById(R.id.btnFilter);
        tvBookingCount = findViewById(R.id.tvBookingCount);
        rvBookings = findViewById(R.id.rvBookings);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);
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
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setEnabled(scrollView.getScrollY() == 0);
                    }
                });
            }
        }
    }

    private void refreshData() {
        // 重新加载所有预订数据
        fetchAllBookings();

        // 停止刷新动画
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(this, "Bookings refreshed", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void setupRecyclerView() {
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(filteredBookings, booking -> {
            Intent intent = new Intent(BookingManagementActivity.this, StaffActionActivity.class);
            intent.putExtra("BOOKING_DOC_ID", booking.getBookingId());
            intent.putExtra("BOOKING_STATUS", booking.getBookingStatus());
            intent.putExtra("REJECT_REASON", booking.getRejectReason());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_PRICE", booking.getDisplayPrice());
            intent.putExtra("STUDENT_NAME", booking.getName());
            intent.putExtra("STUDENT_ID", booking.getStudentId());
            intent.putExtra("PHONE_NUMBER", booking.getPhone());
            intent.putExtra("EMAIL", booking.getEmail());
            intent.putExtra("PROGRAMME", booking.getProgramme());
            intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
            intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());
            intent.putExtra("CREATED_AT", booking.getCreatedAt());
            intent.putExtra("userId", booking.getUid());
            startActivity(intent);
        }, true);
        rvBookings.setAdapter(adapter);
    }

    private void setupSearchFunction() {
        etSearchBooking.addTextChangedListener(new TextWatcher() {
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
                etSearchBooking.setText("");
                currentSearchQuery = "";
                applyFilters();
            });
        }
    }

    private void setupListeners() {
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        }
    }

    private void setupNavigation() {
        if (bottomNavigation == null) return;

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

    private void fetchAllBookings() {
        db.collection("Bookings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allBookings.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = new Booking();

                        // Manually set all fields from Firestore document
                        booking.setBookingId(document.getId());
                        booking.setBookingStatus(document.getString("bookingStatus"));
                        booking.setRoomId(document.getString("roomId"));
                        booking.setRoomType(document.getString("roomType"));
                        booking.setName(document.getString("name"));
                        booking.setStudentId(document.getString("studentId"));
                        booking.setPhone(document.getString("phone"));
                        booking.setEmail(document.getString("email"));
                        booking.setProgramme(document.getString("programme"));
                        booking.setCheckInDate(document.getString("checkInDate"));
                        booking.setLeaseDuration(document.getString("leaseDuration"));
                        booking.setRejectReason(document.getString("rejectReason"));

                        // Handle price (could be Double or Long)
                        Object priceObj = document.get("price");
                        if (priceObj instanceof Double) {
                            booking.setPrice((Double) priceObj);
                        } else if (priceObj instanceof Long) {
                            booking.setPrice(((Long) priceObj).doubleValue());
                        } else if (priceObj instanceof Integer) {
                            booking.setPrice(((Integer) priceObj).doubleValue());
                        } else {
                            booking.setPrice(0.0);
                        }

                        // Handle createdAt (could be Long or Timestamp)
                        Object createdAtObj = document.get("createdAt");
                        if (createdAtObj instanceof Long) {
                            booking.setCreatedAt((Long) createdAtObj);
                        } else if (createdAtObj instanceof com.google.firebase.Timestamp) {
                            booking.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).getSeconds() * 1000);
                        } else {
                            booking.setCreatedAt(System.currentTimeMillis());
                        }

                        booking.setUid(document.getString("uid"));

                        allBookings.add(booking);
                    }

                    applyFilters();

                    // Stop refresh if still refreshing
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
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

        // Action buttons
        TextView btnClear = sheetView.findViewById(R.id.btnClearFilters);
        TextView btnApply = sheetView.findViewById(R.id.btnApplyFilters);

        final String[] tempStatus = {selectedStatus};
        final String[] tempRoomType = {selectedRoomType};

        Runnable updateUI = () -> {
            resetChipStyle(btnStatusAll);
            resetChipStyle(btnStatusPending);
            resetChipStyle(btnStatusApproved);
            resetChipStyle(btnStatusRejected);
            resetChipStyle(btnStatusPaid);

            if ("Pending".equals(tempStatus[0])) {
                setChipSelected(btnStatusPending);
            } else if ("Approved".equals(tempStatus[0])) {
                setChipSelected(btnStatusApproved);
            } else if ("Rejected".equals(tempStatus[0])) {
                setChipSelected(btnStatusRejected);
            } else if ("Paid".equals(tempStatus[0])) {
                setChipSelected(btnStatusPaid);
            } else {
                setChipSelected(btnStatusAll);
            }

            resetChipStyle(btnRoomAll);
            resetChipStyle(btnRoomSingle);
            resetChipStyle(btnRoomDouble);
            resetChipStyle(btnRoomQuad);

            if ("Single".equals(tempRoomType[0])) {
                setChipSelected(btnRoomSingle);
            } else if ("Double".equals(tempRoomType[0])) {
                setChipSelected(btnRoomDouble);
            } else if ("Quad".equals(tempRoomType[0])) {
                setChipSelected(btnRoomQuad);
            } else {
                setChipSelected(btnRoomAll);
            }
        };

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
            Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
        });

        // Clear button
        btnClear.setOnClickListener(v -> {
            selectedStatus = "All";
            selectedRoomType = "All";
            currentSearchQuery = "";
            etSearchBooking.setText("");
            applyFilters();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void resetChipStyle(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        chip.setTextColor(getColor(R.color.tabInactiveText));
    }

    private void setChipSelected(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_selected);
        chip.setTextColor(getColor(android.R.color.white));
    }

    private void applyFilters() {
        filteredBookings.clear();

        for (Booking booking : allBookings) {
            boolean matchesStatus = true;
            boolean matchesRoomType = true;
            boolean matchesSearch = true;

            // Status filter
            if (!"All".equals(selectedStatus)) {
                String status = booking.getBookingStatus();
                matchesStatus = status != null && status.equalsIgnoreCase(selectedStatus);
            }

            // Room Type filter
            if (!"All".equals(selectedRoomType)) {
                String roomType = booking.getRoomType();
                if (roomType != null) {
                    if ("Single".equals(selectedRoomType)) {
                        matchesRoomType = roomType.toLowerCase().contains("single");
                    } else if ("Double".equals(selectedRoomType)) {
                        matchesRoomType = roomType.toLowerCase().contains("double");
                    } else if ("Quad".equals(selectedRoomType)) {
                        matchesRoomType = roomType.toLowerCase().contains("quad");
                    }
                }
            }

            // Search filter - by room number or student name
            if (!currentSearchQuery.isEmpty()) {
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                String roomId = booking.getRoomId() != null ? booking.getRoomId().toLowerCase() : "";
                String studentName = booking.getName() != null ? booking.getName().toLowerCase() : "";
                matchesSearch = roomId.contains(cleanQuery) || studentName.contains(cleanQuery);
            }

            if (matchesStatus && matchesRoomType && matchesSearch) {
                filteredBookings.add(booking);
            }
        }

        adapter.updateList(filteredBookings);
        tvBookingCount.setText(filteredBookings.size() + " applications");

        if (filteredBookings.isEmpty()) {
            rvBookings.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvBookings.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
        fetchAllBookings();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_staff_bookings);
        }
    }
}