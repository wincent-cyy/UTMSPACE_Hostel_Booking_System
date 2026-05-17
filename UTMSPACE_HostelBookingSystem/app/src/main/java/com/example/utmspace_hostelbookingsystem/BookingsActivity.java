package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookingsActivity extends AppCompatActivity {

    // View Component Declarations
    private RecyclerView rvPendingBookings;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigation;

    // List & Adapter Components
    private BookingAdapter adapter;
    private List<Booking> bookingList;

    // Firebase Setup
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        // 1. Initialize Views matching layout XML IDs
        rvPendingBookings = findViewById(R.id.rvPendingBookings);
        emptyState = findViewById(R.id.emptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // 2. Setup RecyclerView Layout Configuration
        rvPendingBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        adapter = new BookingAdapter(bookingList);
        rvPendingBookings.setAdapter(adapter);

        // 3. Initialize Firebase Infrastructure
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 4. Configure operational navigation interactions
        setupBottomNavigation();

        // 5. Query and load Firestore metrics data
        fetchUserBookings();
    }

    private void fetchUserBookings() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("Bookings")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookingList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        bookingList.add(booking);
                    }

                    // Refresh UI items
                    adapter.notifyDataSetChanged();

                    // Dynamic Empty State Conditional Check
                    if (bookingList.isEmpty()) {
                        rvPendingBookings.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvPendingBookings.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading application data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNavigation() {
        // Enforce active item highlight placement context matching this tab
        bottomNavigation.setSelectedItemId(R.id.nav_booking);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_booking) {
                return true; // Already on this tab, do nothing
            }

            else if (id == R.id.nav_home) {
                // Route seamlessly back to the Home Dashboard
                Intent intent = new Intent(BookingsActivity.this, StudentDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_history) {
                // Route seamlessly to the Profile Management Screen
                Intent intent = new Intent(BookingsActivity.this, HistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }

            else if (id == R.id.nav_profile) {
                // Route seamlessly to the Profile Management Screen
                Intent intent = new Intent(BookingsActivity.this, ProfileActivity.class);
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
        // Sync items automatically when navigating back to this view scope
        fetchUserBookings();

        // Ensure navigation sync match during cross-activity execution pops
        bottomNavigation.setSelectedItemId(R.id.nav_booking);
    }
}