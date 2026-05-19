package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvBookingHistory;
    private BottomNavigationView bottomNavigation;
    private TabLayout tabLayout;
    private MaterialButton btnClearHistory;

    // Reference pointer variable for Empty State placeholder layout
    private TextView tvEmptyState;

    private BookingAdapter adapter;
    private List<Booking> allBookings;
    private List<Booking> filteredList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // 當前選擇的分頁名稱預設為 Ongoing
    private String currentTabName = "Ongoing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupTabs();
        setupNavigation();
        setupListeners();
    }

    private void initViews() {
        rvBookingHistory = findViewById(R.id.rvBookingHistory);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tabLayout = findViewById(R.id.tabLayout);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        // Connect empty state text reference placeholder node
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // 預設隱藏清除按鈕（因為第一頁是 Ongoing）
        if (btnClearHistory != null) {
            btnClearHistory.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        allBookings = new ArrayList<>();
        filteredList = new ArrayList<>();

        if (rvBookingHistory != null) {
            rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));

            adapter = new BookingAdapter(filteredList, booking -> {
                Intent intent = new Intent(HistoryActivity.this, BookingDetailsActivity.class);
                passBookingDataIntent(intent, booking);
                startActivity(intent);
            });

            adapter.setOnPaymentClickListener(booking -> {
                Intent intent = new Intent(HistoryActivity.this, PaymentActivity.class);
                passBookingDataIntent(intent, booking);
                startActivity(intent);
            });

            rvBookingHistory.setAdapter(adapter);
        }
    }

    private void passBookingDataIntent(Intent intent, Booking booking) {
        intent.putExtra("BOOKING_DOC_ID", booking.getDocumentId());
        intent.putExtra("BOOKING_STATUS", booking.getStatus());
        intent.putExtra("ROOM_ID", booking.getRoomId());
        intent.putExtra("ROOM_TYPE", booking.getRoomType());
        intent.putExtra("ROOM_PRICE", booking.getRoomPrice());
        intent.putExtra("STUDENT_NAME", booking.getStudentName());
        intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
        intent.putExtra("PHONE_NUMBER", booking.getPhoneNumber());
        intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
        intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());
        intent.putExtra("REJECT_REASON", booking.getRejectReason());
    }

    private void fetchHistoryFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("Bookings")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allBookings.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        booking.setDocumentId(document.getId());
                        allBookings.add(booking);
                    }

                    // 資料載入後，根據目前畫面上亮起的 Tab 進行過濾
                    evaluateCurrentTabState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Firebase Loading Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupTabs() {
        if (tabLayout == null) return;

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab != null && tab.getText() != null) {
                    currentTabName = tab.getText().toString().trim();
                    filterBookingsByTab(currentTabName);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab != null && tab.getText() != null) {
                    currentTabName = tab.getText().toString().trim();
                    filterBookingsByTab(currentTabName);
                }
            }
        });
    }

    private void evaluateCurrentTabState() {
        if (tabLayout == null) {
            filterBookingsByTab(currentTabName);
            return;
        }

        int index = tabLayout.getSelectedTabPosition();
        if (index != TabLayout.Tab.INVALID_POSITION && index >= 0) {
            TabLayout.Tab currentTab = tabLayout.getTabAt(index);
            if (currentTab != null && currentTab.getText() != null) {
                currentTabName = currentTab.getText().toString().trim();
            }
        }
        filterBookingsByTab(currentTabName);
    }

    private void filterBookingsByTab(String tabName) {
        if (filteredList == null) filteredList = new ArrayList<>();
        filteredList.clear();

        // FIXED: Button visibility rules now execute reliably based on active layout position tags
        if (btnClearHistory != null) {
            if (tabName.equalsIgnoreCase("History")) {
                btnClearHistory.setVisibility(View.VISIBLE);
            } else {
                btnClearHistory.setVisibility(View.GONE);
            }
        }

        for (Booking b : allBookings) {
            if (b.getStatus() != null) {
                String status = b.getStatus().trim();

                if (tabName.equalsIgnoreCase("Ongoing")) {
                    // Ongoing 分頁：只顯示 Approved
                    if (status.equalsIgnoreCase("Approved")) {
                        filteredList.add(b);
                    }
                } else if (tabName.equalsIgnoreCase("History")) {
                    // History 分頁：Paid 和 Rejected 通通放進來！
                    if (status.equalsIgnoreCase("Paid") || status.equalsIgnoreCase("Rejected")) {
                        filteredList.add(b);
                    }
                }
            }
        }

        // FIXED: Empty List Verification Layout Visibility Engine Blocks Toggle
        if (tvEmptyState != null) {
            if (filteredList.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                rvBookingHistory.setVisibility(View.GONE); // Hide layout recyclerview container for cleanliness
            } else {
                tvEmptyState.setVisibility(View.GONE);
                rvBookingHistory.setVisibility(View.VISIBLE);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupListeners() {
        if (btnClearHistory == null) return;

        btnClearHistory.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) return;
            String currentUserId = mAuth.getCurrentUser().getUid();

            db.collection("Bookings")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String status = document.getString("status");
                            if (status != null && (status.equalsIgnoreCase("Rejected") || status.equalsIgnoreCase("Paid"))) {
                                document.getReference().delete();
                            }
                        }
                        allBookings.removeIf(b -> b.getStatus() != null &&
                                ("Rejected".equalsIgnoreCase(b.getStatus().trim()) || "Paid".equalsIgnoreCase(b.getStatus().trim())));

                        // Re-evaluate filters automatically after dataset is purged from memory
                        filterBookingsByTab("History");
                        Toast.makeText(HistoryActivity.this, "History cleared.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setupNavigation() {
        if (bottomNavigation == null) return;
        bottomNavigation.setSelectedItemId(R.id.nav_history);
        bottomNavigation.setOnItemSelectedListener(item -> {
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
        fetchHistoryFromFirestore();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_history);
        }
    }
}