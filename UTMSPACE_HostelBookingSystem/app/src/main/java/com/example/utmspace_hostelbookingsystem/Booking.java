package com.example.utmspace_hostelbookingsystem;

import java.io.Serializable;

// Implementing Serializable allows you to pass this object between Activities
public class Booking implements java.io.Serializable {
    private String roomName;
    private String date;
    private String status;
    private String price;

    // Constructor
    public Booking(String roomName, String date, String status, String price) {
        this.roomName = roomName;
        this.date = date;
        this.status = status;
        this.price = price;
    }

    // Getters
    public String getRoomName() { return roomName; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public String getPrice() { return price; }

    // Setters (Optional, but helpful if you need to update data later)
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setDate(String date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
    public void setPrice(String price) { this.price = price; }
}