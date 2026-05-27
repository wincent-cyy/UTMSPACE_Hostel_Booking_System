package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManagementActivity extends AppCompatActivity {

    // UI Elements
    private TabLayout tabLayout;
    private EditText etSearch;
    private RecyclerView rvItemList;
    private LinearLayout emptyState;
    private TextView tvItemCount;
    private BottomNavigationView bottomNavigation;

    // Filter chips for Bookings
    private TextView chipAll, chipPending, chipApproved, chipRejected, chipPaid;

    // Filter chips for Repair Requests
    private TextView repairChipAll, repairChipPending, repairChipInProgress, repairChipCompleted;

    // Layout containers
    private LinearLayout bookingFilters, repairFilters;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Data - 使用独立的列表，不共用
    private List<BookingModel> bookingList = new ArrayList<>();
    private List<RepairRequestModel> repairList = new ArrayList<>();

    private AdminBookingAdapter bookingAdapter;
    private AdminRepairAdapter repairAdapter;

    private String currentTab = "Bookings";
    private String currentFilter = "All";
    private String currentSearchQuery = "";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isProcessing = false;
    private boolean isBookingsLoaded = false;
    private boolean isRepairsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupTabLayout();
        setupFilterChips();
        setupSearchFilter();
        setupBottomNavigation();

        // 初始加载 Bookings
        loadBookings();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        etSearch = findViewById(R.id.etSearch);
        rvItemList = findViewById(R.id.rvItemList);
        emptyState = findViewById(R.id.emptyState);
        tvItemCount = findViewById(R.id.tvItemCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipApproved = findViewById(R.id.chipApproved);
        chipRejected = findViewById(R.id.chipRejected);
        chipPaid = findViewById(R.id.chipPaid);

        repairChipAll = findViewById(R.id.repairChipAll);
        repairChipPending = findViewById(R.id.repairChipPending);
        repairChipInProgress = findViewById(R.id.repairChipInProgress);
        repairChipCompleted = findViewById(R.id.repairChipCompleted);

        bookingFilters = findViewById(R.id.bookingFilters);
        repairFilters = findViewById(R.id.repairFilters);
    }

    private void setupRecyclerView() {
        rvItemList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // 切换到 Bookings
                    currentTab = "Bookings";
                    currentFilter = "All";
                    currentSearchQuery = "";
                    etSearch.setText("");

                    bookingFilters.setVisibility(View.VISIBLE);
                    repairFilters.setVisibility(View.GONE);
                    updateBookingChipStyles(chipAll);

                    // 切换 adapter
                    if (bookingAdapter != null) {
                        rvItemList.setAdapter(bookingAdapter);
                        filterBookings();
                    } else {
                        loadBookings();
                    }
                } else {
                    // 切换到 RepairRequests
                    currentTab = "RepairRequests";
                    currentFilter = "All";
                    currentSearchQuery = "";
                    etSearch.setText("");

                    bookingFilters.setVisibility(View.GONE);
                    repairFilters.setVisibility(View.VISIBLE);
                    updateRepairChipStyles(repairChipAll);

                    // 切换 adapter
                    if (repairAdapter != null) {
                        rvItemList.setAdapter(repairAdapter);
                        filterRepairs();
                    } else {
                        loadRepairRequests();
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFilterChips() {
        // Booking chips
        chipAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateBookingChipStyles(chipAll);
            filterBookings();
        });

        chipPending.setOnClickListener(v -> {
            currentFilter = "Pending";
            updateBookingChipStyles(chipPending);
            filterBookings();
        });

        chipApproved.setOnClickListener(v -> {
            currentFilter = "Approved";
            updateBookingChipStyles(chipApproved);
            filterBookings();
        });

        chipRejected.setOnClickListener(v -> {
            currentFilter = "Rejected";
            updateBookingChipStyles(chipRejected);
            filterBookings();
        });

        chipPaid.setOnClickListener(v -> {
            currentFilter = "Paid";
            updateBookingChipStyles(chipPaid);
            filterBookings();
        });

        // Repair chips
        repairChipAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateRepairChipStyles(repairChipAll);
            filterRepairs();
        });

        repairChipPending.setOnClickListener(v -> {
            currentFilter = "Pending";
            updateRepairChipStyles(repairChipPending);
            filterRepairs();
        });

        repairChipInProgress.setOnClickListener(v -> {
            currentFilter = "In Progress";
            updateRepairChipStyles(repairChipInProgress);
            filterRepairs();
        });

        repairChipCompleted.setOnClickListener(v -> {
            currentFilter = "Completed";
            updateRepairChipStyles(repairChipCompleted);
            filterRepairs();
        });
    }

    private void updateBookingChipStyles(TextView selectedChip) {
        chipAll.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipAll.setTextColor(getColor(android.R.color.black));
        chipPending.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipPending.setTextColor(getColor(android.R.color.black));
        chipApproved.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipApproved.setTextColor(getColor(android.R.color.black));
        chipRejected.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipRejected.setTextColor(getColor(android.R.color.black));
        chipPaid.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipPaid.setTextColor(getColor(android.R.color.black));

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void updateRepairChipStyles(TextView selectedChip) {
        repairChipAll.setBackgroundResource(R.drawable.filter_chip_unselected);
        repairChipAll.setTextColor(getColor(android.R.color.black));
        repairChipPending.setBackgroundResource(R.drawable.filter_chip_unselected);
        repairChipPending.setTextColor(getColor(android.R.color.black));
        repairChipInProgress.setBackgroundResource(R.drawable.filter_chip_unselected);
        repairChipInProgress.setTextColor(getColor(android.R.color.black));
        repairChipCompleted.setBackgroundResource(R.drawable.filter_chip_unselected);
        repairChipCompleted.setTextColor(getColor(android.R.color.black));

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString();
                searchRunnable = () -> {
                    currentSearchQuery = query;
                    if (currentTab.equals("Bookings")) {
                        filterBookings();
                    } else {
                        filterRepairs();
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBookings() {
        db.collection("Bookings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookingList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        BookingModel booking = document.toObject(BookingModel.class);
                        booking.setDocumentId(document.getId());
                        bookingList.add(booking);
                    }
                    Log.d("MANAGEMENT", "Loaded " + bookingList.size() + " bookings");

                    // 创建 adapter
                    bookingAdapter = new AdminBookingAdapter(bookingList, new AdminBookingAdapter.OnBookingActionListener() {
                        @Override
                        public void onUpdateStatus(BookingModel booking, String newStatus) {
                            updateBookingStatus(booking, newStatus);
                        }
                        @Override
                        public void onViewDetails(BookingModel booking) {
                            showBookingDetailsDialog(booking);
                        }
                        @Override
                        public void onDelete(BookingModel booking) {
                            showDeleteBookingConfirmDialog(booking);
                        }
                    });

                    // 如果当前是 Bookings tab，显示数据
                    if (currentTab.equals("Bookings")) {
                        rvItemList.setAdapter(bookingAdapter);
                        filterBookings();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRepairRequests() {
        db.collection("RepairRequests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    repairList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequestModel request = document.toObject(RepairRequestModel.class);
                        request.setDocumentId(document.getId());
                        repairList.add(request);
                    }
                    Log.d("MANAGEMENT", "Loaded " + repairList.size() + " repair requests");

                    // 创建 adapter
                    repairAdapter = new AdminRepairAdapter(repairList, new AdminRepairAdapter.OnRepairActionListener() {
                        @Override
                        public void onViewDetails(RepairRequestModel request) {
                            showRepairDetailsDialog(request);
                        }
                    });

                    // 如果当前是 RepairRequests tab，显示数据
                    if (currentTab.equals("RepairRequests")) {
                        rvItemList.setAdapter(repairAdapter);
                        filterRepairs();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load repair requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterBookings() {
        if (bookingAdapter == null) return;

        List<BookingModel> filtered = new ArrayList<>();

        for (BookingModel booking : bookingList) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;

            if (!currentFilter.equals("All")) {
                String status = booking.getBookingStatus();
                matchesFilter = status != null && status.equalsIgnoreCase(currentFilter);
            }

            if (!currentSearchQuery.isEmpty()) {
                String query = currentSearchQuery.toLowerCase().trim();
                String roomId = booking.getRoomId() != null ? booking.getRoomId().toLowerCase() : "";
                String name = booking.getName() != null ? booking.getName().toLowerCase() : "";
                matchesSearch = roomId.contains(query) || name.contains(query);
            }

            if (matchesFilter && matchesSearch) {
                filtered.add(booking);
            }
        }

        bookingAdapter.updateList(filtered);
        updateUIState(filtered.size(), "bookings");
    }

    private void filterRepairs() {
        if (repairAdapter == null) return;

        List<RepairRequestModel> filtered = new ArrayList<>();

        for (RepairRequestModel request : repairList) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;

            if (!currentFilter.equals("All")) {
                String status = request.getStatus();
                matchesFilter = status != null && status.equalsIgnoreCase(currentFilter);
            }

            if (!currentSearchQuery.isEmpty()) {
                String query = currentSearchQuery.toLowerCase().trim();
                String roomId = request.getRoomId() != null ? request.getRoomId().toLowerCase() : "";
                String description = request.getDescription() != null ? request.getDescription().toLowerCase() : "";
                matchesSearch = roomId.contains(query) || description.contains(query);
            }

            if (matchesFilter && matchesSearch) {
                filtered.add(request);
            }
        }

        repairAdapter.updateList(filtered);
        updateUIState(filtered.size(), "requests");
    }

    private void updateUIState(int size, String type) {
        if (size == 0) {
            rvItemList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            tvItemCount.setText("0 " + type);
        } else {
            rvItemList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            tvItemCount.setText(size + " " + type);
        }
    }

    private void updateBookingStatus(BookingModel booking, String newStatus) {
        if (isProcessing) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;

        Map<String, Object> updates = new HashMap<>();
        updates.put("bookingStatus", newStatus);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection("Bookings").document(booking.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    isProcessing = false;
                    Toast.makeText(this, "Booking " + newStatus.toLowerCase(), Toast.LENGTH_SHORT).show();
                    loadBookings();
                })
                .addOnFailureListener(e -> {
                    isProcessing = false;
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBookingDetailsDialog(BookingModel booking) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_booking_details, null);

        TextView tvRoomId = dialogView.findViewById(R.id.tvRoomId);
        TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = dialogView.findViewById(R.id.tvUserEmail);
        TextView tvCheckIn = dialogView.findViewById(R.id.tvCheckIn);
        TextView tvTotalPrice = dialogView.findViewById(R.id.tvTotalPrice);
        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        TextView tvBookingDate = dialogView.findViewById(R.id.tvBookingDate);

        tvRoomId.setText(booking.getRoomId());
        tvUserName.setText(booking.getName());
        tvUserEmail.setText(booking.getMatricNumber());
        tvCheckIn.setText(booking.getCheckInDate());
        tvTotalPrice.setText("RM " + booking.getPrice());

        String status = booking.getBookingStatus() != null ? booking.getBookingStatus() : "Pending";
        tvStatus.setText(status);
        tvBookingDate.setText(formatDate(booking.getCreatedAt()));

        int statusColor = getStatusColor(status);
        tvStatus.setTextColor(statusColor);

        builder.setTitle("Booking Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showRepairDetailsDialog(RepairRequestModel request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_repair_details, null);

        TextView tvRoomId = dialogView.findViewById(R.id.tvRoomId);
        TextView tvItemName = dialogView.findViewById(R.id.tvItemName);
        TextView tvUrgency = dialogView.findViewById(R.id.tvUrgency);
        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        TextView tvStaffName = dialogView.findViewById(R.id.tvStaffName);
        TextView tvCreatedAt = dialogView.findViewById(R.id.tvCreatedAt);
        TextView tvLastUpdated = dialogView.findViewById(R.id.tvLastUpdated);
        TextView tvDescription = dialogView.findViewById(R.id.tvDescription);
        ImageView ivProofImage = dialogView.findViewById(R.id.ivProofImage);
        Button btnViewFullImage = dialogView.findViewById(R.id.btnViewFullImage);

        // Set text values
        tvRoomId.setText(request.getRoomId());
        tvItemName.setText(request.getItemName() != null ? request.getItemName() : "N/A");
        tvUrgency.setText(request.getUrgency() != null ? request.getUrgency() : "Medium");
        tvStatus.setText(request.getStatus());
        tvStaffName.setText(request.getStaffName() != null ? request.getStaffName() : "Not Assigned");
        tvCreatedAt.setText(formatDateTime(request.getCreatedAt()));
        tvLastUpdated.setText(formatDateTime(request.getLastUpdated()));
        tvDescription.setText(request.getDescription() != null ? request.getDescription() : "No description provided");

        // Set status color
        int statusColor = getStatusColor(request.getStatus());
        tvStatus.setTextColor(statusColor);

        // Set urgency color
        int urgencyColor = getUrgencyColor(request.getUrgency());
        tvUrgency.setTextColor(urgencyColor);

        // Load proof image from Base64 string
        String proofImageBase64 = request.getProofImage();  // 注意字段名是 proofImage
        if (proofImageBase64 != null && !proofImageBase64.isEmpty()) {
            try {
                // 解码 Base64 字符串为字节数组
                byte[] imageBytes = Base64.decode(proofImageBase64, Base64.DEFAULT);
                // 将字节数组转换为 Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                // 显示图片
                ivProofImage.setImageBitmap(bitmap);

                btnViewFullImage.setVisibility(View.VISIBLE);
                btnViewFullImage.setOnClickListener(v -> showFullImageDialog(proofImageBase64));
            } catch (Exception e) {
                Log.e("REPAIR_DETAIL", "Failed to decode image", e);
                ivProofImage.setImageResource(android.R.drawable.ic_menu_gallery);
                btnViewFullImage.setVisibility(View.GONE);
            }
        } else {
            ivProofImage.setImageResource(android.R.drawable.ic_menu_gallery);
            btnViewFullImage.setVisibility(View.GONE);
        }

        builder.setTitle("Repair Request Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showFullImageDialog(String proofImageBase64) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_full_image, null);
        ImageView ivFullImage = dialogView.findViewById(R.id.fullImageView);

        try {
            byte[] imageBytes = Base64.decode(proofImageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ivFullImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e("REPAIR_DETAIL", "Failed to decode full image", e);
            ivFullImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        builder.setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showDeleteBookingConfirmDialog(BookingModel booking) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Are you sure you want to delete booking for room " + booking.getRoomId() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteBooking(booking.getDocumentId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBooking(String documentId) {
        if (isProcessing) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;

        db.collection("Bookings").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isProcessing = false;
                    Toast.makeText(this, "Booking deleted successfully", Toast.LENGTH_SHORT).show();
                    loadBookings();
                })
                .addOnFailureListener(e -> {
                    isProcessing = false;
                    Log.e("DELETE_BOOKING", "Delete failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatDateTime(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private int getStatusColor(String status) {
        if (status == null) return getColor(android.R.color.darker_gray);

        switch (status) {
            case "Approved":
            case "Completed":
                return getColor(android.R.color.holo_green_dark);
            case "Pending":
                return getColor(android.R.color.holo_orange_dark);
            case "Rejected":
            case "Cancelled":
                return getColor(android.R.color.holo_red_dark);
            case "Paid":
                return getColor(android.R.color.holo_blue_dark);
            case "In Progress":
                return getColor(android.R.color.holo_blue_dark);
            default:
                return getColor(android.R.color.darker_gray);
        }
    }

    private int getUrgencyColor(String urgency) {
        if (urgency == null) return android.graphics.Color.parseColor("#64748B");

        switch (urgency.toLowerCase()) {
            case "high":
                return android.graphics.Color.parseColor("#EF4444");
            case "medium":
                return android.graphics.Color.parseColor("#F59E0B");
            case "low":
                return android.graphics.Color.parseColor("#10B981");
            default:
                return android.graphics.Color.parseColor("#64748B");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_management);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_management) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, UserManagementActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_rooms) {
                startActivity(new Intent(this, RoomManagementActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab.equals("Bookings")) {
            loadBookings();
        } else {
            loadRepairRequests();
        }
    }
}