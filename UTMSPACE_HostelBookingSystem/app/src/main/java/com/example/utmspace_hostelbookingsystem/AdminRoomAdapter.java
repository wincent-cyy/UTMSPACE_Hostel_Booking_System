package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminRoomAdapter extends RecyclerView.Adapter<AdminRoomAdapter.ViewHolder> {

    private List<RoomModel> roomList;
    private OnRoomActionListener listener;

    public interface OnRoomActionListener {
        void onViewRoom(RoomModel room);
        void onEditRoom(RoomModel room);
    }

    public AdminRoomAdapter(List<RoomModel> roomList, OnRoomActionListener listener) {
        this.roomList = roomList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (roomList == null || position >= roomList.size()) return;
        RoomModel room = roomList.get(position);
        if (room == null) return;

        // Room Number
        String roomNumber = room.getRoomId();
        holder.tvRoomNumber.setText(roomNumber != null ? roomNumber : "N/A");

        // Room Status
        String status = room.getStatus();
        if (status == null || status.isEmpty()) {
            status = "Available";
        }
        holder.tvRoomStatus.setText(status);
        setStatusColor(holder.tvRoomStatus, status);

        // Room Type
        String roomType = room.getRoomType();
        holder.tvRoomType.setText(roomType != null ? roomType : "N/A");

        // Location
        String location = room.getLocation();
        holder.tvLocation.setText(location != null ? location : "N/A");

        // Price
        double price = room.getPrice();
        holder.tvPrice.setText(String.format("RM %.2f", price));

        // View button
        if (holder.btnViewRoom != null) {
            holder.btnViewRoom.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewRoom(room);
                }
            });
        }

        // Edit button
        if (holder.btnEditRoom != null) {
            holder.btnEditRoom.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditRoom(room);
                }
            });
        }

        // 整个卡片不可点击
        holder.itemView.setClickable(false);
    }

    private void setStatusColor(TextView tvStatus, String status) {
        if (status == null) return;

        switch (status.toLowerCase()) {
            case "available":
                tvStatus.setBackgroundResource(R.drawable.status_badge_available);
                tvStatus.setTextColor(Color.WHITE);
                break;
            case "full":
                tvStatus.setBackgroundResource(R.drawable.status_badge_full);
                tvStatus.setTextColor(Color.WHITE);
                break;
            case "maintenance":
                tvStatus.setBackgroundResource(R.drawable.status_badge_maintenance);
                tvStatus.setTextColor(Color.WHITE);
                break;
            default:
                tvStatus.setBackgroundResource(R.drawable.status_badge_available);
                tvStatus.setTextColor(Color.WHITE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    public void updateList(List<RoomModel> newList) {
        this.roomList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber;
        TextView tvRoomStatus;
        TextView tvRoomType;
        TextView tvLocation;
        TextView tvPrice;
        LinearLayout btnViewRoom;
        LinearLayout btnEditRoom;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvRoomStatus = itemView.findViewById(R.id.tvRoomStatus);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnViewRoom = itemView.findViewById(R.id.btnViewRoom);
            btnEditRoom = itemView.findViewById(R.id.btnEditRoom);
        }
    }
}