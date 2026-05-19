package com.example.utmspace_hostelbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StaffRoomAdapter extends RecyclerView.Adapter<StaffRoomAdapter.ViewHolder> {

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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomModel room = roomList.get(position);

        holder.tvRoomNumber.setText(room.getRoomNumber());
        holder.tvRoomType.setText(room.getRoomType());
        holder.tvLocation.setText(room.getLocation());
        holder.tvStatus.setText(room.getStatus());
        holder.tvOccupancy.setText(room.getCurrentOccupancy() + "/" + room.getMaxCapacity());

        // Set status color
        if (room.getStatus().equalsIgnoreCase("Available")) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else if (room.getStatus().equalsIgnoreCase("Full")) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
        }

        // Use itemView instead of cardView (this is the fix)
        holder.itemView.setOnClickListener(v -> listener.onRoomClick(room));
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber, tvRoomType, tvLocation, tvStatus, tvOccupancy;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOccupancy = itemView.findViewById(R.id.tvOccupancy);
        }
    }
}