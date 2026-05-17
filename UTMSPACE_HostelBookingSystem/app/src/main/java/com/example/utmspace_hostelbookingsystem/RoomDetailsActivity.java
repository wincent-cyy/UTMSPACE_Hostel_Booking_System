package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class RoomDetailsActivity extends AppCompatActivity {

    // UI Element Declarations
    private ImageButton btnBackArrow;
    private Button btnDetailsBack, btnApply;
    private ImageView ivRoomPicture;
    private TextView tvRoomPriceTag, tvRoomNumber, tvRoomStatusBadge, tvRoomType, tvRoomDescription;

    // Data variables to hold passed room values
    private String roomId;
    private String roomType;
    private String roomPrice;
    private String roomDescription;
    private String roomStatus;
    private String roomImageUrl;
    private int roomImageResource = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // 1. Initialize all XML View binders
        initViews();

        // 2. Safely retrieve intent data strings passed from RoomListActivity
        getIntentData();

        // 3. Populate and update the UI with the selected room info
        populateRoomDetails();

        // 4. Bind interactive operational click listeners
        setupClickListeners();
    }

    private void initViews() {
        btnBackArrow = findViewById(R.id.btnBack);
        ivRoomPicture = findViewById(R.id.ivRoomPicture);
        tvRoomPriceTag = findViewById(R.id.tvRoomPriceTag);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRoomStatusBadge = findViewById(R.id.tvRoomStatusBadge);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvRoomDescription = findViewById(R.id.tvRoomDescription);
        btnDetailsBack = findViewById(R.id.btnDetailsBack);
        btnApply = findViewById(R.id.btnApply);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            roomId = intent.getStringExtra("ROOM_ID");
            roomType = intent.getStringExtra("ROOM_TYPE");
            roomPrice = intent.getStringExtra("ROOM_PRICE");
            roomDescription = intent.getStringExtra("ROOM_DESC");
            roomStatus = intent.getStringExtra("ROOM_STATUS");
            roomImageUrl = intent.getStringExtra("ROOM_IMAGE_URL");
            roomImageResource = intent.getIntExtra("ROOM_IMAGE_RES", -1);
        }
    }

    private void populateRoomDetails() {
        if (roomId != null) tvRoomNumber.setText(roomId);
        if (roomType != null) tvRoomType.setText(roomType);
        if (roomPrice != null) tvRoomPriceTag.setText(roomPrice);
        if (roomDescription != null) tvRoomDescription.setText(roomDescription);

        if (roomStatus != null) {
            tvRoomStatusBadge.setText(roomStatus);
            if (roomStatus.equalsIgnoreCase("Full") || roomStatus.equalsIgnoreCase("Occupied")) {
                tvRoomStatusBadge.setBackgroundResource(R.drawable.input_field_rounded);
                tvRoomStatusBadge.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
                tvRoomStatusBadge.setTextColor(getColor(android.R.color.white));
            }
        }

        // --- TROUBLESHOOTING LOG ---
        Log.d("RoomDetails", "The current roomType text is: " + roomType);

        // --- FIXED IMAGE ROUTING BASED ON INCOMING TEXT ---
        if (roomType != null) {
            String lowerType = roomType.toLowerCase().trim();

            if (lowerType.contains("single")) {
                roomImageResource = R.drawable.single_room;
            } else if (lowerType.contains("double")) {
                roomImageResource = R.drawable.double_room;
            } else if (lowerType.contains("quad")) {
                roomImageResource = R.drawable.quad_room;
            } else {
                // If the text does not contain single/double/quad, default to single_room so it's not blank
                roomImageResource = R.drawable.single_room;
            }
        } else {
            // Absolute fallback if roomType completely fails to arrive
            roomImageResource = R.drawable.single_room;
        }

        // Display the image using the layout system
        if (roomImageUrl != null && !roomImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(roomImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivRoomPicture);
        } else {
            // Direct call to image resource for local drawables bypasses layout calculation bugs
            ivRoomPicture.setImageResource(roomImageResource);
        }
    }

    private void setupClickListeners() {
        btnBackArrow.setOnClickListener(v -> handleBackNavigation());
        btnDetailsBack.setOnClickListener(v -> handleBackNavigation());

        btnApply.setOnClickListener(v -> {
            if (roomStatus != null && (roomStatus.equalsIgnoreCase("Full") || roomStatus.equalsIgnoreCase("Occupied"))) {
                Toast.makeText(this, "Sorry, this room is currently fully booked.", Toast.LENGTH_SHORT).show();
            } else {
                processRoomApplication();
            }
        });
    }

    private void handleBackNavigation() {
        Intent intent = new Intent(RoomDetailsActivity.this, RoomListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void processRoomApplication() {
        Toast.makeText(this, "Proceeding with application for " + roomId, Toast.LENGTH_SHORT).show();
        Intent applyIntent = new Intent(RoomDetailsActivity.this, ApplyActivity.class);
        applyIntent.putExtra("SELECTED_ROOM_ID", roomId);
        applyIntent.putExtra("SELECTED_ROOM_TYPE", roomType);
        applyIntent.putExtra("SELECTED_ROOM_PRICE", roomPrice);
        startActivity(applyIntent);
    }
}