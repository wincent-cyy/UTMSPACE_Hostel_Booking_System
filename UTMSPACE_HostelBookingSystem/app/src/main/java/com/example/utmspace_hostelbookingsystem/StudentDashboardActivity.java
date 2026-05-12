package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvStudentName;
    private ViewPager2 newsViewPager;
    private TabLayout bannerIndicator;
    private BottomNavigationView bottomNavigationView;

    // Category Cards
    private CardView cardSingle, cardDouble, cardQuad;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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
        newsViewPager = findViewById(R.id.newsViewPager);
        bannerIndicator = findViewById(R.id.bannerIndicator);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize Category Cards
        cardSingle = findViewById(R.id.cardSingle);
        cardDouble = findViewById(R.id.cardDouble);
        cardQuad = findViewById(R.id.cardQuad);

        // Setup Functions
        fetchUserName();
        setupNavigation();
        setupNewsBanner();
        setupCategoryClicks();
    }

    /**
     * Handles clicks on Room Category Cards
     */
    private void setupCategoryClicks() {
        cardSingle.setOnClickListener(v -> openRoomList("Single Room"));
        cardDouble.setOnClickListener(v -> openRoomList("Double Room"));
        cardQuad.setOnClickListener(v -> openRoomList("Quad Room"));
    }

    /**
     * Helper method to navigate to RoomListActivity with data
     */
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

        // Attach dots indicator
        new TabLayoutMediator(bannerIndicator, newsViewPager, (tab, position) -> {}).attach();

        // Fix for Circle Bubbles (UI adjustment)
        bannerIndicator.post(() -> {
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

        // Auto-slide logic
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

    private void fetchUserName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.get("name") != null) {
                            tvStudentName.setText(doc.getString("name"));
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Dashboard", "Error fetching name", e));
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
                // Optional: finish() if you don't want to keep home in stack
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start auto-slide
        if (sliderRunnable != null) {
            sliderHandler.postDelayed(sliderRunnable, 3000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-slide to save resources
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks
        sliderHandler.removeCallbacksAndMessages(null);
    }
}