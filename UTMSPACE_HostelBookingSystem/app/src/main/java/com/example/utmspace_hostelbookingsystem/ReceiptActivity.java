package com.example.utmspace_hostelbookingsystem;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    private static final String TAG = "ReceiptActivity";

    private LinearLayout btnDownloadReceipt, btnBackToHome;
    private TextView tvTransactionId, tvPaymentDate, tvPaymentMethod, tvPaymentStatus;
    private TextView tvRoomName, tvRoomNumber, tvDuration;
    private TextView tvTotalAmount;
    private TextView tvStudentName, tvStudentEmail, tvStudentPhone;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String bookingDocId, roomId, roomType, roomPrice;
    private String studentName, phoneNumber, studentEmail;
    private String paymentMethod;
    private double amountPaid;
    private long paymentTimestamp;

    // 收据编号
    private String receiptNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupStatusBar();
        initViews();
        getIntentData();
        loadUserData();
        generateReceiptNumber();
        bindData();
        setupClick();
    }

    private void initViews() {
        tvTransactionId = findViewById(R.id.tvTransactionId);
        tvPaymentDate = findViewById(R.id.tvPaymentDate);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvDuration = findViewById(R.id.tvDuration);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentEmail = findViewById(R.id.tvStudentEmail);
        tvStudentPhone = findViewById(R.id.tvStudentPhone);
        btnDownloadReceipt = findViewById(R.id.btnDownloadReceipt);
        btnBackToHome = findViewById(R.id.btnBackToHome);
    }

    private void setupClick() {
        btnDownloadReceipt.setOnClickListener(v -> generateProfessionalPDF());
        btnBackToHome.setOnClickListener(v -> {
            Intent i = new Intent(this, StudentDashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
    }

    private void getIntentData() {
        Intent i = getIntent();
        bookingDocId = i.getStringExtra("BOOKING_DOC_ID");
        roomId = i.getStringExtra("ROOM_ID");
        roomType = i.getStringExtra("ROOM_TYPE");
        roomPrice = i.getStringExtra("ROOM_PRICE");
        studentName = i.getStringExtra("STUDENT_NAME");
        phoneNumber = i.getStringExtra("PHONE_NUMBER");
        paymentMethod = i.getStringExtra("PAYMENT_METHOD");
        amountPaid = i.getDoubleExtra("AMOUNT_PAID", 0);
        paymentTimestamp = i.getLongExtra("PAYMENT_TIMESTAMP", System.currentTimeMillis());

        if (amountPaid == 0 && roomPrice != null) {
            try {
                String cleanPrice = roomPrice.replace("RM", "").replace(" ", "").trim();
                amountPaid = Double.parseDouble(cleanPrice);
            } catch (Exception e) {
                amountPaid = 1500;
            }
        }
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() != null) {
            studentEmail = mAuth.getCurrentUser().getEmail();

            // 从 Firestore 获取最新用户信息
            db.collection("Users").document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            if (studentName == null || studentName.isEmpty()) {
                                studentName = doc.getString("name");
                            }
                            if (phoneNumber == null || phoneNumber.isEmpty()) {
                                phoneNumber = doc.getString("phone");
                            }
                            bindData();
                        }
                    });
        } else {
            studentEmail = "-";
        }
    }

    private void generateReceiptNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String datePart = sdf.format(new Date(paymentTimestamp));
        String idPart = bookingDocId != null && bookingDocId.length() >= 6
                ? bookingDocId.substring(bookingDocId.length() - 6).toUpperCase()
                : String.valueOf(System.currentTimeMillis()).substring(7);
        receiptNumber = "INV-" + datePart + "-" + idPart;
    }

    private void bindData() {
        String txnId = receiptNumber;
        tvTransactionId.setText(txnId);

        tvPaymentDate.setText(formatDate(paymentTimestamp));
        tvPaymentMethod.setText(paymentMethod != null ? paymentMethod : "Credit/Debit Card");
        tvPaymentStatus.setText("Completed");

        tvRoomName.setText(roomType != null ? roomType : "-");
        tvRoomNumber.setText(roomId != null ? roomId : "-");
        tvDuration.setText("1 Semester");

        double totalAmount = amountPaid;
        tvTotalAmount.setText(String.format(Locale.getDefault(), "RM %.2f", totalAmount));

        tvStudentName.setText(studentName != null ? studentName : "-");
        tvStudentEmail.setText(studentEmail != null ? studentEmail : "-");
        tvStudentPhone.setText(phoneNumber != null ? phoneNumber : "-");
    }

    private String formatDate(long time) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                .format(new Date(time));
    }

    /**
     * 生成专业的 PDF 收据（非截图方式）
     */
    private void generateProfessionalPDF() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Generating Receipt")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        dialog.show();

        new Handler().postDelayed(() -> {
            try {
                File pdfFile = createProfessionalPDF();
                dialog.dismiss();

                if (pdfFile != null && pdfFile.exists()) {
                    Toast.makeText(this, "Receipt saved: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
                    openPDFWithDefaultApp(pdfFile);
                } else {
                    Toast.makeText(this, "Failed to generate receipt", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                dialog.dismiss();
                Log.e(TAG, "PDF generation error", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, 500);
    }

    /**
     * 创建专业格式的 PDF 收据
     */
    private File createProfessionalPDF() throws Exception {
        // 页面尺寸：A4 比例 (595 x 842 points)
        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // 设置画笔
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#800000"));
        titlePaint.setTextSize(28);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#800000"));
        headerPaint.setTextSize(16);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#AA7A6C"));
        labelPaint.setTextSize(12);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#3C2A24"));
        valuePaint.setTextSize(12);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL));

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#EEDDD5"));
        linePaint.setStrokeWidth(1);

        Paint totalLabelPaint = new Paint();
        totalLabelPaint.setColor(Color.parseColor("#3C2A24"));
        totalLabelPaint.setTextSize(18);
        totalLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

        Paint totalValuePaint = new Paint();
        totalValuePaint.setColor(Color.parseColor("#800000"));
        totalValuePaint.setTextSize(18);
        totalValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

        int y = 60;
        int leftMargin = 50;
        int rightCol = 350;

        // ===== 标题 =====
        canvas.drawText("HOSTEL HUB", pageWidth / 2, y, titlePaint);
        y += 30;

        Paint subPaint = new Paint();
        subPaint.setColor(Color.parseColor("#AA7A6C"));
        subPaint.setTextSize(12);
        subPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Official Payment Receipt", pageWidth / 2, y, subPaint);
        y += 40;

        // ===== 分隔线 =====
        canvas.drawLine(40, y, pageWidth - 40, y, linePaint);
        y += 25;

        // ===== 收据信息 =====
        headerPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("RECEIPT INFORMATION", leftMargin, y, headerPaint);
        y += 25;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Receipt Number:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(receiptNumber, pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Transaction ID:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        String shortId = bookingDocId != null && bookingDocId.length() > 12
                ? bookingDocId.substring(0, 12) : bookingDocId;
        canvas.drawText(shortId != null ? shortId : "N/A", pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Payment Date:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatDate(paymentTimestamp), pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Payment Method:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(paymentMethod != null ? paymentMethod : "Credit/Debit Card", pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Payment Status:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("COMPLETED", pageWidth - leftMargin, y, valuePaint);
        y += 30;

        canvas.drawLine(40, y, pageWidth - 40, y, linePaint);
        y += 25;

        // ===== 房间信息 =====
        canvas.drawText("ROOM DETAILS", leftMargin, y, headerPaint);
        y += 25;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Room Type:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(roomType != null ? roomType : "-", pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Room Number:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(roomId != null ? roomId : "-", pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Duration:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("1 Semester", pageWidth - leftMargin, y, valuePaint);
        y += 30;

        canvas.drawLine(40, y, pageWidth - 40, y, linePaint);
        y += 25;

        // ===== 客户信息 =====
        canvas.drawText("BILLED TO", leftMargin, y, headerPaint);
        y += 25;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Name:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(studentName != null ? studentName : "-", pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Email:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(studentEmail != null ? studentEmail : "-", pageWidth - leftMargin, y, valuePaint);
        y += 20;

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Phone:", leftMargin, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(phoneNumber != null ? phoneNumber : "-", pageWidth - leftMargin, y, valuePaint);
        y += 35;

        canvas.drawLine(40, y, pageWidth - 40, y, linePaint);
        y += 25;

        // ===== 金额总计 =====
        canvas.drawText("TOTAL", leftMargin, y, totalLabelPaint);
        totalValuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.getDefault(), "RM %.2f", amountPaid),
                pageWidth - leftMargin, y, totalValuePaint);
        y += 35;

        canvas.drawLine(40, y, pageWidth - 40, y, linePaint);
        y += 30;

        // ===== 页脚 =====
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#AA7A6C"));
        footerPaint.setTextSize(10);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Hostel Hub - Official Residence", pageWidth / 2, y, footerPaint);
        y += 15;
        canvas.drawText("This is a computer-generated receipt. No signature required.", pageWidth / 2, y, footerPaint);

        pdfDocument.finishPage(page);

        // 保存文件
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        String fileName = "Receipt_" + receiptNumber + ".pdf";
        File pdfFile = new File(downloadsDir, fileName);

        FileOutputStream fos = new FileOutputStream(pdfFile);
        pdfDocument.writeTo(fos);
        pdfDocument.close();
        fos.close();

        return pdfFile;
    }

    /**
     * 用默认 PDF 查看器打开文件（优先使用 WPS、Adobe 等）
     */
    private void openPDFWithDefaultApp(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 检查是否有应用可以处理 PDF
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

            if (activities.size() > 0) {
                startActivity(intent);
            } else {
                // 如果没有 PDF 查看器，提示用户
                Toast.makeText(this, "No PDF viewer found. Please install WPS Office or Adobe Reader.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF", e);
            Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show();
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupStatusBar();
    }
}