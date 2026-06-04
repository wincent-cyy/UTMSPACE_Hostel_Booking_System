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

public class RepairRequestAdapter extends RecyclerView.Adapter<RepairRequestAdapter.ViewHolder> {

    private List<RepairRequest> requestList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RepairRequest request);
    }

    public RepairRequestAdapter(List<RepairRequest> requestList, OnItemClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repair_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RepairRequest request = requestList.get(position);

        // 房间号
        if (holder.tvRoomNumber != null) {
            String roomNumber = request.getRoomId();
            holder.tvRoomNumber.setText(roomNumber != null ? roomNumber : "N/A");
        }

        // 问题类型
        if (holder.tvIssueType != null) {
            String issueType = request.getIssueType();
            holder.tvIssueType.setText(issueType != null ? issueType : "N/A");
        }

        // 描述
        if (holder.tvDescription != null) {
            String description = request.getDescription();
            holder.tvDescription.setText(description != null ? description : "No description");
        }

        // 状态
        if (holder.tvStatus != null) {
            String status = request.getStatus();
            if (status != null && !status.isEmpty()) {
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
                holder.tvStatus.setText(status);
            } else {
                holder.tvStatus.setText("Pending");
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
            }
        }

        // 优先级
        if (holder.tvPriority != null) {
            String priority = request.getPriority();
            if (priority != null && !priority.isEmpty()) {
                holder.tvPriority.setVisibility(View.VISIBLE);
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
                holder.tvPriority.setText(priority);
            } else {
                holder.tvPriority.setVisibility(View.GONE);
            }
        }

        // 日期显示
        if (holder.tvDate != null) {
            long createdAt = request.getCreatedAt();
            android.util.Log.d("RepairRequestAdapter", "createdAt in adapter: " + createdAt);

            if (createdAt > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String dateString = sdf.format(new Date(createdAt));
                android.util.Log.d("RepairRequestAdapter", "Formatted date: " + dateString);
                holder.tvDate.setText(dateString);
            } else {
                holder.tvDate.setText("N/A");
            }
        }

        // 只有 Details 按钮可以点击，整个卡片不能点击
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
        TextView tvDescription;
        TextView tvStatus;
        TextView tvPriority;
        TextView tvDate;
        LinearLayout btnDetails;  // 添加 Details 按钮

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvIssueType = itemView.findViewById(R.id.tvIssueType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnDetails = itemView.findViewById(R.id.btnDetails);  // 需要在 XML 中添加这个按钮
        }
    }
}