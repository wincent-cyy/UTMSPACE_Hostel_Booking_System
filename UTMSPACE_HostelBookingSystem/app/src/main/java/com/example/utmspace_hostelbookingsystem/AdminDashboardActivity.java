package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    // UI Elements
    private LinearLayout profileAvatar;
    private ShapeableImageView ivProfilePicture;
    private TextView tvAdminName;

    // Statistics TextViews
    private TextView tvTotalUsers;
    private TextView tvTotalBookings;
    private TextView tvTotalRooms;
    private TextView tvRepairRooms;

    // Management Buttons
    private CardView btnUserManagement;
    private CardView btnBookingManagement;
    private CardView btnRoomManagement;

    // 内部的 LinearLayout
    private LinearLayout layoutUserManagement;
    private LinearLayout layoutBookingManagement;
    private LinearLayout layoutRoomManagement;

    // Bottom Navigation
    private BottomNavigationView bottomNavigation;

    // Swipe Refresh
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private Handler handler = new Handler();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        initViews();
        setupSwipeRefresh();
        setupProfileClick();
        setupClickListeners();
        setupBottomNavigation();
        loadAdminData();
        loadDashboardStats();
    }

    private void initViews() {
        profileAvatar = findViewById(R.id.profileAvatar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvAdminName = findViewById(R.id.tvAdminName);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);

        // Statistics
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvTotalRooms = findViewById(R.id.tvPendingApprovals);
        tvRepairRooms = findViewById(R.id.tvActiveRepairs);

        // Management Buttons - CardView
        btnUserManagement = findViewById(R.id.btnUserManagement);
        btnBookingManagement = findViewById(R.id.btnBookingManagement);
        btnRoomManagement = findViewById(R.id.btnRoomManagement);

        // 获取 CardView 内部的 LinearLayout
        if (btnUserManagement != null && btnUserManagement.getChildCount() > 0) {
            layoutUserManagement = (LinearLayout) btnUserManagement.getChildAt(0);
        }
        if (btnBookingManagement != null && btnBookingManagement.getChildCount() > 0) {
            layoutBookingManagement = (LinearLayout) btnBookingManagement.getChildAt(0);
        }
        if (btnRoomManagement != null && btnRoomManagement.getChildCount() > 0) {
            layoutRoomManagement = (LinearLayout) btnRoomManagement.getChildAt(0);
        }

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshDashboard();
            });

            // 只有当 ScrollView 滚动到顶部时才启用下拉刷新
            if (scrollView != null) {
                scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    if (swipeRefreshLayout != null && scrollView != null) {
                        swipeRefreshLayout.setEnabled(scrollView.getScrollY() == 0);
                    }
                });
            }
        }
    }

    private void refreshDashboard() {
        if (isLoading) {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        isLoading = true;

        // 重新加载所有数据
        loadAdminData();
        loadDashboardStats();

        // 停止刷新动画
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            isLoading = false;
            Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void setupProfileClick() {
        if (profileAvatar != null) {
            profileAvatar.setOnClickListener(v -> goToAdminProfile());
        }
        if (ivProfilePicture != null) {
            ivProfilePicture.setOnClickListener(v -> goToAdminProfile());
        }
    }

    private void goToAdminProfile() {
        Intent intent = new Intent(AdminDashboardActivity.this, AdminProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void loadAdminData() {
        if (currentUserId == null) {
            tvAdminName.setText("Admin");
            setDefaultAvatar();
            return;
        }

        db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        tvAdminName.setText(name != null && !name.isEmpty() ? name : "Admin");

                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            loadProfileImageFromBase64(profileImageBase64);
                        } else {
                            setDefaultAvatar();
                        }
                    } else {
                        tvAdminName.setText("Admin");
                        setDefaultAvatar();
                    }
                })
                .addOnFailureListener(e -> {
                    tvAdminName.setText("Admin");
                    setDefaultAvatar();
                });
    }

    private void setDefaultAvatar() {
        Glide.with(this)
                .load(R.drawable.ic_account_circle)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(ivProfilePicture);
        ivProfilePicture.setVisibility(View.VISIBLE);
        if (profileAvatar != null) {
            profileAvatar.setBackgroundResource(R.drawable.avatar_background);
        }
    }

    private void loadProfileImageFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                Glide.with(this)
                        .load(bitmap)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(ivProfilePicture);
                if (profileAvatar != null) {
                    profileAvatar.setBackground(null);
                }
            } else {
                setDefaultAvatar();
            }
        } catch (Exception e) {
            setDefaultAvatar();
        }
    }

    private void loadDashboardStats() {
        db.collection("Users")
                .get()
                .addOnSuccessListener(query -> tvTotalUsers.setText(formatNumber(query.size())))
                .addOnFailureListener(e -> tvTotalUsers.setText("0"));

        db.collection("Bookings")
                .get()
                .addOnSuccessListener(query -> tvTotalBookings.setText(formatNumber(query.size())))
                .addOnFailureListener(e -> tvTotalBookings.setText("0"));

        db.collection("Rooms")
                .get()
                .addOnSuccessListener(query -> tvTotalRooms.setText(formatNumber(query.size())))
                .addOnFailureListener(e -> tvTotalRooms.setText("0"));

        db.collection("Rooms")
                .whereEqualTo("status", "Maintenance")
                .get()
                .addOnSuccessListener(query -> tvRepairRooms.setText(formatNumber(query.size())))
                .addOnFailureListener(e -> tvRepairRooms.setText("0"));
    }

    private void setupClickListeners() {
        btnUserManagement.setOnClickListener(v -> {
            flashCardLayout(layoutUserManagement);
            startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class));
        });

        btnBookingManagement.setOnClickListener(v -> {
            flashCardLayout(layoutBookingManagement);
            startActivity(new Intent(AdminDashboardActivity.this, ManagementActivity.class));
        });

        btnRoomManagement.setOnClickListener(v -> {
            flashCardLayout(layoutRoomManagement);
            startActivity(new Intent(AdminDashboardActivity.this, RoomManagementActivity.class));
        });
    }

    /**
     * 让卡片内部的 LinearLayout 闪烁暗红色效果
     * 所有卡片都是白色背景，点击时变成暗红色
     */
    private void flashCardLayout(LinearLayout layout) {
        if (layout == null) return;

        // 改变为暗红色
        layout.setBackgroundColor(getColor(R.color.primaryColor));

        // 150ms 后恢复白色
        handler.postDelayed(() -> {
            layout.setBackgroundColor(getColor(android.R.color.white));
        }, 150);
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class));
                return true;
            } else if (id == R.id.nav_rooms) {
                startActivity(new Intent(AdminDashboardActivity.this, RoomManagementActivity.class));
                return true;
            } else if (id == R.id.nav_management) {
                startActivity(new Intent(AdminDashboardActivity.this, ManagementActivity.class));
                return true;
            }
            return false;
        });
    }

    private String formatNumber(int number) {
        if (number >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdminData();
        loadDashboardStats();

        // 确保返回时所有卡片背景恢复白色
        if (layoutUserManagement != null) {
            layoutUserManagement.setBackgroundColor(getColor(android.R.color.white));
        }
        if (layoutBookingManagement != null) {
            layoutBookingManagement.setBackgroundColor(getColor(android.R.color.white));
        }
        if (layoutRoomManagement != null) {
            layoutRoomManagement.setBackgroundColor(getColor(android.R.color.white));
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }
}