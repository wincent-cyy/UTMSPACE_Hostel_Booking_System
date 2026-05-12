package com.example.utmspace_hostelbookingsystem;

public class RoomModel {
    private String roomNumber;
    private String roomType;
    private String status;
    private String location; // e.g., "Block A, Level 1"
    private double price;
    private int maxCapacity;
    private int currentOccupancy;

    // Empty constructor is REQUIRED for Firebase to work
    public RoomModel() {}

    // Overloaded constructor for manual creation
    public RoomModel(String roomNumber, String roomType, String status, String location,
                     double price, int maxCapacity, int currentOccupancy) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.status = status;
        this.location = location;
        this.price = price;
        this.maxCapacity = maxCapacity;
        this.currentOccupancy = currentOccupancy;
    }

    // --- GETTERS ---
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
    public double getPrice() { return price; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }

    // --- SETTERS ---
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setStatus(String status) { this.status = status; }
    public void setLocation(String location) { this.location = location; }
    public void setPrice(double price) { this.price = price; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }

    /**
     * Helper method to check if the room is full
     * Usage: if (room.isFull()) { ... }
     */
    public boolean isFull() {
        return currentOccupancy >= maxCapacity;
    }
}