package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StaffRoomAdapter extends RecyclerView.Adapter<StaffRoomAdapter.RoomViewHolder> {

    private List<RoomModel> roomList;
    private OnRoomClickListener listener;

    public interface OnRoomClickListener {
        void onRoomClick(RoomModel room);
    }

    public StaffRoomAdapter(List<RoomModel> roomList, OnRoomClickListener listener) {
        this.roomList = roomList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomModel room = roomList.get(position);

        holder.tvRoomNumber.setText("Room " + room.getRoomNumber());
        holder.tvRoomType.setText(room.getRoomType());
        holder.tvLocation.setText(room.getLocation());
        holder.tvPrice.setText("RM " + (int) room.getPrice() + "/sem");

        // Show occupancy
        String occupancy = "Occupancy: " + room.getCurrentOccupancy() + "/" + room.getMaxCapacity();
        holder.tvOccupancy.setText(occupancy);

        // Show condition
        holder.tvCondition.setText(room.getCondition());

        // Set status color and text
        String status;
        if (room.isFull()) {
            status = "FULL";
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
            holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
        } else if ("Maintenance".equalsIgnoreCase(room.getCondition()) ||
                "Under Maintenance".equalsIgnoreCase(room.getCondition())) {
            status = "MAINTENANCE";
            holder.tvStatus.setTextColor(Color.parseColor("#F59E0B"));
            holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
        } else if ("Available".equalsIgnoreCase(room.getStatus())) {
            status = "AVAILABLE";
            holder.tvStatus.setTextColor(Color.parseColor("#10B981"));
            holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
        } else {
            status = room.getStatus().toUpperCase();
            holder.tvStatus.setTextColor(Color.parseColor("#6B7280"));
            holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
        }
        holder.tvStatus.setText(status);

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRoomClick(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvRoomNumber, tvRoomType, tvLocation, tvPrice, tvOccupancy, tvCondition, tvStatus;

        RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardRoom);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvOccupancy = itemView.findViewById(R.id.tvOccupancy);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}