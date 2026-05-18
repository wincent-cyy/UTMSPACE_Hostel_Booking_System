package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    private BookingAdapter adapter;
    private List<Booking> allBookings;
    private List<Booking> filteredList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // FIXED: Shifted from UPPERCASE to Title Case to match your exact Firestore structure safely
    private String currentStatusFilter = "Approved";

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

        fetchHistoryFromFirestore();
    }

    private void initViews() {
        rvBookingHistory = findViewById(R.id.rvBookingHistory);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tabLayout = findViewById(R.id.tabLayout);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        if (btnClearHistory != null) btnClearHistory.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        allBookings = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));

        // Handler 1: Clicking the card structure anywhere shows detailed parameters profile
        adapter = new BookingAdapter(filteredList, booking -> {
            Intent intent = new Intent(HistoryActivity.this, BookingDetailsActivity.class);
            passBookingDataIntent(intent, booking);
            startActivity(intent);
        });

        // Handler 2: Explicitly handles the specialized Payment action button routes
        adapter.setOnPaymentClickListener(booking -> {
            Intent intent = new Intent(HistoryActivity.this, PaymentActivity.class);
            passBookingDataIntent(intent, booking);
            startActivity(intent);
        });

        rvBookingHistory.setAdapter(adapter);
    }

    // Helper method to keep dynamic navigation parameter injections clean
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
        intent.putExtra("REJECT_REASON", booking.getRejectReason()); // Pass reason if it exists
    }

    private void fetchHistoryFromFirestore() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    filterBookings(currentStatusFilter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load history data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupTabs() {
        if (tabLayout == null) return;

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // FIXED: Normalizes evaluation targets to correct Title Case rules
                if (tab.getPosition() == 0) {
                    currentStatusFilter = "Approved";
                    filterBookings(currentStatusFilter);
                    if (btnClearHistory != null) btnClearHistory.setVisibility(View.GONE);
                } else {
                    currentStatusFilter = "Rejected";
                    filterBookings(currentStatusFilter);
                    if (btnClearHistory != null) btnClearHistory.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterBookings(String status) {
        if (filteredList == null) filteredList = new ArrayList<>();
        filteredList.clear();

        for (Booking b : allBookings) {
            if (b.getStatus() != null && b.getStatus().equalsIgnoreCase(status)) {
                filteredList.add(b);
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
                    .whereEqualTo("status", "Rejected") // Title Case
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().delete();
                        }
                        allBookings.removeIf(b -> b.getStatus() != null && b.getStatus().equalsIgnoreCase("Rejected"));
                        filterBookings("Rejected");
                        Toast.makeText(HistoryActivity.this, "Rejected history cleared.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(HistoryActivity.this, "Error clearing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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