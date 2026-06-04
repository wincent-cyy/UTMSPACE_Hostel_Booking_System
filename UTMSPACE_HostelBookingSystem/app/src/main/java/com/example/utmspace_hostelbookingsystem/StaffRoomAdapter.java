package com.example.utmspace_hostelbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StaffRoomAdapter extends RecyclerView.Adapter<StaffRoomAdapter.ViewHolder> {

    private List<RoomModel> roomList;
    private OnEditClickListener editClickListener;

    public interface OnEditClickListener {
        void onEditClick(RoomModel room);
    }

    public StaffRoomAdapter(List<RoomModel> roomList, OnEditClickListener editClickListener) {
        this.roomList = roomList;
        this.editClickListener = editClickListener;
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

        // 设置房间号
        holder.tvRoomNumber.setText(room.getRoomId() != null ? room.getRoomId() : "N/A");

        // 设置房间类型
        holder.tvRoomType.setText(room.getRoomType() != null ? room.getRoomType() : "N/A");

        // 设置位置
        holder.tvLocation.setText(room.getLocation() != null ? room.getLocation() : "Not specified");

        // 设置状态
        String status = room.getStatus() != null ? room.getStatus() : "Available";
        holder.tvStatus.setText(status);

        // 设置入住人数
        holder.tvOccupancy.setText(room.getCurrentOccupancy() + "/" + room.getMaxCapacity());

        // 设置状态颜色
        if (status.equalsIgnoreCase("Available")) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else if (status.equalsIgnoreCase("Full")) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
        }

        // 编辑按钮点击事件 - 只有点击编辑按钮才触发
        if (holder.btnEdit != null) {
            holder.btnEdit.setOnClickListener(v -> {
                if (editClickListener != null) {
                    editClickListener.onEditClick(room);
                }
            });
        }

        // 整个卡片不设置点击事件（移除 itemView 的点击）
        // holder.itemView.setOnClickListener(null);
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
        TextView tvRoomNumber, tvRoomType, tvLocation, tvStatus, tvOccupancy;
        LinearLayout btnEdit;  // 添加编辑按钮

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvRoomType = itemView.findViewById(R.id.tvRoomType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOccupancy = itemView.findViewById(R.id.tvOccupancy);
            btnEdit = itemView.findViewById(R.id.btnEdit);  // 初始化编辑按钮
        }
    }
}