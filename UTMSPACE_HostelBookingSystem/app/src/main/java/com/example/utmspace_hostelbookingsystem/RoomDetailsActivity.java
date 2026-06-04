package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RoomDetailsActivity extends AppCompatActivity {

    // UI Element Declarations
    private LinearLayout ivBack;
    private ViewPager2 viewPagerImages;
    private TextView tvImageCounter;
    private LinearLayout dotsContainer;
    private TextView tvRoomTitle;
    private TextView tvRoomStatus;
    private TextView tvRoomLocation;
    private TextView tvRoomPrice;
    private TextView tvRoomDesc;
    private LinearLayout featuresContainer;
    private LinearLayout amenitiesContainer;
    private TextView tvOccupancy;
    private LinearLayout btnBookNow;
    private TextView tvBookButton;

    // Auto-scroll for ViewPager
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Data variables
    private String roomId;
    private String roomType;
    private RoomModel currentRoom;
    private String currentUserId;

    // Image resources list
    private List<Integer> imageResources = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get current user ID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // FIXED: Only change status bar color, no other window modifications
        setupStatusBar();

        initViews();
        getIntentData();

        if (roomId != null && !roomId.isEmpty()) {
            loadRoomDetailsFromFirestore();
        } else {
            Toast.makeText(this, "Room ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupClickListeners();
    }

    /**
     * FIXED: Only change status bar color without affecting layout
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Only set status bar color to white
            getWindow().setStatusBarColor(Color.WHITE);

            // Make status bar icons dark for visibility on white background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(flags);
            }
            // DO NOT call setDecorFitsSystemWindows or setNavigationBarColor
            // This prevents layout from being pushed under the status bar
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        dotsContainer = findViewById(R.id.dotsContainer);
        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        tvRoomStatus = findViewById(R.id.tvRoomStatus);
        tvRoomLocation = findViewById(R.id.tvRoomLocation);
        tvRoomPrice = findViewById(R.id.tvRoomPrice);
        tvRoomDesc = findViewById(R.id.tvRoomDesc);
        featuresContainer = findViewById(R.id.featuresContainer);
        amenitiesContainer = findViewById(R.id.amenitiesContainer);
        tvOccupancy = findViewById(R.id.tvOccupancy);
        btnBookNow = findViewById(R.id.btnBookNow);
        tvBookButton = findViewById(R.id.tvBookButton);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            roomId = intent.getStringExtra("room_id");
            roomType = intent.getStringExtra("room_type");
        }
    }

    private void loadRoomDetailsFromFirestore() {
        // 改为通过 roomId 字段查询
        db.collection("Rooms")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        currentRoom = documentSnapshot.toObject(RoomModel.class);
                        if (currentRoom != null) {
                            currentRoom.setDocumentId(documentSnapshot.getId());

                            if (currentRoom.getRoomType() != null) {
                                roomType = currentRoom.getRoomType();
                            }

                            populateRoomDetails();
                            setupImageCarousel();
                            setupFacilitiesAndAmenities();
                        } else {
                            Toast.makeText(this, "Failed to load room data", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e("RoomDetails", "Room not found for roomId: " + roomId);
                        Toast.makeText(this, "Room not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RoomDetails", "Error loading room: " + e.getMessage());
                    Toast.makeText(this, "Error loading room details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupImageCarousel() {
        imageResources.clear();

        if (roomType != null) {
            String lowerType = roomType.toLowerCase().trim();

            if (lowerType.contains("single")) {
                imageResources.add(R.drawable.single_room);
                if (resourceExists(R.drawable.single1)) imageResources.add(R.drawable.single1);
                if (resourceExists(R.drawable.single2)) imageResources.add(R.drawable.single2);
                if (resourceExists(R.drawable.single3)) imageResources.add(R.drawable.single3);
            } else if (lowerType.contains("double")) {
                imageResources.add(R.drawable.double_room);
                if (resourceExists(R.drawable.double1)) imageResources.add(R.drawable.double1);
                if (resourceExists(R.drawable.double2)) imageResources.add(R.drawable.double2);
                if (resourceExists(R.drawable.double3)) imageResources.add(R.drawable.double3);
            } else if (lowerType.contains("quad")) {
                imageResources.add(R.drawable.quad_room);
                if (resourceExists(R.drawable.quad1)) imageResources.add(R.drawable.quad1);
                if (resourceExists(R.drawable.quad2)) imageResources.add(R.drawable.quad2);
                if (resourceExists(R.drawable.quad3)) imageResources.add(R.drawable.quad3);
            } else {
                imageResources.add(R.drawable.single_room);
            }
        } else {
            imageResources.add(R.drawable.single_room);
        }

        viewPagerImages.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ShapeableImageView imageView = new ShapeableImageView(parent.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setScaleType(ShapeableImageView.ScaleType.CENTER_CROP);
                return new RecyclerView.ViewHolder(imageView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ShapeableImageView iv = (ShapeableImageView) holder.itemView;
                iv.setImageResource(imageResources.get(position));
            }

            @Override
            public int getItemCount() {
                return imageResources.size();
            }
        });

        setupDotIndicators();
        updateImageCounter(0);

        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDotIndicators(position);
                updateImageCounter(position);
            }
        });

        if (imageResources.size() > 1) {
            startAutoScroll();
        }
    }

    private void setupDotIndicators() {
        dotsContainer.removeAllViews();
        int imageCount = imageResources.size();

        for (int i = 0; i < imageCount; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);

            if (i == 0) {
                dot.setBackgroundResource(R.drawable.dot_indicator_selected);
            } else {
                dot.setBackgroundResource(R.drawable.dot_indicator_default);
            }
            dotsContainer.addView(dot);
        }
    }

    private void updateDotIndicators(int position) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            if (i == position) {
                dot.setBackgroundResource(R.drawable.dot_indicator_selected);
            } else {
                dot.setBackgroundResource(R.drawable.dot_indicator_default);
            }
        }
    }

    private void updateImageCounter(int position) {
        if (tvImageCounter != null) {
            tvImageCounter.setText((position + 1) + " / " + imageResources.size());
        }
    }

    private void startAutoScroll() {
        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                int currentItem = viewPagerImages.getCurrentItem();
                int totalItems = imageResources.size();
                if (currentItem < totalItems - 1) {
                    viewPagerImages.setCurrentItem(currentItem + 1, true);
                } else {
                    viewPagerImages.setCurrentItem(0, true);
                }
                sliderHandler.postDelayed(this, 4000);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 4000);
    }

    private void stopAutoScroll() {
        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
    }

    private boolean resourceExists(int resId) {
        return resId != 0;
    }

    private void populateRoomDetails() {
        if (currentRoom == null) return;

        if (roomType != null) {
            tvRoomTitle.setText(roomType);
        }

        String status = currentRoom.getStatus();
        if (status == null || status.isEmpty()) {
            status = currentRoom.isFull() ? "Full" : "Available";
        }
        tvRoomStatus.setText(status);

        if (status.equalsIgnoreCase("Full")) {
            tvRoomStatus.setTextColor(getColor(android.R.color.white));
            tvRoomStatus.setBackgroundResource(R.drawable.status_badge_full);
        } else if (status.equalsIgnoreCase("Maintenance")) {
            tvRoomStatus.setTextColor(getColor(android.R.color.white));
            tvRoomStatus.setBackgroundResource(R.drawable.status_badge_maintenance);
        } else {
            tvRoomStatus.setTextColor(getColor(android.R.color.white));
            tvRoomStatus.setBackgroundResource(R.drawable.status_badge_available);
        }

        String location = currentRoom.getLocation();
        if (location == null || location.isEmpty()) {
            location = "Block A, Level 2";
        }
        tvRoomLocation.setText(location);

        double price = currentRoom.getPrice();
        tvRoomPrice.setText("RM " + String.format("%.2f", price) + " / Person");

        tvRoomDesc.setText(getShortDescription());

        int currentOccupancy = currentRoom.getCurrentOccupancy();
        int maxCapacity = currentRoom.getMaxCapacity();

        if (maxCapacity <= 0) {
            maxCapacity = getDefaultMaxCapacity(roomType);
        }
        if (currentOccupancy < 0) {
            currentOccupancy = 0;
        }
        if (currentOccupancy > maxCapacity) {
            currentOccupancy = maxCapacity;
        }

        String occupancyText = currentOccupancy + " / " + maxCapacity;
        tvOccupancy.setText(occupancyText);

        Log.d("RoomDetails", "Room loaded: " + roomId + ", Occupancy: " + occupancyText);
    }

    private String getShortDescription() {
        if (roomType != null) {
            String lowerType = roomType.toLowerCase();

            if (lowerType.contains("single")) {
                return "Comfortable single room with study desk, bed, wardrobe, and air conditioning. Ideal for students who prefer privacy.";
            } else if (lowerType.contains("double")) {
                return "Spacious double room with 2 study desks, 2 beds, shared wardrobe, and air conditioning. Perfect for sharing.";
            } else if (lowerType.contains("quad")) {
                return "Affordable quad room with 4 study desks, 4 beds, shared wardrobes, air conditioning, and balcony. Great for group living.";
            }
        }
        return "Well-maintained room with essential amenities for comfortable student living.";
    }

    private int getDefaultMaxCapacity(String roomType) {
        if (roomType != null) {
            String lowerType = roomType.toLowerCase();
            if (lowerType.contains("single")) {
                return 1;
            } else if (lowerType.contains("double")) {
                return 2;
            } else if (lowerType.contains("quad")) {
                return 4;
            }
        }
        return 1;
    }

    private void setupFacilitiesAndAmenities() {
        featuresContainer.removeAllViews();
        amenitiesContainer.removeAllViews();

        String[] facilities = getFacilitiesByRoomType();
        String[] amenities = getAmenitiesByRoomType();

        for (String facility : facilities) {
            TextView chip = createChipView(facility, R.drawable.feature_chip_background);
            featuresContainer.addView(chip);
        }

        for (String amenity : amenities) {
            TextView chip = createChipView(amenity, R.drawable.amenity_item_background);
            amenitiesContainer.addView(chip);
        }
    }

    private String[] getFacilitiesByRoomType() {
        if (roomType != null) {
            String lowerType = roomType.toLowerCase();
            if (lowerType.contains("single")) {
                return new String[]{"Study Desk", "Single Bed", "Wardrobe", "Air Conditioning", "Wi-Fi"};
            } else if (lowerType.contains("double")) {
                return new String[]{"2 Study Desks", "2 Single Beds", "Shared Wardrobe", "Air Conditioning", "Wi-Fi"};
            } else if (lowerType.contains("quad")) {
                return new String[]{"4 Study Desks", "4 Beds", "Shared Wardrobes", "Air Conditioning", "Wi-Fi", "Balcony"};
            }
        }
        return new String[]{"Study Desk", "Bed", "Wardrobe", "Air Conditioning", "Wi-Fi"};
    }

    private String[] getAmenitiesByRoomType() {
        if (roomType != null) {
            String lowerType = roomType.toLowerCase();
            if (lowerType.contains("single")) {
                return new String[]{"24/7 Security", "Common Kitchen", "Study Room", "Laundry", "Prayer Room"};
            } else if (lowerType.contains("double")) {
                return new String[]{"24/7 Security", "Common Kitchen", "Study Room", "Laundry", "Common Lounge"};
            } else if (lowerType.contains("quad")) {
                return new String[]{"24/7 Security", "Common Kitchen", "Study Room", "Laundry", "Common Lounge"};
            }
        }
        return new String[]{"24/7 Security", "Common Kitchen", "Study Room", "Laundry"};
    }

    private TextView createChipView(String text, int backgroundRes) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(12);
        chip.setTextColor(getColor(R.color.primaryColor));
        chip.setBackgroundResource(backgroundRes);
        chip.setPadding(32, 12, 32, 12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 12);
        chip.setLayoutParams(params);

        return chip;
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> handleBackNavigation());

        btnBookNow.setOnClickListener(v -> {
            if (currentRoom != null) {
                String status = currentRoom.getStatus();
                if (status == null || status.isEmpty()) {
                    status = currentRoom.isFull() ? "Full" : "Available";
                }

                int availableBeds = currentRoom.getMaxCapacity() - currentRoom.getCurrentOccupancy();

                if (status.equalsIgnoreCase("Maintenance")) {
                    Toast.makeText(this, "This room is currently under maintenance. Please choose another room.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (status.equalsIgnoreCase("Full") || currentRoom.isFull() || availableBeds <= 0) {
                    Toast.makeText(this, "Sorry, this room is currently fully booked.", Toast.LENGTH_LONG).show();
                    return;
                }

                // 先检查用户是否登录
                if (currentUserId == null) {
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 检查用户资料是否完整
                checkUserProfileCompleteness();
            } else {
                Toast.makeText(this, "Room data not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserProfileCompleteness() {
        db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 获取 EditInfoActivity 中所有必须填写的字段
                        String studentId = documentSnapshot.getString("studentId");
                        String programme = documentSnapshot.getString("programme");
                        String semester = documentSnapshot.getString("semester");
                        String role = documentSnapshot.getString("role");

                        // 如果是 Staff，还需要检查 position 和 office
                        String position = documentSnapshot.getString("position");
                        String office = documentSnapshot.getString("office");

                        // 如果是 Technician，还需要检查 specialization 和 workshop
                        String specialization = documentSnapshot.getString("specialization");
                        String workshop = documentSnapshot.getString("workshop");

                        // 检查基本信息（这些在 EditInfoActivity 中是只读的，但确保它们存在）
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");

                        boolean hasBasicInfo = (name != null && !name.isEmpty() && !name.equals("Not provided")) &&
                                (phone != null && !phone.isEmpty() && !phone.equals("Not provided")) &&
                                (email != null && !email.isEmpty());

                        boolean hasRoleInfo = (studentId != null && !studentId.isEmpty()) &&
                                (programme != null && !programme.isEmpty()) &&
                                (semester != null && !semester.isEmpty()) &&
                                (role != null && !role.isEmpty() && !role.equals("Select your role"));

                        boolean hasRoleSpecificInfo = true;

                        if (role != null && role.equalsIgnoreCase("Staff")) {
                            hasRoleSpecificInfo = (position != null && !position.isEmpty()) &&
                                    (office != null && !office.isEmpty());
                        } else if (role != null && role.equalsIgnoreCase("Technician")) {
                            hasRoleSpecificInfo = (specialization != null && !specialization.isEmpty()) &&
                                    (workshop != null && !workshop.isEmpty());
                        }

                        boolean isProfileComplete = hasBasicInfo && hasRoleInfo && hasRoleSpecificInfo;

                        Log.d("RoomDetails", "=== Profile Completeness Check ===");
                        Log.d("RoomDetails", "Basic Info: " + hasBasicInfo + " (Name:" + name + ", Phone:" + phone + ")");
                        Log.d("RoomDetails", "Role Info: " + hasRoleInfo + " (ID:" + studentId + ", Programme:" + programme + ", Semester:" + semester + ", Role:" + role + ")");
                        Log.d("RoomDetails", "Role Specific: " + hasRoleSpecificInfo);
                        Log.d("RoomDetails", "Profile Complete: " + isProfileComplete);

                        if (isProfileComplete) {
                            processRoomApplication();
                        } else {
                            showIncompleteProfileDialog();
                        }
                    } else {
                        showIncompleteProfileDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RoomDetails", "Error checking profile: " + e.getMessage());
                    Toast.makeText(this, "Error checking profile. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showIncompleteProfileDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Incomplete Profile")
                .setMessage("Please complete your profile information before making a room booking.\n\nRequired information:\n• Student/Staff ID\n• Programme/Department\n• Semester/Year\n• Role-specific information")
                .setPositiveButton("Complete Now", (dialog, which) -> {
                    Intent intent = new Intent(RoomDetailsActivity.this, EditInfoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void handleBackNavigation() {
        stopAutoScroll();
        finish();
    }

    private void processRoomApplication() {
        Toast.makeText(this, "Proceeding with application for " + roomType, Toast.LENGTH_SHORT).show();
        Intent applyIntent = new Intent(RoomDetailsActivity.this, ApplyActivity.class);
        applyIntent.putExtra("SELECTED_ROOM_ID", roomId);
        applyIntent.putExtra("SELECTED_ROOM_TYPE", roomType);
        applyIntent.putExtra("SELECTED_ROOM_PRICE", tvRoomPrice.getText().toString());
        startActivity(applyIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoScroll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
        if (imageResources.size() > 1) {
            startAutoScroll();
        }
    }
}