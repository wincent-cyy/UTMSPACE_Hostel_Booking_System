package com.example.utmspace_hostelbookingsystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ApplyActivity extends AppCompatActivity {

    // View Element Declarations
    private ImageButton btnBack;
    private Button btnDetailsBack, btnSubmitApplication;
    private TextView tvSummaryRoomId, tvSummaryRoomType, tvBottomPrice, tvCheckInDate;
    private EditText etFullName, etStudentId, etPhoneNumber;
    private Spinner spnDuration;

    // Incoming intent variable captures
    private String selectedRoomId;
    private String selectedRoomType;
    private String selectedRoomPrice;

    // Firebase Integration Components
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Map layout elements to programmatic binders
        initViews();

        // 2. Enforce strict input constraints on entry controls
        applyInputRestrictions();

        // 3. Safely capture data sent down from RoomDetailsActivity
        getIntentData();

        // 4. Initialize custom style spinner options dropdown arrays
        setupSpinnerOptions();

        // 5. Configure operational click callbacks
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnDetailsBack = findViewById(R.id.btnDetailsBack);
        btnSubmitApplication = findViewById(R.id.btnSubmitApplication);

        tvSummaryRoomId = findViewById(R.id.tvSummaryRoomId);
        tvSummaryRoomType = findViewById(R.id.tvSummaryRoomType);
        tvBottomPrice = findViewById(R.id.tvBottomPrice);
        tvCheckInDate = findViewById(R.id.tvCheckInDate);

        etFullName = findViewById(R.id.etFullName);
        etStudentId = findViewById(R.id.etStudentId);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spnDuration = findViewById(R.id.spnDuration);
    }

    private void applyInputRestrictions() {
        // Enforce etFullName to only accept standard alphabet characters and spaces
        etFullName.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isLetter(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                    return ""; // Reject character modification stream
                }
            }
            return null; // Accept character input modification
        }});

        // Constrain Matric Number fields structurally to a maximum character count of 9 length bounds
        etStudentId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});

        // Constrain Phone Number fields structurally to a maximum character length boundary of 11
        etPhoneNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            selectedRoomId = intent.getStringExtra("SELECTED_ROOM_ID");
            selectedRoomType = intent.getStringExtra("SELECTED_ROOM_TYPE");
            selectedRoomPrice = intent.getStringExtra("SELECTED_ROOM_PRICE");

            // Direct data string binding injection
            if (selectedRoomId != null) tvSummaryRoomId.setText(selectedRoomId);
            if (selectedRoomType != null) tvSummaryRoomType.setText(selectedRoomType);
            if (selectedRoomPrice != null) tvBottomPrice.setText(selectedRoomPrice);
        }
    }

    private void setupSpinnerOptions() {
        String[] leaseDurations = {"6 Months (1 Semester)", "12 Months (Full Academic Year)", "Short Term (1 Month)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, leaseDurations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDuration.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Navigational logic bindings matching previous layout behavior
        btnBack.setOnClickListener(v -> finish());
        btnDetailsBack.setOnClickListener(v -> finish());

        // Launch calendar interface selection window dialog safely
        tvCheckInDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String dateString = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, (selectedMonth + 1), selectedYear);
                        tvCheckInDate.setText(dateString);
                    }, year, month, day);

            // Constraint past timeline historical selections from processing
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        // Form handling submittal confirmation logic execution routing
        btnSubmitApplication.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String matric = etStudentId.getText().toString().trim().toUpperCase(); // Force uppercase evaluations
            String phone = etPhoneNumber.getText().toString().trim();
            String date = tvCheckInDate.getText().toString().trim();
            String duration = spnDuration.getSelectedItem().toString();

            // 1. Structural Checklist Validation Empty Bounds
            if (name.isEmpty() || matric.isEmpty() || phone.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please complete all fields before submitting.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Verify Matric format satisfies structural condition: "A24DW0000"
            // Pattern components check: 1 uppercase letter, 2 digits, 2 uppercase letters, 4 digits
            String matricPattern = "^[A-Z]\\d{2}[A-Z]{2}\\d{4}$";
            if (!matric.matches(matricPattern)) {
                etStudentId.setError("Invalid format! Use exact pattern layout (e.g., A24DW0000)");
                return;
            }

            // 3. Verify phone data string fits numerical length boundaries
            if (phone.length() < 10 || phone.length() > 11) {
                etPhoneNumber.setError("Phone number layout must be between 10 to 11 digits length.");
                return;
            }

            // Check if user is authenticated before attempting data push
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable submit button to prevent double-tap submissions
            btnSubmitApplication.setEnabled(false);

            // 4. Construct Data Model HashMap to transmit collection payload (CORRECTED TO .put)
            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("userId", currentUser.getUid());
            bookingData.put("studentName", name);
            bookingData.put("matricNumber", matric);
            bookingData.put("phoneNumber", phone);
            bookingData.put("checkInDate", date);
            bookingData.put("leaseDuration", duration);
            bookingData.put("roomId", selectedRoomId != null ? selectedRoomId : "Unknown Room");
            bookingData.put("roomType", selectedRoomType != null ? selectedRoomType : "Unknown Type");
            bookingData.put("roomPrice", selectedRoomPrice != null ? selectedRoomPrice : "Unknown Price");
            bookingData.put("status", "Pending"); // Automatically sets status to 'Pending'
            bookingData.put("timestamp", com.google.firebase.Timestamp.now());

            // 5. Submit Document transaction block direct payload assignment
            db.collection("Bookings")
                    .add(bookingData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(ApplyActivity.this, "Application submitted successfully! Status: Pending", Toast.LENGTH_LONG).show();

                        // Pass confirmation details back to pipeline stack execution frame
                        Intent intent = new Intent(ApplyActivity.this, StudentDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmitApplication.setEnabled(true); // Re-enable interaction components upon error
                        Toast.makeText(ApplyActivity.this, "Submission failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}