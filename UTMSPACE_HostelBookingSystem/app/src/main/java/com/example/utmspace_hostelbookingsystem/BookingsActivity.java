package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingsActivity extends AppCompatActivity {

    // View Component Declarations
    private RecyclerView rvPendingBookings;
    private TextView emptyState;
    private TextView tvPendingCount;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;

    // List & Adapter Components
    private BookingAdapter adapter;
    private List<Booking> bookingList;

    // Firebase Setup
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentUid;
    private int pendingCount = 0;
    private int completedCount = 0;
    private Map<String, Booking> tempBookingMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        // 1. Initialize Views
        rvPendingBookings = findViewById(R.id.rvPendingBookings);
        emptyState = findViewById(R.id.emptyState);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // 2. Setup RecyclerView
        rvPendingBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();

        // 3. Setup Swipe Refresh
        setupSwipeRefresh();

        // 4. Initialize Adapter
        adapter = new BookingAdapter(bookingList, booking -> {
            Intent intent = new Intent(BookingsActivity.this, BookingDetailsActivity.class);

            intent.putExtra("BOOKING_DOC_ID", booking.getBookingId());
            intent.putExtra("BOOKING_STATUS", booking.getBookingStatus());
            intent.putExtra("REJECT_REASON", booking.getRejectReason());
            intent.putExtra("ROOM_ID", booking.getRoomId());
            intent.putExtra("ROOM_TYPE", booking.getRoomType());
            intent.putExtra("ROOM_LOCATION", booking.getLocation());
            intent.putExtra("ROOM_PRICE", booking.getDisplayPrice());
            intent.putExtra("STUDENT_NAME", booking.getName());
            intent.putExtra("MATRIC_NUMBER", booking.getMatricNumber());
            intent.putExtra("PHONE_NUMBER", booking.getPhone());
            intent.putExtra("EMAIL", booking.getEmail());
            intent.putExtra("PROGRAMME", booking.getProgramme());
            intent.putExtra("CHECK_IN_DATE", booking.getCheckInDate());
            intent.putExtra("LEASE_DURATION", booking.getLeaseDuration());
            intent.putExtra("CREATED_AT", booking.getCreatedAt());

            startActivity(intent);
        }, false);

        rvPendingBookings.setAdapter(adapter);

        // 5. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        // 6. Setup navigation
        setupBottomNavigation();

        // 7. Load user bookings
        fetchUserBookings();
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshData();
            });
        }
    }

    private void refreshData() {
        // Reset data
        bookingList.clear();
        tempBookingMap.clear();
        completedCount = 0;
        pendingCount = 0;

        // Reload data
        fetchUserBookings();

        // Stop refresh animation after data is loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1500);
    }

    private void fetchUserBookings() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        currentUid = mAuth.getCurrentUser().getUid();

        // 先获取用户信息
        db.collection("Users").document(currentUid).get()
                .addOnSuccessListener(userDoc -> {
                    String userEmail = userDoc.getString("email");
                    String userProgramme = userDoc.getString("programme");

                    if (userEmail == null) userEmail = "";
                    if (userProgramme == null) userProgramme = "";

                    final String finalEmail = userEmail;
                    final String finalProgramme = userProgramme;

                    // 获取用户的 Pending Bookings
                    db.collection("Bookings")
                            .whereEqualTo("uid", currentUid)
                            .whereEqualTo("bookingStatus", "Pending")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                bookingList.clear();
                                tempBookingMap.clear();
                                pendingCount = queryDocumentSnapshots.size();
                                completedCount = 0;

                                if (pendingCount == 0) {
                                    updateUI();
                                    return;
                                }

                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    String bookingId = document.getId();

                                    // 检查是否已存在
                                    if (tempBookingMap.containsKey(bookingId)) {
                                        continue;
                                    }

                                    Booking booking = new Booking();

                                    // Booking 基本信息
                                    booking.setBookingId(bookingId);
                                    booking.setUid(document.getString("uid"));
                                    booking.setRoomId(document.getString("roomId"));
                                    booking.setRoomType(document.getString("roomType"));
                                    booking.setName(document.getString("name"));
                                    booking.setMatricNumber(document.getString("matricNumber"));
                                    booking.setPhone(document.getString("phone"));
                                    booking.setEmail(finalEmail);
                                    booking.setProgramme(finalProgramme);
                                    booking.setCurrentSemester(document.getString("currentSemester"));
                                    booking.setCheckInDate(document.getString("checkInDate"));
                                    booking.setLeaseDuration(document.getString("leaseDuration"));
                                    booking.setBookingStatus(document.getString("bookingStatus"));
                                    booking.setRejectReason(document.getString("rejectReason"));

                                    // 价格
                                    Double price = document.getDouble("price");
                                    booking.setPrice(price != null ? price : 0);

                                    // 每学期价格
                                    Double pricePerSemester = document.getDouble("pricePerSemester");
                                    booking.setPricePerSemester(pricePerSemester != null ? pricePerSemester : 0);

                                    // 学期数
                                    Long duration = document.getLong("duration");
                                    booking.setDuration(duration != null ? duration.intValue() : 1);

                                    // 创建时间
                                    Long createdAt = document.getLong("createdAt");
                                    booking.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());

                                    // 获取房间的 Location
                                    String roomId = document.getString("roomId");
                                    if (roomId != null && !roomId.isEmpty()) {
                                        tempBookingMap.put(bookingId, booking);
                                        fetchRoomLocation(roomId, booking, bookingId);
                                    } else {
                                        booking.setLocation("Not specified");
                                        tempBookingMap.put(bookingId, booking);
                                        completedCount++;
                                        checkAndUpdateUI();
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                updateUI();
                                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show();
                    loadBookingsWithoutUserInfo();
                });
    }

    private void fetchRoomLocation(String roomId, Booking booking, String bookingId) {
        db.collection("Rooms")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot roomDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String location = roomDoc.getString("location");
                        booking.setLocation(location != null ? location : "Not specified");
                    } else {
                        booking.setLocation("Not specified");
                    }
                    completedCount++;
                    checkAndUpdateUI();
                })
                .addOnFailureListener(e -> {
                    booking.setLocation("Unknown");
                    completedCount++;
                    checkAndUpdateUI();
                });
    }

    private void loadBookingsWithoutUserInfo() {
        db.collection("Bookings")
                .whereEqualTo("uid", currentUid)
                .whereEqualTo("bookingStatus", "Pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookingList.clear();
                    tempBookingMap.clear();
                    pendingCount = queryDocumentSnapshots.size();
                    completedCount = 0;

                    if (pendingCount == 0) {
                        updateUI();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String bookingId = document.getId();

                        if (tempBookingMap.containsKey(bookingId)) {
                            continue;
                        }

                        Booking booking = new Booking();
                        booking.setBookingId(bookingId);
                        booking.setUid(document.getString("uid"));
                        booking.setRoomId(document.getString("roomId"));
                        booking.setRoomType(document.getString("roomType"));
                        booking.setName(document.getString("name"));
                        booking.setMatricNumber(document.getString("matricNumber"));
                        booking.setPhone(document.getString("phone"));
                        booking.setEmail(document.getString("email"));
                        booking.setProgramme(document.getString("programme"));
                        booking.setCurrentSemester(document.getString("currentSemester"));
                        booking.setCheckInDate(document.getString("checkInDate"));
                        booking.setLeaseDuration(document.getString("leaseDuration"));
                        booking.setBookingStatus(document.getString("bookingStatus"));
                        booking.setRejectReason(document.getString("rejectReason"));

                        Double price = document.getDouble("price");
                        booking.setPrice(price != null ? price : 0);

                        Long createdAt = document.getLong("createdAt");
                        booking.setCreatedAt(createdAt != null ? createdAt : System.currentTimeMillis());

                        String roomId = document.getString("roomId");
                        if (roomId != null && !roomId.isEmpty()) {
                            tempBookingMap.put(bookingId, booking);
                            fetchRoomLocationWithoutUser(roomId, booking, bookingId);
                        } else {
                            booking.setLocation("Not specified");
                            tempBookingMap.put(bookingId, booking);
                            completedCount++;
                            checkAndUpdateUI();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI();
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void fetchRoomLocationWithoutUser(String roomId, Booking booking, String bookingId) {
        db.collection("Rooms")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot roomDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String location = roomDoc.getString("location");
                        booking.setLocation(location != null ? location : "Not specified");
                    } else {
                        booking.setLocation("Not specified");
                    }
                    completedCount++;
                    checkAndUpdateUI();
                })
                .addOnFailureListener(e -> {
                    booking.setLocation("Unknown");
                    completedCount++;
                    checkAndUpdateUI();
                });
    }

    private void checkAndUpdateUI() {
        if (completedCount >= pendingCount) {
            bookingList.clear();
            bookingList.addAll(tempBookingMap.values());
            updateUI();
        }
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();

        int count = bookingList.size();
        tvPendingCount.setText(String.valueOf(count));
        tvPendingCount.setVisibility(count == 0 ? View.GONE : View.VISIBLE);

        if (bookingList.isEmpty()) {
            rvPendingBookings.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("No pending applications\nYour booking requests will appear here");
        } else {
            rvPendingBookings.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        // Stop refresh animation if still showing
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_booking);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_booking) {
                return true;
            } else if (id == R.id.nav_home) {
                Intent intent = new Intent(BookingsActivity.this, StudentDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_history) {
                Intent intent = new Intent(BookingsActivity.this, HistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(BookingsActivity.this, ProfileActivity.class);
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
        fetchUserBookings();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_booking);
        }
    }
}