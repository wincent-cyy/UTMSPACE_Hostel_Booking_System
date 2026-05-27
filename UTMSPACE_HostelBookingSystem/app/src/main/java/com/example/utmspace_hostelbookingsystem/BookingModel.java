package com.example.utmspace_hostelbookingsystem;

public class BookingModel {
    private String documentId;
    private String bookingId;
    private String roomId;
    private String uid;
    private String name;           // 改成 name
    private String matricNumber;
    private String phone;
    private String status;         // Firestore 中没有这个！
    private String bookingStatus;  // 添加这个 - 实际的状态字段
    private String checkInDate;    // 改成 String 类型
    private String leaseDuration;
    private String installmentPlan;
    private String paymentMethod;
    private String location;
    private String roomType;
    private String rejectReason;
    private double price;          // 价格
    private double amountPaid;     // 已付金额
    private long createdAt;        // 创建时间戳
    private long paymentTimestamp; // 支付时间戳
    private long lastUpdated;

    public BookingModel() {
        // Default constructor required for Firestore
    }

    // Getters
    public String getDocumentId() { return documentId; }
    public String getBookingId() { return bookingId; }
    public String getRoomId() { return roomId; }
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getUserName() { return name; }  // 兼容旧代码
    public String getMatricNumber() { return matricNumber; }
    public String getPhone() { return phone; }
    public String getStatus() {
        // 优先使用 bookingStatus
        return bookingStatus != null ? bookingStatus : status;
    }
    public String getBookingStatus() { return bookingStatus; }
    public String getCheckInDate() { return checkInDate; }
    public long getCheckInDateLong() {
        // 如果需要一个 long 值，可以转换或返回 createdAt
        return createdAt;
    }
    public String getLeaseDuration() { return leaseDuration; }
    public String getInstallmentPlan() { return installmentPlan; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getLocation() { return location; }
    public String getRoomType() { return roomType; }
    public String getRejectReason() { return rejectReason; }
    public double getPrice() { return price; }
    public double getTotalPrice() { return price; }  // 兼容旧代码
    public double getAmountPaid() { return amountPaid; }
    public long getCreatedAt() { return createdAt; }
    public long getBookingDate() { return createdAt; }  // 兼容旧代码
    public long getPaymentTimestamp() { return paymentTimestamp; }
    public long getLastUpdated() { return lastUpdated; }
    public long getCheckOutDate() { return 0; }  // 没有这个字段，返回0

    // Setters
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setStatus(String status) { this.status = status; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }
    public void setLeaseDuration(String leaseDuration) { this.leaseDuration = leaseDuration; }
    public void setInstallmentPlan(String installmentPlan) { this.installmentPlan = installmentPlan; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setLocation(String location) { this.location = location; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public void setPrice(double price) { this.price = price; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setPaymentTimestamp(long paymentTimestamp) { this.paymentTimestamp = paymentTimestamp; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setCheckOutDate(long checkOutDate) { /* 不需要 */ }
    public void setTotalPrice(double totalPrice) { this.price = totalPrice; }
    public void setUserName(String userName) { this.name = userName; }
    public void setUserEmail(String userEmail) { /* 可选 */ }
    public void setBookingDate(long bookingDate) { this.createdAt = bookingDate; }
}