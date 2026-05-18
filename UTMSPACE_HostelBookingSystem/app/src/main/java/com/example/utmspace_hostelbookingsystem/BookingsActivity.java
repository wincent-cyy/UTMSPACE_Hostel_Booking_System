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

        // 3. Initialize Adapter with an OnItemClickListener implementation
        adapter = new BookingAdapter(bookingList, booking -> {
            // When a pending card item is clicked, pass the payload data to the real details page
            Intent intent = new Intent(BookingsActivity.this, BookingDetailsActivity.class);

            intent.putExtra("BOOKING_DOC_ID", booking.getDocumentId());
            intent.putExtra("BOOKING_STATUS", booking.getStatus());
            intent.putExtra("REJECT_REASON", booking.getRejectReason());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_PRICE", booking.getRoomPrice());

            intent.putExtra("STUDENT_NAME", booking.getStudentName());
            intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
            intent.putExtra("PHONE_NUMBER", booking.getPhoneNumber());
            intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
            intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());

            startActivity(intent);
        });

        // FIXED: Bind an empty implementation placeholder for the custom payment click listener
        // to stay clean and compliant with our upgraded adapter structure.
        adapter.setOnPaymentClickListener(booking -> {
            // Under normal parameters, a "Pending" application won't trigger this text action,
            // but binding it here prevents null pointer tracking issues.
            Intent intent = new Intent(BookingsActivity.this, PaymentActivity.class);
            intent.putExtra("BOOKING_DOC_ID", booking.getDocumentId());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_PRICE", booking.getRoomPrice());
            startActivity(intent);
        });

        rvPendingBookings.setAdapter(adapter);

        // 4. Initialize Firebase Infrastructure
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 5. Configure operational navigation interactions
        setupBottomNavigation();

        // 6. Query and load Firestore metrics data
        fetchUserBookings();
    }

    private void fetchUserBookings() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        // IMPROVED FIXED: Query targeting changed from uppercase "PENDING" to PascalCase "Pending".
        // This instantly corrects the case-sensitive filter match with the database cluster nodes.
        db.collection("Bookings")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookingList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);

                        // Extract the actual Firestore document ID string dynamically
                        booking.setDocumentId(document.getId());

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
        bottomNavigation.setSelectedItemId(R.id.nav_booking);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_booking) {
                return true;
            }
            else if (id == R.id.nav_home) {
                Intent intent = new Intent(BookingsActivity.this, StudentDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            else if (id == R.id.nav_history) {
                Intent intent = new Intent(BookingsActivity.this, HistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            else if (id == R.id.nav_profile) {
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
        // Refresh database contents to catch live status modifications smoothly
        fetchUserBookings();

        // Safeguard: Ensure the baseline layout visually highlights the booking icon explicitly
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_booking);
        }
    }
}