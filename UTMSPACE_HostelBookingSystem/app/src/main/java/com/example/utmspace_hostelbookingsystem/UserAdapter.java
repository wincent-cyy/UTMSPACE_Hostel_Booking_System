package com.example.utmspace_hostelbookingsystem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener listener;
    private FirebaseFirestore db;

    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
    }

    public UserAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
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
        User user = userList.get(position);

        holder.tvUserName.setText(user.getName() != null ? user.getName().toUpperCase() : "N/A");
        holder.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");

        // 传入 user 对象
        loadMatricNumberFromBookings(user, holder.tvUserDetail);

        // ✅ 圆角矩形背景
        GradientDrawable rectDrawable = new GradientDrawable();
        rectDrawable.setShape(GradientDrawable.RECTANGLE);
        rectDrawable.setCornerRadius(20);  // 圆角半径

        String role = user.getRole();
        if (role != null) {
            switch (role.toLowerCase()) {
                case "student":
                    rectDrawable.setColor(Color.parseColor("#6366F1"));
                    holder.tvRoleBadge.setText("Student");
                    break;
                case "staff":
                    rectDrawable.setColor(Color.parseColor("#10B981"));
                    holder.tvRoleBadge.setText("Staff");
                    break;
                case "technician":
                    rectDrawable.setColor(Color.parseColor("#F59E0B"));
                    holder.tvRoleBadge.setText("Technician");
                    break;
                default:
                    rectDrawable.setColor(Color.parseColor("#94A3B8"));
                    holder.tvRoleBadge.setText(role);
                    break;
            }
        }
        holder.tvRoleBadge.setBackground(rectDrawable);

        // Load profile picture
        String profilePictureBase64 = user.getProfilePictureBase64();
        if (profilePictureBase64 != null && !profilePictureBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(profilePictureBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivAvatar.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.ivAvatar.setImageResource(R.drawable.profile_pic);
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.profile_pic);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(user);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(user);
            }
        });
    }

    private void loadMatricNumberFromBookings(User user, TextView tvUserDetail) {
        String role = user.getRole();

        // 如果是 Staff 或 Technician，直接显示 Phone
        if ("staff".equalsIgnoreCase(role) || "technician".equalsIgnoreCase(role)) {
            String phone = user.getPhone();
            if (phone != null && !phone.isEmpty()) {
                tvUserDetail.setText("Phone: " + phone);
            } else {
                tvUserDetail.setText("Phone: N/A");
            }
            return;
        }

        // 只有 Student 才从 Bookings 获取 Matric Number
        db.collection("Bookings")
                .whereEqualTo("uid", user.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String matricNumber = doc.getString("matricNumber");
                            if (matricNumber != null && !matricNumber.isEmpty()) {
                                tvUserDetail.setText("Matric: " + matricNumber);
                            } else {
                                String phone = user.getPhone();
                                tvUserDetail.setText("Phone: " + (phone != null ? phone : "N/A"));
                            }
                            break;
                        }
                    } else {
                        String phone = user.getPhone();
                        tvUserDetail.setText("Phone: " + (phone != null ? phone : "N/A"));
                    }
                })
                .addOnFailureListener(e -> {
                    String phone = user.getPhone();
                    tvUserDetail.setText("Phone: " + (phone != null ? phone : "N/A"));
                });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUserName, tvUserEmail, tvUserDetail, tvRoleBadge;
        Button btnEdit, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserDetail = itemView.findViewById(R.id.tvUserDetail);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}