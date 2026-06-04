package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TechnicianDashboardActivity extends AppCompatActivity {

    // UI Elements
    private LinearLayout profileAvatar;
    private ShapeableImageView ivProfilePicture;
    private TextView tvTechnicianName;
    private TextView tvPendingRepairs, tvInProgress, tvCompleted, tvTotalRepairs;
    private LinearLayout recentRepairsContainer;
    private BottomNavigationView bottomNavigation;
    private CardView btnViewRepairs;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView mainScrollView;  // ADDED: Reference to main ScrollView

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupSwipeRefresh();
        setupProfileClick();
        setupClickListeners();
        setupBottomNavigation();
        loadTechnicianData();
        loadDashboardStats();
        loadRecentRepairs();
    }

    private void initViews() {
        profileAvatar = findViewById(R.id.profileAvatar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvTechnicianName = findViewById(R.id.tvTechnicianName);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mainScrollView = findViewById(R.id.scrollView);  // ADDED: Find ScrollView

        // Statistics cards
        tvPendingRepairs = findViewById(R.id.tvPendingRepairs);
        tvInProgress = findViewById(R.id.tvInProgress);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvTotalRepairs = findViewById(R.id.tvTotalRepairs);

        // Recent repairs container
        recentRepairsContainer = findViewById(R.id.recentRepairsContainer);

        // Action buttons
        btnViewRepairs = findViewById(R.id.btnViewRepairs);

        // Bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshData();
            });

            // FIXED: 只有当 ScrollView 滚动到顶部时才启用下拉刷新
            if (mainScrollView != null) {
                mainScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    if (swipeRefreshLayout != null && mainScrollView != null) {
                        swipeRefreshLayout.setEnabled(mainScrollView.getScrollY() == 0);
                    }
                });
            }
        }
    }

    private void refreshData() {
        // FIXED: Scroll to top first before refreshing
        scrollToTop();

        // Small delay to ensure scroll completes before refresh starts
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 重新加载所有数据
            loadTechnicianData();
            loadDashboardStats();
            loadRecentRepairs();

            // 停止刷新动画
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 1000);
        }, 150);
    }

    /**
     * FIXED: Scroll to top of the dashboard
     * This ensures the refresh happens from the top of the page
     */
    private void scrollToTop() {
        if (mainScrollView != null) {
            mainScrollView.post(() -> {
                mainScrollView.smoothScrollTo(0, 0);
            });
        } else {
            // Fallback: find any ScrollView in the view hierarchy
            View rootView = findViewById(android.R.id.content);
            ScrollView foundScrollView = findScrollView(rootView);
            if (foundScrollView != null) {
                foundScrollView.smoothScrollTo(0, 0);
            }
        }
    }

    /**
     * Recursively find ScrollView in view hierarchy
     */
    private ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                ScrollView result = findScrollView(viewGroup.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void setupProfileClick() {
        if (profileAvatar != null) {
            profileAvatar.setOnClickListener(v -> goToProfile());
        }
        if (ivProfilePicture != null) {
            ivProfilePicture.setOnClickListener(v -> goToProfile());
        }
    }

    private void goToProfile() {
        Intent intent = new Intent(TechnicianDashboardActivity.this, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void loadTechnicianData() {
        if (currentUser == null) {
            tvTechnicianName.setText("Technician");
            resetToDefaultAvatar();
            return;
        }

        db.collection("Users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        tvTechnicianName.setText(name != null && !name.isEmpty() ? name : "Technician");

                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            loadProfileImageFromBase64(profileImageBase64);
                        } else {
                            resetToDefaultAvatar();
                        }
                    } else {
                        tvTechnicianName.setText("Technician");
                        resetToDefaultAvatar();
                    }
                })
                .addOnFailureListener(e -> {
                    tvTechnicianName.setText("Technician");
                    resetToDefaultAvatar();
                });
    }

    /**
     * 重置为默认头像 - 参考 Student Dashboard 的方式
     */
    private void resetToDefaultAvatar() {
        if (profileAvatar != null) {
            profileAvatar.setBackgroundResource(R.drawable.avatar_background);
        }
        if (ivProfilePicture != null) {
            ivProfilePicture.setVisibility(View.VISIBLE);
            ivProfilePicture.setImageResource(R.drawable.ic_account_circle);
        }
    }

    /**
     * 加载头像图片 - 参考 Student Dashboard 的方式，将图片设置为 profileAvatar 的背景
     */
    private void loadProfileImageFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                // 使用 Glide 加载圆形图片并设置为 profileAvatar 的背景
                Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                if (profileAvatar != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        profileAvatar.setBackground(resource);
                                    } else {
                                        profileAvatar.setBackgroundDrawable(resource);
                                    }
                                    // 隐藏 ImageView，只显示 LinearLayout 背景
                                    if (ivProfilePicture != null) {
                                        ivProfilePicture.setVisibility(View.GONE);
                                    }
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            } else {
                resetToDefaultAvatar();
            }
        } catch (Exception e) {
            resetToDefaultAvatar();
        }
    }

    private void loadDashboardStats() {
        // Count Pending repairs
        db.collection("RepairRequests")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvPendingRepairs.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> tvPendingRepairs.setText("0"));

        // Count In Progress repairs
        db.collection("RepairRequests")
                .whereEqualTo("status", "In Progress")
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvInProgress.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> tvInProgress.setText("0"));

        // Count Completed repairs
        db.collection("RepairRequests")
                .whereEqualTo("status", "Completed")
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvCompleted.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> tvCompleted.setText("0"));

        // Count Total repairs (all status)
        db.collection("RepairRequests")
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvTotalRepairs.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> tvTotalRepairs.setText("0"));
    }

    private void loadRecentRepairs() {
        // Calculate date 2 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -2);
        Date twoDaysAgo = calendar.getTime();
        long twoDaysAgoTimestamp = twoDaysAgo.getTime();

        db.collection("RepairRequests")
                .whereGreaterThanOrEqualTo("createdAt", twoDaysAgoTimestamp)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recentRepairsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        addEmptyStateView("No recent repair requests");
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        View repairView = createRecentRepairView(document, sdf);
                        if (repairView != null) {
                            recentRepairsContainer.addView(repairView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    recentRepairsContainer.removeAllViews();
                    addEmptyStateView("Failed to load recent repairs");
                });
    }

    private View createRecentRepairView(QueryDocumentSnapshot document, SimpleDateFormat sdf) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_technician_repair, null);
        if (itemView == null) return null;

        // 添加底部边距
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 12;
        itemView.setLayoutParams(params);

        TextView tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
        TextView tvIssueType = itemView.findViewById(R.id.tvIssueType);
        TextView tvDescription = itemView.findViewById(R.id.tvDescription);
        TextView tvPriority = itemView.findViewById(R.id.tvPriority);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        LinearLayout btnStartRepair = itemView.findViewById(R.id.btnStartRepair);

        String roomId = document.getString("roomId");
        String issueType = document.getString("issueType");
        String description = document.getString("description");
        String priority = document.getString("priority");
        String status = document.getString("status");
        Long createdAt = document.getLong("createdAt");
        final String documentId = document.getId();

        tvRoomNumber.setText(roomId != null ? roomId : "N/A");
        tvIssueType.setText(issueType != null ? issueType : "N/A");
        tvDescription.setText(description != null ? description : "No description");

        // 设置状态标签
        if (status != null && tvStatus != null) {
            tvStatus.setText(status);
            switch (status.toLowerCase()) {
                case "pending":
                    tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    break;
                case "in progress":
                case "in-progress":
                    tvStatus.setBackgroundResource(R.drawable.status_badge_in_progress);
                    break;
                case "completed":
                    tvStatus.setBackgroundResource(R.drawable.status_badge_completed);
                    break;
                default:
                    tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
                    break;
            }
        }

        // 设置优先级颜色
        if (priority != null) {
            tvPriority.setText(priority);
            switch (priority.toLowerCase()) {
                case "high":
                    tvPriority.setBackgroundResource(R.drawable.urgency_badge_high);
                    break;
                case "medium":
                    tvPriority.setBackgroundResource(R.drawable.urgency_badge_medium);
                    break;
                case "low":
                    tvPriority.setBackgroundResource(R.drawable.urgency_badge_low);
                    break;
                case "emergency":
                    tvPriority.setBackgroundResource(R.drawable.urgency_badge_emergency);
                    break;
                default:
                    tvPriority.setBackgroundResource(R.drawable.urgency_badge);
            }
        }

        if (createdAt != null && createdAt > 0) {
            tvDate.setText(sdf.format(new Date(createdAt)));
        } else {
            tvDate.setText("N/A");
        }

        // 按钮点击始终跳转到详情页
        if (btnStartRepair != null) {
            TextView btnText = (TextView) btnStartRepair.getChildAt(0);
            if (btnText != null) {
                btnText.setText("Details");
            }

            btnStartRepair.setOnClickListener(v -> {
                Intent intent = new Intent(TechnicianDashboardActivity.this, TechnicianRepairDetailActivity.class);
                intent.putExtra("REQUEST_ID", documentId);
                intent.putExtra("roomId", roomId);
                intent.putExtra("roomType", document.getString("roomType"));
                intent.putExtra("issueType", issueType);
                intent.putExtra("priority", priority);
                intent.putExtra("description", description);
                intent.putExtra("status", status);
                intent.putExtra("name", document.getString("name"));
                intent.putExtra("createdAt", createdAt);
                intent.putExtra("availableTime", document.getString("availableTime"));
                intent.putExtra("contactPerson", document.getString("contactPerson"));
                startActivity(intent);
            });
        }

        return itemView;
    }

    private void addEmptyStateView(String message) {
        TextView emptyText = new TextView(this);
        emptyText.setText(message);
        emptyText.setTextSize(13);
        emptyText.setTextColor(getColor(R.color.tabInactiveText));
        emptyText.setPadding(16, 24, 16, 24);
        emptyText.setGravity(android.view.Gravity.CENTER);
        recentRepairsContainer.addView(emptyText);
    }

    private void setupClickListeners() {
        // View Repair Requests button
        if (btnViewRepairs != null) {
            btnViewRepairs.setOnClickListener(v -> {
                Intent intent = new Intent(TechnicianDashboardActivity.this, TechnicianRepairRequestActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_tech_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_tech_home) {
                return true;
            } else if (id == R.id.nav_request) {
                Intent intent = new Intent(this, TechnicianRepairRequestActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_tech_history) {
                Intent intent = new Intent(this, TechnicianHistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
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
        loadTechnicianData();
        loadDashboardStats();
        loadRecentRepairs();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_tech_home);
        }
    }
}