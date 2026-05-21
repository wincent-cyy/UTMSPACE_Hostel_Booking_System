package com.example.utmspace_hostelbookingsystem;

public class Booking {
    // Primary Key
    private String bookingId;        // Document ID

    // Foreign Keys (名字必须与源Collection完全一致)
    private String uid;              // Foreign Key → Users.uid (名字一致)
    private String roomId;           // Foreign Key → Rooms.roomId (名字一致)

    // 从 Users 继承的快照数据
    private String name;
    private String matricNumber;
    private String phone;

    // 从 Rooms 继承的快照数据 (预订时的房间快照，防止房间信息被修改后影响历史记录)
    private String roomType;         // 从 Rooms.roomType 继承
    private double price;            // 从 Rooms.price 继承
    private String status;       // 从 Rooms.status 继承 (改名为 status 避免与 bookingStatus 混淆)
    private String location;         // 从 Rooms.location 继承

    // Booking specific fields
    private String checkInDate;
    private String leaseDuration;
    private String bookingStatus;    // Pending, Approved, Rejected, Paid (预订流程状态)
    private String rejectReason;
    private String paymentMethod;
    private long paymentTimestamp;
    private long createdAt;

    public Booking() {}

    // Getters
    public String getBookingId() { return bookingId; }
    public String getUid() { return uid; }
    public String getRoomId() { return roomId; }

    // Users 继承字段
    public String getName() { return name; }
    public String getMatricNumber() { return matricNumber; }
    public String getPhone() { return phone; }

    // Rooms 继承字段
    public String getRoomType() { return roomType; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }

    // Booking 特有字段
    public String getCheckInDate() { return checkInDate; }
    public String getLeaseDuration() { return leaseDuration; }
    public String getBookingStatus() { return bookingStatus; }
    public String getRejectReason() { return rejectReason; }
    public String getPaymentMethod() { return paymentMethod; }
    public long getPaymentTimestamp() { return paymentTimestamp; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setUid(String uid) { this.uid = uid; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    // Users 继承字段
    public void setName(String name) { this.name = name; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public void setPhone(String phone) { this.phone = phone; }

    // Rooms 继承字段
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setPrice(double price) { this.price = price; }
    public void setStatus(String status) { this.status = status; }
    public void setLocation(String location) { this.location = location; }

    // Booking 特有字段
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }
    public void setLeaseDuration(String leaseDuration) { this.leaseDuration = leaseDuration; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPaymentTimestamp(long paymentTimestamp) { this.paymentTimestamp = paymentTimestamp; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // 在 Booking.java 中添加这个方法
    public String getDisplayPrice() {
        double finalPrice = this.price;

        // 打印日志查看实际的值
        android.util.Log.d("BookingDebug", "Original price: " + this.price);
        android.util.Log.d("BookingDebug", "LeaseDuration: " + this.leaseDuration);

        // ✅ 检查判断条件是否匹配
        if (this.leaseDuration != null && this.leaseDuration.equalsIgnoreCase("2 Semesters (Full Academic Year)")) {
            finalPrice = this.price * 2;
            android.util.Log.d("BookingDebug", "Price doubled to: " + finalPrice);
        }

        return String.format("RM %.2f", finalPrice);
    }
}