package com.example.utmspace_hostelbookingsystem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;

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

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        tvStaffName = findViewById(R.id.tvStaffName);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);

        // Load staff name and profile picture
        loadStaffData();

        // Setup navigation
        setupNavigation();
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

        // Keep home selected in bottom navigation
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_staff_home);
        }
    }
}