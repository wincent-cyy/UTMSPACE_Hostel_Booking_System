package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManagementActivity extends AppCompatActivity {

    // Tab buttons
    private TextView tabBookings, tabRepairs;

    // Search bar
    private EditText etSearchGlobal;
    private ImageView ivClearSearchGlobal;

    // Bookings section
    private LinearLayout bookingsSection;
    private TextView chipAllBookings, chipPendingBookings, chipApprovedBookings, chipRejectedBookings, chipPaidBookings;
    private TextView tvBookingCount;
    private LinearLayout bookingsContainer;

    // Repairs section
    private LinearLayout repairsSection;
    private TextView chipAllRepairs, chipPendingRepairs, chipInProgressRepairs, chipCompletedRepairs;
    private TextView tvRepairCount;
    private LinearLayout repairsContainer;

    // Empty state
    private LinearLayout emptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;

    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<BookingModel> allBookings = new ArrayList<>();
    private List<BookingModel> filteredBookings = new ArrayList<>();
    private List<RepairRequestModel> allRepairs = new ArrayList<>();
    private List<RepairRequestModel> filteredRepairs = new ArrayList<>();

    private String currentTab = "Bookings";
    private String currentBookingFilter = "All";
    private String currentRepairFilter = "All";
    private String currentSearchQuery = "";
    private boolean isLoading = false;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management);

        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupSwipeRefresh();
        setupTabs();
        setupFilterChips();
        setupSearchFunction();
        setupBottomNavigation();

        loadBookings();
        loadRepairs();
    }

    private void initViews() {
        tabBookings = findViewById(R.id.tabBookings);
        tabRepairs = findViewById(R.id.tabRepairs);
        etSearchGlobal = findViewById(R.id.etSearchGlobal);
        ivClearSearchGlobal = findViewById(R.id.ivClearSearchGlobal);
        bookingsSection = findViewById(R.id.bookingsSection);
        chipAllBookings = findViewById(R.id.chipAllBookings);
        chipPendingBookings = findViewById(R.id.chipPendingBookings);
        chipApprovedBookings = findViewById(R.id.chipApprovedBookings);
        chipRejectedBookings = findViewById(R.id.chipRejectedBookings);
        chipPaidBookings = findViewById(R.id.chipPaidBookings);
        tvBookingCount = findViewById(R.id.tvBookingCount);
        bookingsContainer = findViewById(R.id.bookingsContainer);
        repairsSection = findViewById(R.id.repairsSection);
        chipAllRepairs = findViewById(R.id.chipAllRepairs);
        chipPendingRepairs = findViewById(R.id.chipPendingRepairs);
        chipInProgressRepairs = findViewById(R.id.chipInProgressRepairs);
        chipCompletedRepairs = findViewById(R.id.chipCompletedRepairs);
        tvRepairCount = findViewById(R.id.tvRepairCount);
        repairsContainer = findViewById(R.id.repairsContainer);
        emptyState = findViewById(R.id.emptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshData();
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

    private void refreshData() {
        if (isLoading) {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        // Reset search
        currentSearchQuery = "";
        if (etSearchGlobal != null) {
            etSearchGlobal.setText("");
        }
        if (ivClearSearchGlobal != null) {
            ivClearSearchGlobal.setVisibility(View.GONE);
        }

        // Reset filters based on current tab
        if (currentTab.equals("Bookings")) {
            currentBookingFilter = "All";
            updateBookingChipStyles(chipAllBookings);
        } else {
            currentRepairFilter = "All";
            updateRepairChipStyles(chipAllRepairs);
        }

        // Reload data
        loadBookings();
        loadRepairs();

        // Stop refresh animation after data is loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void setupTabs() {
        tabBookings.setOnClickListener(v -> {
            currentTab = "Bookings";
            updateTabStyles(true);
            showBookingsSection();
            etSearchGlobal.setText("");
            currentSearchQuery = "";
            applyBookingFilters();
        });

        tabRepairs.setOnClickListener(v -> {
            currentTab = "Repairs";
            updateTabStyles(false);
            showRepairsSection();
            etSearchGlobal.setText("");
            currentSearchQuery = "";
            applyRepairFilters();
        });
    }

    private void updateTabStyles(boolean isBookingsSelected) {
        if (isBookingsSelected) {
            tabBookings.setBackgroundResource(R.drawable.filter_chip_selected);
            tabBookings.setTextColor(getColor(android.R.color.white));
            tabRepairs.setBackgroundResource(R.drawable.filter_chip_unselected);
            tabRepairs.setTextColor(getColor(R.color.tabInactiveText));
        } else {
            tabRepairs.setBackgroundResource(R.drawable.filter_chip_selected);
            tabRepairs.setTextColor(getColor(android.R.color.white));
            tabBookings.setBackgroundResource(R.drawable.filter_chip_unselected);
            tabBookings.setTextColor(getColor(R.color.tabInactiveText));
        }
    }

    private void showBookingsSection() {
        bookingsSection.setVisibility(View.VISIBLE);
        repairsSection.setVisibility(View.GONE);
        tvEmptyTitle.setText("No Bookings Found");
        tvEmptySubtitle.setText("No bookings available at the moment");
        currentBookingFilter = "All";
        updateBookingChipStyles(chipAllBookings);
    }

    private void showRepairsSection() {
        bookingsSection.setVisibility(View.GONE);
        repairsSection.setVisibility(View.VISIBLE);
        tvEmptyTitle.setText("No Repair Requests Found");
        tvEmptySubtitle.setText("No repair requests available at the moment");
        currentRepairFilter = "All";
        updateRepairChipStyles(chipAllRepairs);
    }

    private void setupFilterChips() {
        chipAllBookings.setOnClickListener(v -> {
            currentBookingFilter = "All";
            updateBookingChipStyles(chipAllBookings);
            applyBookingFilters();
        });
        chipPendingBookings.setOnClickListener(v -> {
            currentBookingFilter = "Pending";
            updateBookingChipStyles(chipPendingBookings);
            applyBookingFilters();
        });
        chipApprovedBookings.setOnClickListener(v -> {
            currentBookingFilter = "Approved";
            updateBookingChipStyles(chipApprovedBookings);
            applyBookingFilters();
        });
        chipRejectedBookings.setOnClickListener(v -> {
            currentBookingFilter = "Rejected";
            updateBookingChipStyles(chipRejectedBookings);
            applyBookingFilters();
        });
        chipPaidBookings.setOnClickListener(v -> {
            currentBookingFilter = "Paid";
            updateBookingChipStyles(chipPaidBookings);
            applyBookingFilters();
        });

        chipAllRepairs.setOnClickListener(v -> {
            currentRepairFilter = "All";
            updateRepairChipStyles(chipAllRepairs);
            applyRepairFilters();
        });
        chipPendingRepairs.setOnClickListener(v -> {
            currentRepairFilter = "Pending";
            updateRepairChipStyles(chipPendingRepairs);
            applyRepairFilters();
        });
        chipInProgressRepairs.setOnClickListener(v -> {
            currentRepairFilter = "In Progress";
            updateRepairChipStyles(chipInProgressRepairs);
            applyRepairFilters();
        });
        chipCompletedRepairs.setOnClickListener(v -> {
            currentRepairFilter = "Completed";
            updateRepairChipStyles(chipCompletedRepairs);
            applyRepairFilters();
        });
    }

    private void updateBookingChipStyles(TextView selectedChip) {
        resetBookingChipStyle(chipAllBookings);
        resetBookingChipStyle(chipPendingBookings);
        resetBookingChipStyle(chipApprovedBookings);
        resetBookingChipStyle(chipRejectedBookings);
        resetBookingChipStyle(chipPaidBookings);
        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void resetBookingChipStyle(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        chip.setTextColor(getColor(R.color.tabInactiveText));
    }

    private void updateRepairChipStyles(TextView selectedChip) {
        resetRepairChipStyle(chipAllRepairs);
        resetRepairChipStyle(chipPendingRepairs);
        resetRepairChipStyle(chipInProgressRepairs);
        resetRepairChipStyle(chipCompletedRepairs);
        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void resetRepairChipStyle(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        chip.setTextColor(getColor(R.color.tabInactiveText));
    }

    private void setupSearchFunction() {
        etSearchGlobal.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etSearchGlobal.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearchGlobal.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        etSearchGlobal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                String query = s.toString();
                if (ivClearSearchGlobal != null) {
                    ivClearSearchGlobal.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }
                searchRunnable = () -> {
                    currentSearchQuery = query.toLowerCase().trim();
                    if (currentTab.equals("Bookings")) {
                        applyBookingFilters();
                    } else {
                        applyRepairFilters();
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (ivClearSearchGlobal != null) {
            ivClearSearchGlobal.setOnClickListener(v -> {
                etSearchGlobal.setText("");
                currentSearchQuery = "";
                if (currentTab.equals("Bookings")) {
                    applyBookingFilters();
                } else {
                    applyRepairFilters();
                }
            });
        }
    }

    private void loadBookings() {
        if (isLoading) return;

        db.collection("Bookings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allBookings.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        BookingModel booking = new BookingModel();
                        booking.setDocumentId(document.getId());
                        booking.setRoomId(document.getString("roomId"));
                        booking.setRoomType(document.getString("roomType"));
                        booking.setName(document.getString("name"));
                        booking.setMatricNumber(document.getString("matricNumber"));
                        booking.setPhone(document.getString("phone"));
                        booking.setEmail(document.getString("email"));
                        booking.setProgramme(document.getString("programme"));
                        booking.setLocation(document.getString("location"));
                        booking.setBookingStatus(document.getString("bookingStatus"));
                        booking.setCreatedAt(document.getLong("createdAt") != null ? document.getLong("createdAt") : 0);
                        booking.setPrice(document.getDouble("price") != null ? document.getDouble("price") : 0);
                        booking.setCheckInDate(document.getString("checkInDate"));
                        booking.setLeaseDuration(document.getString("leaseDuration"));
                        booking.setRejectReason(document.getString("rejectReason"));

                        allBookings.add(booking);
                    }
                    if (currentTab.equals("Bookings")) {
                        applyBookingFilters();
                    }

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void loadRepairs() {
        db.collection("RepairRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRepairs.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequestModel request = new RepairRequestModel();
                        request.setDocumentId(document.getId());
                        request.setRoomId(document.getString("roomId"));
                        request.setRoomType(document.getString("roomType"));
                        request.setIssueType(document.getString("issueType"));
                        request.setPriority(document.getString("priority"));
                        request.setDescription(document.getString("description"));
                        request.setStatus(document.getString("status"));
                        request.setCreatedAt(document.getLong("createdAt") != null ? document.getLong("createdAt") : 0);
                        request.setItemName(document.getString("itemName"));
                        request.setUrgency(document.getString("urgency"));
                        request.setStaffName(document.getString("staffName"));
                        request.setProofImage(document.getString("proofImage"));
                        request.setAvailableTime(document.getString("availableTime"));
                        request.setContactPerson(document.getString("contactPerson"));
                        request.setCompletionPhoto(document.getString("completionPhoto"));

                        request.setStudentName(document.getString("studentName"));
                        request.setStudentId(document.getString("studentId"));
                        request.setStudentEmail(document.getString("studentEmail"));

                        Long startedAt = document.getLong("startedAt");
                        if (startedAt != null) request.setStartedAt(startedAt);

                        Long completedAt = document.getLong("completedAt");
                        if (completedAt != null) request.setCompletedAt(completedAt);

                        Long lastUpdated = document.getLong("lastUpdated");
                        if (lastUpdated != null) request.setLastUpdated(lastUpdated);

                        allRepairs.add(request);
                    }
                    if (currentTab.equals("Repairs")) {
                        applyRepairFilters();
                    }

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load repair requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void applyBookingFilters() {
        filteredBookings.clear();
        for (BookingModel booking : allBookings) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;
            if (!"All".equals(currentBookingFilter)) {
                String status = booking.getBookingStatus();
                matchesFilter = status != null && status.equalsIgnoreCase(currentBookingFilter);
            }
            if (!currentSearchQuery.isEmpty()) {
                String roomId = booking.getRoomId() != null ? booking.getRoomId().toLowerCase() : "";
                String name = booking.getName() != null ? booking.getName().toLowerCase() : "";
                matchesSearch = roomId.contains(currentSearchQuery) || name.contains(currentSearchQuery);
            }
            if (matchesFilter && matchesSearch) {
                filteredBookings.add(booking);
            }
        }
        displayBookings();
    }

    private void applyRepairFilters() {
        filteredRepairs.clear();
        for (RepairRequestModel request : allRepairs) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;
            if (!"All".equals(currentRepairFilter)) {
                String status = request.getStatus();
                matchesFilter = status != null && status.equalsIgnoreCase(currentRepairFilter);
            }
            if (!currentSearchQuery.isEmpty()) {
                String roomId = request.getRoomId() != null ? request.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(currentSearchQuery);
            }
            if (matchesFilter && matchesSearch) {
                filteredRepairs.add(request);
            }
        }
        displayRepairs();
    }

    private void displayBookings() {
        bookingsContainer.removeAllViews();
        if (filteredBookings.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            bookingsContainer.setVisibility(View.GONE);
            tvBookingCount.setText("0 bookings");
            return;
        }
        emptyState.setVisibility(View.GONE);
        bookingsContainer.setVisibility(View.VISIBLE);
        tvBookingCount.setText(filteredBookings.size() + " bookings");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        for (int i = 0; i < filteredBookings.size(); i++) {
            BookingModel booking = filteredBookings.get(i);
            View card = createBookingCard(booking, sdf);
            bookingsContainer.addView(card);
            if (i < filteredBookings.size() - 1) {
                addDivider(bookingsContainer);
            }
        }
    }

    private void displayRepairs() {
        repairsContainer.removeAllViews();
        if (filteredRepairs.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            repairsContainer.setVisibility(View.GONE);
            tvRepairCount.setText("0 requests");
            return;
        }
        emptyState.setVisibility(View.GONE);
        repairsContainer.setVisibility(View.VISIBLE);
        tvRepairCount.setText(filteredRepairs.size() + " requests");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        for (int i = 0; i < filteredRepairs.size(); i++) {
            RepairRequestModel request = filteredRepairs.get(i);
            View card = createRepairCard(request, sdf);
            repairsContainer.addView(card);
            if (i < filteredRepairs.size() - 1) {
                addDivider(repairsContainer);
            }
        }
    }

    private void addDivider(LinearLayout container) {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 12);
        divider.setLayoutParams(params);
        container.addView(divider);
    }

    private View createBookingCard(BookingModel booking, SimpleDateFormat sdf) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_admin_booking, null);
        TextView tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
        TextView tvStudentName = itemView.findViewById(R.id.tvStudentName);
        TextView tvRoomType = itemView.findViewById(R.id.tvRoomType);
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        LinearLayout btnView = itemView.findViewById(R.id.btnView);

        tvRoomNumber.setText(booking.getRoomId() != null ? booking.getRoomId() : "N/A");
        tvStudentName.setText(booking.getName() != null ? booking.getName() : "N/A");
        tvRoomType.setText(booking.getRoomType() != null ? booking.getRoomType() : "N/A");
        tvDate.setText(booking.getCreatedAt() > 0 ? sdf.format(new Date(booking.getCreatedAt())) : "N/A");
        String status = booking.getBookingStatus() != null ? booking.getBookingStatus() : "Pending";
        tvStatus.setText(status);
        setBookingStatusColor(tvStatus, status);

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(ManagementActivity.this, AdminBookingDetailActivity.class);
            intent.putExtra("BOOKING_ID", booking.getDocumentId());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_LOCATION", booking.getLocation());
            intent.putExtra("STUDENT_NAME", booking.getName());
            intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
            intent.putExtra("PHONE", booking.getPhone());
            intent.putExtra("EMAIL", booking.getEmail());
            intent.putExtra("PROGRAMME", booking.getProgramme());
            intent.putExtra("PRICE", booking.getPrice());
            intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
            intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());
            intent.putExtra("STATUS", booking.getBookingStatus());
            intent.putExtra("REJECT_REASON", booking.getRejectReason());
            intent.putExtra("CREATED_AT", booking.getCreatedAt());
            startActivity(intent);
        });
        return itemView;
    }

    private View createRepairCard(RepairRequestModel request, SimpleDateFormat sdf) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_admin_repair, null);
        TextView tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
        TextView tvIssueType = itemView.findViewById(R.id.tvIssueType);
        TextView tvPriority = itemView.findViewById(R.id.tvPriority);
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        LinearLayout btnView = itemView.findViewById(R.id.btnView);

        tvRoomNumber.setText(request.getRoomId() != null ? request.getRoomId() : "N/A");
        String issueType = request.getIssueType();
        if (issueType == null || issueType.isEmpty()) {
            issueType = request.getItemName();
        }
        tvIssueType.setText(issueType != null ? issueType : "N/A");
        String priority = request.getPriority();
        if (priority == null || priority.isEmpty()) {
            priority = request.getUrgency();
        }
        tvPriority.setText(priority != null ? priority : "Medium");
        setPriorityColor(tvPriority, priority);
        tvDate.setText(request.getCreatedAt() > 0 ? sdf.format(new Date(request.getCreatedAt())) : "N/A");
        String status = request.getStatus() != null ? request.getStatus() : "Pending";
        tvStatus.setText(status);
        setRepairStatusColor(tvStatus, status);

        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(ManagementActivity.this, AdminRepairDetailActivity.class);
            intent.putExtra("REPAIR_ID", request.getDocumentId());
            intent.putExtra("ROOM_ID", request.getRoomId());
            intent.putExtra("ROOM_TYPE", request.getRoomType());
            intent.putExtra("ISSUE_TYPE", request.getIssueType());
            intent.putExtra("ITEM_NAME", request.getItemName());
            intent.putExtra("PRIORITY", request.getPriority());
            intent.putExtra("URGENCY", request.getUrgency());
            intent.putExtra("DESCRIPTION", request.getDescription());
            intent.putExtra("STATUS", request.getStatus());
            intent.putExtra("STAFF_NAME", request.getStaffName());
            intent.putExtra("STUDENT_NAME", request.getStudentName());
            intent.putExtra("STUDENT_ID", request.getStudentId());
            intent.putExtra("STUDENT_EMAIL", request.getStudentEmail());
            intent.putExtra("PROOF_IMAGE", request.getProofImage());
            intent.putExtra("AVAILABLE_TIME", request.getAvailableTime());
            intent.putExtra("CONTACT_PERSON", request.getContactPerson());
            intent.putExtra("COMPLETION_PHOTO", request.getCompletionPhoto());
            intent.putExtra("CREATED_AT", request.getCreatedAt());
            intent.putExtra("STARTED_AT", request.getStartedAt());
            intent.putExtra("COMPLETED_AT", request.getCompletedAt());
            startActivity(intent);
        });
        return itemView;
    }

    private void setBookingStatusColor(TextView tvStatus, String status) {
        if ("Pending".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.pending_bg)));
            tvStatus.setTextColor(getColor(R.color.pending_text));
        } else if ("Approved".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.approved_bg)));
            tvStatus.setTextColor(getColor(R.color.approved_text));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.rejected_bg)));
            tvStatus.setTextColor(getColor(R.color.rejected_text));
        } else if ("Paid".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.paid_bg)));
            tvStatus.setTextColor(getColor(R.color.paid_text));
        }
    }

    private void setRepairStatusColor(TextView tvStatus, String status) {
        if ("Pending".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.pending_bg)));
            tvStatus.setTextColor(getColor(R.color.pending_text));
        } else if ("In Progress".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.approved_bg)));
            tvStatus.setTextColor(getColor(R.color.approved_text));
        } else if ("Completed".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.completed_bg)));
            tvStatus.setTextColor(getColor(R.color.completed_text));
        }
    }

    private void setPriorityColor(TextView tvPriority, String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            tvPriority.setTextColor(getColor(android.R.color.holo_red_dark));
        } else if ("Medium".equalsIgnoreCase(priority)) {
            tvPriority.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else if ("Low".equalsIgnoreCase(priority)) {
            tvPriority.setTextColor(getColor(android.R.color.holo_green_dark));
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;
        bottomNavigation.setSelectedItemId(R.id.nav_management);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_management) {
                return true;
            } else if (id == R.id.nav_home) {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_users) {
                Intent intent = new Intent(this, UserManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_rooms) {
                Intent intent = new Intent(this, RoomManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
        loadRepairs();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_management);
        }
    }
}