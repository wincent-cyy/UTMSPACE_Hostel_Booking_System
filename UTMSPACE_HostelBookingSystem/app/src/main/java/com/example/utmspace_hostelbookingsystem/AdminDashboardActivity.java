package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvAdminName;
    private ShapeableImageView btnAdminProfile;
    private EditText etAdminSearch;

    // Statistics TextViews
    private TextView tvTotalUsers, tvTotalRooms, tvTotalBookings, tvRepairRooms;

    // Quick Action Cards
    private CardView cardDeleteUser, cardAddRoom, cardDeleteBookings;

    // Bottom Navigation
    private BottomNavigationView bottomNavigation;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Current Admin Name
    private String adminName = "Administrator";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupBottomNavigation();
        loadAdminName();
        loadStatistics();
        setupSearchFunction();
        setupQuickActions();
        setupProfileClick();
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tvAdminName);
        btnAdminProfile = findViewById(R.id.btnAdminProfile);
        etAdminSearch = findViewById(R.id.etAdminSearch);

        // Statistics Views
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalRooms = findViewById(R.id.tvTotalRooms);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvRepairRooms = findViewById(R.id.tvRepairRooms);

        // Quick Action Cards
        cardDeleteUser = findViewById(R.id.cardDeleteUser);
        cardAddRoom = findViewById(R.id.cardAddRoom);
        cardDeleteBookings = findViewById(R.id.cardDeleteBookings);

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String updatedName = data.getStringExtra("updatedName");
            String updatedImageBase64 = data.getStringExtra("updatedImageBase64");

            if (updatedName != null) {
                tvAdminName.setText(updatedName);
            }

            if (updatedImageBase64 != null && !updatedImageBase64.isEmpty()) {
                try {
                    byte[] imageBytes = android.util.Base64.decode(updatedImageBase64, android.util.Base64.DEFAULT);
                    Glide.with(this)
                            .load(imageBytes)
                            .circleCrop()
                            .placeholder(R.drawable.profile_pic)
                            .error(R.drawable.profile_pic)
                            .into(btnAdminProfile);
                } catch (Exception e) {
                    btnAdminProfile.setImageResource(R.drawable.profile_pic);
                }
            }
        }
    }

    private void loadAdminName() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId != null) {
            db.collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            // 改成 profilePictureBase64
                            String profilePictureBase64 = documentSnapshot.getString("profilePictureBase64");

                            if (name != null && !name.isEmpty()) {
                                adminName = name;
                                tvAdminName.setText(name);
                            } else {
                                tvAdminName.setText("Administrator");
                            }

                            // 加载图片 - 改成 profilePictureBase64
                            if (profilePictureBase64 != null && !profilePictureBase64.isEmpty()) {
                                try {
                                    byte[] imageBytes = android.util.Base64.decode(profilePictureBase64, android.util.Base64.DEFAULT);
                                    Glide.with(this)
                                            .load(imageBytes)
                                            .circleCrop()
                                            .placeholder(R.drawable.profile_pic)
                                            .error(R.drawable.profile_pic)
                                            .into(btnAdminProfile);
                                } catch (Exception e) {
                                    btnAdminProfile.setImageResource(R.drawable.profile_pic);
                                }
                            } else {
                                btnAdminProfile.setImageResource(R.drawable.profile_pic);
                            }
                        }
                    });
        }
    }

    private void loadStatistics() {
        // Load Total Users
        db.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalUsers = queryDocumentSnapshots.size();
                    tvTotalUsers.setText(formatNumber(totalUsers));
                })
                .addOnFailureListener(e -> {
                    tvTotalUsers.setText("0");
                });

        // Load Total Rooms
        db.collection("Rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalRooms = queryDocumentSnapshots.size();
                    tvTotalRooms.setText(formatNumber(totalRooms));
                })
                .addOnFailureListener(e -> {
                    tvTotalRooms.setText("0");
                });

        // Load Total Bookings
        db.collection("Bookings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalBookings = queryDocumentSnapshots.size();
                    tvTotalBookings.setText(formatNumber(totalBookings));
                })
                .addOnFailureListener(e -> {
                    tvTotalBookings.setText("0");
                });

        // Load Repair Rooms (Rooms with status "Maintenance" or repair requests pending)
        db.collection("Rooms")
                .whereEqualTo("status", "Maintenance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int repairRooms = queryDocumentSnapshots.size();
                    tvRepairRooms.setText(formatNumber(repairRooms));
                })
                .addOnFailureListener(e -> {
                    // Also check RepairRequests collection
                    db.collection("RepairRequests")
                            .whereEqualTo("status", "Pending")
                            .get()
                            .addOnSuccessListener(task -> {
                                tvRepairRooms.setText(formatNumber(task.size()));
                            })
                            .addOnFailureListener(err -> {
                                tvRepairRooms.setText("0");
                            });
                });
    }

    private void setupSearchFunction() {
        etAdminSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) return;

                // Search for room number
                if (query.matches(".*[A-Za-z].*") || query.matches(".*\\d.*")) {
                    // Check if it looks like a room number
                    db.collection("Rooms")
                            .whereEqualTo("roomId", query)
                            .get()
                            .addOnSuccessListener(task -> {
                                if (!task.isEmpty()) {
                                    // Found room, go to Room Management
                                    Intent intent = new Intent(AdminDashboardActivity.this, RoomManagementActivity.class);
                                    intent.putExtra("searchQuery", query);
                                    startActivity(intent);
                                    etAdminSearch.setText("");
                                }
                            });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search on submit
        etAdminSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etAdminSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                // Go to User Management with search query
                Intent intent = new Intent(AdminDashboardActivity.this, UserManagementActivity.class);
                intent.putExtra("searchQuery", query);
                startActivity(intent);
                etAdminSearch.setText("");
            }
            return true;
        });
    }

    private void setupQuickActions() {
        // Delete User - Go to User Management
        cardDeleteUser.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, UserManagementActivity.class);
            intent.putExtra("action", "delete");
            startActivity(intent);
        });

        // Add Room - Go to Room Management with add mode
        cardAddRoom.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, RoomManagementActivity.class);
            intent.putExtra("action", "add");
            startActivity(intent);
        });

        // Delete Bookings - Go to Management (Bookings tab)
        cardDeleteBookings.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManagementActivity.class);
            intent.putExtra("action", "delete");
            intent.putExtra("tab", "Bookings");
            startActivity(intent);
        });
    }

    private void setupProfileClick() {
        btnAdminProfile.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminProfileActivity.class);
            startActivityForResult(intent, 1);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, UserManagementActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_rooms) {
                startActivity(new Intent(this, RoomManagementActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_management) {
                startActivity(new Intent(this, ManagementActivity.class));
                finish();
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

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics();
        loadAdminName();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }
}