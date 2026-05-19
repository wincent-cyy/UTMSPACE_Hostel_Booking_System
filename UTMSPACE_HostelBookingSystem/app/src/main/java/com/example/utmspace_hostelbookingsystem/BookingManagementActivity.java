package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_management);

        // Initialize Firestore core entry node
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupSearchFilter();
        setupNavigation();

        // Fetch ALL bookings (not just pending)
        fetchAllBookingsFromFirestore();
    }

    private void initViews() {
        etStaffSearch = findViewById(R.id.etStaffSearch);
        rvStaffBookings = findViewById(R.id.rvStaffBookings);
        staffEmptyState = findViewById(R.id.staffEmptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupRecyclerView() {
        masterAllBookingsList = new ArrayList<>();
        filteredBookingsList = new ArrayList<>();

        rvStaffBookings.setLayoutManager(new LinearLayoutManager(this));

        // Setup adapter
        adapter = new BookingAdapter(filteredBookingsList, booking -> {
            Intent intent = new Intent(BookingManagementActivity.this, StaffActionActivity.class);

            // Inject complete transactional keys for evaluation mapping
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
            intent.putExtra("userId", booking.getUserId());

            startActivity(intent);
        });

        rvStaffBookings.setAdapter(adapter);
    }

    private void fetchAllBookingsFromFirestore() {
        // Fetch ALL bookings (not just pending)
        db.collection("Bookings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterAllBookingsList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        booking.setDocumentId(document.getId());
                        masterAllBookingsList.add(booking);
                    }

                    // Apply search filter
                    filterStaffData(etStaffSearch.getText().toString());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookingManagementActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSearchFilter() {
        etStaffSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStaffData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterStaffData(String query) {
        filteredBookingsList.clear();
        String cleanQuery = query.toLowerCase().trim();

        for (Booking booking : masterAllBookingsList) {
            if (cleanQuery.isEmpty()) {
                filteredBookingsList.add(booking);
            } else {
                String name = booking.getStudentName() != null ? booking.getStudentName().toLowerCase() : "";
                String matric = booking.getMatricNumber() != null ? booking.getMatricNumber().toLowerCase() : "";

                if (name.contains(cleanQuery) || matric.contains(cleanQuery)) {
                    filteredBookingsList.add(booking);
                }
            }
        }

        // Toggle Empty Screen layout views
        if (filteredBookingsList.isEmpty()) {
            rvStaffBookings.setVisibility(View.GONE);
            staffEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvStaffBookings.setVisibility(View.VISIBLE);
            staffEmptyState.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        // Highlight active layout state position
        bottomNavigation.setSelectedItemId(R.id.nav_staff_bookings);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_staff_bookings) {
                return true; // We are already here
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list when returning from StaffActionActivity
        fetchAllBookingsFromFirestore();

        // Re-force bottom icon highlight
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_staff_bookings);
        }
    }
}