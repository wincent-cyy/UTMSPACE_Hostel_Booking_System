package com.example.utmspace_hostelbookingsystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ApplyActivity extends AppCompatActivity {

    // View Element Declarations
    private LinearLayout ivBack;
    private LinearLayout btnSubmitApplication;
    private TextView tvRoomName;
    private TextView tvRoomPrice;
    private TextView tvRoomInfo;

    private TextInputEditText etStudentName;
    private TextInputEditText etStudentId;
    private TextInputEditText etPhoneNumber;
    private TextInputEditText etEmail;
    private TextInputEditText etProgramme;
    private TextInputEditText etSemester;
    private TextInputEditText etCheckInDate;
    private TextInputEditText etCheckOutDate;

    private TextView chip1Semester;
    private TextView chip2Semester;

    // Incoming intent variable captures
    private String selectedRoomId;
    private String selectedRoomType;
    private String selectedRoomPrice;

    // 从 Firestore 获取的房间信息
    private double roomPriceValue;
    private String roomStatus;
    private String roomLocation;

    // Firebase Integration Components
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private int selectedDuration = 1; // 1 or 2 semesters

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupInputFilters();
        getIntentData();
        setupChipListeners();
        setupClickListeners();

        loadUserInfo();
        loadRoomInfo();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomPrice = findViewById(R.id.tvRoomPrice);
        tvRoomInfo = findViewById(R.id.tvRoomInfo);

        etStudentName = findViewById(R.id.etStudentName);
        etStudentId = findViewById(R.id.etStudentId);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etEmail);
        etProgramme = findViewById(R.id.etProgramme);
        etSemester = findViewById(R.id.etSemester);
        etCheckInDate = findViewById(R.id.etCheckInDate);
        etCheckOutDate = findViewById(R.id.etCheckOutDate);

        chip1Semester = findViewById(R.id.chip1Semester);
        chip2Semester = findViewById(R.id.chip2Semester);
        btnSubmitApplication = findViewById(R.id.btnSubmitApplication);
    }

    private void setupInputFilters() {
        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetter(c) && !Character.isSpaceChar(c)) {
                    return "";
                }
            }
            return null;
        };
        etStudentName.setFilters(new InputFilter[]{nameFilter, new InputFilter.LengthFilter(50)});

        InputFilter phoneFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        };
        etPhoneNumber.setFilters(new InputFilter[]{phoneFilter, new InputFilter.LengthFilter(11)});

        InputFilter programmeFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetter(c) && !Character.isSpaceChar(c) && c != '.') {
                    return "";
                }
            }
            return null;
        };
        etProgramme.setFilters(new InputFilter[]{programmeFilter, new InputFilter.LengthFilter(50)});

        InputFilter semesterFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        };
        etSemester.setFilters(new InputFilter[]{semesterFilter, new InputFilter.LengthFilter(2)});

        // 设置 Student ID 和 Email 为不可编辑
        etStudentId.setFocusable(false);
        etStudentId.setClickable(false);
        etStudentId.setEnabled(false);

        etEmail.setFocusable(false);
        etEmail.setClickable(false);
        etEmail.setEnabled(false);

        // 设置日期字段为不可编辑（通过点击选择器）
        etCheckInDate.setFocusable(false);
        etCheckInDate.setClickable(true);
        etCheckOutDate.setFocusable(false);
        etCheckOutDate.setClickable(true);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            selectedRoomId = intent.getStringExtra("SELECTED_ROOM_ID");
            selectedRoomType = intent.getStringExtra("SELECTED_ROOM_TYPE");
            selectedRoomPrice = intent.getStringExtra("SELECTED_ROOM_PRICE");

            if (selectedRoomType != null) tvRoomName.setText(selectedRoomType);
            updateRoomInfoText();
        }
    }

    /**
     * 根据房间类型更新房间信息显示
     */
    private void updateRoomInfoText() {
        if (selectedRoomType == null) return;

        String lowerType = selectedRoomType.toLowerCase();
        String roomInfoText = selectedRoomType + " · ";

        if (lowerType.contains("single")) {
            roomInfoText += "1 Bed · Air Conditioning · Study Desk · Wi-Fi";
        } else if (lowerType.contains("double")) {
            roomInfoText += "2 Beds · Air Conditioning · 2 Study Desks · Wi-Fi";
        } else if (lowerType.contains("quad")) {
            roomInfoText += "4 Beds · Air Conditioning · 4 Study Desks · Wi-Fi · Balcony";
        } else {
            roomInfoText += "Air Conditioning · Study Desk · Wi-Fi";
        }

        tvRoomInfo.setText(roomInfoText);
    }

    private void setupChipListeners() {
        chip1Semester.setOnClickListener(v -> {
            setSelectedChip(1);
            selectedDuration = 1;
            updateTotalPrice();
        });

        chip2Semester.setOnClickListener(v -> {
            setSelectedChip(2);
            selectedDuration = 2;
            updateTotalPrice();
        });
    }

    /**
     * 根据学期数更新总价格
     */
    private void updateTotalPrice() {
        double totalPrice = roomPriceValue * selectedDuration;
        String durationText = selectedDuration + " Semester" + (selectedDuration > 1 ? "s" : "");
        tvRoomPrice.setText("RM " + String.format("%.0f", totalPrice) + " (" + durationText + ")");
    }

    private void setSelectedChip(int semester) {
        if (semester == 1) {
            chip1Semester.setBackgroundResource(R.drawable.chip_selected);
            chip1Semester.setTextColor(getColor(android.R.color.white));
            chip2Semester.setBackgroundResource(R.drawable.chip_unselected);
            chip2Semester.setTextColor(getColor(R.color.primaryColor));
        } else {
            chip2Semester.setBackgroundResource(R.drawable.chip_selected);
            chip2Semester.setTextColor(getColor(android.R.color.white));
            chip1Semester.setBackgroundResource(R.drawable.chip_unselected);
            chip1Semester.setTextColor(getColor(R.color.primaryColor));
        }
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");
                        String programme = documentSnapshot.getString("programme");
                        String semester = documentSnapshot.getString("semester");
                        String studentId = documentSnapshot.getString("studentId");

                        if (name != null) etStudentName.setText(name);
                        if (studentId != null) etStudentId.setText(studentId);
                        if (phone != null) etPhoneNumber.setText(phone);
                        if (email != null) etEmail.setText(email);
                        if (programme != null) etProgramme.setText(programme);
                        if (semester != null) etSemester.setText(semester);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRoomInfo() {
        if (selectedRoomId == null) return;

        // 方式2：通过 roomId 字段查询，而不是 document ID
        db.collection("Rooms")
                .whereEqualTo("roomId", selectedRoomId)  // 使用 roomId 字段查询
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        roomPriceValue = documentSnapshot.getDouble("price") != null ? documentSnapshot.getDouble("price") : 0;
                        roomStatus = documentSnapshot.getString("status");
                        roomLocation = documentSnapshot.getString("location");

                        Log.d("ApplyActivity", "Room loaded - Price: " + roomPriceValue + ", Status: " + roomStatus);

                        updateTotalPrice();
                        updateRoomInfoText();
                    } else {
                        Log.e("ApplyActivity", "Room document not found for roomId: " + selectedRoomId);
                        Toast.makeText(this, "Room information not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ApplyActivity", "Failed to load room info: " + e.getMessage());
                    Toast.makeText(this, "Failed to load room info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        btnSubmitApplication.setOnClickListener(v -> validateAndSubmit());

        // Date picker for check-in and check-out
        etCheckInDate.setOnClickListener(v -> showDatePicker(etCheckInDate, true));
        etCheckOutDate.setOnClickListener(v -> showDatePicker(etCheckOutDate, false));
    }

    /**
     * 解析日期字符串为 Calendar 对象
     */
    private Calendar parseDateToCalendar(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            String[] parts = dateStr.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查退房日期是否在入住日期之后
     */
    private boolean isCheckOutDateValid(String checkInDate, String checkOutDate) {
        if (checkInDate == null || checkInDate.isEmpty() || checkOutDate == null || checkOutDate.isEmpty()) {
            return true; // 如果任一为空，跳过验证（由必填验证处理）
        }

        Calendar checkInCal = parseDateToCalendar(checkInDate);
        Calendar checkOutCal = parseDateToCalendar(checkOutDate);

        if (checkInCal == null || checkOutCal == null) {
            return true;
        }

        return !checkOutCal.before(checkInCal);
    }

    /**
     * 显示日期选择器，并限制不能选择过去的日期
     */
    private void showDatePicker(TextInputEditText dateField, boolean isCheckIn) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Calendar minDate = Calendar.getInstance();
        minDate.set(Calendar.HOUR_OF_DAY, 0);
        minDate.set(Calendar.MINUTE, 0);
        minDate.set(Calendar.SECOND, 0);
        minDate.set(Calendar.MILLISECOND, 0);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    dateField.setText(date);

                    // 如果是入住日期，检查并清除无效的退房日期
                    if (isCheckIn) {
                        String currentCheckOut = etCheckOutDate.getText().toString();
                        if (!currentCheckOut.isEmpty()) {
                            if (!isCheckOutDateValid(date, currentCheckOut)) {
                                etCheckOutDate.setText("");
                                Toast.makeText(this, "Check-out date cannot be before check-in date. Please re-select check-out date.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        // 如果是退房日期，检查是否在入住日期之后
                        String currentCheckIn = etCheckInDate.getText().toString();
                        if (!currentCheckIn.isEmpty()) {
                            if (!isCheckOutDateValid(currentCheckIn, date)) {
                                dateField.setText("");
                                Toast.makeText(this, "Check-out date cannot be before check-in date!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePickerDialog.show();
    }

    private void validateAndSubmit() {
        String name = etStudentName.getText().toString().trim();
        String matric = etStudentId.getText().toString().trim().toUpperCase();
        String phone = etPhoneNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String programme = etProgramme.getText().toString().trim();
        String semester = etSemester.getText().toString().trim();
        String checkInDate = etCheckInDate.getText().toString().trim();
        String checkOutDate = etCheckOutDate.getText().toString().trim();
        String duration = selectedDuration + " Semester" + (selectedDuration > 1 ? "s" : "");

        double totalPrice = roomPriceValue * selectedDuration;
        String priceDisplay = "RM " + String.format("%.0f", totalPrice);

        // Validation
        if (name.isEmpty()) {
            etStudentName.setError("Name is required");
            etStudentName.requestFocus();
            return;
        }

        if (matric.isEmpty()) {
            etStudentId.setError("Student ID is required");
            etStudentId.requestFocus();
            return;
        }

        String matricPattern = "^[A-Za-z]\\d{2}[A-Za-z]{2}\\d{4}$";
        if (!matric.matches(matricPattern)) {
            etStudentId.setError("Invalid format! Use: A24DW0000");
            etStudentId.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhoneNumber.setError("Phone number is required");
            etPhoneNumber.requestFocus();
            return;
        }

        if (phone.length() < 10 || phone.length() > 11) {
            etPhoneNumber.setError("Phone number must be 10-11 digits");
            etPhoneNumber.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            etEmail.requestFocus();
            return;
        }

        if (programme.isEmpty()) {
            etProgramme.setError("Programme/Course is required");
            etProgramme.requestFocus();
            return;
        }

        if (semester.isEmpty()) {
            etSemester.setError("Semester is required");
            etSemester.requestFocus();
            return;
        }

        if (checkInDate.isEmpty()) {
            etCheckInDate.setError("Check-in date is required");
            etCheckInDate.requestFocus();
            return;
        }

        if (checkOutDate.isEmpty()) {
            etCheckOutDate.setError("Check-out date is required");
            etCheckOutDate.requestFocus();
            return;
        }

        // ========== 提交前验证退房日期不能早于入住日期 ==========
        if (!isCheckOutDateValid(checkInDate, checkOutDate)) {
            etCheckOutDate.setError("Check-out date cannot be before check-in date!");
            etCheckOutDate.requestFocus();
            Toast.makeText(this, "Please select a check-out date that is after check-in date", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitApplication.setEnabled(false);
        checkAndSubmitApplication(currentUser.getUid(), name, matric, phone, email, programme, semester,
                duration, totalPrice, priceDisplay, checkInDate, checkOutDate);
    }

    private void checkAndSubmitApplication(String uid, String name, String matric, String phone, String email,
                                           String programme, String semester, String duration,
                                           double totalPrice, String priceDisplay,
                                           String checkInDate, String checkOutDate) {
        db.collection("Bookings")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean hasActiveApplication = false;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("bookingStatus");
                            if (status != null && !status.equalsIgnoreCase("Rejected")) {
                                hasActiveApplication = true;
                                break;
                            }
                        }

                        if (hasActiveApplication) {
                            Toast.makeText(ApplyActivity.this,
                                    "You already have an active or pending booking application!",
                                    Toast.LENGTH_LONG).show();
                            btnSubmitApplication.setEnabled(true);
                        } else {
                            executeApplicationSubmission(uid, name, matric, phone, email, programme, semester,
                                    duration, totalPrice, priceDisplay, checkInDate, checkOutDate);
                        }
                    } else {
                        btnSubmitApplication.setEnabled(true);
                        Toast.makeText(ApplyActivity.this, "Error checking active bookings: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void executeApplicationSubmission(String uid, String name, String matric, String phone, String email,
                                              String programme, String semester, String duration,
                                              double totalPrice, String priceDisplay,
                                              String checkInDate, String checkOutDate) {

        // 1. 先更新 Users 集合（同步用户编辑的信息）
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("name", name);
        userUpdate.put("phone", phone);
        userUpdate.put("email", email);
        userUpdate.put("programme", programme);
        userUpdate.put("currentSemester", semester);
        userUpdate.put("studentId", matric);

        db.collection("Users").document(uid)
                .update(userUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ApplyActivity", "User info updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("ApplyActivity", "Failed to update user: " + e.getMessage());
                });

        // 2. 创建 Booking 记录
        Map<String, Object> bookingData = new HashMap<>();

        bookingData.put("uid", uid);
        bookingData.put("roomId", selectedRoomId);
        bookingData.put("name", name);
        bookingData.put("matricNumber", matric);
        bookingData.put("phone", phone);
        bookingData.put("email", email);
        bookingData.put("programme", programme);
        bookingData.put("currentSemester", semester);
        bookingData.put("roomType", selectedRoomType);
        bookingData.put("price", totalPrice);
        bookingData.put("pricePerSemester", roomPriceValue);
        bookingData.put("duration", selectedDuration);
        bookingData.put("status", roomStatus);
        bookingData.put("location", roomLocation);
        bookingData.put("leaseDuration", duration);
        bookingData.put("checkInDate", checkInDate);
        bookingData.put("checkOutDate", checkOutDate);
        bookingData.put("bookingStatus", "Pending");
        bookingData.put("rejectReason", "");
        bookingData.put("paymentMethod", "");
        bookingData.put("paymentTimestamp", 0);
        bookingData.put("createdAt", System.currentTimeMillis());

        db.collection("Bookings")
                .add(bookingData)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update("bookingId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ApplyActivity.this, "Application submitted successfully! Total: " + priceDisplay + " for " + duration, Toast.LENGTH_LONG).show();
                            });

                    Intent intent = new Intent(ApplyActivity.this, StudentDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitApplication.setEnabled(true);
                    Toast.makeText(ApplyActivity.this, "Submission failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}