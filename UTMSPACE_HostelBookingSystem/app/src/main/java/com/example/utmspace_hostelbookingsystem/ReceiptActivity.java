package com.example.utmspace_hostelbookingsystem;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    private MaterialButton btnDownload, btnBackHome;
    private TextView tvTotalAmount, tvReceiptMethod, tvReceiptRoom, tvReceiptDate, tvTransactionId, tvReceiptMatric, tvInstallmentPlan;
    private View receiptCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        initViews();
        displayData();
        setupListeners();
    }

    private void initViews() {
        btnDownload = findViewById(R.id.btnDownload);
        btnBackHome = findViewById(R.id.btnBackHome);

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvReceiptMethod = findViewById(R.id.tvReceiptMethod);
        tvReceiptRoom = findViewById(R.id.tvReceiptRoom);
        tvReceiptDate = findViewById(R.id.tvReceiptDate);
        tvTransactionId = findViewById(R.id.tvTransactionId);
        tvReceiptMatric = findViewById(R.id.tvReceiptMatric);
        tvInstallmentPlan = findViewById(R.id.tvInstallmentPlan);  // 新增：分期计划显示

        receiptCard = findViewById(R.id.receiptCard);
    }

    private void displayData() {
        Intent intent = getIntent();

        // 获取数据
        String method = intent.getStringExtra("PAYMENT_METHOD");
        String room = intent.getStringExtra("ROOM_ID");
        double amountPaid = intent.getDoubleExtra("AMOUNT_PAID", 0.0);
        String bookingId = intent.getStringExtra("BOOKING_DOC_ID");
        String matric = intent.getStringExtra("MATRIC_NUMBER");
        String installmentPlan = intent.getStringExtra("INSTALLMENT_PLAN");  // 获取分期计划

        // 设置文本
        tvReceiptMethod.setText(method != null ? method : "N/A");
        tvReceiptRoom.setText(room != null ? room : "N/A");
        tvTotalAmount.setText(String.format("RM %.2f", amountPaid));
        tvReceiptMatric.setText(matric != null ? matric : "N/A");

        // 设置分期计划显示
        if (installmentPlan != null && !installmentPlan.equals("Full")) {
            tvInstallmentPlan.setVisibility(View.VISIBLE);
            tvInstallmentPlan.setText("Payment Plan: " + installmentPlan);
        } else {
            tvInstallmentPlan.setVisibility(View.GONE);
        }

        // 设置当前日期
        String currentDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        tvReceiptDate.setText(currentDate);

        // 设置交易ID
        tvTransactionId.setText(bookingId != null ? bookingId : "UTM-" + System.currentTimeMillis() / 1000);
    }

    private void setupListeners() {
        btnDownload.setOnClickListener(v -> generatePDF());

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiptActivity.this, StudentDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void generatePDF() {
        // Ensure the view is measured for the bitmap
        receiptCard.measure(View.MeasureSpec.makeMeasureSpec(receiptCard.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(receiptCard.getHeight(), View.MeasureSpec.EXACTLY));

        Bitmap bitmap = Bitmap.createBitmap(receiptCard.getWidth(), receiptCard.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        receiptCard.draw(canvas);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas pdfCanvas = page.getCanvas();
        pdfCanvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        String fileName = "UTMSpace_Receipt_" + System.currentTimeMillis() + ".pdf";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        try {
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                document.writeTo(outputStream);
                document.close();
                outputStream.close();
                Toast.makeText(this, "Receipt saved to Downloads", Toast.LENGTH_SHORT).show();
                openReceipt(uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openReceipt(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Open Receipt PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
        }
    }
}