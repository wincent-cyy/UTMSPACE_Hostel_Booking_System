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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    // 从 Firestore 获取的房间信息
    private double roomPriceValue;      // 存储价格数值
    private String roomStatus;          // 存储房间状态
    private String roomLocation;        // 存储房间位置

    // Firebase Integration Components
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        applyInputRestrictions();
        getIntentData();
        setupSpinnerOptions();
        setupClickListeners();

        // 加载用户信息并自动填充
        loadUserInfo();

        // 加载房间详细信息
        loadRoomInfo();
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

        // ✅ name 和 phone 可以编辑，但会自动填充初始值
        etFullName.setFocusable(true);
        etFullName.setClickable(true);
        etFullName.setEnabled(true);

        etPhoneNumber.setFocusable(true);
        etPhoneNumber.setClickable(true);
        etPhoneNumber.setEnabled(true);

        // ✅ matricNumber 让用户手动输入
        etStudentId.setFocusable(true);
        etStudentId.setClickable(true);
        etStudentId.setEnabled(true);

        // ✅ 设置输入限制
        setupInputFilters();
    }

    private void setupInputFilters() {
        // 1. Name: 只允许字母和空格，禁止数字和特殊字符
        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetter(c) && !Character.isSpaceChar(c)) {
                    return ""; // 拒绝非字母和非空格字符
                }
            }
            return null; // 接受输入
        };
        etFullName.setFilters(new InputFilter[]{nameFilter, new InputFilter.LengthFilter(50)});

        // 2. Matric Number: 格式 A24DW0000，最大9字符，自动转大写
        etStudentId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});

        // 3. Phone: 只允许数字，10-11位
        InputFilter phoneFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    return ""; // 拒绝非数字字符
                }
            }
            return null; // 接受输入
        };
        etPhoneNumber.setFilters(new InputFilter[]{phoneFilter, new InputFilter.LengthFilter(11)});
    }

    private void applyInputRestrictions() {
        // 保留但禁用编辑，所以不需要输入限制
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            selectedRoomId = intent.getStringExtra("SELECTED_ROOM_ID");
            selectedRoomType = intent.getStringExtra("SELECTED_ROOM_TYPE");
            selectedRoomPrice = intent.getStringExtra("SELECTED_ROOM_PRICE");

            if (selectedRoomId != null) tvSummaryRoomId.setText(selectedRoomId);
            if (selectedRoomType != null) tvSummaryRoomType.setText(selectedRoomType);
            if (selectedRoomPrice != null) tvBottomPrice.setText(selectedRoomPrice);
        }
    }

    // 新增: 从 Users 加载用户信息并自动填充
    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");

                        // ✅ 只自动填充 name 和 phone
                        if (name != null) etFullName.setText(name);
                        if (phone != null) etPhoneNumber.setText(phone);

                        // ✅ 学号不清空，让用户自己填
                        // 如果用户之前填过，可以保留
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    // 新增: 从 Rooms 加载房间详细信息
    // 新增: 从 Rooms 加载房间详细信息
    private void loadRoomInfo() {
        if (selectedRoomId == null) return;

        db.collection("Rooms")
                .whereEqualTo("roomId", selectedRoomId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // ✅ 修正: 使用 DocumentSnapshot 而不是 QueryDocumentSnapshot
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        roomPriceValue = doc.getDouble("price") != null ? doc.getDouble("price") : 0;
                        roomStatus = doc.getString("status");
                        roomLocation = doc.getString("location");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load room info", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSpinnerOptions() {
        String[] leaseDurations = {"1 Semester", "2 Semesters (Full Academic Year)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, leaseDurations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDuration.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnDetailsBack.setOnClickListener(v -> finish());

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

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        btnSubmitApplication.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String matric = etStudentId.getText().toString().trim().toUpperCase();
            String phone = etPhoneNumber.getText().toString().trim();
            String date = tvCheckInDate.getText().toString().trim();
            String duration = spnDuration.getSelectedItem().toString();

            // 验证姓名 - 不能为空
            if (name.isEmpty()) {
                etFullName.setError("Name is required");
                etFullName.requestFocus();
                return;
            }

            // 验证学号 - 不能为空
            if (matric.isEmpty()) {
                etStudentId.setError("Matric Number is required");
                etStudentId.requestFocus();
                return;
            }

            // ✅ 验证学号格式: A24DW0000 (字母 + 2数字 + 2字母 + 4数字)
            String matricPattern = "^[A-Za-z]\\d{2}[A-Za-z]{2}\\d{4}$";
            if (!matric.matches(matricPattern)) {
                etStudentId.setError("Invalid format! Use: A24DW0000 (e.g., A24DW1234)");
                etStudentId.requestFocus();
                return;
            }

            // 验证手机号 - 不能为空
            if (phone.isEmpty()) {
                etPhoneNumber.setError("Phone number is required");
                etPhoneNumber.requestFocus();
                return;
            }

            // ✅ 验证手机号长度: 10-11位
            if (phone.length() < 10 || phone.length() > 11) {
                etPhoneNumber.setError("Phone number must be 10-11 digits");
                etPhoneNumber.requestFocus();
                return;
            }

            // 验证日期是否已选择
            if (date.isEmpty() || date.toLowerCase().contains("select")) {
                Toast.makeText(this, "Please select an intended check-in date.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证用户是否已登录
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmitApplication.setEnabled(false);
            checkAndSubmitApplication(currentUser.getUid(), name, matric, phone, date, duration);
        });
    }

    private void checkAndSubmitApplication(String uid, String name, String matric, String phone, String date, String duration) {
        db.collection("Bookings")
                .whereEqualTo("uid", uid)  // ✅ 改为 uid (与Users.uid一致)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean hasActiveApplication = false;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("bookingStatus");  // ✅ 改为 bookingStatus
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
                            executeApplicationSubmission(uid, name, matric, phone, date, duration);
                        }
                    } else {
                        btnSubmitApplication.setEnabled(true);
                        Toast.makeText(ApplyActivity.this, "Error checking active bookings: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void executeApplicationSubmission(String uid, String name, String matric, String phone, String date, String duration) {
        // ✅ 修正: 使用正确的字段名 (与Users和Rooms一致)
        Map<String, Object> bookingData = new HashMap<>();

        // Foreign Keys (名字与源Collection完全一致)
        bookingData.put("uid", uid);                    // ✅ 改为 uid (不是 userId)
        bookingData.put("roomId", selectedRoomId);       // ✅ 保持 roomId

        // 从 Users 继承的快照数据
        bookingData.put("name", name);                  // ✅ 改为 name (不是 studentName)
        bookingData.put("matricNumber", matric);        // ✅ 保持 matricNumber
        bookingData.put("phone", phone);                // ✅ 改为 phone (不是 phoneNumber)

        // 从 Rooms 继承的快照数据
        bookingData.put("roomType", selectedRoomType);
        bookingData.put("price", roomPriceValue);       // ✅ 改为 price (double, 不是 roomPrice)
        bookingData.put("status", roomStatus);     // ✅ 新增: 预订时的房间状态
        bookingData.put("location", roomLocation);      // ✅ 新增: 房间位置

        // Booking 特有字段
        bookingData.put("checkInDate", date);
        bookingData.put("leaseDuration", duration);
        bookingData.put("bookingStatus", "Pending");    // ✅ 改为 bookingStatus (不是 status)
        bookingData.put("rejectReason", "");
        bookingData.put("paymentMethod", "");
        bookingData.put("paymentTimestamp", 0);
        bookingData.put("createdAt", System.currentTimeMillis());

        db.collection("Bookings")
                .add(bookingData)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update("bookingId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ApplyActivity.this, "Application submitted!", Toast.LENGTH_SHORT).show();
                                Toast.makeText(ApplyActivity.this, "Application submitted successfully! Status: Pending", Toast.LENGTH_LONG).show();
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