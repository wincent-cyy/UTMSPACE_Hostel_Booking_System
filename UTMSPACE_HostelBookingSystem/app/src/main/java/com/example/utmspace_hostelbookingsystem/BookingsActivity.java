package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

public class BookingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NO EdgeToEdge.enable(this)
        // NO setSystemUiVisibility
        // This keeps the Top and Bottom "Safe" automatically

        setContentView(R.layout.activity_bookings);

        setupNavigation();
        setupList();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_booking);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, StudentDashboardActivity.class));
                return true;
            } else if (id == R.id.nav_booking) {
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupList() {
        RecyclerView rv = findViewById(R.id.rvPendingBookings);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<String> dummyRooms = Arrays.asList("A-101", "B-205", "C-302");
        rv.setAdapter(new RecyclerView.Adapter<QuickViewHolder>() {
            @NonNull
            @Override
            public QuickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
                return new QuickViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull QuickViewHolder holder, int position) {
                holder.roomNum.setText("Room " + dummyRooms.get(position));
                holder.status.setText("PENDING");
                holder.date.setText("Applied on: 12 May 2026");
            }

            @Override
            public int getItemCount() { return dummyRooms.size(); }
        });
    }

    static class QuickViewHolder extends RecyclerView.ViewHolder {
        TextView roomNum, status, date;
        public QuickViewHolder(@NonNull View itemView) {
            super(itemView);
            roomNum = itemView.findViewById(R.id.tvBookingRoomNumber);
            status = itemView.findViewById(R.id.tvBookingStatus);
            date = itemView.findViewById(R.id.tvBookingDate);
        }
    }
}