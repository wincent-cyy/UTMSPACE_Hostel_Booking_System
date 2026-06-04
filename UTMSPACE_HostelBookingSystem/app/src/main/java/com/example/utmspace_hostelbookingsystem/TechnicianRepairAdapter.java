package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.graphics.PorterDuff;
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

        // 问题类型 (XML 中是 tvIssueType，不是 tvItemName)
        String issueType = request.getIssueType();
        holder.tvIssueType.setText(issueType != null ? issueType : "N/A");

        // 描述 (XML 中有 tvDescription)
        String description = request.getDescription();
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(description != null ? description : "No description");
        }

        // 状态 (XML 中有 tvStatus)
        String status = request.getStatus();
        if (status != null && holder.tvStatus != null) {
            holder.tvStatus.setText(status);
            switch (status.toLowerCase()) {
                case "pending":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    break;
                case "in progress":
                case "in-progress":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_in_progress);
                    break;
                case "completed":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_completed);
                    break;
                default:
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    break;
            }
        }

        // 优先级 (XML 中是 tvPriority，不是 tvUrgency)
        String priority = request.getPriority();
        if (priority != null && holder.tvPriority != null) {
            holder.tvPriority.setText(priority);
            holder.tvPriority.setBackgroundResource(R.drawable.urgency_badge);

            int color;
            switch (priority.toLowerCase()) {
                case "low":
                    color = Color.parseColor("#10B981");
                    break;
                case "medium":
                    color = Color.parseColor("#F59E0B");
                    break;
                case "high":
                    color = Color.parseColor("#EF4444");
                    break;
                case "emergency":
                    color = Color.parseColor("#7F1D1D");
                    break;
                default:
                    color = Color.parseColor("#94A3B8");
                    break;
            }
            holder.tvPriority.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }

        // 日期
        long createdAt = request.getCreatedAt();
        if (createdAt > 0 && holder.tvDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(createdAt)));
        } else {
            holder.tvDate.setText("N/A");
        }

        // 只有 Details 按钮可以点击，整个卡片不可点击
        if (holder.btnStartRepair != null) {
            holder.btnStartRepair.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(request);
                }
            });
        }

        // 整个卡片不设置点击事件
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
        TextView tvIssueType;      // 改为 tvIssueType
        TextView tvDescription;    // 添加 tvDescription
        TextView tvStatus;
        TextView tvPriority;       // 改为 tvPriority
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