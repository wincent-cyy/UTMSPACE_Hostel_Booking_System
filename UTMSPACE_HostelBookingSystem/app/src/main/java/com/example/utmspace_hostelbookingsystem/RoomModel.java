package com.example.utmspace_hostelbookingsystem;

public class RoomModel {
    private String documentId;  // Firestore document ID
    private String roomNumber;
    private String roomType;     // Single Room, Double Room, Quad Room
    private String status;       // Available, Full, Maintenance
    private String location;     // Block A, Level 1
    private double price;
    private int maxCapacity;
    private int currentOccupancy;
    private String condition;    // Good, Needs Repair, Under Maintenance
    private String lastUpdated;

    // Empty constructor required for Firebase
    public RoomModel() {}

    // Full constructor
    public RoomModel(String roomNumber, String roomType, String status, String location,
                     double price, int maxCapacity, int currentOccupancy, String condition) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.status = status;
        this.location = location;
        this.price = price;
        this.maxCapacity = maxCapacity;
        this.currentOccupancy = currentOccupancy;
        this.condition = condition;
    }

    // Getters
    public String getDocumentId() { return documentId; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
    public double getPrice() { return price; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public String getCondition() { return condition; }
    public String getLastUpdated() { return lastUpdated; }

    // Setters
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setStatus(String status) { this.status = status; }
    public void setLocation(String location) { this.location = location; }
    public void setPrice(double price) { this.price = price; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isFull() {
        return currentOccupancy >= maxCapacity;
    }
}