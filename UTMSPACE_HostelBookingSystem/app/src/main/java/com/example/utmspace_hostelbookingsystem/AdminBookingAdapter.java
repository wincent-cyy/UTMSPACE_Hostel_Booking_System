package com.example.utmspace_hostelbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.ViewHolder> {

    private List<BookingModel> bookingList;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onViewDetails(BookingModel booking);
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
        if (bookingList == null || position >= bookingList.size()) return;
        BookingModel booking = bookingList.get(position);
        if (booking == null) return;

        // Room Number - 使用 getRoomId()
        String roomNumber = booking.getRoomId();
        holder.tvRoomNumber.setText(roomNumber != null ? roomNumber : "N/A");

        // Student Name - 使用 getName()
        String studentName = booking.getName();
        holder.tvStudentName.setText(studentName != null ? studentName : "N/A");

        // Room Type
        String roomType = booking.getRoomType();
        holder.tvRoomType.setText(roomType != null ? roomType : "N/A");

        // Date - 使用 getCreatedAt()
        long createdAt = booking.getCreatedAt();
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(createdAt)));
        } else {
            holder.tvDate.setText("N/A");
        }

        // Status - 使用 getBookingStatus()
        String status = booking.getBookingStatus();
        if (status == null || status.isEmpty()) {
            status = "Pending";
        }
        holder.tvStatus.setText(status);
        setStatusColor(holder.tvStatus, status);

        // View Details button only
        if (holder.btnView != null) {
            holder.btnView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetails(booking);
                }
            });
        }

        // 整个卡片不可点击
        holder.itemView.setClickable(false);
    }

    private void setStatusColor(TextView tvStatus, String status) {
        if (status == null) return;

        switch (status.toLowerCase()) {
            case "pending":
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FEF3C7")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#D97706"));
                break;
            case "approved":
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#DCFCE7")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#15803D"));
                break;
            case "rejected":
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FEE2E2")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#B91C1C"));
                break;
            case "paid":
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#DBEAFE")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#1E40AF"));
                break;
            default:
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E5E7EB")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#374151"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber;
        TextView tvStudentName;
        TextView tvRoomType;
        TextView tvDate;
        TextView tvStatus;
        LinearLayout btnView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnView = itemView.findViewById(R.id.btnView);
        }
    }
}