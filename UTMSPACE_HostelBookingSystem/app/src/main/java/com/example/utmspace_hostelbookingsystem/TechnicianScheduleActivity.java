package com.example.utmspace_hostelbookingsystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TechnicianScheduleActivity extends AppCompatActivity {

    private LinearLayout ivBack;
    private LinearLayout btnDatePicker;
    private TextView tvSelectedDate;
    private TextView tvTaskCount;
    private LinearLayout taskContainer;
    private LinearLayout emptyState;

    private FirebaseFirestore db;
    private List<RepairRequest> taskList;
    private Calendar selectedCalendar;
    private SimpleDateFormat displayDateFormat;
    private SimpleDateFormat firestoreDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_schedule);

        db = FirebaseFirestore.getInstance();
        taskList = new ArrayList<>();
        displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        firestoreDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        setupClickListeners();

        // 默认显示今天的任务
        selectedCalendar = Calendar.getInstance();
        updateDateDisplay();
        loadTasksForDate();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvTaskCount = findViewById(R.id.tvTaskCount);
        taskContainer = findViewById(R.id.taskContainer);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnDatePicker.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
                    updateDateDisplay();
                    loadTasksForDate();
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        String dateStr = displayDateFormat.format(selectedCalendar.getTime());
        tvSelectedDate.setText(dateStr);
    }

    private void loadTasksForDate() {
        String selectedDateStr = firestoreDateFormat.format(selectedCalendar.getTime());

        taskContainer.removeAllViews();
        taskList.clear();

        // 查询当天的维修任务（根据 availableTime 字段）
        db.collection("RepairRequests")
                .whereEqualTo("availableTime", selectedDateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RepairRequest request = createRequestFromDocument(document);
                        taskList.add(request);
                    }
                    displayTasks();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    displayTasks();
                });
    }

    private RepairRequest createRequestFromDocument(QueryDocumentSnapshot document) {
        RepairRequest request = new RepairRequest();
        request.setDocumentId(document.getId());
        request.setRoomId(document.getString("roomId"));
        request.setRoomType(document.getString("roomType"));
        request.setIssueType(document.getString("issueType"));
        request.setPriority(document.getString("priority"));
        request.setDescription(document.getString("description"));
        request.setStatus(document.getString("status"));
        request.setName(document.getString("name"));

        Long createdAt = document.getLong("createdAt");
        request.setCreatedAt(createdAt != null ? createdAt : 0);

        request.setAvailableTime(document.getString("availableTime"));
        request.setContactPerson(document.getString("contactPerson"));
        request.setProofImage(document.getString("proofImage"));
        return request;
    }

    private void displayTasks() {
        taskContainer.removeAllViews();

        if (taskList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            taskContainer.setVisibility(View.GONE);
            tvTaskCount.setText("0 tasks");
            return;
        }

        emptyState.setVisibility(View.GONE);
        taskContainer.setVisibility(View.VISIBLE);
        tvTaskCount.setText(taskList.size() + " tasks");

        for (RepairRequest request : taskList) {
            View taskView = createTaskView(request);
            taskContainer.addView(taskView);
        }
    }

    private View createTaskView(RepairRequest request) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_schedule_task, null);

        TextView tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
        TextView tvIssueType = itemView.findViewById(R.id.tvIssueType);
        TextView tvDescription = itemView.findViewById(R.id.tvDescription);
        TextView tvPriority = itemView.findViewById(R.id.tvPriority);
        TextView tvPreferredTime = itemView.findViewById(R.id.tvPreferredTime);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);

        tvRoomNumber.setText(request.getRoomId() != null ? request.getRoomId() : "N/A");
        tvIssueType.setText(request.getIssueType() != null ? request.getIssueType() : "N/A");
        tvDescription.setText(request.getDescription() != null ? request.getDescription() : "No description");

        // 设置优先级颜色
        String priority = request.getPriority();
        tvPriority.setText(priority != null ? priority : "Medium");
        if ("High".equalsIgnoreCase(priority)) {
            tvPriority.setBackgroundResource(R.drawable.urgency_badge_high);
        } else if ("Medium".equalsIgnoreCase(priority)) {
            tvPriority.setBackgroundResource(R.drawable.urgency_badge_medium);
        } else if ("Low".equalsIgnoreCase(priority)) {
            tvPriority.setBackgroundResource(R.drawable.urgency_badge_low);
        } else if ("Emergency".equalsIgnoreCase(priority)) {
            tvPriority.setBackgroundResource(R.drawable.urgency_badge_emergency);
        }

        // 设置首选时间
        String availableTime = request.getAvailableTime();
        tvPreferredTime.setText(availableTime != null ? availableTime : "Time not specified");

        // 设置状态
        String status = request.getStatus();
        tvStatus.setText(status != null ? status : "Pending");
        if ("Pending".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
        } else if ("In Progress".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.status_badge_in_progress);
        } else if ("Completed".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.status_badge_completed);
        }

        // 点击任务跳转到详情页
        final String documentId = request.getDocumentId();
        final String roomId = request.getRoomId();
        final String roomType = request.getRoomType();
        final String issueType = request.getIssueType();
        final String priorityStr = request.getPriority();
        final String description = request.getDescription();
        final String statusStr = request.getStatus();
        final String name = request.getName();
        final long createdAt = request.getCreatedAt();

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(TechnicianScheduleActivity.this, TechnicianRepairDetailActivity.class);
            intent.putExtra("REQUEST_ID", documentId);
            intent.putExtra("roomId", roomId);
            intent.putExtra("roomType", roomType);
            intent.putExtra("issueType", issueType);
            intent.putExtra("priority", priorityStr);
            intent.putExtra("description", description);
            intent.putExtra("status", statusStr);
            intent.putExtra("name", name);
            intent.putExtra("createdAt", createdAt);
            startActivity(intent);
        });

        return itemView;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasksForDate();
    }
}