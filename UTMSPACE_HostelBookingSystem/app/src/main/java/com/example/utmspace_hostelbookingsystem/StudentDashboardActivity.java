package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvStudentName;
    private ShapeableImageView ivProfilePicture;
    private ViewPager2 newsViewPager;
    private TabLayout bannerIndicator;
    private BottomNavigationView bottomNavigationView;
    private EditText etSearchRoom;
    private MaterialButton btnFilter;
    private LinearLayout searchResultsLayout;

    // Category Cards
    private CardView cardSingle, cardDouble, cardQuad;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userDataListener;
    private ListenerRegistration notificationListener;

    // News Banner
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI Views
        initViews();

        // Setup Functions
        setupNavigation();
        setupNewsBanner();
        setupCategoryClicks();
        setupRoomSearch();
        setupFilterButton();

        // Listen to live profile data updates
        startRealtimeUserListener();
        setupProfilePictureClick();
        checkAndShowNotifications();
        createNotificationChannel();
        startNotificationListener();
    }

    private void initViews() {
        tvStudentName = findViewById(R.id.tvStudentName);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        newsViewPager = findViewById(R.id.newsViewPager);
        bannerIndicator = findViewById(R.id.bannerIndicator);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Search and Filter Views
        etSearchRoom = findViewById(R.id.etSearchRoom);
        btnFilter = findViewById(R.id.btnFilter);
        searchResultsLayout = findViewById(R.id.searchResultsLayout);

        // Category Cards
        cardSingle = findViewById(R.id.cardSingle);
        cardDouble = findViewById(R.id.cardDouble);
        cardQuad = findViewById(R.id.cardQuad);
    }

    private void setupCategoryClicks() {
        cardSingle.setOnClickListener(v -> openRoomList("Single Room"));
        cardDouble.setOnClickListener(v -> openRoomList("Double Room"));
        cardQuad.setOnClickListener(v -> openRoomList("Quad Room"));
    }

    private void openRoomList(String roomType) {
        Intent intent = new Intent(StudentDashboardActivity.this, RoomListActivity.class);
        intent.putExtra("ROOM_TYPE", roomType);
        startActivity(intent);
    }

    private void setupNewsBanner() {
        List<Integer> images = new ArrayList<>();
        images.add(R.drawable.news_img1);
        images.add(R.drawable.news_img2);
        images.add(R.drawable.news_img3);

        NewsAdapter adapter = new NewsAdapter(images);
        newsViewPager.setAdapter(adapter);

        // RESTORED: Reverted ViewPager2 parent configurations completely to default matching your original specs
        newsViewPager.setPadding(0, 0, 0, 0);
        newsViewPager.setClipToPadding(true);
        newsViewPager.setClipChildren(true);

        new TabLayoutMediator(bannerIndicator, newsViewPager, (tab, position) -> {}).attach();

        bannerIndicator.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            ViewGroup tabStrip = (ViewGroup) bannerIndicator.getChildAt(0);
            if (tabStrip != null) {
                for (int i = 0; i < tabStrip.getChildCount(); i++) {
                    View tabView = tabStrip.getChildAt(i);
                    int size = (int) (8 * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
                    lp.width = size;
                    lp.height = size;
                    lp.setMargins(10, 0, 10, 0);
                    tabView.setLayoutParams(lp);
                }
            }
        });

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (newsViewPager != null && adapter.getItemCount() > 0) {
                    int nextItem = (newsViewPager.getCurrentItem() + 1) % images.size();
                    newsViewPager.setCurrentItem(nextItem, true);
                    sliderHandler.postDelayed(this, 3000);
                }
            }
        };
    }

    private void setupRoomSearch() {
        // Handle touch input directly on the search icon drawable bounding region
        etSearchRoom.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etSearchRoom.getCompoundDrawables()[0] != null || event.getX() <= (etSearchRoom.getPaddingLeft() + 50)) {
                    String query = etSearchRoom.getText().toString().trim();
                    if (!query.isEmpty()) {
                        handleRoomSearchExecution(query);
                    } else {
                        searchResultsLayout.setVisibility(View.GONE);
                    }
                    return true;
                }
            }
            return false;
        });

        // Keyboard Actions Handler
        etSearchRoom.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = etSearchRoom.getText().toString().trim();
                if (!query.isEmpty()) {
                    handleRoomSearchExecution(query);
                } else {
                    searchResultsLayout.setVisibility(View.GONE);
                }
                return true;
            }
            return false;
        });
    }

    // IMPROVED: Directs user flow to custom destinations based on category matches vs explicit listings
    private void handleRoomSearchExecution(String query) {
        String cleanQuery = query.toLowerCase().trim();

        // Only allow these 3 search terms
        if (cleanQuery.equals("single") || cleanQuery.equals("single room")) {
            openRoomList("Single Room");
        }
        else if (cleanQuery.equals("double") || cleanQuery.equals("double room")) {
            openRoomList("Double Room");
        }
        else if (cleanQuery.equals("quad") || cleanQuery.equals("quad room")) {
            openRoomList("Quad Room");
        }
        else {
            // Search other words - do nothing, just clear and show message
            searchResultsLayout.setVisibility(View.GONE);
            Toast.makeText(this, "Please search: 'single', 'double', or 'quad' only", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_filter_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        // HIDE Status and Location sections (since Dashboard doesn't need them)
        View statusSection = sheetView.findViewById(R.id.statusSection);
        View locationSection = sheetView.findViewById(R.id.locationSection);
        TextView tvStatusTitle = sheetView.findViewById(R.id.tvStatusTitle);
        TextView tvLocationTitle = sheetView.findViewById(R.id.tvLocationTitle);
        View roomTypeSection = sheetView.findViewById(R.id.roomTypeSection);

        // Hide status and location sections
        if (statusSection != null) statusSection.setVisibility(View.GONE);
        if (locationSection != null) locationSection.setVisibility(View.GONE);
        if (tvStatusTitle != null) tvStatusTitle.setVisibility(View.GONE);
        if (tvLocationTitle != null) tvLocationTitle.setVisibility(View.GONE);

        // Show room type section
        if (roomTypeSection != null) roomTypeSection.setVisibility(View.VISIBLE);

        // Change main title
        TextView filterTitle = sheetView.findViewById(R.id.tvFilterTitle);
        if (filterTitle != null) {
            filterTitle.setText("Filter Rooms");
        }

        // Map room type views
        TextView btnRoomAll = sheetView.findViewById(R.id.btnRoomAll);
        TextView btnRoomSingle = sheetView.findViewById(R.id.btnRoomSingle);
        TextView btnRoomDouble = sheetView.findViewById(R.id.btnRoomDouble);
        TextView btnRoomQuad = sheetView.findViewById(R.id.btnRoomQuad);

        // Map operational button entities
        MaterialButton btnClearFilters = sheetView.findViewById(R.id.btnClearFilters);
        MaterialButton btnApplyFilters = sheetView.findViewById(R.id.btnApplyFilters);

        // Track selected room type
        final String[] selectedRoomType = {"All Rooms"};

        // UI Update function for room type chips
        Runnable updateUISelectionStates = new Runnable() {
            @Override
            public void run() {
                // Reset all room type chips to unselected
                btnRoomAll.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnRoomAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
                btnRoomSingle.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnRoomSingle.setTextColor(android.graphics.Color.parseColor("#0369A1"));
                btnRoomDouble.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnRoomDouble.setTextColor(android.graphics.Color.parseColor("#0369A1"));
                btnRoomQuad.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnRoomQuad.setTextColor(android.graphics.Color.parseColor("#0369A1"));

                // Set selected chip style
                if (selectedRoomType[0].equals("Single Room")) {
                    btnRoomSingle.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnRoomSingle.setTextColor(android.graphics.Color.WHITE);
                } else if (selectedRoomType[0].equals("Double Room")) {
                    btnRoomDouble.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnRoomDouble.setTextColor(android.graphics.Color.WHITE);
                } else if (selectedRoomType[0].equals("Quad Room")) {
                    btnRoomQuad.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnRoomQuad.setTextColor(android.graphics.Color.WHITE);
                } else { // All Rooms
                    btnRoomAll.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnRoomAll.setTextColor(android.graphics.Color.WHITE);
                }
            }
        };

        // Execute baseline selection state rendering
        updateUISelectionStates.run();

        // Register click listeners for Room Type options
        btnRoomAll.setOnClickListener(v -> {
            selectedRoomType[0] = "All Rooms";
            updateUISelectionStates.run();
        });

        btnRoomSingle.setOnClickListener(v -> {
            selectedRoomType[0] = "Single Room";
            updateUISelectionStates.run();
        });

        btnRoomDouble.setOnClickListener(v -> {
            selectedRoomType[0] = "Double Room";
            updateUISelectionStates.run();
        });

        btnRoomQuad.setOnClickListener(v -> {
            selectedRoomType[0] = "Quad Room";
            updateUISelectionStates.run();
        });

        // Clear button - resets to "All Rooms"
        btnClearFilters.setOnClickListener(v -> {
            selectedRoomType[0] = "All Rooms";
            applyRoomFilter("All Rooms");
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Showing: All Rooms", Toast.LENGTH_SHORT).show();
        });

        // Apply button - commits the selected filter
        btnApplyFilters.setOnClickListener(v -> {
            applyRoomFilter(selectedRoomType[0]);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void resetRoomChipStyles(TextView... chips) {
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        }
    }

    private void applyRoomFilter(String filter) {
        cardSingle.setVisibility(View.VISIBLE);
        cardDouble.setVisibility(View.VISIBLE);
        cardQuad.setVisibility(View.VISIBLE);

        switch (filter) {
            case "Single Room":
                cardDouble.setVisibility(View.GONE);
                cardQuad.setVisibility(View.GONE);
                Toast.makeText(this, "Showing: Single Rooms", Toast.LENGTH_SHORT).show();
                break;
            case "Double Room":
                cardSingle.setVisibility(View.GONE);
                cardQuad.setVisibility(View.GONE);
                Toast.makeText(this, "Showing: Double Rooms", Toast.LENGTH_SHORT).show();
                break;
            case "Quad Room":
                cardSingle.setVisibility(View.GONE);
                cardDouble.setVisibility(View.GONE);
                Toast.makeText(this, "Showing: Quad Rooms", Toast.LENGTH_SHORT).show();
                break;
            case "All Rooms":
                Toast.makeText(this, "Showing: All Room Types", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void searchAvailableRooms(String query) {
        db.collection("Rooms")
                .whereEqualTo("status", "Available")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RoomModel> availableRooms = new ArrayList<>();
                    String cleanQuery = query.toLowerCase().trim();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RoomModel room = document.toObject(RoomModel.class);

                        boolean matchesRoomId = room.getRoomId() != null && room.getRoomId().toLowerCase().contains(cleanQuery);
                        boolean matchesLocation = room.getLocation() != null && room.getLocation().toLowerCase().contains(cleanQuery);
                        boolean matchesRoomType = room.getRoomType() != null && room.getRoomType().toLowerCase().contains(cleanQuery);

                        if (matchesRoomId || matchesLocation || matchesRoomType) {
                            room.setDocumentId(document.getId());
                            availableRooms.add(room);
                        }
                    }

                    if (!availableRooms.isEmpty()) {
                    } else {
                        searchResultsLayout.setVisibility(View.GONE);
                        Toast.makeText(this, "No matching rooms found for '" + query + "'", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SearchError", "Failed to search rooms: " + e.getMessage());
                    Toast.makeText(this, "Search failed. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private String getRoomDescription(String roomType) {
        if (roomType.toLowerCase().contains("single")) {
            return "Premium private personal space located near the central campus facilities. Complete with a private study desk configuration, high-speed networking access, wardrobe, and continuous window ventilation.";
        } else if (roomType.toLowerCase().contains("double")) {
            return "Spacious shared living suite layout optimal for companions or project partners. Features individual workspaces, split multi-tier shelving, and personal storage lockboxes.";
        } else {
            return "Affordable and highly social 4-sharing layout variant setup. Equipped with bunk bed systems, private desks per student, individual wardrobes, and communal balcony access points.";
        }
    }

    private void startRealtimeUserListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userDataListener = db.collection("Users").document(user.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            Log.e("DashboardDebug", "Firestore snapshot connection error", error);
                            return;
                        }

                        if (isFinishing() || isDestroyed()) return;

                        if (snapshot != null && snapshot.exists()) {
                            String name = snapshot.getString("name");
                            if (name != null) {
                                tvStudentName.setText(name);
                            }

                            String base64String = snapshot.getString("profilePictureBase64");

                            if (base64String != null && !base64String.trim().isEmpty()) {
                                try {
                                    byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                                    Bitmap decodedByteMap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                    if (decodedByteMap != null) {
                                        Glide.with(getApplicationContext())
                                                .load(decodedByteMap)
                                                .override(200, 200)
                                                .placeholder(R.drawable.profile_pic)
                                                .error(R.drawable.profile_pic)
                                                .centerCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(ivProfilePicture);
                                    } else {
                                        ivProfilePicture.setImageResource(R.drawable.profile_pic);
                                    }
                                } catch (IllegalArgumentException e) {
                                    Log.e("DashboardDebug", "Base64 decoding error", e);
                                    ivProfilePicture.setImageResource(R.drawable.profile_pic);
                                }
                            } else {
                                Glide.with(getApplicationContext()).load(R.drawable.profile_pic).centerCrop().into(ivProfilePicture);
                            }
                        }
                    });
        }
    }

    // Add after startRealtimeUserListener() method
    private void setupProfilePictureClick() {
        ivProfilePicture.setOnClickListener(v -> {
            // Get the current profile picture
            String base64String = null;
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                db.collection("Users").document(user.getUid()).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String imgBase64 = doc.getString("profilePictureBase64");
                                if (imgBase64 != null && !imgBase64.isEmpty()) {
                                    showFullScreenImage(imgBase64);
                                } else {
                                    Toast.makeText(this, "No profile picture set", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    private void showFullScreenImage(String base64String) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_full_image, null);
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

        // Make image clickable to close
        fullImageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) return true;

            Intent intent = null;
            if (id == R.id.nav_booking) {
                intent = new Intent(this, BookingsActivity.class);
            } else if (id == R.id.nav_history) {
                intent = new Intent(this, HistoryActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    // 在 StudentDashboardActivity 中添加这个方法
    private void checkAndShowNotifications() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String message = doc.getString("message");

                        // 显示弹窗通知
                        showSystemNotification(title, message);

                        // 标记为已读
                        doc.getReference().update("isRead", true);
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "utm_channel",
                    "UTM Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("UTM Booking Notifications");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void showSystemNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, StudentDashboardActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "utm_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void startNotificationListener() {
        String uid = mAuth.getCurrentUser().getUid();

        notificationListener = db.collection("Notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots == null) return;

                    for (var doc : snapshots.getDocuments()) {
                        String title = doc.getString("title");
                        String message = doc.getString("message");

                        showSystemNotification(title, message);

                        // mark as read
                        doc.getReference().update("isRead", true);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        if (sliderRunnable != null) {
            sliderHandler.postDelayed(sliderRunnable, 3000);
        }

        if (searchResultsLayout != null) {
            searchResultsLayout.setVisibility(View.GONE);
        }
        if (etSearchRoom != null) {
            etSearchRoom.setText("");
        }

        checkAndShowNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sliderHandler.removeCallbacksAndMessages(null);
        if (userDataListener != null) {
            userDataListener.remove();
        }
    }
}