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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // ✅ 修正: 使用新的字段名
        holder.tvRoomType.setText(booking.getRoomType() != null ? booking.getRoomType() : "N/A");
        holder.tvRoomId.setText("Room: " + (booking.getRoomId() != null ? booking.getRoomId() : "N/A"));

        String checkInDate = booking.getCheckInDate() != null ? booking.getCheckInDate() : "N/A";
        String duration = booking.getLeaseDuration() != null ? booking.getLeaseDuration() : "N/A";
        holder.tvDetails.setText("Check-in: " + checkInDate + " • " + duration);

        // ✅ 修正: 使用 getPrice() 并格式化
        holder.tvTotalPrice.setText(String.format("RM %.2f", booking.getPrice()));

        // ✅ 修正: 使用 getBookingStatus()
        String status = booking.getBookingStatus() != null ? booking.getBookingStatus().trim() : "Pending";
        holder.tvStatus.setText(status);

        // Default Reset State
        holder.tvClickPayment.setVisibility(View.GONE);

        // UI status badge and click engine handler processing logic
        if (status.equalsIgnoreCase("Approved")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DCFCE7"));
            holder.tvStatus.setTextColor(Color.parseColor("#15803D"));

            holder.tvClickPayment.setVisibility(View.VISIBLE);
            holder.tvClickPayment.setText("Pay Now →");

            holder.tvClickPayment.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, PaymentActivity.class);
                    passCompleteBookingPayload(intent, booking);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Payment Route Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else if (status.equalsIgnoreCase("Paid")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DBEAFE"));
            holder.tvStatus.setTextColor(Color.parseColor("#1E40AF"));
            holder.tvClickPayment.setVisibility(View.GONE);

        } else if (status.equalsIgnoreCase("Rejected")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2"));
            holder.tvStatus.setTextColor(Color.parseColor("#B91C1C"));
            holder.tvClickPayment.setVisibility(View.GONE);

        } else if (status.equalsIgnoreCase("Pending")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            holder.tvClickPayment.setVisibility(View.GONE);

        } else {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#E5E7EB"));
            holder.tvStatus.setTextColor(Color.parseColor("#374151"));
            holder.tvClickPayment.setVisibility(View.GONE);
        }

        // Entire Row Click Setup
        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(context, BookingDetailsActivity.class);
                passCompleteBookingPayload(intent, booking);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Details Route Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ 修正: 使用新的 getter 方法
    private void passCompleteBookingPayload(Intent intent, Booking booking) {
        intent.putExtra("BOOKING_DOC_ID", booking.getBookingId());
        intent.putExtra("BOOKING_STATUS", booking.getBookingStatus());
        intent.putExtra("ROOM_ID", booking.getRoomId());
        intent.putExtra("ROOM_TYPE", booking.getRoomType());
        intent.putExtra("ROOM_PRICE", String.valueOf(booking.getPrice()));

        intent.putExtra("STUDENT_NAME", booking.getName());
        intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
        intent.putExtra("PHONE_NUMBER", booking.getPhone());
        intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
        intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());
        intent.putExtra("REJECT_REASON", booking.getRejectReason());
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomType, tvRoomId, tvDetails, tvStatus, tvTotalPrice, tvClickPayment;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvClickPayment = itemView.findViewById(R.id.tvClickPayment);
        }
    }
}