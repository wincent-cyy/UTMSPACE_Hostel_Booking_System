package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private List<RoomModel> roomList;
    private OnRoomClickListener clickListener; // Added click listener variable

    // 1. Define the click callback interface
    public interface OnRoomClickListener {
        void onRoomClick(RoomModel room);
    }

    // 2. Updated constructor to accept the click listener interface
    public RoomAdapter(List<RoomModel> roomList, OnRoomClickListener clickListener) {
        this.roomList = roomList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomModel room = roomList.get(position);

        // --- Keep your original UI logic completely intact ---
        // 1. Basic Info
        holder.tvRoomNumber.setText("Room " + room.getRoomNumber());
        holder.tvLocation.setText(room.getLocation());
        holder.tvPrice.setText("RM " + (int)room.getPrice());

        // 2. Capacity Logic (e.g., Beds: 1/2)
        String capacityText = "Beds: " + room.getCurrentOccupancy() + "/" + room.getMaxCapacity();
        holder.tvCapacity.setText(capacityText);

        // 3. Status & Color Logic
        String status = room.getStatus();

        // If room is physically full, override status to show "FULL"
        if (room.isFull()) {
            holder.tvStatus.setText("Full");
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Red
            holder.tvCapacity.setTextColor(Color.parseColor("#EF4444")); // Red for urgency
        } else if ("Available".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("Available");
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Green
            holder.tvCapacity.setTextColor(Color.parseColor("#64748B")); // Gray
        } else {
            holder.tvStatus.setText(status);
            holder.tvStatus.setTextColor(Color.parseColor("#F59E0B")); // Orange/Amber
            holder.tvCapacity.setTextColor(Color.parseColor("#64748B"));
        }

        // --- New: Added click tracking handler ---
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRoomClick(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber, tvPrice, tvStatus, tvLocation, tvCapacity;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvItemRoomNumber);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvCapacity = itemView.findViewById(R.id.tvItemCapacity);
        }
    }
}