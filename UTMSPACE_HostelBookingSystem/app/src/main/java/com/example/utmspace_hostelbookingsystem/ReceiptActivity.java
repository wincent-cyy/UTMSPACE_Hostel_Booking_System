package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    // Header
    private LinearLayout ivBack;
    private TextView tvTransactionId;
    private TextView tvPaymentDate;
    private TextView tvPaymentMethod;
    private TextView tvPaymentStatus;
    private TextView tvRoomName;
    private TextView tvRoomNumber;
    private TextView tvDuration;
    private TextView tvSubtotal;
    private TextView tvServiceCharge;
    private TextView tvTotalAmount;
    private TextView tvStudentName;
    private TextView tvStudentEmail;
    private TextView tvStudentPhone;

    // Buttons
    private LinearLayout btnDownloadReceipt;
    private LinearLayout btnBackToHome;

    // Data
    private String bookingDocId;
    private String roomId;
    private String roomType;
    private String roomPrice;
    private String studentName;
    private String matricNumber;
    private String phoneNumber;
    private String checkInDate;
    private String leaseDuration;
    private String paymentMethod;
    private double amountPaid;
    private long paymentTimestamp;
    private String studentEmail;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        mAuth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        initViews();
        getIntentData();
        loadStudentEmail();
        displayReceiptData();
        setupClickListeners();
    }

    private void initViews() {
        tvTransactionId = findViewById(R.id.tvTransactionId);
        tvPaymentDate = findViewById(R.id.tvPaymentDate);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvDuration = findViewById(R.id.tvDuration);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvServiceCharge = findViewById(R.id.tvServiceCharge);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentEmail = findViewById(R.id.tvStudentEmail);
        tvStudentPhone = findViewById(R.id.tvStudentPhone);

        btnDownloadReceipt = findViewById(R.id.btnDownloadReceipt);
        btnBackToHome = findViewById(R.id.btnBackToHome);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            bookingDocId = intent.getStringExtra("BOOKING_DOC_ID");
            roomId = intent.getStringExtra("ROOM_ID");
            roomType = intent.getStringExtra("ROOM_TYPE");
            roomPrice = intent.getStringExtra("ROOM_PRICE");
            studentName = intent.getStringExtra("STUDENT_NAME");
            matricNumber = intent.getStringExtra("MATRIC_NUMBER");
            phoneNumber = intent.getStringExtra("PHONE_NUMBER");
            checkInDate = intent.getStringExtra("CHECK_IN_DATE");
            leaseDuration = intent.getStringExtra("LEASE_DURATION");
            paymentMethod = intent.getStringExtra("PAYMENT_METHOD");
            amountPaid = intent.getDoubleExtra("AMOUNT_PAID", 0);
            paymentTimestamp = intent.getLongExtra("PAYMENT_TIMESTAMP", 0);

            String amountStr = intent.getStringExtra("AMOUNT_PAID");
            if (amountStr != null && amountPaid == 0) {
                try {
                    amountPaid = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    amountPaid = 0;
                }
            }

            if (amountPaid == 0 && roomPrice != null) {
                try {
                    amountPaid = Double.parseDouble(roomPrice.replace("RM ", "").trim());
                } catch (NumberFormatException e) {
                    amountPaid = 1500;
                }
            }
        }
    }

    private void loadStudentEmail() {
        if (mAuth.getCurrentUser() != null) {
            studentEmail = mAuth.getCurrentUser().getEmail();
        } else {
            studentEmail = "student@university.edu";
        }
    }

    private void displayReceiptData() {
        String transactionId = bookingDocId != null ?
                bookingDocId.substring(Math.max(0, bookingDocId.length() - 12)) :
                "TXN" + System.currentTimeMillis();
        tvTransactionId.setText("Transaction ID: " + transactionId);

        String formattedDate = formatDate(paymentTimestamp);
        tvPaymentDate.setText(formattedDate);

        tvPaymentMethod.setText(paymentMethod != null ? paymentMethod : "Credit/Debit Card");
        tvPaymentStatus.setText("Completed");

        tvRoomName.setText(roomType != null ? roomType : "N/A");
        tvRoomNumber.setText(roomId != null ? roomId : "N/A");
        tvDuration.setText(leaseDuration != null ? leaseDuration : "1 Semester");

        double subtotal = amountPaid;
        double serviceCharge = subtotal * 0.06;
        double total = subtotal + serviceCharge;

        tvSubtotal.setText(String.format("RM %.2f", subtotal));
        tvServiceCharge.setText(String.format("RM %.2f", serviceCharge));
        tvTotalAmount.setText(String.format("RM %.2f", total));

        tvStudentName.setText(studentName != null ? studentName : "N/A");
        tvStudentEmail.setText(studentEmail != null ? studentEmail : "N/A");
        tvStudentPhone.setText(phoneNumber != null ? phoneNumber : "N/A");
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        if (timestamp == 0) {
            return sdf.format(new Date());
        }
        return sdf.format(new Date(timestamp));
    }

    private void setupClickListeners() {
        btnDownloadReceipt.setOnClickListener(v -> downloadReceiptAsPDF());
        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiptActivity.this, StudentDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void downloadReceiptAsPDF() {
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Generating PDF")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        new Handler().postDelayed(() -> {
            try {
                File pdfFile = createPDFFromView();
                loadingDialog.dismiss();

                if (pdfFile != null && pdfFile.exists()) {
                    Toast.makeText(this, "Receipt saved: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
                    openPDF(pdfFile);
                } else {
                    Toast.makeText(this, "Failed to generate receipt", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                loadingDialog.dismiss();
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }, 500);
    }

    private File createPDFFromView() {
        try {
            // 获取根视图
            View rootView = getWindow().getDecorView().getRootView();

            // 测量视图
            rootView.measure(
                    View.MeasureSpec.makeMeasureSpec(rootView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );

            int width = rootView.getMeasuredWidth();
            int height = rootView.getMeasuredHeight();

            if (width <= 0) width = 800;
            if (height <= 0) height = 1200;

            // 创建 Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            rootView.draw(canvas);

            // 创建 PDF
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Canvas pdfCanvas = page.getCanvas();
            pdfCanvas.drawBitmap(bitmap, 0, 0, null);

            pdfDocument.finishPage(page);

            // 保存到 Downloads 目录
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Receipt_" + timeStamp + ".pdf";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File pdfFile = new File(downloadsDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                pdfDocument.writeTo(fos);
                pdfDocument.close();
                bitmap.recycle();
                return pdfFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openPDF(File pdfFile) {
        try {
            // 使用正确的 authorities (匹配你的 AndroidManifest.xml)
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);

            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(pdfUri, "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(pdfIntent);
        } catch (Exception e) {
            // 如果直接打开失败，尝试用文件管理器打开
            try {
                Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Open Receipt With"));
            } catch (Exception e2) {
                Toast.makeText(this, "PDF saved to: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        }
    }
}