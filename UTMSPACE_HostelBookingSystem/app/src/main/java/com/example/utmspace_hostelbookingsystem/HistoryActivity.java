package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvBookingHistory;
    private BottomNavigationView bottomNavigation;
    private TabLayout tabLayout;
    private MaterialButton btnClearHistory;

    private HistoryAdapter adapter;
    private List<Booking> allBookings;
    private List<Booking> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initViews();
        setupRecyclerView();
        setupTabs();
        setupNavigation(); // New Navigation Logic
        setupListeners();
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
        // Mock Data
        allBookings.add(new Booking("Premium Single Room", "12 May 2026", "Approved", "RM 500.00"));
        allBookings.add(new Booking("Standard Double Room", "05 May 2026", "Rejected", "RM 350.00"));
        allBookings.add(new Booking("Basic Single Room", "01 May 2026", "Approved", "RM 400.00"));

        filteredList = new ArrayList<>();
        filterBookings("Approved");

        rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredList, this);
        rvBookingHistory.setAdapter(adapter);
    }

    private void setupTabs() {
        if (tabLayout == null) return;

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    filterBookings("Approved");
                    if (btnClearHistory != null) btnClearHistory.setVisibility(View.GONE);
                } else {
                    filterBookings("Rejected");
                    if (btnClearHistory != null) btnClearHistory.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupNavigation() {
        if (bottomNavigation == null) return;

        // Set History as the selected item
        bottomNavigation.setSelectedItemId(R.id.nav_history);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Prevent restarting if already on History
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
                startActivity(intent);
                // Optional: finish() if you don't want the user to go back to history via back button
                return true;
            }
            return false;
        });
    }

    private void filterBookings(String status) {
        if (filteredList == null) filteredList = new ArrayList<>();
        filteredList.clear();

        for (Booking b : allBookings) {
            if (b.getStatus().equalsIgnoreCase(status)) {
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
            Iterator<Booking> iterator = allBookings.iterator();
            while (iterator.hasNext()) {
                Booking b = iterator.next();
                if (b.getStatus().equalsIgnoreCase("Rejected")) {
                    iterator.remove();
                }
            }
            filterBookings("Rejected");
        });
    }
}