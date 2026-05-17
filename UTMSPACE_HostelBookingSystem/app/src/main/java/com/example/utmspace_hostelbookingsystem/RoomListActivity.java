package com.example.utmspace_hostelbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class RoomListActivity extends AppCompatActivity {

    private TextView tvRoomTypeTitle;
    private RecyclerView rvRoomList;
    private FirebaseFirestore db;
    private List<RoomModel> roomList;
    private RoomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        db = FirebaseFirestore.getInstance();

        // 1. Initialize Views
        tvRoomTypeTitle = findViewById(R.id.tvRoomTypeTitle);
        rvRoomList = findViewById(R.id.rvRoomList);
        rvRoomList.setLayoutManager(new LinearLayoutManager(this));

        // 2. Get Data from Intent (Passed from Dashboard)
        String selectedType = getIntent().getStringExtra("ROOM_TYPE");
        if (selectedType != null) {
            tvRoomTypeTitle.setText(selectedType);
            fetchRoomsFromFirebase(selectedType);
        }

        // 3. Back Button Functionality
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void fetchRoomsFromFirebase(String type) {
        roomList = new ArrayList<>();

        // Pass a click listener instance into the adapter constructor
        adapter = new RoomAdapter(roomList, room -> {
            Intent intent = new Intent(RoomListActivity.this, RoomDetailsActivity.class);

            // Pass room details safely
            intent.putExtra("ROOM_ID", "Room " + room.getRoomNumber());

            // CRITICAL FIX: Force pass the guaranteed 'type' string from the dashboard category
            // instead of room.getRoomType() which might be evaluating to null
            intent.putExtra("ROOM_TYPE", type);

            intent.putExtra("ROOM_PRICE", "RM " + (int)room.getPrice() + " / Month");
            intent.putExtra("ROOM_STATUS", room.isFull() ? "Full" : room.getStatus());

            // Set up local hardcoded descriptions for safety
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
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            RoomModel room = document.toObject(RoomModel.class);
                            roomList.add(room);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("FirebaseError", "Error getting documents: ", task.getException());
                    }
                });
    }
}