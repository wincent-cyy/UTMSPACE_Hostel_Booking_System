package com.example.utmspace_hostelbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminRepairAdapter extends RecyclerView.Adapter<AdminRepairAdapter.ViewHolder> {

    private List<RepairRequestModel> repairList;
    private OnRepairActionListener listener;

    public interface OnRepairActionListener {
        void onViewDetails(RepairRequestModel repair);
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
        if (repairList == null || position >= repairList.size()) return;
        RepairRequestModel repair = repairList.get(position);
        if (repair == null) return;

        // Room Number - 使用 getRoomId()
        String roomNumber = repair.getRoomId();
        holder.tvRoomNumber.setText(roomNumber != null ? roomNumber : "N/A");

        // Issue Type - 使用 getIssueType() 或 getItemName()
        String issueType = repair.getIssueType();
        if (issueType == null || issueType.isEmpty()) {
            issueType = repair.getItemName();
        }
        holder.tvIssueType.setText(issueType != null ? issueType : "N/A");

        // Priority - 使用 getPriority() 或 getUrgency()
        String priority = repair.getPriority();
        if (priority == null || priority.isEmpty()) {
            priority = repair.getUrgency();
        }
        holder.tvPriority.setText(priority != null ? priority : "Medium");
        setPriorityColor(holder.tvPriority, priority);

        // Date - 使用 getCreatedAt()
        long createdAt = repair.getCreatedAt();
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(createdAt)));
        } else {
            holder.tvDate.setText("N/A");
        }

        // Status
        String status = repair.getStatus();
        if (status == null || status.isEmpty()) {
            status = "Pending";
        }
        holder.tvStatus.setText(status);
        setStatusColor(holder.tvStatus, status);

        // View Details button only
        if (holder.btnView != null) {
            holder.btnView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetails(repair);
                }
            });
        }

        // 整个卡片不可点击
        holder.itemView.setClickable(false);
    }

    private void setPriorityColor(TextView tvPriority, String priority) {
        if (priority == null) return;

        switch (priority.toLowerCase()) {
            case "high":
                tvPriority.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                break;
            case "medium":
                tvPriority.setTextColor(android.graphics.Color.parseColor("#F59E0B"));
                break;
            case "low":
                tvPriority.setTextColor(android.graphics.Color.parseColor("#10B981"));
                break;
            case "emergency":
                tvPriority.setTextColor(android.graphics.Color.parseColor("#7F1D1D"));
                break;
            default:
                tvPriority.setTextColor(android.graphics.Color.parseColor("#64748B"));
                break;
        }
    }

    private void setStatusColor(TextView tvStatus, String status) {
        if (status == null) return;

        switch (status.toLowerCase()) {
            case "pending":
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FEF3C7")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#D97706"));
                break;
            case "in progress":
            case "in-progress":
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#DBEAFE")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#1E40AF"));
                break;
            case "completed":
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#DCFCE7")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#15803D"));
                break;
            default:
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E5E7EB")));
                tvStatus.setTextColor(android.graphics.Color.parseColor("#374151"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return repairList != null ? repairList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber;
        TextView tvIssueType;
        TextView tvPriority;
        TextView tvDate;
        TextView tvStatus;
        LinearLayout btnView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvIssueType = itemView.findViewById(R.id.tvIssueType);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnView = itemView.findViewById(R.id.btnView);
        }
    }
}