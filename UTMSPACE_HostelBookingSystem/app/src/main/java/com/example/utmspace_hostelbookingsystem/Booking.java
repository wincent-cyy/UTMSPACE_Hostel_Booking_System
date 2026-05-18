package com.example.utmspace_hostelbookingsystem;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class Booking implements Serializable {
    // Variable field parameters exactly matching your Firestore Document structure keys
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
    private String rejectReason;

    // Excluded identifier mapping tracking property
    private String documentId;

    // Mandatory default zero-argument constructor for Firebase structural mapping
    public Booking() {}

    // Main parameterized initialization builder constructor template block
    public Booking(String roomId, String roomType, String roomPrice, String status,
                   String checkInDate, String leaseDuration, String userId,
                   String studentName, String matricNumber, String phoneNumber, String rejectReason) {
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
        this.rejectReason = rejectReason;
    }

    // Clear explicit clean getter structures without string mutation fallbacks
    public String getRoomId() { return roomId; }
    public String getRoomType() { return roomType; }
    public String getRoomPrice() { return roomPrice; }
    public String getStatus() { return status; }
    public String getCheckInDate() { return checkInDate; }
    public String getLeaseDuration() { return leaseDuration; }
    public String getUserId() { return userId; }
    public String getStudentName() { return studentName; }
    public String getMatricNumber() { return matricNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRejectReason() { return rejectReason; }

    // Exclude DocumentID parameter from writing loops when executing transactions to Firestore
    @Exclude
    public String getDocumentId() { return documentId; }

    // Standard structural setter methods mapping framework interactions cleanly
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
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}