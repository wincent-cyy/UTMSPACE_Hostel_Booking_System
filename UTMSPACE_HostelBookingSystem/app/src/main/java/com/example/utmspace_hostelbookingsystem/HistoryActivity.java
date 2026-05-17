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

    // Change adapter reference to BookingAdapter for uniform list UI bindings
    private BookingAdapter adapter;
    private List<Booking> allBookings;
    private List<Booking> filteredList;

    // Firebase Components
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentStatusFilter = "Approved";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize Firebase Infrastructure
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupTabs();
        setupNavigation();
        setupListeners();

        // Load operational historical data entries
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
        // Reused your verified BookingAdapter to manage item display profiles seamlessly
        adapter = new BookingAdapter(filteredList);
        rvBookingHistory.setAdapter(adapter);
    }

    private void fetchHistoryFromFirestore() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        // Pull application documents assigned directly to the current student
        db.collection("Bookings")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allBookings.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        allBookings.add(booking);
                    }
                    // Refresh the filtered views based on active tab item selection context
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

            // Clear rejected items locally and push changes to update your Firestore environment
            db.collection("Bookings")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("status", "Rejected")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().delete();
                        }

                        // Clean data structures locally to synchronize user displays
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
        // Refresh items automatically when transitioning view focuses
        fetchHistoryFromFirestore();
        bottomNavigation.setSelectedItemId(R.id.nav_history);
    }
}