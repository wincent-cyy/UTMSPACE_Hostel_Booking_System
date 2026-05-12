package com.example.utmspace_hostelbookingsystem;

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
    private List<RoomModel> roomList; // You will need a RoomModel class
    private RoomAdapter adapter;      // You will need a RoomAdapter class

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
        adapter = new RoomAdapter(roomList);
        rvRoomList.setAdapter(adapter);

        // REMOVED .whereEqualTo("status", "Available")
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
                    }
                });
    }
}