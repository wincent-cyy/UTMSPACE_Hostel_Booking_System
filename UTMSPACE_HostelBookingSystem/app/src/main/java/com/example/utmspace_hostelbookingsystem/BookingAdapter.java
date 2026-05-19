package com.example.utmspace_hostelbookingsystem;

import android.content.res.ColorStateList;
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

    public interface OnItemClickListener {
        void onItemClick(Booking booking);
    }

    public interface OnPaymentClickListener {
        void onPaymentClick(Booking booking);
    }

    public BookingAdapter(List<Booking> bookingList, OnItemClickListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    public void setOnPaymentClickListener(OnPaymentClickListener paymentClickListener) {
        this.paymentClickListener = paymentClickListener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_history, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        if (bookingList == null || position >= bookingList.size()) return;
        Booking booking = bookingList.get(position);
        if (booking == null) return;

        // Populate text details safely
        if (holder.tvRoomId != null) {
            holder.tvRoomId.setText(booking.getRoomId() != null ? booking.getRoomId() : "N/A");
        }
        if (holder.tvRoomType != null) {
            holder.tvRoomType.setText(booking.getRoomType() != null ? booking.getRoomType() : "Room");
        }
        if (holder.tvDetails != null) {
            String date = booking.getCheckInDate() != null ? booking.getCheckInDate() : "N/A";
            String duration = booking.getLeaseDuration() != null ? booking.getLeaseDuration() : "N/A";
            holder.tvDetails.setText("Check-in: " + date + " • " + duration);
        }

        // Clean string inputs from Firestore
        if (holder.tvTotalPrice != null) {
            String rawPrice = booking.getRoomPrice();
            if (rawPrice != null) {
                String cleanPrice = rawPrice.split("(?i)/")[0].trim();
                holder.tvTotalPrice.setText(cleanPrice);
            } else {
                holder.tvTotalPrice.setText("RM 0");
            }
        }

        String status = booking.getStatus() != null ? booking.getStatus().trim() : "Pending";
        if (holder.tvStatus != null) {
            holder.tvStatus.setText(status);

            // Handle status background colors safely
            if ("Pending".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            } else if ("Approved".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
                holder.tvStatus.setTextColor(Color.parseColor("#15803D"));
            } else if ("Rejected".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                holder.tvStatus.setTextColor(Color.parseColor("#B91C1C"));
            } else if ("Paid".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
                holder.tvStatus.setTextColor(Color.parseColor("#1E40AF"));
            } else {
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
                holder.tvStatus.setTextColor(Color.parseColor("#374151"));
            }
        }

        // Action Link: "Pay Now" logic remains active and distinct
        if (holder.tvClickPayment != null) {
            if ("Approved".equalsIgnoreCase(status)) {
                holder.tvClickPayment.setVisibility(View.VISIBLE);
                holder.tvClickPayment.setOnClickListener(v -> {
                    if (paymentClickListener != null) {
                        paymentClickListener.onPaymentClick(booking);
                    }
                });
            } else {
                holder.tvClickPayment.setVisibility(View.GONE);
                holder.tvClickPayment.setOnClickListener(null);
            }
        }

        // FIXED: Conditional evaluation logic block removes item click functionality for ongoing cards
        if ("Approved".equalsIgnoreCase(status)) {
            // Disable whole card interaction when card state matches "Approved" (Ongoing Tab)
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
        } else {
            // Maintain regular structural click transitions exclusively for past items (History Tab: Paid/Rejected)
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (listener != null && currentPos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(bookingList.get(currentPos));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomId, tvRoomType, tvDetails, tvStatus;
        TextView tvClickPayment;
        TextView tvTotalPrice;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvClickPayment = itemView.findViewById(R.id.tvClickPayment);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
        }
    }
}