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

public class TechnicianHistoryAdapter extends RecyclerView.Adapter<TechnicianHistoryAdapter.ViewHolder> {

    private List<RepairRequest> requestList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RepairRequest request);
    }

    public TechnicianHistoryAdapter(List<RepairRequest> requestList, OnItemClickListener listener) {
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

        // 状态
        String status = request.getStatus();
        if (status != null && holder.tvStatus != null) {
            holder.tvStatus.setText(status);
            if ("In Progress".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_in_progress);
            } else if ("Completed".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_completed);
            }
        }

        // 优先级 - 使用 PorterDuff 兼容所有 API
        String priority = request.getPriority();
        if (priority != null && holder.tvPriority != null) {
            holder.tvPriority.setText(priority);
            holder.tvPriority.setBackgroundResource(R.drawable.urgency_badge);

            int color;
            switch (priority) {
                case "Low":
                    color = Color.parseColor("#10B981");
                    break;
                case "Medium":
                    color = Color.parseColor("#F59E0B");
                    break;
                case "High":
                    color = Color.parseColor("#EF4444");
                    break;
                case "Emergency":
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
        if (holder.btnDetails != null) {
            holder.btnDetails.setOnClickListener(v -> {
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
        TextView tvIssueType;
        TextView tvStatus;
        TextView tvPriority;
        TextView tvDate;
        LinearLayout btnDetails;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 根据你的 XML 布局 ID 修改
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvIssueType = itemView.findViewById(R.id.tvIssueType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
}