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

    private boolean useCardLayout = false;
    private boolean isStaffView = false;  // 是否是 Staff 视图

    public interface OnItemClickListener {
        void onItemClick(Booking booking);
    }

    public interface OnPaymentClickListener {
        void onPaymentClick(Booking booking);
    }

    // Student 构造函数
    public BookingAdapter(List<Booking> bookingList, OnItemClickListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
        this.isStaffView = false;  // Student 视图
    }

    // Staff 构造函数
    public BookingAdapter(List<Booking> bookingList, OnItemClickListener listener, boolean useCardLayout) {
        this.bookingList = bookingList;
        this.listener = listener;
        this.useCardLayout = useCardLayout;
        this.isStaffView = true;  // Staff 视图
    }


    public void setOnPaymentClickListener(OnPaymentClickListener paymentClickListener) {
        this.paymentClickListener = paymentClickListener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = useCardLayout ? R.layout.item_booking_card : R.layout.item_booking_history;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new BookingViewHolder(view, useCardLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        if (bookingList == null || position >= bookingList.size()) return;
        Booking booking = bookingList.get(position);
        if (booking == null) return;

        // ========== 填充文本数据 ==========
        // 房间ID
        if (holder.tvRoomId != null) {
            holder.tvRoomId.setText(booking.getRoomId() != null ? booking.getRoomId() : "N/A");
        }
        // 房间类型
        if (holder.tvRoomType != null) {
            holder.tvRoomType.setText(booking.getRoomType() != null ? booking.getRoomType() : "Room");
        }
        // 详情 (Check-in 和 Duration)
        if (holder.tvDetails != null) {
            String date = booking.getCheckInDate() != null ? booking.getCheckInDate() : "N/A";
            String duration = booking.getLeaseDuration() != null ? booking.getLeaseDuration() : "N/A";
            holder.tvDetails.setText("Check-in: " + date + " • " + duration);
        }

        // 价格 (根据学期计算)
        if (holder.tvTotalPrice != null) {
            double finalPrice = booking.getPrice();
            String duration = booking.getLeaseDuration();
            if (duration != null && duration.equals("2 Semesters (Full Academic Year)")) {
                finalPrice = booking.getPrice() * 2;
            }
            holder.tvTotalPrice.setText(String.format("RM %.2f", finalPrice));
        }

        // ========== 状态和颜色 ==========
        String status = booking.getBookingStatus() != null ? booking.getBookingStatus().trim() : "Pending";
        if (holder.tvStatus != null) {
            holder.tvStatus.setText(status);

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

        // ========== Staff 视图：所有卡片都可以点击，没有 Pay Now 按钮 ==========
        if (isStaffView) {
            // Staff: 整个卡片都可以点击
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (listener != null && currentPos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(bookingList.get(currentPos));
                }
            });

            // 隐藏 Pay Now 按钮 (Staff 不需要)
            if (holder.tvClickPayment != null) {
                holder.tvClickPayment.setVisibility(View.GONE);
            }
            return;  // Staff 视图处理完毕，直接返回
        }

        // ========== Student 视图：有 Pay Now 按钮，只有部分状态可点击 ==========

        // Pay Now 按钮 (只有 Approved 状态显示)
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

        // Student: 只有 Pending 和 Rejected 可以点击查看详情
        // Approved 和 Paid 状态不可点击 (因为 Approved 有 Pay Now 按钮)
        // Student: Pending 和 Rejected 可以点击，Approved 不可点击（因为有 Pay Now 按钮），Paid 可以点击
        if ("Approved".equalsIgnoreCase(status)) {
            // Approved 禁用点击（因为有 Pay Now 按钮）
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
        } else {
            // Pending, Rejected, Paid 都可以点击
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

        public BookingViewHolder(@NonNull View itemView, boolean useCardLayout) {
            super(itemView);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);

            if (!useCardLayout) {
                tvClickPayment = itemView.findViewById(R.id.tvClickPayment);
            } else {
                tvClickPayment = null;
            }
        }
    }
}