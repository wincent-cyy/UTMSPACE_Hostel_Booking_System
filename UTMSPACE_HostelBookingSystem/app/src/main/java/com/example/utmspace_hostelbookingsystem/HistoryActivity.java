package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity {

    private static final String PREF_NAME = "HistoryPrefs";
    private static final String KEY_HIDDEN_IDS = "hidden_booking_ids";

    private LinearLayout ongoingOrdersContainer;
    private LinearLayout historyOrdersContainer;
    private TextView tabOngoing;
    private TextView tabHistory;
    private TextView tvEmptyState;
    private BottomNavigationView bottomNavigationView;
    private MaterialButton btnClearHistory;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> ongoingBookings = new ArrayList<>();
    private List<Booking> historyBookings = new ArrayList<>();

    private Set<String> hiddenBookingIds = new HashSet<>();
    private boolean isOngoingSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        loadHiddenIds();

        initViews();
        setupSwipeRefresh();
        setupTabs();
        setupNavigation();

        tvEmptyState.setText("No bookings found");

        fetchBookingsFromFirestore();
    }

    private void loadHiddenIds() {
        hiddenBookingIds.clear();
        String hiddenIdsString = sharedPreferences.getString(KEY_HIDDEN_IDS, "");
        if (!hiddenIdsString.isEmpty()) {
            String[] ids = hiddenIdsString.split(",");
            for (String id : ids) {
                if (!id.isEmpty()) {
                    hiddenBookingIds.add(id);
                }
            }
        }
    }

    private void saveHiddenIds() {
        String hiddenIdsString = TextUtils.join(",", hiddenBookingIds);
        sharedPreferences.edit().putString(KEY_HIDDEN_IDS, hiddenIdsString).apply();
    }

    private void initViews() {
        ongoingOrdersContainer = findViewById(R.id.ongoingOrdersContainer);
        historyOrdersContainer = findViewById(R.id.historyOrdersContainer);
        tabOngoing = findViewById(R.id.tabOngoing);
        tabHistory = findViewById(R.id.tabHistory);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (btnClearHistory != null) {
            btnClearHistory.setVisibility(View.GONE);
        }
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
        // Reload data
        loadHiddenIds();
        fetchBookingsFromFirestore();

        // Stop refresh animation after data is loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1500);
    }

    private void setupTabs() {
        tabOngoing.setOnClickListener(v -> {
            if (!isOngoingSelected) {
                isOngoingSelected = true;
                updateTabStyles();
                displayOngoingOrders();
            }
        });

        tabHistory.setOnClickListener(v -> {
            if (isOngoingSelected) {
                isOngoingSelected = false;
                updateTabStyles();
                displayHistoryOrders();
            }
        });
    }

    private void updateTabStyles() {
        GradientDrawable selectedBg = new GradientDrawable();
        selectedBg.setColor(Color.parseColor("#800000"));
        selectedBg.setCornerRadius(30f);

        GradientDrawable unselectedBg = new GradientDrawable();
        unselectedBg.setColor(Color.TRANSPARENT);
        unselectedBg.setCornerRadius(30f);

        if (isOngoingSelected) {
            tabOngoing.setBackground(selectedBg);
            tabOngoing.setTextColor(Color.WHITE);
            tabHistory.setBackground(unselectedBg);
            tabHistory.setTextColor(Color.parseColor("#A16A5E"));
        } else {
            tabHistory.setBackground(selectedBg);
            tabHistory.setTextColor(Color.WHITE);
            tabOngoing.setBackground(unselectedBg);
            tabOngoing.setTextColor(Color.parseColor("#A16A5E"));
        }
    }

    private void fetchBookingsFromFirestore() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        String currentUid = mAuth.getCurrentUser().getUid();

        db.collection("Bookings")
                .whereEqualTo("uid", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allBookings.clear();
                    ongoingBookings.clear();
                    historyBookings.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = new Booking();

                        booking.setBookingId(document.getId());
                        booking.setUid(document.getString("uid"));
                        booking.setRoomId(document.getString("roomId"));
                        booking.setRoomType(document.getString("roomType"));
                        booking.setName(document.getString("name"));
                        booking.setMatricNumber(document.getString("matricNumber"));
                        booking.setPhone(document.getString("phone"));
                        booking.setEmail(document.getString("email"));
                        booking.setProgramme(document.getString("programme"));
                        booking.setCheckInDate(document.getString("checkInDate"));
                        booking.setLeaseDuration(document.getString("leaseDuration"));
                        booking.setBookingStatus(document.getString("bookingStatus"));
                        booking.setRejectReason(document.getString("rejectReason"));
                        booking.setLocation(document.getString("location"));

                        Double price = document.getDouble("price");
                        booking.setPrice(price != null ? price : 0);

                        Long createdAt = document.getLong("createdAt");
                        booking.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());

                        allBookings.add(booking);

                        String status = booking.getBookingStatus();
                        if (status != null) {
                            if (status.equalsIgnoreCase("Approved")) {
                                ongoingBookings.add(booking);
                            } else if (status.equalsIgnoreCase("Paid") ||
                                    status.equalsIgnoreCase("Rejected") ||
                                    status.equalsIgnoreCase("Completed") ||
                                    status.equalsIgnoreCase("Cancelled")) {
                                if (!hiddenBookingIds.contains(booking.getBookingId())) {
                                    historyBookings.add(booking);
                                }
                            }
                        }
                    }

                    updateTabStyles();

                    if (isOngoingSelected) {
                        displayOngoingOrders();
                    } else {
                        displayHistoryOrders();
                    }

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

    private void displayOngoingOrders() {
        ongoingOrdersContainer.removeAllViews();

        if (ongoingBookings.isEmpty()) {
            showEmptyState(true);
            if (btnClearHistory != null) {
                btnClearHistory.setVisibility(View.GONE);
            }
            return;
        }

        showEmptyState(false);
        ongoingOrdersContainer.setVisibility(View.VISIBLE);
        historyOrdersContainer.setVisibility(View.GONE);

        if (btnClearHistory != null) {
            btnClearHistory.setVisibility(View.GONE);
        }

        for (Booking booking : ongoingBookings) {
            View orderView = createOrderItemView(booking);
            ongoingOrdersContainer.addView(orderView);
            addDivider(ongoingOrdersContainer);
        }
    }

    private void displayHistoryOrders() {
        historyOrdersContainer.removeAllViews();

        if (historyBookings.isEmpty()) {
            showEmptyState(true);
            if (btnClearHistory != null) {
                btnClearHistory.setVisibility(View.GONE);
            }
            return;
        }

        showEmptyState(false);
        ongoingOrdersContainer.setVisibility(View.GONE);
        historyOrdersContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < historyBookings.size(); i++) {
            Booking booking = historyBookings.get(i);
            View orderView = createOrderItemView(booking);
            historyOrdersContainer.addView(orderView);

            // Add divider between cards (except last one)
            if (i < historyBookings.size() - 1) {
                addDivider(historyOrdersContainer);
            }
        }

        if (btnClearHistory != null && !historyBookings.isEmpty()) {
            btnClearHistory.setVisibility(View.VISIBLE);
            setupClearHistoryButton();
        } else if (btnClearHistory != null) {
            btnClearHistory.setVisibility(View.GONE);
        }
    }

    private void addDivider(LinearLayout container) {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                12
        );
        divider.setLayoutParams(params);
        container.addView(divider);
    }

    private View createOrderItemView(Booking booking) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_history, null);

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

        TextView tvRoomName = itemView.findViewById(R.id.tvRoomName);
        TextView tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
        TextView tvDuration = itemView.findViewById(R.id.tvDuration);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        TextView btnViewDetails = itemView.findViewById(R.id.btnViewDetails);

        String roomDisplay = booking.getRoomType() != null ? booking.getRoomType() : "Room";
        String roomId = booking.getRoomId() != null ? booking.getRoomId() : "N/A";
        tvRoomName.setText(roomDisplay + " - " + roomId);
        tvTotalPrice.setText(booking.getDisplayPrice());

        String duration = booking.getLeaseDuration() != null ? booking.getLeaseDuration() : "1 Semester";
        tvDuration.setText("Duration: " + duration);

        long createdAt = booking.getCreatedAt();
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateString = sdf.format(new Date(createdAt));
            tvDate.setText("Applied: " + dateString);
        } else {
            tvDate.setText("Applied: N/A");
        }

        String status = booking.getBookingStatus();
        if (status != null) {
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setCornerRadius(30f);

            switch (status.toLowerCase()) {
                case "approved":
                    statusBg.setColor(Color.parseColor("#DCFCE7"));
                    tvStatus.setText("Approved");
                    tvStatus.setTextColor(Color.parseColor("#15803D"));
                    break;
                case "paid":
                    statusBg.setColor(Color.parseColor("#DBEAFE"));
                    tvStatus.setText("Paid");
                    tvStatus.setTextColor(Color.parseColor("#1E40AF"));
                    break;
                case "rejected":
                    statusBg.setColor(Color.parseColor("#FEE2E2"));
                    tvStatus.setText("Rejected");
                    tvStatus.setTextColor(Color.parseColor("#B91C1C"));
                    break;
                case "completed":
                    statusBg.setColor(Color.parseColor("#DCFCE7"));
                    tvStatus.setText("Completed");
                    tvStatus.setTextColor(Color.parseColor("#15803D"));
                    break;
                case "cancelled":
                    statusBg.setColor(Color.parseColor("#FEE2E2"));
                    tvStatus.setText("Cancelled");
                    tvStatus.setTextColor(Color.parseColor("#B91C1C"));
                    break;
                default:
                    statusBg.setColor(Color.parseColor("#FEF3C7"));
                    tvStatus.setText(status);
                    tvStatus.setTextColor(Color.parseColor("#D97706"));
                    break;
            }
            tvStatus.setBackground(statusBg);
            tvStatus.setPadding(16, 8, 16, 8);
        }

        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, BookingDetailsActivity.class);
            intent.putExtra("BOOKING_DOC_ID", booking.getBookingId());
            intent.putExtra("BOOKING_STATUS", booking.getBookingStatus());
            intent.putExtra("REJECT_REASON", booking.getRejectReason());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_LOCATION", booking.getLocation());
            intent.putExtra("ROOM_PRICE", booking.getDisplayPrice());
            intent.putExtra("STUDENT_NAME", booking.getName());
            intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
            intent.putExtra("PHONE_NUMBER", booking.getPhone());
            intent.putExtra("EMAIL", booking.getEmail());
            intent.putExtra("PROGRAMME", booking.getProgramme());
            intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
            intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());
            intent.putExtra("CREATED_AT", booking.getCreatedAt());
            startActivity(intent);
        });

        return itemView;
    }

    private void showEmptyState(boolean show) {
        if (show) {
            tvEmptyState.setVisibility(View.VISIBLE);
            ongoingOrdersContainer.setVisibility(View.GONE);
            historyOrdersContainer.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void setupClearHistoryButton() {
        if (btnClearHistory == null) return;

        btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear History")
                    .setMessage("This will remove all history records from the display list. Data will remain in the database.")
                    .setPositiveButton("Clear Display", (dialog, which) -> {
                        // 将所有历史记录的 ID 加入隐藏列表
                        for (Booking booking : historyBookings) {
                            hiddenBookingIds.add(booking.getBookingId());
                        }
                        saveHiddenIds();
                        // 清空显示列表
                        historyBookings.clear();
                        displayHistoryOrders();
                        Toast.makeText(HistoryActivity.this, "History display cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setSelectedItemId(R.id.nav_history);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) return true;

            Intent intent = null;
            if (id == R.id.nav_home) {
                intent = new Intent(this, StudentDashboardActivity.class);
            } else if (id == R.id.nav_booking) {
                intent = new Intent(this, BookingsActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
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
        fetchBookingsFromFirestore();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_history);
        }
    }
}