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
        RepairRequest request = requestList.get(position);

        holder.tvRoomNumber.setText("Room " + request.getRoomId());
        holder.tvItemName.setText(request.getItemName());

        // Set status background and text
        String status = request.getStatus();
        if (status != null) {
            switch (status) {
                case "Pending":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    holder.tvStatus.setText("Pending");
                    break;
                case "In Progress":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_scheduled);
                    holder.tvStatus.setText("In Progress");
                    break;
                case "Completed":
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_completed);
                    holder.tvStatus.setText("Completed");
                    break;
                default:
                    holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    holder.tvStatus.setText(status);
                    break;
            }
        }

        // Set urgency color
        String urgency = request.getUrgency();
        if (urgency != null) {
            holder.tvUrgency.setBackgroundResource(R.drawable.urgency_badge);
            switch (urgency) {
                case "Low":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#10B981"));
                    holder.tvUrgency.setText("Low");
                    break;
                case "Medium":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#F59E0B"));
                    holder.tvUrgency.setText("Medium");
                    break;
                case "High":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#EF4444"));
                    holder.tvUrgency.setText("High");
                    break;
                case "Emergency":
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#7F1D1D"));
                    holder.tvUrgency.setText("Emergency");
                    break;
                default:
                    holder.tvUrgency.getBackground().setTint(Color.parseColor("#94A3B8"));
                    holder.tvUrgency.setText(urgency);
                    break;
            }
        }

        // Set date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
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