package com.example.utmspace_hostelbookingsystem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StaffDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StaffDashboard";

    // UI Elements
    private BottomNavigationView bottomNavigationView;
    private TextView tvStaffName;
    private ShapeableImageView ivProfilePicture;
    private LinearLayout profileAvatar;
    private LinearLayout cardTotalBookings;  // Total Bookings 卡片容器
    private LinearLayout cardActiveIssues;    // Active Issues 卡片容器
    private TextView tvTotalBookings, tvRoomIssues, tvOccupiedRooms, tvVacantRooms;
    private LinearLayout recentBookingsContainer;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        // Initialize views
        initViews();

        // Load staff name and profile picture
        loadStaffData();

        // Setup navigation
        setupNavigation();
        setupProfileClick();

        // Load dashboard data
        loadDashboardStats();
        loadRecentBookings();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        tvStaffName = findViewById(R.id.tvStaffName);
        profileAvatar = findViewById(R.id.profileAvatar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);

        // 卡片容器
        cardTotalBookings = findViewById(R.id.cardTotalBookings);
        cardActiveIssues = findViewById(R.id.cardActiveIssues);

        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvRoomIssues = findViewById(R.id.tvRoomIssues);
        tvOccupiedRooms = findViewById(R.id.tvOccupiedRooms);
        tvVacantRooms = findViewById(R.id.tvVacantRooms);
        recentBookingsContainer = findViewById(R.id.recentBookingsContainer);

        View btnRepairList = findViewById(R.id.btnRepairList);
        btnRepairList.setOnClickListener(v -> {
            Intent intent = new Intent(StaffDashboardActivity.this, StaffRepairTrackingActivity.class);
            startActivity(intent);
        });

        // 设置卡片点击事件
        setupCardClickListeners();
    }

    private void setupCardClickListeners() {
        // Total Bookings 卡片点击 - 跳转到 BookingManagementActivity
        if (cardTotalBookings != null) {
            cardTotalBookings.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, BookingManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        // Active Issues 卡片点击 - 跳转到 StaffRepairTrackingActivity
        if (cardActiveIssues != null) {
            cardActiveIssues.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, StaffRepairTrackingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
    }

    private void setupProfileClick() {
        // Click on avatar to go to Profile page
        profileAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(StaffDashboardActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        ivProfilePicture.setOnClickListener(v -> {
            Intent intent = new Intent(StaffDashboardActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }

    private void showFullScreenImage(String base64String) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_full_image, null);
        ShapeableImageView fullImageView = dialogView.findViewById(R.id.fullImageView);

        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            fullImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            fullImageView.setImageResource(R.drawable.profile_pic);
        }

        builder.setView(dialogView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        fullImageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void loadDashboardStats() {
        // Total Bookings (all time)
        db.collection("Bookings").get()
                .addOnSuccessListener(query -> {
                    tvTotalBookings.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> tvTotalBookings.setText("0"));

        // Active Issues (Maintenance rooms)
        db.collection("Rooms")
                .whereEqualTo("status", "Maintenance")
                .get()
                .addOnSuccessListener(query -> {
                    tvRoomIssues.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> tvRoomIssues.setText("0"));

        // Full and Available rooms - Show numbers only, no "Rooms" text
        db.collection("Rooms").get()
                .addOnSuccessListener(query -> {
                    int full = 0;
                    int available = 0;
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String status = doc.getString("status");
                        if ("Full".equalsIgnoreCase(status)) {
                            full++;
                        } else if ("Available".equalsIgnoreCase(status)) {
                            available++;
                        }
                    }
                    tvOccupiedRooms.setText(String.valueOf(full));
                    tvVacantRooms.setText(String.valueOf(available));
                })
                .addOnFailureListener(e -> {
                    tvOccupiedRooms.setText("0");
                    tvVacantRooms.setText("0");
                });
    }

    private void loadRecentBookings() {
        // Calculate date 7 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date sevenDaysAgo = calendar.getTime();
        long sevenDaysAgoTimestamp = sevenDaysAgo.getTime();

        // Query all bookings from last 7 days
        db.collection("Bookings")
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgoTimestamp)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 先清除所有现有视图
                    recentBookingsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        addEmptyStateView("No recent bookings found");
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    int count = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        if (count >= 5) break;

                        View bookingView = createRecentBookingView(document, sdf);
                        if (bookingView != null) {
                            recentBookingsContainer.addView(bookingView);
                            count++;
                        }
                    }

                    if (count == 0) {
                        addEmptyStateView("No recent bookings");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load recent bookings: " + e.getMessage());
                    recentBookingsContainer.removeAllViews();
                    addErrorStateView("Failed to load recent bookings: " + e.getMessage());
                });
    }

    private void addEmptyStateView(String message) {
        TextView emptyText = new TextView(this);
        emptyText.setText(message);
        emptyText.setTextSize(13);
        emptyText.setTextColor(getColor(R.color.tabInactiveText));
        emptyText.setPadding(16, 24, 16, 24);
        emptyText.setGravity(android.view.Gravity.CENTER);
        recentBookingsContainer.addView(emptyText);
    }

    private void addErrorStateView(String message) {
        TextView errorText = new TextView(this);
        errorText.setText(message);
        errorText.setTextSize(13);
        errorText.setTextColor(getColor(R.color.tabInactiveText));
        errorText.setPadding(16, 24, 16, 24);
        errorText.setGravity(android.view.Gravity.CENTER);
        recentBookingsContainer.addView(errorText);
    }

    private View createRecentBookingView(QueryDocumentSnapshot document, SimpleDateFormat sdf) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_booking_card, null);
        if (itemView == null) return null;

        // 添加底部边距，让卡片之间有间距
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 16;  // 16dp 间距
        itemView.setLayoutParams(params);

        // 使用布局中正确的 ID
        TextView tvRoomType = itemView.findViewById(R.id.tvRoomType);
        TextView tvRoomId = itemView.findViewById(R.id.tvRoomId);
        TextView tvStudentName = itemView.findViewById(R.id.tvStudentName);
        TextView tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);

        // 从 Firestore 获取数据
        String roomType = document.getString("roomType");
        String roomId = document.getString("roomId");
        String studentName = document.getString("name");
        Long createdAt = document.getLong("createdAt");
        String status = document.getString("bookingStatus");

        // 设置房间类型
        if (tvRoomType != null) {
            tvRoomType.setText(roomType != null ? roomType : "Room");
        }

        // 设置房间号
        if (tvRoomId != null) {
            tvRoomId.setText(roomId != null ? roomId : "N/A");
        }

        // 设置学生姓名
        if (tvStudentName != null) {
            tvStudentName.setText(studentName != null ? studentName : "Student");
        }

        // 设置申请日期
        if (tvBookingDate != null) {
            if (createdAt != null && createdAt > 0) {
                tvBookingDate.setText(sdf.format(new Date(createdAt)));
            } else {
                tvBookingDate.setText("N/A");
            }
        }

        // 设置状态和颜色
        if (tvStatus != null) {
            String statusText = status != null ? status : "Pending";
            tvStatus.setText(statusText);

            if ("Pending".equalsIgnoreCase(statusText)) {
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.pending_bg)));
                tvStatus.setTextColor(getColor(R.color.pending_text));
            } else if ("Approved".equalsIgnoreCase(statusText)) {
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.approved_bg)));
                tvStatus.setTextColor(getColor(R.color.approved_text));
            } else if ("Rejected".equalsIgnoreCase(statusText)) {
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.rejected_bg)));
                tvStatus.setTextColor(getColor(R.color.rejected_text));
            }
        }

        // 点击事件 - 跳转到 StaffActionActivity
        final String finalDocumentId = document.getId();
        final String finalRoomId = roomId;
        final String finalRoomType = roomType;
        final String finalStudentName = studentName;
        final String finalStatus = status;

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(StaffDashboardActivity.this, StaffActionActivity.class);
            intent.putExtra("BOOKING_DOC_ID", finalDocumentId);
            intent.putExtra("BOOKING_STATUS", finalStatus);
            intent.putExtra("ROOM_ID", finalRoomId);
            intent.putExtra("ROOM_TYPE", finalRoomType);
            intent.putExtra("STUDENT_NAME", finalStudentName);
            startActivity(intent);
        });

        return itemView;
    }

    private void loadStaffData() {
        if (currentUser == null) {
            tvStaffName.setText("Staff Member");
            setDefaultAvatar();
            return;
        }

        String userId = currentUser.getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get staff name
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvStaffName.setText(name);
                        } else {
                            tvStaffName.setText("Staff Member");
                        }

                        // Load profile picture
                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            loadProfileImageFromBase64(profileImageBase64);
                        } else {
                            setDefaultAvatar();
                        }
                    } else {
                        tvStaffName.setText("Staff Member");
                        setDefaultAvatar();
                    }
                })
                .addOnFailureListener(e -> {
                    tvStaffName.setText("Staff Member");
                    setDefaultAvatar();
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void setDefaultAvatar() {
        // Use Glide to load default avatar as circle
        Glide.with(this)
                .load(R.drawable.ic_account_circle)
                .circleCrop()
                .into(ivProfilePicture);
        ivProfilePicture.setVisibility(View.VISIBLE);
        profileAvatar.setBackgroundResource(R.drawable.avatar_background);
    }

    private void loadProfileImageFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            if (bitmap != null) {
                // 计算目标尺寸 - 放大头像
                int targetWidth = 200;
                int targetHeight = 200;

                // 缩放 Bitmap
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

                // 使用 Glide 加载缩放后的图片为圆形
                Glide.with(this)
                        .load(scaledBitmap)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivProfilePicture);
                ivProfilePicture.setVisibility(View.VISIBLE);
                profileAvatar.setBackground(null);
                Log.d(TAG, "Image loaded successfully as circle");
            } else {
                setDefaultAvatar();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
            setDefaultAvatar();
        }
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_staff_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_staff_home) {
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_staff_bookings) {
                Intent intent = new Intent(this, BookingManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_rooms) {
                Intent intent = new Intent(this, StaffRoomListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStaffData();
        loadDashboardStats();
        loadRecentBookings();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_staff_home);
        }
    }
}