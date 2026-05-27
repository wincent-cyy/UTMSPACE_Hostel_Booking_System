package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminRoomAdapter extends RecyclerView.Adapter<AdminRoomAdapter.ViewHolder> {

    private List<RoomModel> roomList;
    private OnRoomActionListener listener;

    public interface OnRoomActionListener {
        void onEdit(RoomModel room);
        void onDelete(RoomModel room);
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
        RoomModel room = roomList.get(position);

        holder.tvRoomId.setText(room.getRoomId());
        holder.tvRoomType.setText(room.getRoomType());
        holder.tvLocation.setText(room.getLocation());
        holder.tvPrice.setText("RM " + String.format("%.2f", room.getPrice()));
        holder.tvOccupancy.setText(room.getCurrentOccupancy() + "/" + room.getMaxCapacity());

        // Set status color
        if ("Available".equalsIgnoreCase(room.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#10B981"));
        } else if ("Full".equalsIgnoreCase(room.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#F59E0B"));
        }
        holder.tvStatus.setText(room.getStatus());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(room);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomId, tvRoomType, tvLocation, tvPrice, tvOccupancy, tvStatus;
        Button btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvOccupancy = itemView.findViewById(R.id.tvOccupancy);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}