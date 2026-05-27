package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        holder.tvRoomNumber.setText("Room " + request.getRoomId());
        holder.tvItemName.setText(request.getItemName());

        // Set status background using drawable
        String status = request.getStatus();
        if (status != null) {
            switch (status) {
                case "Pending":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    break;
                case "Scheduled":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_scheduled);
                    break;
                case "Completed":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_completed);
                    break;
                default:
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    break;
            }
            holder.tvStatus.setText(status);
        }

        // Set urgency - 使用同一个 drawable，动态设置颜色
        String urgency = request.getUrgency();
        if (urgency != null) {
            holder.tvUrgency.setBackgroundResource(R.drawable.urgency_badge);
            switch (urgency) {
                case "Low":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#10B981"));
                    break;
                case "Medium":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#F59E0B"));
                    break;
                case "High":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#EF4444"));
                    break;
                case "Emergency":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#7F1D1D"));
                    break;
                default:
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#94A3B8"));
                    break;
            }
            holder.tvUrgency.setText(urgency);
        }

        // Set date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(request.getCreatedAt())));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(request));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber, tvStatus, tvItemName, tvUrgency, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvUrgency = itemView.findViewById(R.id.tvUrgency);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}