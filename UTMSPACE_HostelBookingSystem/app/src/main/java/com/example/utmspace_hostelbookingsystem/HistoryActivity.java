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
import java.util.Iterator; // Better for compatibility
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
        setupListeners();
        setupNavigation();
    }

    private void initViews() {
        rvBookingHistory = findViewById(R.id.rvBookingHistory);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tabLayout = findViewById(R.id.tabLayout);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        // Safety: Hide clear button by default
        if (btnClearHistory != null) btnClearHistory.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        allBookings = new ArrayList<>();
        // Mock Data
        allBookings.add(new Booking("Premium Single Room", "12 May 2026", "Approved", "RM 500.00"));
        allBookings.add(new Booking("Standard Double Room", "05 May 2026", "Rejected", "RM 350.00"));
        allBookings.add(new Booking("Basic Single Room", "01 May 2026", "Approved", "RM 400.00"));

        filteredList = new ArrayList<>();
        // Important: Load initial tab data
        filterBookings("Approved");

        rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));
        // Pass 'this' as context for the Intent in the adapter
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
            // Safer way to remove items than removeIf for older Android versions
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

    private void setupNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_history);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, StudentDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish(); // Helps with memory
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return itemId == R.id.nav_history;
        });
    }
}