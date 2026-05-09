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

public class ReceiptActivity extends AppCompatActivity {

    private MaterialButton btnDownload, btnBackHome;
    private TextView tvTotalAmount, tvReceiptMethod, tvReceiptRoom, tvReceiptDate, tvTransactionId;
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

        receiptCard = findViewById(R.id.receiptCard);

        // Fallback if ID is missing in XML
        if (receiptCard == null) {
            receiptCard = (View) tvTotalAmount.getParent().getParent();
        }
    }

    private void displayData() {
        String method = getIntent().getStringExtra("PAYMENT_METHOD");
        String room = getIntent().getStringExtra("ROOM_NAME");
        String price = getIntent().getStringExtra("PRICE");

        tvReceiptMethod.setText(method != null ? method : "E-Wallet");
        tvReceiptRoom.setText(room != null ? room : "N/A");
        tvTotalAmount.setText(price != null ? price : "RM 0.00");

        tvReceiptDate.setText("09 May 2026, 08:30 PM");
        tvTransactionId.setText("UTM-" + System.currentTimeMillis() / 1000);
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
        // 1. Create a bitmap of the receipt card
        Bitmap bitmap = Bitmap.createBitmap(receiptCard.getWidth(), receiptCard.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        receiptCard.draw(canvas);

        // 2. Create PDF document
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas pdfCanvas = page.getCanvas();
        pdfCanvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        // 3. Save to Downloads folder using MediaStore
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

                // 4. OPEN THE PDF IMMEDIATELY
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
            // This triggers the system "Open with..." dialog
            startActivity(Intent.createChooser(intent, "Open Receipt PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer found on this device", Toast.LENGTH_SHORT).show();
        }
    }
}