package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    // UI Elements
    private EditText etSearchUser;
    private RecyclerView rvUserList;
    private LinearLayout emptyState;
    private TextView tvUserCount;

    private TextView chipAll, chipStudent, chipStaff, chipTechnician;
    private BottomNavigationView bottomNavigation;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private UserAdapter adapter;
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

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupFilterChips();
        setupSearchFilter();
        loadUsers();
        setupBottomNavigation();
    }

    private void initViews() {
        etSearchUser = findViewById(R.id.etSearchUser);
        rvUserList = findViewById(R.id.rvUserList);
        emptyState = findViewById(R.id.emptyState);
        tvUserCount = findViewById(R.id.tvUserCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        chipAll = findViewById(R.id.chipAll);
        chipStudent = findViewById(R.id.chipStudent);
        chipStaff = findViewById(R.id.chipStaff);
        chipTechnician = findViewById(R.id.chipTechnician);
    }

    private void setupRecyclerView() {
        allUsersList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvUserList.setLayoutManager(new LinearLayoutManager(this));

        // ✅ 直接使用 OnUserActionListener（因为在同一个包下，接口是公开的）
        adapter = new UserAdapter(filteredList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
                showEditDialog(user);
            }

            @Override
            public void onDelete(User user) {
                showDeleteConfirmDialog(user);
            }
        });
        rvUserList.setAdapter(adapter);
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
    }

    private void updateChipStyles(TextView selectedChip) {
        chipAll.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipStudent.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipStudent.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipStaff.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipStaff.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        chipTechnician.setBackgroundResource(R.drawable.filter_chip_unselected);
        chipTechnician.setTextColor(android.graphics.Color.parseColor("#0369A1"));

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void setupSearchFilter() {
        etSearchUser.addTextChangedListener(new TextWatcher() {
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
                    applyFilters();
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        db.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("UserManagement", "Successfully loaded " + queryDocumentSnapshots.size() + " users");
                    allUsersList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUid(document.getId());
                        allUsersList.add(user);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserManagement", "Error loading users: ", e);
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
                String cleanQuery = currentSearchQuery.toLowerCase().trim();
                String name = user.getName() != null ? user.getName().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                matchesSearch = name.contains(cleanQuery) || email.contains(cleanQuery);
            }

            if (matchesRole && matchesSearch) {
                filteredList.add(user);
            }
        }

        if (filteredList.isEmpty()) {
            rvUserList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvUserList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        tvUserCount.setText(filteredList.size() + " users");
        adapter.notifyDataSetChanged();
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etPhone = dialogView.findViewById(R.id.etPhone);
        android.widget.Spinner spnRole = dialogView.findViewById(R.id.spnRole);

        etName.setText(user.getName());
        etPhone.setText(user.getPhone());

        String[] roles = {"student", "staff", "technician"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnRole.setAdapter(roleAdapter);

        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(user.getRole())) {
                spnRole.setSelection(i);
                break;
            }
        }

        builder.setTitle("Edit User")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim().toUpperCase();
                    String newPhone = etPhone.getText().toString().trim();
                    String newRole = roles[spnRole.getSelectedItemPosition()];

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPhone.length() < 10 || newPhone.length() > 11) {
                        Toast.makeText(this, "Phone number must be 10-11 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateUser(user.getUid(), newName, newPhone, newRole);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUser(String uid, String name, String phone, String role) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("role", role);

        android.util.Log.d("UserManagement", "Updating user: " + uid);
        android.util.Log.d("UserManagement", "Updates: " + updates.toString());

        db.collection("Users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("UserManagement", "Update successful");
                    Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserManagement", "Update failed: ", e);
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showDeleteConfirmDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(User user) {
        String userEmail = user.getEmail();

        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("To completely delete " + user.getName() + " (" + userEmail + "):\n\n" +
                        "1. Go to Firebase Console\n" +
                        "2. Authentication → Users\n" +
                        "3. Find and delete this user\n\n" +
                        "Remove from Firestore now?")
                .setPositiveButton("Remove from Firestore", (dialog, which) -> {
                    db.collection("Users").document(user.getUid())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "User removed from Firestore.\nPlease also delete from Firebase Console.", Toast.LENGTH_LONG).show();
                                loadUsers();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_users);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_users) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_users);
        }
    }
}