package com.example.utmspace_hostelbookingsystem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
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
    private LinearLayout profileAvatar;  // ADDED: 头像容器 (LinearLayout)
    private LinearLayout cardTotalBookings;
    private LinearLayout cardActiveIssues;
    private TextView tvTotalBookings, tvRoomIssues, tvOccupiedRooms, tvVacantRooms;
    private LinearLayout recentBookingsContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

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
        setupSwipeRefresh();

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
        profileAvatar = findViewById(R.id.profileAvatar);  // ADDED
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

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

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );

            // 获取 ScrollView
            ScrollView scrollView = findViewById(R.id.scrollView);

            // 设置只有当 ScrollView 滚动到顶部时才启用下拉刷新
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshDashboard();
            });

            // 监听 ScrollView 滚动状态
            if (scrollView != null) {
                scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    if (scrollView.getScrollY() == 0) {
                        swipeRefreshLayout.setEnabled(true);
                    } else {
                        swipeRefreshLayout.setEnabled(false);
                    }
                });
            }
        }
    }

    private void refreshDashboard() {
        // 重新加载所有数据
        loadStaffData();
        loadDashboardStats();
        loadRecentBookings();

        // 停止刷新动画
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void setupCardClickListeners() {
        if (cardTotalBookings != null) {
            cardTotalBookings.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, BookingManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

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
        if (profileAvatar != null) {
            profileAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        if (ivProfilePicture != null) {
            ivProfilePicture.setOnClickListener(v -> {
                Intent intent = new Intent(StaffDashboardActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
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
        db.collection("Bookings").get()
                .addOnSuccessListener(query -> {
                    tvTotalBookings.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> tvTotalBookings.setText("0"));

        db.collection("Rooms")
                .whereEqualTo("status", "Maintenance")
                .get()
                .addOnSuccessListener(query -> {
                    tvRoomIssues.setText(String.valueOf(query.size()));
                })
                .addOnFailureListener(e -> tvRoomIssues.setText("0"));

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
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date sevenDaysAgo = calendar.getTime();
        long sevenDaysAgoTimestamp = sevenDaysAgo.getTime();

        db.collection("Bookings")
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgoTimestamp)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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
                    addErrorStateView("Failed to load recent bookings");
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 16;
        itemView.setLayoutParams(params);

        TextView tvRoomType = itemView.findViewById(R.id.tvRoomType);
        TextView tvRoomId = itemView.findViewById(R.id.tvRoomId);
        TextView tvStudentName = itemView.findViewById(R.id.tvStudentName);
        TextView tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);

        String roomType = document.getString("roomType");
        String roomId = document.getString("roomId");
        String studentName = document.getString("name");
        Long createdAt = document.getLong("createdAt");
        String status = document.getString("bookingStatus");

        if (tvRoomType != null) {
            tvRoomType.setText(roomType != null ? roomType : "Room");
        }

        if (tvRoomId != null) {
            tvRoomId.setText(roomId != null ? roomId : "N/A");
        }

        if (tvStudentName != null) {
            tvStudentName.setText(studentName != null ? studentName : "Student");
        }

        if (tvBookingDate != null) {
            if (createdAt != null && createdAt > 0) {
                tvBookingDate.setText(sdf.format(new Date(createdAt)));
            } else {
                tvBookingDate.setText("N/A");
            }
        }

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
            resetToDefaultAvatar();
            return;
        }

        String userId = currentUser.getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvStaffName.setText(name);
                        } else {
                            tvStaffName.setText("Staff Member");
                        }

                        String base64String = documentSnapshot.getString("profileImageBase64");

                        if (base64String != null && !base64String.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                if (bitmap != null) {
                                    // FIXED: 参考 Student Dashboard 的方式，将图片设置为 profileAvatar 的背景
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
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, "Base64 decode error", e);
                                resetToDefaultAvatar();
                            }
                        } else {
                            resetToDefaultAvatar();
                        }
                    } else {
                        resetToDefaultAvatar();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load staff data", e);
                    resetToDefaultAvatar();
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * FIXED: 重置为默认头像 - 参考 Student Dashboard 的方式
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