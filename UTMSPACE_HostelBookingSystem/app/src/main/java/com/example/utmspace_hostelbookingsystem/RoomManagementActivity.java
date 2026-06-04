package com.example.utmspace_hostelbookingsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoomManagementActivity extends AppCompatActivity {

    // UI Elements
    private EditText etSearchRoom;
    private ImageView ivClearSearch;
    private LinearLayout roomListContainer;
    private LinearLayout emptyState;
    private TextView tvRoomCount;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddRoom;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    private TextView chipAll, chipSingle, chipDouble, chipQuad;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<RoomModel> allRoomsList;
    private List<RoomModel> filteredList;

    private String currentTypeFilter = "All";
    private String currentSearchQuery = "";

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isProcessing = false;
    private boolean isLoading = false;

    // Room type options
    private String[] roomTypes = {"Single Room", "Double Room", "Quad Room"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_management);

        // Use ADJUST_PAN instead of ADJUST_RESIZE to prevent bottom nav from being pushed up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        db = FirebaseFirestore.getInstance();

        // FIXED: Set white status bar without affecting layout
        setupStatusBar();

        initViews();
        setupSwipeRefresh();
        setupFilterChips();
        setupSearchFilter();
        loadRooms();
        setupClickListeners();
        setupBottomNavigation();
    }

    /**
     * Setup status bar to be white with dark icons
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Only set status bar color to white
            getWindow().setStatusBarColor(Color.WHITE);

            // Make status bar icons dark for visibility on white background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    private void initViews() {
        etSearchRoom = findViewById(R.id.etSearchRoom);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        roomListContainer = findViewById(R.id.roomListContainer);
        emptyState = findViewById(R.id.emptyState);
        tvRoomCount = findViewById(R.id.tvRoomCount);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddRoom = findViewById(R.id.fabAddRoom);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);

        chipAll = findViewById(R.id.chipAll);
        chipSingle = findViewById(R.id.chipSingle);
        chipDouble = findViewById(R.id.chipDouble);
        chipQuad = findViewById(R.id.chipQuad);

        allRoomsList = new ArrayList<>();
        filteredList = new ArrayList<>();
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primaryColor)
            );
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshData();
            });

            // 只有当 ScrollView 滚动到顶部时才启用下拉刷新
            if (scrollView != null) {
                scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    if (swipeRefreshLayout != null && scrollView != null) {
                        swipeRefreshLayout.setEnabled(scrollView.getScrollY() == 0);
                    }
                });
            }
        }
    }

    private void refreshData() {
        if (isLoading) {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        // Reset filters and search
        currentTypeFilter = "All";
        currentSearchQuery = "";

        if (etSearchRoom != null) {
            etSearchRoom.setText("");
        }
        if (ivClearSearch != null) {
            ivClearSearch.setVisibility(View.GONE);
        }

        // Update chips UI
        updateChipStyles(chipAll);

        // Reload rooms
        loadRooms();

        // Stop refresh animation after data is loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(this, "Rooms refreshed", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            currentTypeFilter = "All";
            updateChipStyles(chipAll);
            applyFilters();
        });

        chipSingle.setOnClickListener(v -> {
            currentTypeFilter = "Single";
            updateChipStyles(chipSingle);
            applyFilters();
        });

        chipDouble.setOnClickListener(v -> {
            currentTypeFilter = "Double";
            updateChipStyles(chipDouble);
            applyFilters();
        });

        chipQuad.setOnClickListener(v -> {
            currentTypeFilter = "Quad";
            updateChipStyles(chipQuad);
            applyFilters();
        });
    }

    private void updateChipStyles(TextView selectedChip) {
        resetChipStyle(chipAll);
        resetChipStyle(chipSingle);
        resetChipStyle(chipDouble);
        resetChipStyle(chipQuad);

        selectedChip.setBackgroundResource(R.drawable.filter_chip_selected);
        selectedChip.setTextColor(getColor(android.R.color.white));
    }

    private void resetChipStyle(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.filter_chip_unselected);
        chip.setTextColor(getColor(R.color.tabInactiveText));
    }

    private void setupSearchFilter() {
        // Set keyboard to close when done typing
        etSearchRoom.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etSearchRoom.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearchRoom.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        etSearchRoom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString();
                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }

                searchRunnable = () -> {
                    currentSearchQuery = query.toLowerCase().trim();
                    applyFilters();
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                etSearchRoom.setText("");
                currentSearchQuery = "";
                applyFilters();
            });
        }
    }

    private void loadRooms() {
        if (isLoading) return;

        isLoading = true;

        db.collection("Rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRoomsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RoomModel room = new RoomModel();
                        room.setDocumentId(document.getId());
                        room.setRoomId(document.getString("roomId"));
                        room.setRoomType(document.getString("roomType"));
                        room.setLocation(document.getString("location"));
                        room.setPrice(document.getDouble("price") != null ? document.getDouble("price") : 0);
                        room.setStatus(document.getString("status"));
                        room.setMaxCapacity(document.getLong("maxCapacity") != null ?
                                document.getLong("maxCapacity").intValue() : 1);
                        room.setCurrentOccupancy(document.getLong("currentOccupancy") != null ?
                                document.getLong("currentOccupancy").intValue() : 0);

                        allRoomsList.add(room);
                    }
                    applyFilters();
                    isLoading = false;

                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isLoading = false;
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void applyFilters() {
        filteredList.clear();

        for (RoomModel room : allRoomsList) {
            boolean matchesType = true;
            boolean matchesSearch = true;

            // Type filter
            if (!"All".equals(currentTypeFilter)) {
                String roomType = room.getRoomType();
                matchesType = roomType != null && roomType.toLowerCase().contains(currentTypeFilter.toLowerCase());
            }

            // Search filter
            if (!currentSearchQuery.isEmpty()) {
                String roomId = room.getRoomId() != null ? room.getRoomId().toLowerCase() : "";
                matchesSearch = roomId.contains(currentSearchQuery);
            }

            if (matchesType && matchesSearch) {
                filteredList.add(room);
            }
        }

        displayRooms();
    }

    private void displayRooms() {
        roomListContainer.removeAllViews();

        tvRoomCount.setText(filteredList.size() + " rooms");

        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            roomListContainer.setVisibility(View.GONE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        roomListContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < filteredList.size(); i++) {
            RoomModel room = filteredList.get(i);
            View roomCard = createRoomCard(room);
            roomListContainer.addView(roomCard);

            // Add spacing between cards
            if (i < filteredList.size() - 1) {
                addDivider(roomListContainer);
            }
        }
    }

    private void addDivider(LinearLayout container) {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                12
        );
        divider.setLayoutParams(params);
        container.addView(divider);
    }

    private View createRoomCard(RoomModel room) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_admin_room, null);

        TextView tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
        TextView tvRoomStatus = itemView.findViewById(R.id.tvRoomStatus);
        TextView tvRoomType = itemView.findViewById(R.id.tvRoomType);
        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
        TextView tvPrice = itemView.findViewById(R.id.tvPrice);
        LinearLayout btnViewRoom = itemView.findViewById(R.id.btnViewRoom);
        LinearLayout btnEditRoom = itemView.findViewById(R.id.btnEditRoom);

        tvRoomNumber.setText(room.getRoomId() != null ? room.getRoomId() : "N/A");
        tvRoomType.setText(room.getRoomType() != null ? room.getRoomType() : "N/A");
        tvLocation.setText(room.getLocation() != null ? room.getLocation() : "N/A");
        tvPrice.setText(String.format("RM %.2f", room.getPrice()));

        String status = room.getStatus() != null ? room.getStatus() : "Available";
        tvRoomStatus.setText(status);
        if ("Available".equalsIgnoreCase(status)) {
            tvRoomStatus.setBackgroundResource(R.drawable.status_badge_available);
        } else if ("Full".equalsIgnoreCase(status)) {
            tvRoomStatus.setBackgroundResource(R.drawable.status_badge_full);
        } else if ("Maintenance".equalsIgnoreCase(status)) {
            tvRoomStatus.setBackgroundResource(R.drawable.status_badge_maintenance);
        }

        // View button - navigate to AdminEditRoomActivity in view mode
        btnViewRoom.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditRoomActivity.class);
            intent.putExtra("ROOM_DOCUMENT_ID", room.getDocumentId());
            intent.putExtra("ROOM_ID", room.getRoomId());
            intent.putExtra("ROOM_TYPE", room.getRoomType());
            intent.putExtra("LOCATION", room.getLocation());
            intent.putExtra("PRICE", room.getPrice());
            intent.putExtra("MAX_CAPACITY", room.getMaxCapacity());
            intent.putExtra("CURRENT_OCCUPANCY", room.getCurrentOccupancy());
            intent.putExtra("STATUS", room.getStatus());
            intent.putExtra("VIEW_ONLY", true);
            startActivity(intent);
        });

        // Edit button - navigate to AdminEditRoomActivity in edit mode
        btnEditRoom.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditRoomActivity.class);
            intent.putExtra("ROOM_DOCUMENT_ID", room.getDocumentId());
            intent.putExtra("ROOM_ID", room.getRoomId());
            intent.putExtra("ROOM_TYPE", room.getRoomType());
            intent.putExtra("LOCATION", room.getLocation());
            intent.putExtra("PRICE", room.getPrice());
            intent.putExtra("MAX_CAPACITY", room.getMaxCapacity());
            intent.putExtra("CURRENT_OCCUPANCY", room.getCurrentOccupancy());
            intent.putExtra("STATUS", room.getStatus());
            intent.putExtra("VIEW_ONLY", false);
            startActivity(intent);
        });

        itemView.setClickable(false);

        return itemView;
    }

    private void showAddRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_room, null);

        EditText etRoomId = dialogView.findViewById(R.id.etRoomId);
        EditText etRoomType = dialogView.findViewById(R.id.etRoomType);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etMaxCapacity = dialogView.findViewById(R.id.etMaxCapacity);
        EditText etCurrentOccupancy = dialogView.findViewById(R.id.etCurrentOccupancy);
        LinearLayout btnCancel = dialogView.findViewById(R.id.btnCancel);
        LinearLayout btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        // Set default current occupancy to 0
        etCurrentOccupancy.setText("0");

        // Setup Room Type dropdown only
        etRoomType.setFocusable(false);
        etRoomType.setClickable(true);
        etRoomType.setOnClickListener(v -> showRoomTypePickerDialog(etRoomType, etMaxCapacity, etCurrentOccupancy));

        AlertDialog dialog = builder.setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String roomId = etRoomId.getText().toString().trim();
            String roomType = etRoomType.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String capacityStr = etMaxCapacity.getText().toString().trim();
            String currentOccupancyStr = etCurrentOccupancy.getText().toString().trim();

            if (roomId.isEmpty()) {
                etRoomId.setError("Room number required");
                return;
            }
            if (roomType.isEmpty()) {
                Toast.makeText(this, "Please select room type", Toast.LENGTH_SHORT).show();
                return;
            }
            if (location.isEmpty()) {
                etLocation.setError("Location required");
                return;
            }
            if (priceStr.isEmpty()) {
                etPrice.setError("Price required");
                return;
            }
            if (capacityStr.isEmpty()) {
                etMaxCapacity.setError("Max capacity required");
                return;
            }
            if (currentOccupancyStr.isEmpty()) {
                etCurrentOccupancy.setError("Current occupancy required");
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int maxCapacity = Integer.parseInt(capacityStr);
                int currentOccupancy = Integer.parseInt(currentOccupancyStr);

                if (currentOccupancy > maxCapacity) {
                    etCurrentOccupancy.setError("Current occupancy cannot exceed max capacity");
                    return;
                }

                String status;
                if (currentOccupancy == 0) {
                    status = "Available";
                } else if (currentOccupancy >= maxCapacity) {
                    status = "Full";
                } else {
                    status = "Available";
                }

                addRoom(roomId, roomType, location, price, maxCapacity, currentOccupancy, status);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showRoomTypePickerDialog(EditText targetEditText, EditText etMaxCapacity, EditText etCurrentOccupancy) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Room Type")
                .setItems(roomTypes, (dialog, which) -> {
                    String selectedType = roomTypes[which];
                    targetEditText.setText(selectedType);

                    // Auto-set max capacity based on room type
                    if (selectedType.equals("Single Room")) {
                        if (etMaxCapacity != null && etMaxCapacity.getText().toString().isEmpty()) {
                            etMaxCapacity.setText("1");
                        }
                        if (etCurrentOccupancy != null && etCurrentOccupancy.getText().toString().isEmpty()) {
                            etCurrentOccupancy.setText("0");
                        }
                    } else if (selectedType.equals("Double Room")) {
                        if (etMaxCapacity != null && etMaxCapacity.getText().toString().isEmpty()) {
                            etMaxCapacity.setText("2");
                        }
                        if (etCurrentOccupancy != null && etCurrentOccupancy.getText().toString().isEmpty()) {
                            etCurrentOccupancy.setText("0");
                        }
                    } else if (selectedType.equals("Quad Room")) {
                        if (etMaxCapacity != null && etMaxCapacity.getText().toString().isEmpty()) {
                            etMaxCapacity.setText("4");
                        }
                        if (etCurrentOccupancy != null && etCurrentOccupancy.getText().toString().isEmpty()) {
                            etCurrentOccupancy.setText("0");
                        }
                    }
                });
        builder.show();
    }

    private void addRoom(String roomId, String roomType, String location, double price,
                         int maxCapacity, int currentOccupancy, String status) {
        if (isProcessing) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;

        // Extract only numbers from roomId for document name
        String documentName = "Room " + roomId.replaceAll("[^0-9]", "");

        // If no numbers found, use a fallback
        if (documentName.equals("Room ")) {
            documentName = "Room " + System.currentTimeMillis();
        }

        DocumentReference roomRef = db.collection("Rooms").document(documentName);

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("roomId", roomId);
        roomData.put("roomType", roomType);
        roomData.put("location", location);
        roomData.put("price", price);
        roomData.put("maxCapacity", maxCapacity);
        roomData.put("currentOccupancy", currentOccupancy);
        roomData.put("status", status);
        roomData.put("createdAt", System.currentTimeMillis());
        roomData.put("updatedAt", System.currentTimeMillis());

        roomRef.set(roomData)
                .addOnSuccessListener(aVoid -> {
                    isProcessing = false;
                    Toast.makeText(this, "Room added successfully", Toast.LENGTH_SHORT).show();
                    loadRooms();
                })
                .addOnFailureListener(e -> {
                    isProcessing = false;
                    Toast.makeText(this, "Failed to add room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_rooms);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_rooms) {
                return true;
            } else if (id == R.id.nav_home) {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_users) {
                Intent intent = new Intent(this, UserManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_management) {
                Intent intent = new Intent(this, ManagementActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure status bar stays white when activity resumes
        setupStatusBar();
        loadRooms();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_rooms);
        }
    }
}