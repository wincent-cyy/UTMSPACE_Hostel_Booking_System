package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookingList;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the item view design matching your layout resource definition
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // Safely extract string mappings directly from the Model setters/getters
        holder.tvRoomId.setText(booking.getRoomId());
        holder.tvRoomType.setText(booking.getRoomType());
        holder.tvDetails.setText("Check-in: " + booking.getCheckInDate() + " • " + booking.getLeaseDuration());

        String status = booking.getStatus();
        holder.tvStatus.setText(status);

        // UI decoration handling relative to real-time process monitoring states
        if ("Pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7")); // Soft Yellow background
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));      // Deep Orange Amber text
        } else if ("Approved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DCFCE7")); // Soft Green background
            holder.tvStatus.setTextColor(Color.parseColor("#15803D"));      // Emerald Green text
        } else if ("Rejected".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2")); // Soft Red background
            holder.tvStatus.setTextColor(Color.parseColor("#B91C1C"));      // Crimson Red text
        }
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomId, tvRoomType, tvDetails, tvStatus;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            // Links the individual view elements using your XML item IDs
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}