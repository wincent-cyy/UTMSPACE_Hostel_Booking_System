package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class AdminEditUserActivity extends AppCompatActivity {

    private LinearLayout ivBack;
    private TextView tvTitle;
    private LinearLayout btnSave;
    private LinearLayout btnToggleEdit;
    private TextView btnToggleEditText;

    // Basic Information
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;

    // Role Information
    private TextInputEditText etRole;
    private TextInputEditText etId;
    private TextInputEditText etProgramme;
    private TextInputEditText etSemester;
    private TextInputLayout tilId;
    private TextInputLayout tilProgramme;
    private TextInputLayout tilSemester;

    // Danger Zone
    private LinearLayout dangerZone;
    private LinearLayout btnDeleteUser;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseFunctions functions;
    private String userId;
    private String userRole;
    private boolean isEditMode = false;
    private String originalName;
    private String originalEmail;
    private String originalPhone;
    private String originalRole;
    private String currentAdminId;
    private boolean isAdminUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_user);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        functions = FirebaseFunctions.getInstance();

        setupStatusBar();

        initViews();
        getIntentData();
        setupClickListeners();
        checkCurrentUserRole();
        loadUserData();
        setEditMode(false);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        btnSave = findViewById(R.id.btnSave);
        btnToggleEdit = findViewById(R.id.btnToggleEdit);
        btnToggleEditText = findViewById(R.id.btnToggleEditText);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etRole = findViewById(R.id.etRole);
        etId = findViewById(R.id.etId);
        etProgramme = findViewById(R.id.etProgramme);
        etSemester = findViewById(R.id.etSemester);
        tilId = findViewById(R.id.tilId);
        tilProgramme = findViewById(R.id.tilProgramme);
        tilSemester = findViewById(R.id.tilSemester);

        dangerZone = findViewById(R.id.dangerZone);
        btnDeleteUser = findViewById(R.id.btnDeleteUser);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            userId = intent.getStringExtra("USER_ID");
            userRole = intent.getStringExtra("USER_ROLE");
            tvTitle.setText("Edit User");

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                currentAdminId = currentUser.getUid();
            }
        }
    }

    private void checkCurrentUserRole() {
        if (currentAdminId == null) {
            isAdminUser = false;
            return;
        }

        db.collection("Users").document(currentAdminId)
                .get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        String adminRole = adminDoc.getString("role");
                        isAdminUser = "Admin".equalsIgnoreCase(adminRole);
                    }
                })
                .addOnFailureListener(e -> {
                    isAdminUser = false;
                });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnToggleEdit.setOnClickListener(v -> {
            isEditMode = !isEditMode;
            setEditMode(isEditMode);
        });

        btnSave.setOnClickListener(v -> saveUserData());
        btnDeleteUser.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void setEditMode(boolean editMode) {
        if (btnToggleEditText == null) return;

        if (editMode) {
            btnToggleEditText.setText("Cancel");
            btnSave.setVisibility(View.VISIBLE);
            enableFields(true);
            updateDangerZoneVisibility();
        } else {
            btnToggleEditText.setText("Edit Mode");
            btnSave.setVisibility(View.GONE);
            enableFields(false);
            dangerZone.setVisibility(View.GONE);
        }
    }

    private void updateDangerZoneVisibility() {
        if (isEditMode && isAdminUser && currentAdminId != null && !userId.equals(currentAdminId)) {
            dangerZone.setVisibility(View.VISIBLE);
        } else {
            dangerZone.setVisibility(View.GONE);
        }
    }

    private void enableFields(boolean enable) {
        etFullName.setEnabled(enable);
        etEmail.setEnabled(enable);
        etPhone.setEnabled(enable);
        etId.setEnabled(enable);
        etProgramme.setEnabled(enable);
        etSemester.setEnabled(enable);

        if (!enable) {
            etFullName.setError(null);
            etEmail.setError(null);
            etPhone.setError(null);
            etId.setError(null);
            etProgramme.setError(null);
            etSemester.setError(null);
        }
    }

    private void loadUserData() {
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        originalName = documentSnapshot.getString("name");
                        originalEmail = documentSnapshot.getString("email");
                        originalPhone = documentSnapshot.getString("phone");
                        String role = documentSnapshot.getString("role");
                        originalRole = role;

                        etFullName.setText(originalName != null ? originalName : "N/A");
                        etEmail.setText(originalEmail != null ? originalEmail : "N/A");
                        etPhone.setText(originalPhone != null ? originalPhone : "N/A");
                        etRole.setText(role != null ? role : "Student");

                        if ("Student".equalsIgnoreCase(role)) {
                            tilId.setHint("Student ID");
                            tilProgramme.setHint("Programme");
                            tilSemester.setHint("Semester");
                            tilSemester.setVisibility(View.VISIBLE);

                            String studentId = documentSnapshot.getString("studentId");
                            String programme = documentSnapshot.getString("programme");
                            String semester = documentSnapshot.getString("semester");

                            etId.setText(studentId != null ? studentId : "");
                            etProgramme.setText(programme != null ? programme : "");
                            etSemester.setText(semester != null ? semester : "");
                        } else if ("Staff".equalsIgnoreCase(role)) {
                            tilId.setHint("Staff ID");
                            tilProgramme.setHint("Department");
                            tilSemester.setHint("Year");
                            tilSemester.setVisibility(View.VISIBLE);

                            String staffId = documentSnapshot.getString("staffId");
                            String department = documentSnapshot.getString("department");
                            String year = documentSnapshot.getString("year");

                            etId.setText(staffId != null ? staffId : "");
                            etProgramme.setText(department != null ? department : "");
                            etSemester.setText(year != null ? year : "");
                        } else if ("Technician".equalsIgnoreCase(role)) {
                            tilId.setHint("Specialization");
                            tilProgramme.setHint("Workshop");
                            tilSemester.setVisibility(View.GONE);

                            String specialization = documentSnapshot.getString("specialization");
                            String workshop = documentSnapshot.getString("workshop");

                            etId.setText(specialization != null ? specialization : "");
                            etProgramme.setText(workshop != null ? workshop : "");
                        } else if ("Admin".equalsIgnoreCase(role)) {
                            tilId.setHint("Admin ID");
                            tilProgramme.setHint("Department");
                            tilSemester.setVisibility(View.GONE);

                            String adminId = documentSnapshot.getString("adminId");
                            String department = documentSnapshot.getString("department");

                            etId.setText(adminId != null ? adminId : "");
                            etProgramme.setText(department != null ? department : "");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveUserData() {
        if (!validateFields()) {
            return;
        }

        final String newName = etFullName.getText().toString().trim();
        final String newEmail = etEmail.getText().toString().trim();
        final String newPhone = etPhone.getText().toString().trim();
        final String newRole = etRole.getText().toString().trim();

        btnSave.setEnabled(false);
        btnSave.setAlpha(0.5f);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("phone", newPhone);
        updates.put("role", newRole);

        if ("Student".equalsIgnoreCase(newRole)) {
            updates.put("studentId", etId.getText().toString().trim().toUpperCase());
            updates.put("programme", etProgramme.getText().toString().trim().toUpperCase());
            updates.put("semester", etSemester.getText().toString().trim());
        } else if ("Staff".equalsIgnoreCase(newRole)) {
            updates.put("staffId", etId.getText().toString().trim().toUpperCase());
            updates.put("department", etProgramme.getText().toString().trim().toUpperCase());
            updates.put("year", etSemester.getText().toString().trim());
        } else if ("Technician".equalsIgnoreCase(newRole)) {
            updates.put("specialization", etId.getText().toString().trim());
            updates.put("workshop", etProgramme.getText().toString().trim());
        } else if ("Admin".equalsIgnoreCase(newRole)) {
            updates.put("adminId", etId.getText().toString().trim().toUpperCase());
            updates.put("department", etProgramme.getText().toString().trim().toUpperCase());
        }

        db.collection("Users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    updateRelatedCollections(newName, newEmail, newPhone);
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setAlpha(1f);
                    Toast.makeText(this, "Failed to update user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRelatedCollections(final String newName, final String newEmail, final String newPhone) {
        // 只需要等待 Bookings 和 RepairRequests 完成，Notifications 不需要更新
        final int[] pendingUpdates = {2};
        final boolean[] hasError = {false};

        // 1. Update Bookings
        db.collection("Bookings")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // 没有找到相关 Bookings，直接标记完成
                        checkAndFinish(pendingUpdates, hasError);
                        return;
                    }

                    // 批量更新所有 Bookings
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> bookingUpdates = new HashMap<>();
                        bookingUpdates.put("name", newName);
                        bookingUpdates.put("email", newEmail);
                        bookingUpdates.put("phone", newPhone);
                        document.getReference().update(bookingUpdates);
                    }
                    checkAndFinish(pendingUpdates, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinish(pendingUpdates, hasError);
                });

        // 2. Update RepairRequests
        db.collection("RepairRequests")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // 没有找到相关 RepairRequests，直接标记完成
                        checkAndFinish(pendingUpdates, hasError);
                        return;
                    }

                    // 批量更新所有 RepairRequests
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> repairUpdates = new HashMap<>();
                        repairUpdates.put("name", newName);
                        document.getReference().update(repairUpdates);
                    }
                    checkAndFinish(pendingUpdates, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinish(pendingUpdates, hasError);
                });
    }

    private void checkAndFinish(int[] pendingUpdates, boolean[] hasError) {
        pendingUpdates[0]--;
        if (pendingUpdates[0] == 0) {
            btnSave.setEnabled(true);
            btnSave.setAlpha(1f);

            if (hasError[0]) {
                Toast.makeText(this, "User updated with some errors. Please check data consistency.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "User updated successfully!", Toast.LENGTH_SHORT).show();
            }
            isEditMode = false;
            setEditMode(false);
            loadUserData();
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        if (TextUtils.isEmpty(etFullName.getText())) {
            etFullName.setError("Full name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(etEmail.getText())) {
            etEmail.setError("Email is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(etPhone.getText())) {
            etPhone.setError("Phone number is required");
            isValid = false;
        }

        String role = etRole.getText().toString().trim();
        if ("Student".equalsIgnoreCase(role)) {
            if (TextUtils.isEmpty(etId.getText())) {
                etId.setError("Student ID is required");
                isValid = false;
            }
            if (TextUtils.isEmpty(etProgramme.getText())) {
                etProgramme.setError("Programme is required");
                isValid = false;
            }
            if (TextUtils.isEmpty(etSemester.getText())) {
                etSemester.setError("Semester is required");
                isValid = false;
            }
        } else if ("Staff".equalsIgnoreCase(role)) {
            if (TextUtils.isEmpty(etId.getText())) {
                etId.setError("Staff ID is required");
                isValid = false;
            }
            if (TextUtils.isEmpty(etProgramme.getText())) {
                etProgramme.setError("Department is required");
                isValid = false;
            }
        } else if ("Technician".equalsIgnoreCase(role)) {
            if (TextUtils.isEmpty(etId.getText())) {
                etId.setError("Specialization is required");
                isValid = false;
            }
            if (TextUtils.isEmpty(etProgramme.getText())) {
                etProgramme.setError("Workshop is required");
                isValid = false;
            }
        } else if ("Admin".equalsIgnoreCase(role)) {
            if (TextUtils.isEmpty(etId.getText())) {
                etId.setError("Admin ID is required");
                isValid = false;
            }
            if (TextUtils.isEmpty(etProgramme.getText())) {
                etProgramme.setError("Department is required");
                isValid = false;
            }
        }

        return isValid;
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete User Account")
                .setMessage("Are you sure you want to delete this user account? This action cannot be undone and all associated data will be permanently removed.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser() {
        btnDeleteUser.setEnabled(false);
        btnDeleteUser.setAlpha(0.5f);
        deleteUserRelatedData();
    }

    private void deleteUserRelatedData() {
        final int[] pendingDeletions = {5};  // 5 个操作
        final boolean[] hasError = {false};

        // 1. Delete Bookings
        db.collection("Bookings")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });

        // 2. Delete RepairRequests
        db.collection("RepairRequests")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });

        // 3. Delete Notifications
        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });

        // 4. Delete User from Firestore (这个不能删！)
        db.collection("Users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });

        // 5. Delete User from Authentication (调用云函数)
        deleteUserFromAuth(pendingDeletions, hasError);
    }

    private void deleteUserFromAuth(int[] pendingDeletions, boolean[] hasError) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", userId);

        functions.getHttpsCallable("deleteUserByAdmin")
                .call(data)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Authentication account deleted", Toast.LENGTH_SHORT).show();
                    checkAndFinishDeletion(pendingDeletions, hasError);
                })
                .addOnFailureListener(e -> {
                    hasError[0] = true;
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    checkAndFinishDeletion(pendingDeletions, hasError);
                });
    }

    private void checkAndFinishDeletion(int[] pendingDeletions, boolean[] hasError) {
        pendingDeletions[0]--;
        if (pendingDeletions[0] == 0) {
            if (hasError[0]) {
                Toast.makeText(this, "User data deleted. Please check Firebase Console.", Toast.LENGTH_LONG).show();
            } else {
                // 可选：显示一个短暂的提示或不显示
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
            }
            // 直接返回上一页
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupStatusBar();
    }
}