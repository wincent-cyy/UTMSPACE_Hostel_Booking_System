package com.example.utmspace_hostelbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.ViewHolder> {

    private List<BookingModel> bookingList;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onUpdateStatus(BookingModel booking, String newStatus);
        void onViewDetails(BookingModel booking);
        void onDelete(BookingModel booking);
    }

    public AdminBookingAdapter(List<BookingModel> bookingList, OnBookingActionListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    public void updateList(List<BookingModel> newList) {
        this.bookingList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingModel booking = bookingList.get(position);

        holder.tvRoomId.setText(booking.getRoomId());
        holder.tvUserName.setText(booking.getName());  // 使用 getName()

        // 修复日期显示 - 使用 checkInDate 和 leaseDuration
        String checkIn = booking.getCheckInDate();  // 直接是 String
        String leaseDuration = booking.getLeaseDuration() != null ? booking.getLeaseDuration() : "N/A";
        holder.tvDates.setText(checkIn + " (" + leaseDuration + ")");

        holder.tvTotalPrice.setText("RM " + booking.getPrice());  // 使用 getPrice()

        // 使用 bookingStatus
        String status = booking.getBookingStatus() != null ? booking.getBookingStatus() : "Pending";
        holder.tvStatus.setText(status);

        // Set status color
        int statusColor = getStatusColor(status);
        holder.tvStatus.setTextColor(statusColor);

        // Action buttons
        holder.btnViewDetails.setOnClickListener(v -> listener.onViewDetails(booking));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(booking));

        // Status update buttons
        if ("Pending".equals(status)) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnApprove.setOnClickListener(v -> listener.onUpdateStatus(booking, "Approved"));
            holder.btnReject.setOnClickListener(v -> listener.onUpdateStatus(booking, "Rejected"));
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        }

        // Show paid button for approved bookings
        if ("Approved".equals(status)) {
            holder.btnMarkPaid.setVisibility(View.VISIBLE);
            holder.btnMarkPaid.setOnClickListener(v -> listener.onUpdateStatus(booking, "Paid"));
        } else {
            holder.btnMarkPaid.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private int getStatusColor(String status) {
        if (status == null) return android.graphics.Color.parseColor("#64748B");

        switch (status) {
            case "Approved":
            case "Paid":
                return android.graphics.Color.parseColor("#10B981");
            case "Pending":
                return android.graphics.Color.parseColor("#F59E0B");
            case "Rejected":
                return android.graphics.Color.parseColor("#EF4444");
            default:
                return android.graphics.Color.parseColor("#64748B");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvRoomId, tvUserName, tvDates, tvTotalPrice, tvStatus;
        Button btnViewDetails, btnApprove, btnReject, btnMarkPaid, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}