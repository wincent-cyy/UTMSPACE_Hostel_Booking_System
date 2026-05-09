package com.example.utmspace_hostelbookingsystem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// Using Firestore instead of Realtime Database
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private EditText signupName, signupPhone, signupEmail, signupPassword, signupConfirmPassword;
    private Spinner roleSpinner;
    private CheckBox termsCheckbox;
    private Button btnSignup;
    private TextView tvGoToLogin, tvTermsLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Firestore instance

    private ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> termsLauncher;

    private boolean isProcessing = false;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        try {
            mAuth = FirebaseAuth.getInstance();
            // Initialize Firestore
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase Initialization Error: " + e.getMessage());
        }

        initUI();
        setupFilters();
        setupSpinner();
        setupClickListeners();
    }

    private void initUI() {
        signupName = findViewById(R.id.signupName);
        signupPhone = findViewById(R.id.signupPhone);
        signupEmail = findViewById(R.id.signupEmail);
        roleSpinner = findViewById(R.id.roleSpinner);
        signupPassword = findViewById(R.id.signupPassword);
        signupConfirmPassword = findViewById(R.id.signupConfirmPassword);
        termsCheckbox = findViewById(R.id.termsCheckbox);
        btnSignup = findViewById(R.id.btnSignup);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        tvTermsLink = findViewById(R.id.tvTermsLink);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating Account...");
        progressDialog.setCancelable(false);

        termsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        termsCheckbox.setChecked(true);
                    }
                }
        );
    }

    private void setupFilters() {
        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (Character.isLetter(character) || Character.isSpaceChar(character)) {
                    filtered.append(Character.toUpperCase(character));
                }
            }
            return filtered.toString();
        };
        signupName.setFilters(new InputFilter[]{nameFilter, new InputFilter.LengthFilter(50)});
        signupEmail.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> source.toString().toLowerCase()});
        signupPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
    }

    private void setupSpinner() {
        List<String> roles = new ArrayList<>();
        roles.add("Select your role");
        roles.add("Student");
        roles.add("Staff");
        roles.add("Technician");
        roles.add("Admin");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        roleSpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        tvTermsLink.setOnClickListener(v -> {
            try {
                termsLauncher.launch(new Intent(this, TermsActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Terms page not found", Toast.LENGTH_SHORT).show();
            }
        });
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        btnSignup.setOnClickListener(v -> registerUser());
    }

    private void setFieldError(EditText textField, String errorMsg) {
        ForegroundColorSpan fcs = new ForegroundColorSpan(Color.WHITE);
        SpannableStringBuilder ssb = new SpannableStringBuilder(errorMsg);
        ssb.setSpan(fcs, 0, errorMsg.length(), 0);

        textField.setError(ssb);
        textField.requestFocus();
    }

    private void registerUser() {
        if (isProcessing) return;

        String name = signupName.getText().toString().trim();
        String phone = signupPhone.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString();
        String pass = signupPassword.getText().toString().trim();
        String confirmPass = signupConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { setFieldError(signupName, "Full Name is required"); return; }
        if (phone.length() < 10) { setFieldError(signupPhone, "Enter a valid phone number"); return; }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setFieldError(signupEmail, "Valid email required"); return;
        }
        if (role.equals("Select your role")) {
            Toast.makeText(this, "Please select a valid role", Toast.LENGTH_SHORT).show(); return;
        }

        Pattern passPattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{7,}$");
        if (!passPattern.matcher(pass).matches()) {
            setFieldError(signupPassword, "Must be 7+ chars with letters & numbers"); return;
        }
        if (!pass.equals(confirmPass)) {
            setFieldError(signupConfirmPassword, "Passwords do not match"); return;
        }
        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to Terms and Conditions", Toast.LENGTH_SHORT).show(); return;
        }

        startSignupProcess(email, pass, name, phone, role);
    }

    private void startSignupProcess(String email, String pass, String name, String phone, String role) {
        isProcessing = true;
        if (!isFinishing()) progressDialog.show();

        timeoutRunnable = () -> {
            if (isProcessing) {
                stopTimeoutAndResetUI();
                Toast.makeText(this, "Network Timeout. Please check connection.", Toast.LENGTH_SHORT).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 25000);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    // Successfully created Auth account, now save details to Firestore
                                    saveUserToFirestore(user.getUid(), name, phone, email, role);
                                } else {
                                    stopTimeoutAndResetUI();
                                    Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        stopTimeoutAndResetUI();
                        Toast.makeText(this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String phone, String email, String role) {
        // Prepare the data map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("phone", phone);
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("uid", uid); // Keeping the UID for reference in Firestore
        userMap.put("timestamp", com.google.firebase.Timestamp.now());

        // Save to collection "Users" with Document ID as the user's UID
        db.collection("Users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    stopTimeoutAndResetUI();
                    Toast.makeText(this, "Success! Please check your email to verify.", Toast.LENGTH_LONG).show();
                    mAuth.signOut(); // Sign out until they verify email
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    stopTimeoutAndResetUI();
                    Log.e(TAG, "Firestore Error: " + e.getMessage());
                    Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void stopTimeoutAndResetUI() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        isProcessing = false;
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        super.onDestroy();
    }
}