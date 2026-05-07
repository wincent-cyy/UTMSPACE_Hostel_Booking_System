package com.example.utmspace_hostelbookingsystem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Booking> bookingList;
    private Context context;

    public HistoryAdapter(List<Booking> bookingList, Context context) {
        this.bookingList = bookingList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.tvRoomType.setText(booking.getRoomName());
        holder.tvDate.setText(booking.getDate());
        holder.tvStatus.setText(booking.getStatus());
        holder.tvPrice.setText(booking.getPrice());

        // 1. Reset Click Listener first to avoid "Ghost Clicks" when scrolling
        holder.itemView.setOnClickListener(null);

        if (booking.getStatus().equalsIgnoreCase("Approved")) {
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Green

            // 2. Click Logic with Safety Checks
            holder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, PaymentActivity.class);
                    // Use hardcoded strings for a moment to test if the variables are the problem
                    intent.putExtra("ROOM_NAME", booking.getRoomName() != null ? booking.getRoomName() : "N/A");
                    intent.putExtra("PRICE", booking.getPrice() != null ? booking.getPrice() : "0.00");

                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace(); // This prints the error to your Logcat
                }
            });

        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Red
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomType, tvDate, tvStatus, tvPrice;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvDate = itemView.findViewById(R.id.tvBookingDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPrice = itemView.findViewById(R.id.tvTotalPrice);
        }
    }
}