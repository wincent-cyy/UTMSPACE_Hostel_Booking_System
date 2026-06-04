package com.example.utmspace_hostelbookingsystem;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookingList;
    private OnItemClickListener listener;
    private boolean isStaffView;

    public interface OnItemClickListener {
        void onItemClick(Booking booking);
    }

    // Staff 构造函数
    public BookingAdapter(List<Booking> bookingList, OnItemClickListener listener, boolean isStaffView) {
        this.bookingList = bookingList;
        this.listener = listener;
        this.isStaffView = isStaffView;
    }

    public void updateList(List<Booking> newList) {
        this.bookingList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        if (bookingList == null || position >= bookingList.size()) return;
        Booking booking = bookingList.get(position);
        if (booking == null) return;

        // 1. 设置房间号 (Room Number)
        String roomId = booking.getRoomId();
        if (roomId == null || roomId.isEmpty()) {
            roomId = "N/A";
        }
        holder.tvRoomId.setText(roomId);

        // 2. 设置房间类型 (Room Type)
        String roomType = booking.getRoomType();
        if (roomType == null || roomType.isEmpty()) {
            roomType = "N/A";
        }
        holder.tvRoomType.setText(roomType);

        // 3. 设置学生姓名 (Student Name)
        String studentName = booking.getName();
        if (studentName == null || studentName.isEmpty()) {
            studentName = "N/A";
        }
        holder.tvStudentName.setText(studentName);

        // 4. 设置申请日期 (Application Date)
        long createdAt = booking.getCreatedAt();
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvBookingDate.setText(sdf.format(new Date(createdAt)));
        } else {
            holder.tvBookingDate.setText("N/A");
        }

        // 5. 设置状态 (Status)
        String status = booking.getBookingStatus();
        if (status == null || status.isEmpty()) {
            status = "Pending";
        }
        holder.tvStatus.setText(status);
        setStatusColor(holder.tvStatus, status);

        // 6. 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(booking);
            }
        });
    }

    private void setStatusColor(TextView tvStatus, String status) {
        if (status == null) return;

        switch (status.toLowerCase()) {
            case "pending":
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                tvStatus.setTextColor(Color.parseColor("#D97706"));
                break;
            case "approved":
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
                tvStatus.setTextColor(Color.parseColor("#15803D"));
                break;
            case "rejected":
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                tvStatus.setTextColor(Color.parseColor("#B91C1C"));
                break;
            case "paid":
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
                tvStatus.setTextColor(Color.parseColor("#1E40AF"));
                break;
            default:
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
                tvStatus.setTextColor(Color.parseColor("#374151"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomId;
        TextView tvRoomType;
        TextView tvStudentName;
        TextView tvBookingDate;
        TextView tvStatus;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            // 确保这些 ID 与你的 XML 布局中的 ID 一致
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}