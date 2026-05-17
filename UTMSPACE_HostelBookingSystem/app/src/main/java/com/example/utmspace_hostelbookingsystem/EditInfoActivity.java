package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditInfoActivity extends AppCompatActivity {

    private static final String TAG = "EditInfoActivity";

    // View Component Bindings
    private FrameLayout btnBack;
    private TextView tvReadOnlyStudentId, tvReadOnlyEmail, tvReadOnlyJoinDate, tvGenderValue;
    private EditText etFullName, etPhoneNumber, etEmergencyPhone;
    private RelativeLayout layoutSelectGender;
    private MaterialButton btnSaveChanges;

    // Firebase Architecture Configurations
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String targetedCollection = "Students"; // Defaults to Students

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        // 1. Initialize Firebase Components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        // 2. Map Layout View Component Hooks
        initViews();

        // 3. Setup Default Navigation & Dropdown Selection Frameworks
        setupClickListeners();

        // 4. Fetch Existing Profiles From Firebase to Prepopulate Fields
        loadUserProfileData(user);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvReadOnlyStudentId = findViewById(R.id.tvReadOnlyStudentId);
        tvReadOnlyEmail = findViewById(R.id.tvReadOnlyEmail);
        tvReadOnlyJoinDate = findViewById(R.id.tvReadOnlyJoinDate);
        tvGenderValue = findViewById(R.id.tvGenderValue);

        etFullName = findViewById(R.id.etFullName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmergencyPhone = findViewById(R.id.etEmergencyPhone);

        layoutSelectGender = findViewById(R.id.layoutSelectGender);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        layoutSelectGender.setOnClickListener(v -> showGenderSelectionDialog());
        btnSaveChanges.setOnClickListener(v -> saveProfileChangesToFirestore());
    }

    private void loadUserProfileData(FirebaseUser user) {
        tvReadOnlyStudentId.setText(currentUserId.substring(0, Math.min(currentUserId.length(), 12)).toUpperCase());
        tvReadOnlyEmail.setText(user.getEmail());

        long creationTimestamp = user.getMetadata().getCreationTimestamp();
        if (creationTimestamp != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            tvReadOnlyJoinDate.setText(sdf.format(new Date(creationTimestamp)));
        }

        // Check Students Collection First
        db.collection("Students").document(currentUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        targetedCollection = "Students";
                        populateFieldsFromDocument(task.getResult());
                    } else {
                        // Fallback to Users Collection
                        db.collection("Users").document(currentUserId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        targetedCollection = "Users";
                                        populateFieldsFromDocument(userDoc);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error fetching from Users", e));
                    }
                });
    }

    private void populateFieldsFromDocument(DocumentSnapshot doc) {
        String nameValue = doc.getString("name");
        String phoneValue = doc.getString("phone");

        if (nameValue != null && !nameValue.isEmpty()) etFullName.setText(nameValue);
        if (phoneValue != null && !phoneValue.isEmpty()) etPhoneNumber.setText(phoneValue);

        if (doc.contains("emergencyContact")) etEmergencyPhone.setText(doc.getString("emergencyContact"));
        if (doc.contains("gender")) tvGenderValue.setText(doc.getString("gender"));
        if (doc.contains("studentId")) tvReadOnlyStudentId.setText(doc.getString("studentId"));
    }

    private void showGenderSelectionDialog() {
        String[] genderOptions = {"Male", "Female"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Gender");
        builder.setItems(genderOptions, (dialog, which) -> {
            tvGenderValue.setText(genderOptions[which]);
            dialog.dismiss();
        });
        builder.show();
    }

    private void saveProfileChangesToFirestore() {
        String updatedName = etFullName.getText().toString().trim();
        String updatedPhone = etPhoneNumber.getText().toString().trim();
        String emergencyPhone = etEmergencyPhone.getText().toString().trim();
        String selectedGender = tvGenderValue.getText().toString().trim();

        // 1. Check for Empty Fields
        if (updatedName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (updatedPhone.isEmpty()) {
            etPhoneNumber.setError("Phone number is required");
            etPhoneNumber.requestFocus();
            return;
        }

        if (emergencyPhone.isEmpty()) {
            etEmergencyPhone.setError("Emergency contact number is required");
            etEmergencyPhone.requestFocus();
            return;
        }

        if (selectedGender.isEmpty() || selectedGender.equalsIgnoreCase("Select Gender") || selectedGender.equalsIgnoreCase("▼")) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Validate Full Name (Alphabets and spaces only)
        if (!updatedName.matches("^[a-zA-Z\\s]+$")) {
            etFullName.setError("Full name must only contain alphabetic letters");
            etFullName.requestFocus();
            return;
        }

        // 3. Validate Phone Numbers (Extract pure digits to check length limits accurately)
        String cleanPhone = updatedPhone.replaceAll("[^0-9]", "");
        String cleanEmergencyPhone = emergencyPhone.replaceAll("[^0-9]", "");

        if (cleanPhone.length() < 10 || cleanPhone.length() > 11) {
            etPhoneNumber.setError("Phone number must be between 10 and 11 digits");
            etPhoneNumber.requestFocus();
            return;
        }

        if (cleanEmergencyPhone.length() < 10 || cleanEmergencyPhone.length() > 11) {
            etEmergencyPhone.setError("Emergency number must be between 10 and 11 digits");
            etEmergencyPhone.requestFocus();
            return;
        }

        // Disable button temporarily to prevent duplicate background network task threads
        btnSaveChanges.setEnabled(false);
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();

        // Package data fields safely
        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("name", updatedName);
        profileUpdates.put("phone", updatedPhone);
        profileUpdates.put("emergencyContact", emergencyPhone);
        profileUpdates.put("gender", selectedGender);

        // Keep targeted data syncing strategy using SetOptions.merge()
        db.collection(targetedCollection).document(currentUserId)
                .set(profileUpdates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully updated user profile details inside collection: " + targetedCollection);
                    Toast.makeText(EditInfoActivity.this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();

                    // Terminate and navigate backward onto Profile screen context
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore Write Failed: ", e);
                    btnSaveChanges.setEnabled(true); // Re-enable button on failure to allow re-submission attempts
                    Toast.makeText(EditInfoActivity.this, "Save Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}