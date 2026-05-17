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
        // Inflates your original custom history item layout design row
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // CORRECTED: Safely map parameters to match our updated Booking object properties
        holder.tvRoomType.setText(booking.getRoomType());
        holder.tvDate.setText(booking.getCheckInDate());
        holder.tvStatus.setText(booking.getStatus());
        holder.tvPrice.setText(booking.getRoomPrice());

        // Default Setup State: Clear visibility metrics to prevent recycling glitches
        holder.tvClickPayment.setVisibility(View.GONE);
        holder.tvClickPayment.setOnClickListener(null);
        holder.itemView.setOnClickListener(null);

        String status = booking.getStatus() != null ? booking.getStatus() : "";

        // Status Logic evaluations
        if (status.equalsIgnoreCase("Approved")) {
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Emerald Green

            // Show the payment context link ONLY for approved items
            holder.tvClickPayment.setVisibility(View.VISIBLE);

            // Bind click engine listener framework to navigate towards checkouts
            holder.tvClickPayment.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, PaymentActivity.class);
                    // Pass matching variables to the checkout activity pipeline
                    intent.putExtra("ROOM_NAME", booking.getRoomType());
                    intent.putExtra("PRICE", booking.getRoomPrice());
                    intent.putExtra("ROOM_ID", booking.getRoomId());

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Navigation Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else if (status.equalsIgnoreCase("Paid")) {
            holder.tvStatus.setTextColor(Color.parseColor("#6366F1")); // Indigo Blue
            holder.tvClickPayment.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Crimson Red
            holder.tvClickPayment.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomType, tvDate, tvStatus, tvPrice, tvClickPayment;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvDate = itemView.findViewById(R.id.tvBookingDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvClickPayment = itemView.findViewById(R.id.tvClickPayment);
        }
    }
}