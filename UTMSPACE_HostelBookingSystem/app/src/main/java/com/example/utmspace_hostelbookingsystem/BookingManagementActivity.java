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
    private BookingAdapter adapter; // Reusing model properties for systemic unity
    private List<Booking> masterPendingList;
    private List<Booking> filteredPendingList;

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

        // Automatically stream pending requests right on initialization
        fetchPendingApplicationsFromFirestore();
    }

    private void initViews() {
        etStaffSearch = findViewById(R.id.etStaffSearch);
        rvStaffBookings = findViewById(R.id.rvStaffBookings);
        staffEmptyState = findViewById(R.id.staffEmptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupRecyclerView() {
        masterPendingList = new ArrayList<>();
        filteredPendingList = new ArrayList<>();

        rvStaffBookings.setLayoutManager(new LinearLayoutManager(this));

        // Setup adapter targeting your staff detailed evaluation layout sheet view profile
        adapter = new BookingAdapter(filteredPendingList, booking -> {
            // Explicitly route to a dedicated Staff action script screen
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

            // FIXED CRITICAL GAP: Pass the student user mapping token so StaffAction Activity can track identity bounds
            intent.putExtra("userId", booking.getUserId());

            startActivity(intent);
        });

        rvStaffBookings.setAdapter(adapter);
    }

    private void fetchPendingApplicationsFromFirestore() {
        // Automatically fetches ALL records from the system where status is "Pending"
        db.collection("Bookings")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterPendingList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);

                        // Inject unique document key identifier sequence for modification passes
                        booking.setDocumentId(document.getId());
                        masterPendingList.add(booking);
                    }

                    // Synchronize and render views
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
        filteredPendingList.clear();
        String cleanQuery = query.toLowerCase().trim();

        for (Booking b : masterPendingList) {
            String name = b.getStudentName() != null ? b.getStudentName().toLowerCase() : "";
            String matric = b.getMatricNumber() != null ? b.getMatricNumber().toLowerCase() : "";

            if (name.contains(cleanQuery) || matric.contains(cleanQuery)) {
                filteredPendingList.add(b);
            }
        }

        // Toggle Empty Screen layout views seamlessly
        if (filteredPendingList.isEmpty()) {
            rvStaffBookings.setVisibility(View.GONE);
            staffEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvStaffBookings.setVisibility(View.VISIBLE);
            staffEmptyState.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        // Highlight active layout state position item profile anchor matching your Staff menu configuration
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
                Toast.makeText(this, "Room Management system coming soon!", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (intent != null) {
                // Brings the background task forward instead of destroying and recreating layout layers
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
        // Force list state refresh upon interaction focus re-entry
        fetchPendingApplicationsFromFirestore();

        // Safety Synchronization: Re-forces the correct bottom icon highlight ring state explicitly
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_staff_bookings);
        }
    }
}