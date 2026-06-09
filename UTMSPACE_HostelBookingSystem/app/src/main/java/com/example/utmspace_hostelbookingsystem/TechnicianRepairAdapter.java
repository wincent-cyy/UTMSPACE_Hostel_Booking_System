package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
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

public class TechnicianRepairAdapter extends RecyclerView.Adapter<TechnicianRepairAdapter.ViewHolder> {

    private List<RepairRequest> requestList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RepairRequest request);
    }

    public TechnicianRepairAdapter(List<RepairRequest> requestList, OnItemClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_technician_repair, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (requestList == null || position >= requestList.size()) return;
        RepairRequest request = requestList.get(position);
        if (request == null) return;

        // 房间号
        String roomId = request.getRoomId();
        holder.tvRoomNumber.setText(roomId != null ? roomId : "N/A");

        // 问题类型
        String issueType = request.getIssueType();
        holder.tvIssueType.setText(issueType != null ? issueType : "N/A");

        // 描述
        String description = request.getDescription();
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(description != null ? description : "No description");
        }

        // ==================== 状态设置 ====================
        String status = request.getStatus();
        if (status != null && holder.tvStatus != null) {
            holder.tvStatus.setText(status);

            // 设置统一的圆角背景
            holder.tvStatus.setBackgroundResource(R.drawable.status_badge_rounded);
            holder.tvStatus.setPadding(32, 8, 32, 8);

            switch (status.toLowerCase()) {
                case "pending":
                    holder.tvStatus.setTextColor(Color.parseColor("#D97706"));  // 深黄色文字
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));  // 浅黄色背景
                    break;
                case "in progress":
                case "in-progress":
                    holder.tvStatus.setTextColor(Color.parseColor("#2563EB"));  // 深蓝色文字
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#DBEAFE"));  // 浅蓝色背景
                    break;
                case "completed":
                    holder.tvStatus.setTextColor(Color.parseColor("#059669"));  // 深青色文字
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#D1FAE5"));  // 浅青色背景
                    break;
                default:
                    holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
                    break;
            }
        }

        // ==================== 优先级设置 ====================
        String priority = request.getPriority();
        if (priority != null && holder.tvPriority != null) {
            holder.tvPriority.setText(priority);

            // 设置统一的圆角背景
            holder.tvPriority.setBackgroundResource(R.drawable.status_badge_rounded);
            holder.tvPriority.setPadding(24, 4, 24, 4);

            switch (priority.toLowerCase()) {
                case "high":
                    holder.tvPriority.setTextColor(Color.parseColor("#DC2626"));  // 深红色文字
                    holder.tvPriority.setBackgroundColor(Color.parseColor("#FEE2E2"));  // 浅红色背景
                    break;
                case "medium":
                    holder.tvPriority.setTextColor(Color.parseColor("#D97706"));  // 深黄色文字
                    holder.tvPriority.setBackgroundColor(Color.parseColor("#FEF3C7"));  // 浅黄色背景
                    break;
                case "low":
                    holder.tvPriority.setTextColor(Color.parseColor("#059669"));  // 深青色文字
                    holder.tvPriority.setBackgroundColor(Color.parseColor("#D1FAE5"));  // 浅青色背景
                    break;
                case "emergency":
                    holder.tvPriority.setTextColor(Color.parseColor("#7F1D1D"));  // 深红色文字
                    holder.tvPriority.setBackgroundColor(Color.parseColor("#FEE2E2"));  // 浅红色背景
                    break;
                default:
                    holder.tvPriority.setTextColor(Color.parseColor("#6B7280"));  // 灰色文字
                    holder.tvPriority.setBackgroundColor(Color.parseColor("#F3F4F6"));  // 浅灰色背景
                    break;
            }
        }

        // 日期
        long createdAt = request.getCreatedAt();
        if (createdAt > 0 && holder.tvDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(createdAt)));
        } else {
            holder.tvDate.setText("N/A");
        }

        // 只有 Details 按钮可以点击
        if (holder.btnStartRepair != null) {
            holder.btnStartRepair.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(request);
                }
            });
        }

        holder.itemView.setClickable(false);
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    public void updateList(List<RepairRequest> newList) {
        this.requestList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber;
        TextView tvIssueType;
        TextView tvDescription;
        TextView tvStatus;
        TextView tvPriority;
        TextView tvDate;
        LinearLayout btnStartRepair;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvIssueType = itemView.findViewById(R.id.tvIssueType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnStartRepair = itemView.findViewById(R.id.btnStartRepair);
        }
    }
}