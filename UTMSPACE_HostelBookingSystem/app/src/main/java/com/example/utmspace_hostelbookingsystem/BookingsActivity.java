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

        // 1. Initialize Views
        rvPendingBookings = findViewById(R.id.rvPendingBookings);
        emptyState = findViewById(R.id.emptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // 2. Setup RecyclerView
        rvPendingBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();

        // 3. Initialize Adapter with OnItemClickListener
        adapter = new BookingAdapter(bookingList, booking -> {
            Intent intent = new Intent(BookingsActivity.this, BookingDetailsActivity.class);

            intent.putExtra("BOOKING_DOC_ID", booking.getBookingId());
            intent.putExtra("BOOKING_STATUS", booking.getBookingStatus());
            intent.putExtra("REJECT_REASON", booking.getRejectReason());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());

            // ✅ 使用 getDisplayPrice() 方法（这个方法在 Booking.java 中定义）
            intent.putExtra("ROOM_PRICE", booking.getDisplayPrice());

            intent.putExtra("STUDENT_NAME", booking.getName());
            intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
            intent.putExtra("PHONE_NUMBER", booking.getPhone());
            intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
            intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());

            startActivity(intent);
        });

        // ✅ Payment click listener - 只写一次
        adapter.setOnPaymentClickListener(booking -> {
            Intent intent = new Intent(BookingsActivity.this, PaymentActivity.class);
            intent.putExtra("BOOKING_DOC_ID", booking.getBookingId());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_PRICE", booking.getDisplayPrice());
            startActivity(intent);
        });

        rvPendingBookings.setAdapter(adapter);

        // 4. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 5. Setup navigation
        setupBottomNavigation();

        // 6. Load user bookings
        fetchUserBookings();
    }

    private void fetchUserBookings() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = mAuth.getCurrentUser().getUid();

        db.collection("Bookings")
                .whereEqualTo("uid", currentUid)
                .whereEqualTo("bookingStatus", "Pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookingList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        booking.setBookingId(document.getId());
                        bookingList.add(booking);
                    }

                    adapter.notifyDataSetChanged();

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
        fetchUserBookings();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_booking);
        }
    }
}