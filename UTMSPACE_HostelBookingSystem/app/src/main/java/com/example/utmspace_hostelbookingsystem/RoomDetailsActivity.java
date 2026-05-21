package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class RoomDetailsActivity extends AppCompatActivity {

    // UI Element Declarations
    private ImageButton btnBackArrow;
    private Button btnDetailsBack, btnApply;
    private ViewPager2 viewPagerImages;
    private ImageButton btnPrevImage, btnNextImage;
    private TextView tvRoomPriceTag, tvRoomNumber, tvRoomStatusBadge, tvRoomType, tvRoomDescription;

    // Data variables to hold passed room values
    private String roomId;
    private String roomType;
    private String roomPrice;
    private String roomDescription;
    private String roomStatus;
    private String roomImageUrl;
    private List<Integer> imageResources = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // 1. Initialize all XML View binders
        initViews();

        // 2. Safely retrieve intent data strings passed from RoomListActivity
        getIntentData();

        // 3. Setup image carousel (MUST BE CALLED)
        setupImageCarousel();

        // 4. Populate and update the UI with the selected room info
        populateRoomDetails();

        // 5. Bind interactive operational click listeners
        setupClickListeners();
    }

    private void initViews() {
        btnBackArrow = findViewById(R.id.btnBack);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        btnPrevImage = findViewById(R.id.btnPrevImage);
        btnNextImage = findViewById(R.id.btnNextImage);
        tvRoomPriceTag = findViewById(R.id.tvRoomPriceTag);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRoomStatusBadge = findViewById(R.id.tvRoomStatusBadge);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvRoomDescription = findViewById(R.id.tvRoomDescription);
        btnDetailsBack = findViewById(R.id.btnDetailsBack);
        btnApply = findViewById(R.id.btnApply);
    }

    private void setupImageCarousel() {
        // Add images based on room type
        if (roomType != null) {
            String lowerType = roomType.toLowerCase().trim();

            if (lowerType.contains("single")) {
                imageResources.add(R.drawable.single_room);
                // Only add if the drawable resources exist
                if (resourceExists(R.drawable.single1)) imageResources.add(R.drawable.single1);
                if (resourceExists(R.drawable.single2)) imageResources.add(R.drawable.single2);
                if (resourceExists(R.drawable.single3)) imageResources.add(R.drawable.single3);
            } else if (lowerType.contains("double")) {
                imageResources.add(R.drawable.double_room);
                if (resourceExists(R.drawable.double1)) imageResources.add(R.drawable.double1);
                if (resourceExists(R.drawable.double2)) imageResources.add(R.drawable.double2);
                if (resourceExists(R.drawable.double3)) imageResources.add(R.drawable.double3);
            } else if (lowerType.contains("quad")) {
                imageResources.add(R.drawable.quad_room);
                if (resourceExists(R.drawable.quad1)) imageResources.add(R.drawable.quad1);
                if (resourceExists(R.drawable.quad2)) imageResources.add(R.drawable.quad2);
                if (resourceExists(R.drawable.quad3)) imageResources.add(R.drawable.quad3);
            } else {
                imageResources.add(R.drawable.single_room);
            }
        } else {
            imageResources.add(R.drawable.single_room);
        }

        // Simple adapter for ViewPager2
        viewPagerImages.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ImageView imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return new RecyclerView.ViewHolder(imageView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ImageView iv = (ImageView) holder.itemView;
                iv.setImageResource(imageResources.get(position));
            }

            @Override
            public int getItemCount() {
                return imageResources.size();
            }
        });

        // Show arrows only if more than 1 image
        if (imageResources.size() > 1) {
            btnPrevImage.setVisibility(View.VISIBLE);
            btnNextImage.setVisibility(View.VISIBLE);
        } else {
            btnPrevImage.setVisibility(View.GONE);
            btnNextImage.setVisibility(View.GONE);
        }

        // Handle arrow clicks
        btnPrevImage.setOnClickListener(v -> {
            int current = viewPagerImages.getCurrentItem();
            if (current > 0) {
                viewPagerImages.setCurrentItem(current - 1, true);
            }
        });

        btnNextImage.setOnClickListener(v -> {
            int current = viewPagerImages.getCurrentItem();
            if (current < imageResources.size() - 1) {
                viewPagerImages.setCurrentItem(current + 1, true);
            }
        });
    }

    // Helper method to check if a drawable resource exists
    private boolean resourceExists(int resId) {
        return resId != 0;
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
        }
    }

    private void populateRoomDetails() {
        if (roomId != null) tvRoomNumber.setText(roomId);
        if (roomType != null) tvRoomType.setText(roomType);
        if (roomDescription != null) tvRoomDescription.setText(roomDescription);

        // Pricing display
        if (roomPrice != null && !roomPrice.isEmpty()) {
            String formattedPrice = roomPrice.trim();
            if (formattedPrice.contains("/")) {
                formattedPrice = formattedPrice.split("/")[0].trim();
            }
            if (!formattedPrice.toUpperCase().startsWith("RM")) {
                formattedPrice = "RM " + formattedPrice;
            }
            tvRoomPriceTag.setText(formattedPrice + " / Semester");
        }

        // Status badge
        if (roomStatus != null) {
            tvRoomStatusBadge.setText(roomStatus);
            if (roomStatus.equalsIgnoreCase("Full")) {
                tvRoomStatusBadge.setBackgroundResource(R.drawable.input_field_rounded);
                tvRoomStatusBadge.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
                tvRoomStatusBadge.setTextColor(getColor(android.R.color.white));
            }
        }

        Log.d("RoomDetails", "The current roomType text is: " + roomType);
    }

    private void setupClickListeners() {
        btnBackArrow.setOnClickListener(v -> handleBackNavigation());
        btnDetailsBack.setOnClickListener(v -> handleBackNavigation());

        btnApply.setOnClickListener(v -> {
            if (roomStatus != null && (roomStatus.equalsIgnoreCase("Full"))) {
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
        applyIntent.putExtra("SELECTED_ROOM_PRICE", tvRoomPriceTag.getText().toString());
        startActivity(applyIntent);
    }
}