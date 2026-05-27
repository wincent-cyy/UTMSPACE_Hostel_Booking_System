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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TechnicianDashboardActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvTechnicianName;
    private ShapeableImageView ivProfilePicture;
    private EditText etSearchRepair;
    private TextView tvPendingCount, tvScheduledCount, tvCompletedCount;
    private TextView tvActiveRoom, tvActiveDetails;
    // 在类顶部添加
    private String activeJobRoomId;
    private String activeJobItemName;
    private String activeJobUrgency;
    private String activeJobDescription;
    private MaterialButton btnViewRequests, btnUpdateActiveJob;
    private BottomNavigationView bottomNavigation;
    private android.view.View activeJobCard;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private String activeJobId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupProfileClick();
        loadTechnicianData();
        loadDashboardStats();
        loadActiveJob();
        setupSearchFilter();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        tvTechnicianName = findViewById(R.id.tvTechnicianName);
        ivProfilePicture = findViewById(R.id.btnAdminProfile);
        etSearchRepair = findViewById(R.id.etSearchRepair);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvScheduledCount = findViewById(R.id.tvScheduledCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvActiveRoom = findViewById(R.id.tvActiveRoom);
        tvActiveDetails = findViewById(R.id.tvActiveDetails);
        btnViewRequests = findViewById(R.id.btnViewRequests);
        btnUpdateActiveJob = findViewById(R.id.btnUpdateActiveJob);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        activeJobCard = findViewById(R.id.activeJobCard);
    }

    private void setupProfileClick() {
        ivProfilePicture.setOnClickListener(v -> {
            if (currentUser == null) return;
            db.collection("Users").document(currentUser.getUid()).get()
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

    private void loadTechnicianData() {
        if (currentUser == null) {
            tvTechnicianName.setText("Technician");
            return;
        }

        db.collection("Users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvTechnicianName.setText(name);
                        } else {
                            tvTechnicianName.setText("Technician");
                        }

                        String profilePictureBase64 = documentSnapshot.getString("profilePictureBase64");
                        if (profilePictureBase64 != null && !profilePictureBase64.isEmpty()) {
                            loadProfileImageFromBase64(profilePictureBase64);
                        }
                    } else {
                        tvTechnicianName.setText("Technician");
                    }
                })
                .addOnFailureListener(e -> {
                    tvTechnicianName.setText("Technician");
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImageFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                ivProfilePicture.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDashboardStats() {
        // Count Pending repairs
        db.collection("RepairRequests")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(query -> tvPendingCount.setText(query.size() + " Rooms"));

        // Count Scheduled/In Progress repairs
        db.collection("RepairRequests")
                .whereEqualTo("status", "In Progress")
                .get()
                .addOnSuccessListener(query -> tvScheduledCount.setText(String.valueOf(query.size())));

        // Count Completed repairs
        db.collection("RepairRequests")
                .whereEqualTo("status", "Completed")
                .get()
                .addOnSuccessListener(query -> tvCompletedCount.setText(String.valueOf(query.size())));
    }

    private void loadActiveJob() {
        db.collection("RepairRequests")
                .whereEqualTo("status", "In Progress")
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        for (QueryDocumentSnapshot doc : query) {
                            activeJobId = doc.getId();
                            String roomId = doc.getString("roomId");
                            String itemName = doc.getString("itemName");
                            String urgency = doc.getString("urgency");
                            String description = doc.getString("description");

                            tvActiveRoom.setText("Room " + roomId);

                            // 设置详细信息
                            if (tvActiveDetails != null) {
                                tvActiveDetails.setText(itemName + " • " + urgency);
                            }

                            // 存储完整信息用于跳转
                            activeJobRoomId = roomId;
                            activeJobItemName = itemName;
                            activeJobUrgency = urgency;
                            activeJobDescription = description;

                            activeJobCard.setVisibility(View.VISIBLE);
                            break;
                        }
                    } else {
                        activeJobCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    activeJobCard.setVisibility(View.GONE);
                });
    }

    private void setupSearchFilter() {
        etSearchRepair.addTextChangedListener(new TextWatcher() {
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

                        // 检查是否是有效的房间号格式
                        boolean isValidRoomFormat = cleanQuery.matches("^[A-Za-z]-?\\d+$") ||
                                cleanQuery.matches("^[A-Za-z]\\d+$");

                        // 如果不是房间号，检查是否是 itemName（非数字开头）
                        boolean isItemName = !isValidRoomFormat && !cleanQuery.matches("^\\d+$");

                        if (isValidRoomFormat || isItemName) {
                            Intent intent = new Intent(TechnicianDashboardActivity.this, TechnicianRepairRequestActivity.class);
                            intent.putExtra("SEARCH_ROOM", cleanQuery);
                            startActivity(intent);
                        } else {
                            Toast.makeText(TechnicianDashboardActivity.this,
                                    "Please enter a valid Room Number (e.g., A-101, A101) or item name",
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

    private void setupClickListeners() {
        btnViewRequests.setOnClickListener(v -> {
            Intent intent = new Intent(TechnicianDashboardActivity.this, TechnicianRepairRequestActivity.class);
            startActivity(intent);
        });

        btnUpdateActiveJob.setOnClickListener(v -> {
            if (activeJobId != null) {
                // 先从 Firestore 获取最新数据
                db.collection("RepairRequests").document(activeJobId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                Intent intent = new Intent(TechnicianDashboardActivity.this, TechnicianRepairDetailActivity.class);
                                intent.putExtra("REQUEST_ID", activeJobId);
                                intent.putExtra("ROOM_ID", doc.getString("roomId"));
                                intent.putExtra("ITEM_NAME", doc.getString("itemName"));
                                intent.putExtra("URGENCY", doc.getString("urgency"));
                                intent.putExtra("DESCRIPTION", doc.getString("description"));
                                intent.putExtra("STATUS", doc.getString("status"));
                                intent.putExtra("STAFF_NAME", doc.getString("staffName"));
                                intent.putExtra("CREATED_AT", doc.getLong("createdAt"));
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to load job details", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "No active job found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_tech_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_tech_home) {
                return true;
            } else if (id == R.id.nav_request) {
                startActivity(new Intent(this, TechnicianRepairRequestActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_tech_history) {
                startActivity(new Intent(this, TechnicianHistoryActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
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
        loadActiveJob();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_tech_home);
        }
    }
}