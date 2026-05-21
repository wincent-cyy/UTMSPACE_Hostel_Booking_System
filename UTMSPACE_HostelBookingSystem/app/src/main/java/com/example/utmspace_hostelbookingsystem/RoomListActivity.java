package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText; // Or androidx.appcompat.widget.SearchView depending on your XML
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RoomListActivity extends AppCompatActivity {

    private TextView tvRoomTypeTitle;
    private RecyclerView rvRoomList;
    private FirebaseFirestore db;

    // UI Layout Search Element Referencer
    private EditText etSearchRoom;

    // Dataset management lists
    private List<RoomModel> completeRoomList = new ArrayList<>();  // Caches pristine Firebase entries
    private List<RoomModel> displayedRoomList = new ArrayList<>(); // Feeds directly to the RecyclerView adapter
    private RoomAdapter adapter;

    // Filter tracking criteria states ("None" signifies completely cleared/unselected states)
    private String selectedStatusCriteria = "None";
    private String selectedBlockCriteria = "None";
    private String currentActiveRoomType = "";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        db = FirebaseFirestore.getInstance();

        // 1. Initialize Layout Views
        tvRoomTypeTitle = findViewById(R.id.tvRoomTypeTitle);
        rvRoomList = findViewById(R.id.rvRoomList);
        rvRoomList.setLayoutManager(new LinearLayoutManager(this));

        // CRITICAL FIX: Bind the actual XML search field component to your Java logic
        // (Verify that your XML element ID matches R.id.etSearchRoom or update this reference)
        etSearchRoom = findViewById(R.id.etSearchRoomNumber);

        // 2. Setup Data Intent Streams from Dashboard Context
        currentActiveRoomType = getIntent().getStringExtra("ROOM_TYPE");
        if (currentActiveRoomType != null) {
            tvRoomTypeTitle.setText(currentActiveRoomType);
            fetchRoomsFromFirebase(currentActiveRoomType);
        }

        // 3. System Navigation Controls
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 4. Floating Action / Bottom Sheet Filter Trigger Listener
        View btnFilter = findViewById(R.id.btnListFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilteringBottomSheetDialog());
        }

        // 5. CRITICAL FIX: The UI Search Bar Event Listener
        if (etSearchRoom != null) {
            etSearchRoom.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Left blank intentionally
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Dynamically capture the keystroke buffer sequence as it modifies
                    currentSearchQuery = s.toString();

                    // Instantly trigger and apply full dataset validation pipeline
                    applyCombinedFiltersAndSorting();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Left blank intentionally
                }
            });
        }
    }

    private void fetchRoomsFromFirebase(String type) {
        adapter = new RoomAdapter(displayedRoomList, room -> {
            Intent intent = new Intent(RoomListActivity.this, RoomDetailsActivity.class);

            // Pass essential intent parameters forward
            intent.putExtra("ROOM_ID", room.getRoomId());
            intent.putExtra("ROOM_TYPE", type);
            intent.putExtra("ROOM_PRICE", "RM " + String.format("%.2f", room.getPrice()) + " / Month");

            // Map status text dynamically for UI consistency across cards
            intent.putExtra("ROOM_STATUS",
                    room.isFull() ? "Full" :
                            (room.getStatus() != null ? room.getStatus() : "Available"));

            // Build structural context descriptions depending safely on room classification flags
            String roomDesc = "Comfortable hostel residential living space perfect for student focus.";
            if (type != null) {
                if (type.toLowerCase().contains("single")) {
                    roomDesc = "Premium private personal space located near the central campus facilities. Complete with a private study desk configuration, high-speed networking access, wardrobe, and continuous window ventilation.";
                } else if (type.toLowerCase().contains("double")) {
                    roomDesc = "Spacious shared living suite layout optimal for companions or project partners. Features individual workspaces, split multi-tier shelving, and personal storage lockboxes.";
                } else if (type.toLowerCase().contains("quad")) {
                    roomDesc = "Affordable and highly social 4-sharing layout variant setup. Equipped with bunk bed systems, private desks per student, individual wardrobes, and communal balcony access points.";
                }
            }
            intent.putExtra("ROOM_DESC", roomDesc);
            startActivity(intent);
        });

        rvRoomList.setAdapter(adapter);

        db.collection("Rooms")
                .whereEqualTo("roomType", type)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        completeRoomList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            RoomModel room = document.toObject(RoomModel.class);
                            completeRoomList.add(room);
                        }
                        applyCombinedFiltersAndSorting();
                    } else {
                        Log.d("FirebaseError", "Error getting documents: ", task.getException());
                    }
                });
    }

    private void showFilteringBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_filter_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        // Map status view selectors
        TextView btnStatusAll = sheetView.findViewById(R.id.btnStatusAll);
        TextView btnStatusAvailable = sheetView.findViewById(R.id.btnStatusAvailable);
        TextView btnStatusFull = sheetView.findViewById(R.id.btnStatusFull);

        // Map building/location block views
        TextView btnBlockAll = sheetView.findViewById(R.id.btnBlockAll);
        TextView btnBlockA = sheetView.findViewById(R.id.btnBlockA);
        TextView btnBlockB = sheetView.findViewById(R.id.btnBlockB);

        // Map operational button entities
        MaterialButton btnClearFilters = sheetView.findViewById(R.id.btnClearFilters);
        MaterialButton btnApplyFilters = sheetView.findViewById(R.id.btnApplyFilters);

        // Temporary local parameters to safely retain changes until confirmation action is executed
        final String[] tempStatus = {selectedStatusCriteria};
        final String[] tempBlock = {selectedBlockCriteria};

        // UI View Updater Lifecycle block (Soft ocean blue / white palette guidelines)
        Runnable updateUISelectionStates = new Runnable() {
            @Override
            public void run() {
                // 1. Flush and reset structural configurations for Availability Status Row
                btnStatusAll.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnStatusAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
                btnStatusAvailable.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnStatusAvailable.setTextColor(android.graphics.Color.parseColor("#0369A1"));
                btnStatusFull.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnStatusFull.setTextColor(android.graphics.Color.parseColor("#0369A1"));

                if (tempStatus[0].equals("Available")) {
                    btnStatusAvailable.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnStatusAvailable.setTextColor(android.graphics.Color.WHITE);
                } else if (tempStatus[0].equals("Full")) {
                    btnStatusFull.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnStatusFull.setTextColor(android.graphics.Color.WHITE);
                } else if (tempStatus[0].equals("All")) {
                    btnStatusAll.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnStatusAll.setTextColor(android.graphics.Color.WHITE);
                }

                // 2. Flush and reset structural configurations for Hostel Block Location Row
                btnBlockAll.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnBlockAll.setTextColor(android.graphics.Color.parseColor("#0369A1"));
                btnBlockA.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnBlockA.setTextColor(android.graphics.Color.parseColor("#0369A1"));
                btnBlockB.setBackgroundResource(R.drawable.filter_chip_unselected);
                btnBlockB.setTextColor(android.graphics.Color.parseColor("#0369A1"));

                if (tempBlock[0].equalsIgnoreCase("Block A")) {
                    btnBlockA.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnBlockA.setTextColor(android.graphics.Color.WHITE);
                } else if (tempBlock[0].equalsIgnoreCase("Block B")) {
                    btnBlockB.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnBlockB.setTextColor(android.graphics.Color.WHITE);
                } else if (tempBlock[0].equalsIgnoreCase("All")) {
                    btnBlockAll.setBackgroundResource(R.drawable.filter_chip_selected);
                    btnBlockAll.setTextColor(android.graphics.Color.WHITE);
                }
            }
        };

        // Execute baseline selection state rendering on dialog draw
        updateUISelectionStates.run();

        // Register interactive tracking clicks for Status Options
        btnStatusAll.setOnClickListener(v -> { tempStatus[0] = "All"; updateUISelectionStates.run(); });
        btnStatusAvailable.setOnClickListener(v -> { tempStatus[0] = "Available"; updateUISelectionStates.run(); });
        btnStatusFull.setOnClickListener(v -> { tempStatus[0] = "Full"; updateUISelectionStates.run(); });

        // Register interactive tracking clicks for Location Block Options
        btnBlockAll.setOnClickListener(v -> { tempBlock[0] = "All"; updateUISelectionStates.run(); });
        btnBlockA.setOnClickListener(v -> { tempBlock[0] = "Block A"; updateUISelectionStates.run(); });
        btnBlockB.setOnClickListener(v -> { tempBlock[0] = "Block B"; updateUISelectionStates.run(); });

        // Action Handlers: Clear Requirements Operation Flow (Turns off all selections completely)
        btnClearFilters.setOnClickListener(v -> {
            selectedStatusCriteria = "None";
            selectedBlockCriteria = "None";

            applyCombinedFiltersAndSorting();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Filters Cleared", Toast.LENGTH_SHORT).show();
        });

        // Action Handlers: Commit and Apply Operational Flow
        btnApplyFilters.setOnClickListener(v -> {
            selectedStatusCriteria = tempStatus[0];
            selectedBlockCriteria = tempBlock[0];

            applyCombinedFiltersAndSorting();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void applyCombinedFiltersAndSorting() {
        List<RoomModel> filteredStageList = new ArrayList<>();

        for (RoomModel room : completeRoomList) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;
            boolean matchesBlock = true;

            // 1. FIXED SEARCH: Flexible Alphanumeric check. Matches if query is part of the room number (e.g., "101", "A", "A-101")
            if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty() && room.getRoomId() != null) {
                String cleanQuery = currentSearchQuery.trim().toLowerCase();
                String roomNum = room.getRoomId().trim().toLowerCase();

                matchesSearch = roomNum.contains(cleanQuery);
            }

            // 2. STATUS FILTER FIX: Maps database status string value "Occupied" safely to filter choice "Full"
            if (!selectedStatusCriteria.equals("None") && !selectedStatusCriteria.equals("All")) {
                if (selectedStatusCriteria.equalsIgnoreCase("Full")) {
                    // Item matches if boolean is true, status is explicitly "Full", OR status is explicitly "Occupied"
                    matchesStatus = room.isFull() ||
                            (room.getStatus() != null && (room.getStatus().equalsIgnoreCase("Full")));
                } else if (selectedStatusCriteria.equalsIgnoreCase("Available")) {
                    // Item matches if boolean is false AND status string is neither "Full" nor "Occupied"
                    matchesStatus = !room.isFull() &&
                            (room.getStatus() == null || (!room.getStatus().equalsIgnoreCase("Full")));
                }
            }

            // 3. Verify Block Location Constraints (Skip constraint check if cleared via "None")
            if (!selectedBlockCriteria.equals("None") && !selectedBlockCriteria.equals("All") && room.getLocation() != null) {
                matchesBlock = room.getLocation().toLowerCase().contains(selectedBlockCriteria.toLowerCase());
            }

            // Append item to display collection if all constraints match
            if (matchesSearch && matchesStatus && matchesBlock) {
                filteredStageList.add(room);
            }
        }

        // Swap list context safely and update presentation interface layer
        displayedRoomList.clear();
        displayedRoomList.addAll(filteredStageList);
        adapter.notifyDataSetChanged();
    }
}