package com.example.utmspace_hostelbookingsystem;

public class Booking {
    // These variable names MUST match your Firestore keys exactly
    private String roomId;
    private String roomType;
    private String roomPrice;
    private String status;
    private String checkInDate;
    private String leaseDuration;
    private String userId;
    private String studentName;
    private String matricNumber;
    private String phoneNumber;

    // Empty constructor required for Firestore serialization
    public Booking() {}

    public Booking(String roomId, String roomType, String roomPrice, String status,
                   String checkInDate, String leaseDuration, String userId,
                   String studentName, String matricNumber, String phoneNumber) {
        this.roomId = roomId;
        this.roomType = roomType;
        this.roomPrice = roomPrice;
        this.status = status;
        this.checkInDate = checkInDate;
        this.leaseDuration = leaseDuration;
        this.userId = userId;
        this.studentName = studentName;
        this.matricNumber = matricNumber;
        this.phoneNumber = phoneNumber;
    }

    // Explicit Getters matching your Adapter requirements
    public String getRoomId() {
        return roomId != null ? roomId : "N/A";
    }

    public String getRoomType() {
        return roomType != null ? roomType : "Unknown Type";
    }

    public String getRoomPrice() {
        return roomPrice != null ? roomPrice : "N/A";
    }

    public String getStatus() {
        return status != null ? status : "Pending";
    }

    public String getCheckInDate() {
        return checkInDate != null ? checkInDate : "N/A";
    }

    public String getLeaseDuration() {
        return leaseDuration != null ? leaseDuration : "N/A";
    }

    public String getUserId() { return userId; }
    public String getStudentName() { return studentName; }
    public String getMatricNumber() { return matricNumber; }
    public String getPhoneNumber() { return phoneNumber; }

    // Explicit Setters so Firestore can map incoming document changes
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setRoomPrice(String roomPrice) { this.roomPrice = roomPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }
    public void setLeaseDuration(String leaseDuration) { this.leaseDuration = leaseDuration; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}