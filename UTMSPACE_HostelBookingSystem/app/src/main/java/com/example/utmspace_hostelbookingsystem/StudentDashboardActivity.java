package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvStudentName;
    private ShapeableImageView ivProfilePicture;
    private ViewPager2 newsViewPager;
    private TabLayout bannerIndicator;
    private BottomNavigationView bottomNavigationView;

    // Category Cards
    private CardView cardSingle, cardDouble, cardQuad;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userDataListener;

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI Views
        tvStudentName = findViewById(R.id.tvStudentName);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        newsViewPager = findViewById(R.id.newsViewPager);
        bannerIndicator = findViewById(R.id.bannerIndicator);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize Category Cards
        cardSingle = findViewById(R.id.cardSingle);
        cardDouble = findViewById(R.id.cardDouble);
        cardQuad = findViewById(R.id.cardQuad);

        // Setup Functions
        setupNavigation();
        setupNewsBanner();
        setupCategoryClicks();

        // Listen to live profile data updates
        startRealtimeUserListener();
    }

    private void setupCategoryClicks() {
        cardSingle.setOnClickListener(v -> openRoomList("Single Room"));
        cardDouble.setOnClickListener(v -> openRoomList("Double Room"));
        cardQuad.setOnClickListener(v -> openRoomList("Quad Room"));
    }

    private void openRoomList(String roomType) {
        Intent intent = new Intent(StudentDashboardActivity.this, RoomListActivity.class);
        intent.putExtra("ROOM_TYPE", roomType);
        startActivity(intent);
    }

    private void setupNewsBanner() {
        List<Integer> images = new ArrayList<>();
        images.add(R.drawable.news_img1);
        images.add(R.drawable.news_img2);
        images.add(R.drawable.news_img3);

        NewsAdapter adapter = new NewsAdapter(images);
        newsViewPager.setAdapter(adapter);

        new TabLayoutMediator(bannerIndicator, newsViewPager, (tab, position) -> {}).attach();

        bannerIndicator.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            ViewGroup tabStrip = (ViewGroup) bannerIndicator.getChildAt(0);
            for (int i = 0; i < tabStrip.getChildCount(); i++) {
                View tabView = tabStrip.getChildAt(i);
                int size = (int) (8 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                lp.setMargins(10, 0, 10, 0);
                tabView.setLayoutParams(lp);
            }
        });

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (newsViewPager != null && adapter.getItemCount() > 0) {
                    int nextItem = (newsViewPager.getCurrentItem() + 1) % images.size();
                    newsViewPager.setCurrentItem(nextItem, true);
                    sliderHandler.postDelayed(this, 3000);
                }
            }
        };
    }

    /**
     * Listens to Firestore changes in real-time and decodes Base64 data automatically.
     */
    private void startRealtimeUserListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userDataListener = db.collection("Users").document(user.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            Log.e("DashboardDebug", "❌ Firestore snapshot connection error", error);
                            return;
                        }

                        if (isFinishing() || isDestroyed()) return;

                        if (snapshot != null && snapshot.exists()) {
                            // Update user profile name text
                            String name = snapshot.getString("name");
                            if (name != null) {
                                tvStudentName.setText(name);
                            }

                            // CHANGED HERE: Extracting the explicit profilePictureBase64 field key
                            String base64String = snapshot.getString("profilePictureBase64");
                            Log.d("DashboardDebug", "🔥 Base64 string segment detected.");

                            if (base64String != null && !base64String.trim().isEmpty()) {
                                try {
                                    // 1. Convert the Base64 text string into bytes
                                    byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);

                                    // 2. Decode the bytes into an Android image Bitmap
                                    Bitmap decodedByteMap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                    if (decodedByteMap != null) {
                                        // 3. Load the clean system bitmap into Glide
                                        Glide.with(getApplicationContext())
                                                .load(decodedByteMap)
                                                .override(200, 200) // Forces layout resolution constraints inside scroll view layers
                                                .placeholder(R.drawable.profile_pic)
                                                .error(R.drawable.profile_pic)
                                                .centerCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true) // Erases internal device memory maps so updates show immediately
                                                .into(ivProfilePicture);

                                        Log.d("DashboardDebug", "✅ SUCCESS: Decoded Base64 Bitmap applied to dashboard picture view.");
                                    } else {
                                        Log.e("DashboardDebug", "❌ Error decoding Base64 image byte arrays.");
                                        ivProfilePicture.setImageResource(R.drawable.profile_pic);
                                    }

                                } catch (IllegalArgumentException e) {
                                    Log.e("DashboardDebug", "❌ Base64 string structural decoding pattern error.", e);
                                    ivProfilePicture.setImageResource(R.drawable.profile_pic);
                                }
                            } else {
                                Log.w("DashboardDebug", "⚠️ profilePictureBase64 field is empty or missing. Loading default image resource placeholder.");
                                Glide.with(getApplicationContext()).load(R.drawable.profile_pic).centerCrop().into(ivProfilePicture);
                            }
                        }
                    });
        }
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) return true;

            Intent intent = null;
            if (id == R.id.nav_booking) {
                intent = new Intent(this, BookingsActivity.class);
            } else if (id == R.id.nav_history) {
                intent = new Intent(this, HistoryActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sliderRunnable != null) {
            sliderHandler.postDelayed(sliderRunnable, 3000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sliderHandler.removeCallbacksAndMessages(null);
        if (userDataListener != null) {
            userDataListener.remove();
        }
    }
}