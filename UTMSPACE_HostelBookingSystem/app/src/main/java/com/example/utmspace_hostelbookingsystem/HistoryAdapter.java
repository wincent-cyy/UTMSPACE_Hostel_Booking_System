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

        // Fill basic data
        holder.tvRoomType.setText(booking.getRoomName());
        holder.tvDate.setText(booking.getDate());
        holder.tvStatus.setText(booking.getStatus());
        holder.tvPrice.setText(booking.getPrice());

        // Default: Hide payment link and clear listeners
        holder.tvClickPayment.setVisibility(View.GONE);
        holder.tvClickPayment.setOnClickListener(null);
        holder.itemView.setOnClickListener(null); // Ensure the whole card is NOT clickable

        // Logic based on Status
        String status = booking.getStatus() != null ? booking.getStatus() : "";

        if (status.equalsIgnoreCase("Approved")) {
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Green

            // Show the payment link ONLY if status is Approved
            holder.tvClickPayment.setVisibility(View.VISIBLE);

            // Set listener ONLY on the "Click to Payment" TextView
            holder.tvClickPayment.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, PaymentActivity.class);
                    intent.putExtra("ROOM_NAME", booking.getRoomName() != null ? booking.getRoomName() : "N/A");
                    intent.putExtra("PRICE", booking.getPrice() != null ? booking.getPrice() : "0.00");

                    // Recommended: add flags if context is not an Activity
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Navigation Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else if (status.equalsIgnoreCase("Paid")) {
            holder.tvStatus.setTextColor(Color.parseColor("#6366F1")); // Indigo/Blue
            holder.tvClickPayment.setVisibility(View.GONE); // No need to pay again
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Red
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
            // Link the payment text view
            tvClickPayment = itemView.findViewById(R.id.tvClickPayment);
        }
    }
}