package com.example.utmspace_hostelbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminRepairAdapter extends RecyclerView.Adapter<AdminRepairAdapter.ViewHolder> {

    private List<RepairRequestModel> repairList;
    private OnRepairActionListener listener;

    public interface OnRepairActionListener {
        void onViewDetails(RepairRequestModel request);
    }

    public AdminRepairAdapter(List<RepairRequestModel> repairList, OnRepairActionListener listener) {
        this.repairList = repairList;
        this.listener = listener;
    }

    public void updateList(List<RepairRequestModel> newList) {
        this.repairList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_repair, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RepairRequestModel request = repairList.get(position);

        holder.tvRoomId.setText(request.getRoomId());

        // 显示 Item Name
        String itemName = request.getItemName() != null ? request.getItemName() : "N/A";
        holder.tvItemName.setText(itemName);

        // 显示 Staff Name (Assigned Staff)
        String staffName = request.getStaffName() != null ? request.getStaffName() : "Not Assigned";
        holder.tvStaffName.setText("Assigned: " + staffName);

        // 显示 Description
        String description = request.getDescription() != null ? request.getDescription() : "No description";
        holder.tvDescription.setText(description);

        // 显示 Status
        holder.tvStatus.setText(request.getStatus());

        // 显示 Created At
        holder.tvCreatedAt.setText(formatDate(request.getCreatedAt()));

        // Set status color
        int statusColor = getStatusColor(request.getStatus());
        holder.tvStatus.setTextColor(statusColor);

        // View details button
        holder.btnViewDetails.setOnClickListener(v -> listener.onViewDetails(request));
    }

    @Override
    public int getItemCount() {
        return repairList.size();
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private int getStatusColor(String status) {
        if (status == null) return android.graphics.Color.parseColor("#64748B");

        switch (status) {
            case "Completed":
                return android.graphics.Color.parseColor("#10B981");
            case "In Progress":
                return android.graphics.Color.parseColor("#3B82F6");
            case "Pending":
                return android.graphics.Color.parseColor("#F59E0B");
            case "Rejected":
                return android.graphics.Color.parseColor("#EF4444");
            default:
                return android.graphics.Color.parseColor("#64748B");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvRoomId, tvItemName, tvStaffName, tvDescription, tvStatus, tvCreatedAt;
        Button btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvRoomId = itemView.findViewById(R.id.tvRoomId);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvStaffName = itemView.findViewById(R.id.tvStaffName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}