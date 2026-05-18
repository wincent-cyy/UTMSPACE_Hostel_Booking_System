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
    private OnItemClickListener listener;
    private OnPaymentClickListener paymentClickListener;

    // Functional Interface declarations
    public interface OnItemClickListener {
        void onItemClick(Booking booking);
    }

    public interface OnPaymentClickListener {
        void onPaymentClick(Booking booking);
    }

    // Constructor 1: Original base signature mapping to maintain non-breaking compatibility
    public BookingAdapter(List<Booking> bookingList, OnItemClickListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    // Optional Setter interface handler required explicitly by your History Activity component
    public void setOnPaymentClickListener(OnPaymentClickListener paymentClickListener) {
        this.paymentClickListener = paymentClickListener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // Populate baseline data elements safely
        holder.tvRoomId.setText(booking.getRoomId());
        holder.tvRoomType.setText(booking.getRoomType());
        holder.tvDetails.setText("Check-in: " + booking.getCheckInDate() + " • " + booking.getLeaseDuration());

        String status = booking.getStatus();
        holder.tvStatus.setText(status);

        // UI status badge decoration handling
        if ("Pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7")); // Soft Yellow background
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));      // Deep Orange Amber text
        } else if ("Approved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DCFCE7")); // Soft Green background
            holder.tvStatus.setTextColor(Color.parseColor("#15803D"));      // Emerald Green text
        } else if ("Rejected".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2")); // Soft Red background
            holder.tvStatus.setTextColor(Color.parseColor("#B91C1C"));      // Crimson Red text
        } else if ("Paid".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DBEAFE")); // Soft Blue background
            holder.tvStatus.setTextColor(Color.parseColor("#1E40AF"));      // Dark Blue text
        }

        // DYNAMIC PAYMENT TEXT LINK INTERACTION PROCESSING BOUNDARY
        if (holder.tvClickPayment != null) {
            // Only displays the operational payment click option if explicitly marked as "Approved"
            if ("Approved".equalsIgnoreCase(status)) {
                holder.tvClickPayment.setVisibility(View.VISIBLE);
                holder.tvClickPayment.setOnClickListener(v -> {
                    if (paymentClickListener != null) {
                        paymentClickListener.onPaymentClick(booking);
                    }
                });
            } else {
                // Hides interaction node paths automatically if context evaluates to Pending, Rejected, or Paid
                holder.tvClickPayment.setVisibility(View.GONE);
            }
        }

        // Entire row item root card view click routing
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                listener.onItemClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomId, tvRoomType, tvDetails, tvStatus;
        TextView tvClickPayment; // Fixed: Changed type from MaterialButton to TextView to prevent crashes

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            // Safe initialization: Successfully binds to your clickable TextView text element
            tvClickPayment = itemView.findViewById(R.id.tvClickPayment);
        }
    }
}