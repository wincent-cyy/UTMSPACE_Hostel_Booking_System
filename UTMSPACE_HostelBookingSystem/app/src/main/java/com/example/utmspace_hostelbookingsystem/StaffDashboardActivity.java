package com.example.utmspace_hostelbookingsystem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class StaffDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private TextView tvStaffName;
    private ShapeableImageView ivProfilePicture;
    private TextView tvTotalBookings, tvRoomIssues, tvOccupiedRooms, tvVacantRooms;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        tvStaffName = findViewById(R.id.tvStaffName);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvRoomIssues = findViewById(R.id.tvRoomIssues);
        tvOccupiedRooms = findViewById(R.id.tvOccupiedRooms);
        tvVacantRooms = findViewById(R.id.tvVacantRooms);

        // 在 initViews 或 setupClickListeners 中添加
        View btnRepairList = findViewById(R.id.btnRepairList);
        btnRepairList.setOnClickListener(v -> {
            Intent intent = new Intent(StaffDashboardActivity.this, RepairRequestActivity.class);
            startActivity(intent);
        });

        // Load staff name and profile picture
        loadStaffData();

        // Setup navigation
        setupNavigation();
        setupProfilePictureClick();
        setupRoomSearch();
        loadDashboardStats();
    }

    private void setupProfilePictureClick() {
        ivProfilePicture.setOnClickListener(v -> {
            String userId = currentUser.getUid();
            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        String imgBase64 = doc.getString("profilePictureBase64");
                        if (imgBase64 != null && !imgBase64.isEmpty()) {
                            showFullScreenImage(imgBase64);
                        } else {
                            Toast.makeText(this, "No profile picture set", Toast.LENGTH_SHORT).show();
                        }
                    });
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
        fullImageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void setupRoomSearch() {
        EditText etSearchRoom = findViewById(R.id.etSearchRoom);

        etSearchRoom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString();
                searchRunnable = () -> {
                    if (!query.isEmpty()) {
                        String cleanQuery = query.toLowerCase().trim();
                        boolean isValidRoomFormat = cleanQuery.matches("^[A-Za-z]-?\\d+$") ||
                                cleanQuery.matches("^[A-Za-z]\\d+$");

                        if (isValidRoomFormat) {
                            Intent intent = new Intent(StaffDashboardActivity.this, StaffRoomListActivity.class);
                            intent.putExtra("SEARCH_ROOM", cleanQuery);
                            startActivity(intent);
                        } else {
                            Toast.makeText(StaffDashboardActivity.this,
                                    "Please enter a valid Room Number (e.g., A-101, A101)",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDashboardStats() {
        // 获取总预订数
        db.collection("Bookings").get()
                .addOnSuccessListener(query -> tvTotalBookings.setText(String.valueOf(query.size())));

        // 获取维修中的房间数（condition 为 "Under Maintenance" 或 "Needs Repair"）
        // 改为查询 status 为 "Maintenance"
        db.collection("Rooms")
                .whereEqualTo("status", "Maintenance")
                .get()
                .addOnSuccessListener(query -> tvRoomIssues.setText(String.valueOf(query.size())));

        // 获取满房和空房数量
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
                    tvOccupiedRooms.setText(full + " Rooms");
                    tvVacantRooms.setText(available + " Rooms");
                })
                .addOnFailureListener(e -> {
                    tvOccupiedRooms.setText("0 Rooms");
                    tvVacantRooms.setText("0 Rooms");
                });
    }

    private void loadStaffData() {
        if (currentUser == null) {
            tvStaffName.setText("Staff Member");
            return;
        }

        String userId = currentUser.getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get staff name from different possible field names
                        String name = documentSnapshot.getString("name");
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");

                        // Set staff name
                        if (name != null && !name.isEmpty()) {
                            tvStaffName.setText(name);
                        } else if (firstName != null && lastName != null) {
                            tvStaffName.setText(firstName + " " + lastName);
                        } else if (firstName != null) {
                            tvStaffName.setText(firstName);
                        } else {
                            tvStaffName.setText("Staff Member");
                        }

                        // Load profile picture from Base64 (matches your ProfileActivity)
                        String profilePictureBase64 = documentSnapshot.getString("profilePictureBase64");
                        if (profilePictureBase64 != null && !profilePictureBase64.isEmpty()) {
                            loadProfileImageFromBase64(profilePictureBase64);
                        } else {
                            // Try old field name as fallback
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                // Note: This would require Glide, but we're avoiding it
                                // Just set default for now
                                ivProfilePicture.setImageResource(R.drawable.profile_pic);
                            } else {
                                ivProfilePicture.setImageResource(R.drawable.profile_pic);
                            }
                        }
                    } else {
                        tvStaffName.setText("Staff Member");
                        ivProfilePicture.setImageResource(R.drawable.profile_pic);
                    }
                })
                .addOnFailureListener(e -> {
                    tvStaffName.setText("Staff Member");
                    ivProfilePicture.setImageResource(R.drawable.profile_pic);
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImageFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                ivProfilePicture.setImageBitmap(bitmap);
            } else {
                ivProfilePicture.setImageResource(R.drawable.profile_pic);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ivProfilePicture.setImageResource(R.drawable.profile_pic);
        }
    }

    private void setupNavigation() {
        // Set home as default selected
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
        // Refresh staff data when returning to dashboard (in case profile was updated)
        loadStaffData();
        loadDashboardStats();

        // Keep home selected in bottom navigation
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_staff_home);
        }
    }
}