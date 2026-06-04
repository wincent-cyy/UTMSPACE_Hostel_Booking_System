package com.example.utmspace_hostelbookingsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onViewDetails(User user);
        void onEditUser(User user);
    }

    public UserAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        if (userList == null || position >= userList.size()) return;
        User user = userList.get(position);
        if (user == null) return;

        // User Name
        String userName = user.getName();
        holder.tvUserName.setText(userName != null ? userName : "N/A");

        // User Email
        String userEmail = user.getEmail();
        holder.tvUserEmail.setText(userEmail != null ? userEmail : "N/A");

        // User Phone
        String userPhone = user.getPhone();
        holder.tvUserPhone.setText(userPhone != null ? userPhone : "N/A");

        // User Role Badge - 使用你指定的颜色
        String role = user.getRole();
        if (role != null) {
            switch (role.toLowerCase()) {
                case "student":
                    holder.tvUserRole.setText("Student");
                    holder.tvUserRole.setBackgroundColor(Color.parseColor("#10B981")); // 绿色
                    break;
                case "staff":
                    holder.tvUserRole.setText("Staff");
                    holder.tvUserRole.setBackgroundColor(Color.parseColor("#3B82F6")); // 蓝色
                    break;
                case "technician":
                    holder.tvUserRole.setText("Technician");
                    holder.tvUserRole.setBackgroundColor(Color.parseColor("#F59E0B")); // 橙色
                    break;
                case "admin":
                    holder.tvUserRole.setText("Admin");
                    holder.tvUserRole.setBackgroundColor(Color.parseColor("#800000")); // 深红色
                    break;
                default:
                    holder.tvUserRole.setText(role);
                    holder.tvUserRole.setBackgroundColor(Color.parseColor("#10B981")); // 默认绿色
                    break;
            }
        } else {
            holder.tvUserRole.setText("Student");
            holder.tvUserRole.setBackgroundColor(Color.parseColor("#10B981")); // 默认绿色
        }

        // 设置文字颜色为白色
        holder.tvUserRole.setTextColor(Color.WHITE);

        // 设置圆角背景
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setCornerRadius(30f);
        drawable.setColor(holder.tvUserRole.getCurrentTextColor());
        holder.tvUserRole.setBackground(drawable);
        holder.tvUserRole.setPadding(24, 8, 24, 8);

        // View Details button
        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetails(user);
                }
            });
        }

        // Edit User button
        if (holder.btnEditUser != null) {
            holder.btnEditUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditUser(user);
                }
            });
        }

        // 整个卡片不可点击
        holder.itemView.setClickable(false);
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName;
        TextView tvUserEmail;
        TextView tvUserPhone;
        TextView tvUserRole;
        LinearLayout btnViewDetails;
        LinearLayout btnEditUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserPhone = itemView.findViewById(R.id.tvUserPhone);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnEditUser = itemView.findViewById(R.id.btnEditUser);
        }
    }
}