package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    // UI Elements
    private EditText etSearchUser;
    private ImageView ivClearSearch;
    private LinearLayout userListContainer;
    private LinearLayout emptyState;
    private TextView tvUserCount;

    private TextView chipAll, chipStudent, chipStaff, chipTechnician, chipAdmin;
    private BottomNavigationView bottomNavigation;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<User> allUsersList;
    private List<User> filteredList;

    private String currentRoleFilter = "All";
    private String currentSearchQuery = "";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Use ADJUST_PAN instead of ADJUST_RESIZE to prevent bottom nav from being pushed up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupFilterChips();
        setupSearchFilter();
        loadUsers();
        setupBottomNavigation();
    }

    private void initViews() {
        etSearchUser = findViewById(R.id.etSearchUser);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        userListContainer = findViewById(R.id.userListContainer);
        emptyState = findViewById(R.id.emptyState);
        tvUserCount = findViewById(R.id.tvUserCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        chipAll = findViewById(R.id.chipAll);
        chipStudent = findViewById(R.id.chipStudent);
        chipStaff = findViewById(R.id.chipStaff);
        chipTechnician = findViewById(R.id.chipTechnician);
        chipAdmin = findViewById(R.id.chipAdmin);

        allUsersList = new ArrayList<>();
        filteredList = new ArrayList<>();
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            currentRoleFilter = "All";
            updateChipStyles(chipAll);
            applyFilters();
        });

        chipStudent.setOnClickListener(v -> {
            currentRoleFilter = "student";
            updateChipStyles(chipStudent);
            applyFilters();
        });

        chipStaff.setOnClickListener(v -> {
            currentRoleFilter = "staff";
            updateChipStyles(chipStaff);
            applyFilters();
        });

        chipTechnician.setOnClickListener(v -> {
            currentRoleFilter = "technician";
            updateChipStyles(chipTechnician);
            applyFilters();
        });

        chipAdmin.setOnClickListener(v -> {
            currentRoleFilter = "admin";
            updateChipStyles(chipAdmin);
            applyFilters();
        });
    }

    private void updateChipStyles(TextView selectedChip) {
        resetChipStyle(chipAll);
        resetChipStyle(chipStudent);
        resetChipStyle(chipStaff);
        resetChipStyle(chipTechnician);
        resetChipStyle(chipAdmin);

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void resetChipStyle(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        chip.setTextColor(getColor(R.color.tabInactiveText));
    }

    private void setupSearchFilter() {
        // Set keyboard to close when done typing
        etSearchUser.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etSearchUser.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearchUser.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString();
                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }

                searchRunnable = () -> {
                    currentSearchQuery = query.toLowerCase().trim();
                    applyFilters();
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etSearchUser.setText("");
                currentSearchQuery = "";
                applyFilters();
            });
        }
    }

    private void loadUsers() {
        db.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsersList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = new User();
                        user.setUid(document.getId());
                        user.setName(document.getString("name"));
                        user.setEmail(document.getString("email"));
                        user.setPhone(document.getString("phone"));
                        user.setRole(document.getString("role"));
                        user.setStudentId(document.getString("studentId"));
                        user.setStaffId(document.getString("staffId"));
                        user.setProgramme(document.getString("programme"));
                        user.setDepartment(document.getString("department"));
                        user.setSpecialization(document.getString("specialization"));
                        user.setWorkshop(document.getString("workshop"));

                        allUsersList.add(user);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load users: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void applyFilters() {
        filteredList.clear();

        for (User user : allUsersList) {
            boolean matchesRole = true;
            if (!"All".equals(currentRoleFilter)) {
                String role = user.getRole();
                matchesRole = role != null && role.equalsIgnoreCase(currentRoleFilter);
            }

            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String name = user.getName() != null ? user.getName().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                matchesSearch = name.contains(currentSearchQuery) || email.contains(currentSearchQuery);
            }

            if (matchesRole && matchesSearch) {
                filteredList.add(user);
            }
        }

        displayUsers();
    }

    private void displayUsers() {
        userListContainer.removeAllViews();

        tvUserCount.setText(filteredList.size() + " users");

        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            userListContainer.setVisibility(View.GONE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        userListContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < filteredList.size(); i++) {
            User user = filteredList.get(i);
            View userCard = createUserCard(user);
            userListContainer.addView(userCard);

            // Add spacing between cards
            if (i < filteredList.size() - 1) {
                addDivider();
            }
        }
    }

    private void addDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                12
        );
        divider.setLayoutParams(params);
        userListContainer.addView(divider);
    }

    private View createUserCard(User user) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_user_card, null);

        TextView tvUserName = itemView.findViewById(R.id.tvUserName);
        TextView tvUserRole = itemView.findViewById(R.id.tvUserRole);
        TextView tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
        TextView tvUserPhone = itemView.findViewById(R.id.tvUserPhone);
        LinearLayout btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        LinearLayout btnEditUser = itemView.findViewById(R.id.btnEditUser);

        tvUserName.setText(user.getName() != null ? user.getName() : "N/A");
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "N/A");

        String role = user.getRole() != null ? user.getRole() : "Student";
        tvUserRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase());

        // 设置圆角背景 - 只设置颜色和圆角
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(30f);

        switch (role.toLowerCase()) {
            case "student":
                drawable.setColor(Color.parseColor("#10B981")); // 绿色
                break;
            case "staff":
                drawable.setColor(Color.parseColor("#3B82F6")); // 蓝色
                break;
            case "technician":
                drawable.setColor(Color.parseColor("#F59E0B")); // 橙色
                break;
            case "admin":
                drawable.setColor(Color.parseColor("#800000")); // 深红色
                break;
            default:
                drawable.setColor(Color.parseColor("#10B981"));
                break;
        }

        tvUserRole.setBackground(drawable);
        tvUserRole.setTextColor(Color.WHITE);
        // 使用 TextView 的 setPadding 方法
        tvUserRole.setPadding(24, 8, 24, 8);

        // View Details 和 Edit 按钮都跳转到 AdminEditUserActivity
        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(UserManagementActivity.this, AdminEditUserActivity.class);
            intent.putExtra("USER_ID", user.getUid());
            intent.putExtra("USER_ROLE", user.getRole());
            intent.putExtra("USER_NAME", user.getName());
            startActivity(intent);
        });

        btnEditUser.setOnClickListener(v -> {
            Intent intent = new Intent(UserManagementActivity.this, AdminEditUserActivity.class);
            intent.putExtra("USER_ID", user.getUid());
            intent.putExtra("USER_ROLE", user.getRole());
            intent.putExtra("USER_NAME", user.getName());
            startActivity(intent);
        });

        // 整个卡片不可点击
        itemView.setClickable(false);

        return itemView;
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_users);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_users) {
                return true;
            } else if (id == R.id.nav_home) {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
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
            } else if (id == R.id.nav_management) {
                Intent intent = new Intent(this, ManagementActivity.class);
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
        loadUsers();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_users);
        }
    }

    // User Model Class
    public static class User {
        private String uid;
        private String name;
        private String email;
        private String phone;
        private String role;
        private String studentId;
        private String staffId;
        private String programme;
        private String department;
        private String specialization;
        private String workshop;

        // Getters and Setters
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getStaffId() { return staffId; }
        public void setStaffId(String staffId) { this.staffId = staffId; }

        public String getProgramme() { return programme; }
        public void setProgramme(String programme) { this.programme = programme; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }

        public String getWorkshop() { return workshop; }
        public void setWorkshop(String workshop) { this.workshop = workshop; }
    }
}