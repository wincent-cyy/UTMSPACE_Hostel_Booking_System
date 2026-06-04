package com.example.utmspace_hostelbookingsystem;

public class RoomModel {

    private String documentId;
    private String roomId;
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

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // ========== 輔助方法 ==========

    // 檢查房間是否已滿
    public boolean isFull() {
        return currentOccupancy >= maxCapacity;
    }

    // 檢查房間是否可用
    public boolean isAvailable() {
        return !isFull() && "Available".equalsIgnoreCase(status);
    }

    // 獲取可用床位數
    public int getAvailableBeds() {
        return maxCapacity - currentOccupancy;
    }

    // 獲取房間狀態的文字描述
    public String getStatusText() {
        if (isFull()) {
            return "Full";
        } else if ("Available".equalsIgnoreCase(status)) {
            return "Available";
        } else {
            return status != null ? status : "Unknown";
        }
    }

    // 獲取房間狀態顏色資源
    public int getStatusColor() {
        if (isFull()) {
            return android.R.color.holo_red_dark;
        } else if ("Available".equalsIgnoreCase(status)) {
            return android.R.color.holo_green_dark;
        } else {
            return android.R.color.holo_orange_dark;
        }
    }

    // 獲取價格格式化的字符串
    public String getFormattedPrice() {
        return "RM " + String.format("%.0f", price) + " / Semester";
    }
}