package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";

    // UI Elements
    private TextView tvStudentName;
    private ImageView ivProfilePicture;
    private LinearLayout profileAvatar;
    private BottomNavigationView bottomNavigationView;
    private EditText etSearchInput;
    private ImageView ivSearchIcon;
    private TextView tvViewAll;

    // Swipe Refresh
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView mainScrollView;  // ADDED: Reference to main ScrollView

    // Banner elements
    private HorizontalScrollView bannerScrollView;
    private LinearLayout bannerContainer;
    private LinearLayout bannerItem1, bannerItem2, bannerItem3;
    private ImageView bannerImage1, bannerImage2, bannerImage3;
    private TextView bannerTitle1, bannerTitle2, bannerTitle3;
    private TextView bannerSubtitle1, bannerSubtitle2, bannerSubtitle3;
    private int bannerItemWidth = 0;
    private int bannerCount = 3;
    private int currentBannerIndex = 0;

    // Category Cards
    private LinearLayout roomCardSingle, roomCardDouble, roomCardQuad;
    private TextView tvSingleTitle, tvDoubleTitle, tvQuadTitle;
    private TextView tvSinglePrice, tvDoublePrice, tvQuadPrice;
    private ImageView imgSingleRoom, imgDoubleRoom, imgQuadRoom;

    // Auto Scroll Banner
    private Handler bannerHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerRunnable;
    private boolean isBannerScrolling = false;

    private boolean isUserInteracting = false;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }

        // Initialize UI Views
        initViews();

        // Setup Swipe Refresh
        setupSwipeRefresh();

        // Setup Functions
        setupNavigation();
        setupCategoryClicks();
        setupSearchFunction();
        setupViewAllClick();
        setupBannerClicks();

        // Load banner images
        loadBannerImages();

        // Delayed banner setup to get correct measurements
        bannerScrollView.post(() -> {
            calculateBannerWidths();
            setupAutoScrollBanner();
        });

        // Load data
        loadUserData();
    }

    private void initViews() {
        // Header
        tvStudentName = findViewById(R.id.tvStudentName);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        profileAvatar = findViewById(R.id.profileAvatar);

        // Swipe Refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // ADDED: Find the main ScrollView (assuming it has this ID, adjust if different)
        mainScrollView = findViewById(R.id.mainScrollView);
        // If your ScrollView has a different ID, use that instead
        // If no ScrollView ID exists, we'll create one programmatically

        // Search
        etSearchInput = findViewById(R.id.etSearchInput);
        ivSearchIcon = findViewById(R.id.ivSearchIcon);

        // Banner
        bannerScrollView = findViewById(R.id.bannerScrollView);
        bannerContainer = findViewById(R.id.bannerContainer);
        bannerItem1 = findViewById(R.id.bannerItem1);
        bannerItem2 = findViewById(R.id.bannerItem2);
        bannerItem3 = findViewById(R.id.bannerItem3);

        // Banner images
        bannerImage1 = findViewById(R.id.bannerImage1);
        bannerImage2 = findViewById(R.id.bannerImage2);
        bannerImage3 = findViewById(R.id.bannerImage3);

        // Banner titles
        bannerTitle1 = findViewById(R.id.bannerTitle1);
        bannerTitle2 = findViewById(R.id.bannerTitle2);
        bannerTitle3 = findViewById(R.id.bannerTitle3);

        // Banner subtitles
        bannerSubtitle1 = findViewById(R.id.bannerSubtitle1);
        bannerSubtitle2 = findViewById(R.id.bannerSubtitle2);
        bannerSubtitle3 = findViewById(R.id.bannerSubtitle3);

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Category Cards
        roomCardSingle = findViewById(R.id.roomCardSingle);
        roomCardDouble = findViewById(R.id.roomCardDouble);
        roomCardQuad = findViewById(R.id.roomCardQuad);

        // Category Titles
        tvSingleTitle = findViewById(R.id.tvSingleTitle);
        tvDoubleTitle = findViewById(R.id.tvDoubleTitle);
        tvQuadTitle = findViewById(R.id.tvQuadTitle);

        // Category Prices
        tvSinglePrice = findViewById(R.id.tvSinglePrice);
        tvDoublePrice = findViewById(R.id.tvDoublePrice);
        tvQuadPrice = findViewById(R.id.tvQuadPrice);

        // Category Images
        imgSingleRoom = findViewById(R.id.imgSingleRoom);
        imgDoubleRoom = findViewById(R.id.imgDoubleRoom);
        imgQuadRoom = findViewById(R.id.imgQuadRoom);

        // View All
        tvViewAll = findViewById(R.id.tvViewAll);

        // Load category images
        loadCategoryImages();
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshDashboard();
            });
        }
    }

    private void refreshDashboard() {
        // IMPROVED: Scroll to top first before refreshing
        scrollToTop();

        // Small delay to ensure scroll completes before refresh animation starts
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Reload all dashboard data
            loadUserData();
            loadBannerImages();
            loadCategoryImages();

            // Reset banner scroll position
            if (bannerScrollView != null && bannerRunnable != null) {
                currentBannerIndex = 0;
                isUserInteracting = false;  // ADD THIS
                bannerScrollView.scrollTo(0, 0);
                bannerHandler.removeCallbacks(bannerRunnable);
                isBannerScrolling = false;
                bannerHandler.postDelayed(bannerRunnable, 2000);
            }

            // Reset active category to Single (default)
            setActiveCategory("Single");

            // Stop refresh animation after data is loaded
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
            }, 1000);
        }, 100);
    }

    /**
     * IMPROVED: Scroll to top of the dashboard
     * This ensures the refresh happens from the top of the page
     */
    private void scrollToTop() {
        // Try to find the main ScrollView
        if (mainScrollView == null) {
            // If not found, try to find any ScrollView in the layout
            mainScrollView = findViewById(android.R.id.content).findViewById(android.R.id.list);

            // If still null, look for ScrollView programmatically
            if (mainScrollView == null) {
                View rootView = findViewById(android.R.id.content);
                if (rootView instanceof ScrollView) {
                    mainScrollView = (ScrollView) rootView;
                } else {
                    // Search for ScrollView in the view hierarchy
                    mainScrollView = findScrollView(rootView);
                }
            }
        }

        // Scroll to top if ScrollView exists
        if (mainScrollView != null) {
            mainScrollView.smoothScrollTo(0, 0);
        } else {
            // Fallback: Scroll banner and other views to top
            if (bannerScrollView != null) {
                bannerScrollView.scrollTo(0, 0);
            }
            // Also try to scroll the window to top
            final View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.requestFocus();
                rootView.scrollTo(0, 0);
            }
        }
    }

    /**
     * Recursively find ScrollView in view hierarchy
     */
    private ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                ScrollView result = findScrollView(viewGroup.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void loadBannerImages() {
        // 加载 Banner 图片
        Glide.with(this)
                .load(R.drawable.banner_item1)
                .centerCrop()
                .into(bannerImage1);

        Glide.with(this)
                .load(R.drawable.banner_item2)
                .centerCrop()
                .into(bannerImage2);

        Glide.with(this)
                .load(R.drawable.banner_item3)
                .centerCrop()
                .into(bannerImage3);

        // 设置 Banner 文字
        bannerTitle1.setText("Limited Offer · Promo Week");
        bannerSubtitle1.setText("Book double room with 30% off");

        bannerTitle2.setText("Double Room Special");
        bannerSubtitle2.setText("Get free breakfast included");

        bannerTitle3.setText("Early Bird Discount");
        bannerSubtitle3.setText("Save up to 50% today");
    }

    private void loadCategoryImages() {
        Glide.with(this)
                .load(R.drawable.single_room)
                .centerCrop()
                .into(imgSingleRoom);

        Glide.with(this)
                .load(R.drawable.double_room)
                .centerCrop()
                .into(imgDoubleRoom);

        Glide.with(this)
                .load(R.drawable.quad_room)
                .centerCrop()
                .into(imgQuadRoom);
    }

    private void calculateBannerWidths() {
        if (bannerItem1 != null) {
            bannerItemWidth = bannerItem1.getWidth();
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bannerItem1.getLayoutParams();
            if (params != null) {
                bannerItemWidth += params.leftMargin + params.rightMargin;
            }
            Log.d(TAG, "Banner item width: " + bannerItemWidth);
        } else {
            float density = getResources().getDisplayMetrics().density;
            bannerItemWidth = (int) (312 * density);
        }
    }

    private void setupAutoScrollBanner() {
        if (bannerScrollView == null) return;

        // ADD THIS: Detect user touch on banner
        bannerScrollView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE:
                    // User is interacting - stop auto scroll
                    isUserInteracting = true;
                    bannerHandler.removeCallbacks(bannerRunnable);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // User released - resume auto scroll after delay
                    isUserInteracting = false;
                    bannerHandler.removeCallbacks(bannerRunnable);
                    // Resume scrolling after 3 seconds of inactivity
                    bannerHandler.postDelayed(bannerRunnable, 3000);
                    break;
            }
            return false; // Allow scroll to continue
        });

        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                // SKIP if user is interacting
                if (bannerScrollView == null || isBannerScrolling || isUserInteracting) {
                    if (!isUserInteracting) {
                        bannerHandler.postDelayed(this, 1000);
                    }
                    return;
                }

                // Calculate next index
                currentBannerIndex = (currentBannerIndex + 1) % bannerCount;

                // Calculate target scroll position
                int targetScrollX = currentBannerIndex * bannerItemWidth;

                Log.d(TAG, "Scrolling to banner " + (currentBannerIndex + 1) +
                        ", targetScrollX: " + targetScrollX);

                isBannerScrolling = true;
                bannerScrollView.smoothScrollTo(targetScrollX, 0);

                // Reset scroll flag after animation completes
                bannerHandler.postDelayed(() -> isBannerScrolling = false, 500);

                // Repeat every 4 seconds
                bannerHandler.postDelayed(this, 4000);
            }
        };

        // Start after 2 seconds
        bannerHandler.postDelayed(bannerRunnable, 2000);
    }

    private void setupBannerClicks() {
        if (bannerItem1 != null) {
            bannerItem1.setOnClickListener(v -> {
                Toast.makeText(this, "Promo Week: 30% off for double room!", Toast.LENGTH_SHORT).show();
            });
        }

        if (bannerItem2 != null) {
            bannerItem2.setOnClickListener(v -> {
                Toast.makeText(this, "Double Room Special: Free breakfast included!", Toast.LENGTH_SHORT).show();
            });
        }

        if (bannerItem3 != null) {
            bannerItem3.setOnClickListener(v -> {
                Toast.makeText(this, "Early Bird: Save up to 50%!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupViewAllClick() {
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                navigateToAllRooms();
            });
        }
    }

    private void setupCategoryClicks() {
        roomCardSingle.setOnClickListener(v -> {
            setActiveCategory("Single");
            navigateToRoomList("Single Room");
        });

        roomCardDouble.setOnClickListener(v -> {
            setActiveCategory("Double");
            navigateToRoomList("Double Room");
        });

        roomCardQuad.setOnClickListener(v -> {
            setActiveCategory("Quad");
            navigateToRoomList("Quad Room");
        });
    }

    /**
     * Navigate to AllRoomsActivity with smooth animation
     */
    private void navigateToAllRooms() {
        Intent intent = new Intent(this, AllRoomsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Navigate to RoomListActivity with smooth animation
     */
    private void navigateToRoomList(String roomType) {
        Intent intent = new Intent(this, RoomListActivity.class);
        intent.putExtra("ROOM_TYPE", roomType);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setActiveCategory(String category) {
        // Reset all cards to normal
        roomCardSingle.setBackgroundResource(R.drawable.room_card_normal);
        roomCardDouble.setBackgroundResource(R.drawable.room_card_normal);
        roomCardQuad.setBackgroundResource(R.drawable.room_card_normal);

        // Reset text colors
        tvSingleTitle.setTextColor(0xFF1E293B);
        tvDoubleTitle.setTextColor(0xFF1E293B);
        tvQuadTitle.setTextColor(0xFF1E293B);

        // Reset price backgrounds
        tvSinglePrice.setBackgroundResource(R.drawable.price_chip_normal);
        tvDoublePrice.setBackgroundResource(R.drawable.price_chip_normal);
        tvQuadPrice.setBackgroundResource(R.drawable.price_chip_normal);
        tvSinglePrice.setTextColor(0xFF800000);
        tvDoublePrice.setTextColor(0xFF800000);
        tvQuadPrice.setTextColor(0xFF800000);

        // Reset image color filters
        imgSingleRoom.setColorFilter(null);
        imgDoubleRoom.setColorFilter(null);
        imgQuadRoom.setColorFilter(null);
        imgSingleRoom.setVisibility(View.VISIBLE);
        imgDoubleRoom.setVisibility(View.VISIBLE);
        imgQuadRoom.setVisibility(View.VISIBLE);

        // Set active card based on category
        if (category.equals("Single")) {
            roomCardSingle.setBackgroundResource(R.drawable.room_card_active);
            tvSingleTitle.setTextColor(0xFFFFFFFF);
            tvSinglePrice.setBackgroundResource(R.drawable.price_chip_active);
            tvSinglePrice.setTextColor(0xFFFFFFFF);
        } else if (category.equals("Double")) {
            roomCardDouble.setBackgroundResource(R.drawable.room_card_active);
            tvDoubleTitle.setTextColor(0xFFFFFFFF);
            tvDoublePrice.setBackgroundResource(R.drawable.price_chip_active);
            tvDoublePrice.setTextColor(0xFFFFFFFF);
        } else if (category.equals("Quad")) {
            roomCardQuad.setBackgroundResource(R.drawable.room_card_active);
            tvQuadTitle.setTextColor(0xFFFFFFFF);
            tvQuadPrice.setBackgroundResource(R.drawable.price_chip_active);
            tvQuadPrice.setTextColor(0xFFFFFFFF);
        }
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userDataListener = db.collection("Users").document(user.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Firestore snapshot error", error);
                            return;
                        }
                        if (isFinishing() || isDestroyed() || snapshot == null) return;

                        String name = snapshot.getString("name");
                        if (name != null) {
                            tvStudentName.setText(name);
                        }

                        String base64String = snapshot.getString("profileImageBase64");

                        if (base64String != null && !base64String.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                if (bitmap != null) {
                                    Glide.with(this)
                                            .load(bitmap)
                                            .circleCrop()
                                            .into(new CustomTarget<Drawable>() {
                                                @Override
                                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                        profileAvatar.setBackground(resource);
                                                    } else {
                                                        profileAvatar.setBackgroundDrawable(resource);
                                                    }
                                                }

                                                @Override
                                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                                }
                                            });

                                    ivProfilePicture.setVisibility(View.GONE);
                                } else {
                                    resetToDefaultAvatar();
                                }
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, "Base64 decode error", e);
                                resetToDefaultAvatar();
                            }
                        } else {
                            resetToDefaultAvatar();
                        }
                    });

            profileAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        } else {
            tvStudentName.setText("Guest");
            resetToDefaultAvatar();
        }
    }

    private void resetToDefaultAvatar() {
        profileAvatar.setBackgroundResource(R.drawable.avatar_background);
        ivProfilePicture.setVisibility(View.VISIBLE);
        ivProfilePicture.setImageResource(R.drawable.ic_account_circle);
    }

    private void setupSearchFunction() {
        // Search icon click
        ivSearchIcon.setOnClickListener(v -> {
            navigateToAllRooms();
        });

        // Search input click
        etSearchInput.setFocusable(false);
        etSearchInput.setClickable(true);
        etSearchInput.setOnClickListener(v -> {
            navigateToAllRooms();
        });
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            }

            Intent intent = null;
            if (id == R.id.nav_booking) {
                intent = new Intent(this, BookingsActivity.class);
            } else if (id == R.id.nav_history) {
                intent = new Intent(this, HistoryActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        if (etSearchInput != null) {
            etSearchInput.setText("");
        }

        loadCategoryImages();
        loadBannerImages();
        loadUserData();

        // 重置 Banner 索引和滚动
        currentBannerIndex = 0;
        isUserInteracting = false;
        bannerScrollView.post(() -> {
            calculateBannerWidths();
            if (bannerRunnable != null) {
                bannerHandler.removeCallbacks(bannerRunnable);
                isBannerScrolling = false;
                bannerHandler.postDelayed(bannerRunnable, 2000);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userDataListener != null) {
            userDataListener.remove();
        }
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }
}