package com.example.utmspace_hostelbookingsystem;

public class RoomModel {

    private String documentId;  // Firestore document ID
    private String roomId;      // ✅ FIXED (replace roomNumber)
    private String roomType;
    private String status;
    private String location;
    private double price;
    private int maxCapacity;
    private int currentOccupancy;
    private String condition;
    private long lastUpdated;

    // Empty constructor (Firebase required)
    public RoomModel() {}

    public RoomModel(String roomId, String roomType, String status, String location,
                     double price, int maxCapacity, int currentOccupancy, String condition) {
        this.roomId = roomId;
        this.roomType = roomType;
        this.status = status;
        this.location = location;
        this.price = price;
        this.maxCapacity = maxCapacity;
        this.currentOccupancy = currentOccupancy;
        this.condition = condition;
    }

    // Getters
    public String getDocumentId() {
        return documentId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    public double getPrice() {
        return price;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public String getCondition() {
        return condition;
    }

    // Getter
    public long getLastUpdated() {
        return lastUpdated;
    }

    // Setters
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void setCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = currentOccupancy;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    // Setter
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isFull() {
        return currentOccupancy >= maxCapacity;
    }
}