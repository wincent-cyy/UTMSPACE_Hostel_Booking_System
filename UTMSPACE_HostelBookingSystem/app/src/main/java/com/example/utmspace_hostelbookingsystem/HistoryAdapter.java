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
        // IMPROVED: pointed layout to item_booking_card to keep UI design perfectly uniform
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // Populate baseline layout elements safely using our unified model mappings
        holder.tvRoomType.setText(booking.getRoomType());
        holder.tvRoomId.setText("Room: " + booking.getRoomId());
        holder.tvDetails.setText("Check-in: " + booking.getCheckInDate() + " • " + booking.getLeaseDuration());
        holder.tvTotalPrice.setText(booking.getRoomPrice());
        holder.tvStatus.setText(booking.getStatus());

        // Default Reset State: Prevents row recycling visual state bugs
        holder.tvClickPayment.setVisibility(View.GONE);

        String status = booking.getStatus() != null ? booking.getStatus().trim() : "";

        // UI status badge and click engine handler processing logic
        if (status.equalsIgnoreCase("Approved")) {
            // Match design profiles: soft background colors handled by your architecture or code
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DCFCE7")); // Soft Green
            holder.tvStatus.setTextColor(Color.parseColor("#15803D"));      // Emerald Green

            // Show payment interactive text link option
            holder.tvClickPayment.setVisibility(View.VISIBLE);
            holder.tvClickPayment.setText("Pay Now →");

            // Direct route pathway to checkout system terminal layout
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
            holder.tvStatus.setBackgroundColor(Color.parseColor("#DBEAFE")); // Soft Blue
            holder.tvStatus.setTextColor(Color.parseColor("#1E40AF"));      // Dark Blue
            holder.tvClickPayment.setVisibility(View.GONE);

        } else if (status.equalsIgnoreCase("Rejected")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2")); // Soft Red
            holder.tvStatus.setTextColor(Color.parseColor("#B91C1C"));      // Crimson Red
            holder.tvClickPayment.setVisibility(View.GONE);

        } else {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7")); // Soft Amber fallback
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            holder.tvClickPayment.setVisibility(View.GONE);
        }

        // Entire Row Click Setup: Passes complete data directly to Detail view screen
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

    // Helper method to make sure both checkout and details intents receive the complete data package safely
    private void passCompleteBookingPayload(Intent intent, Booking booking) {
        intent.putExtra("BOOKING_DOC_ID", booking.getDocumentId());
        intent.putExtra("BOOKING_STATUS", booking.getStatus());
        intent.putExtra("ROOM_ID", booking.getRoomId());
        intent.putExtra("ROOM_TYPE", booking.getRoomType());
        intent.putExtra("ROOM_PRICE", booking.getRoomPrice());

        intent.putExtra("STUDENT_NAME", booking.getStudentName());
        intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
        intent.putExtra("PHONE_NUMBER", booking.getPhoneNumber());
        intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
        intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());
        intent.putExtra("REJECT_REASON", booking.getRejectReason());
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        // Updated field variables list to match item_booking_card layout structure completely
        TextView tvRoomType, tvRoomId, tvDetails, tvStatus, tvTotalPrice, tvClickPayment;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);         // Fixed: mapped safely away from tvBookingDate
            tvDetails = itemView.findViewById(R.id.tvDetails);       // Maps check-in and lease text strings
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice); // Maps final price value field
            tvClickPayment = itemView.findViewById(R.id.tvClickPayment);
        }
    }
}