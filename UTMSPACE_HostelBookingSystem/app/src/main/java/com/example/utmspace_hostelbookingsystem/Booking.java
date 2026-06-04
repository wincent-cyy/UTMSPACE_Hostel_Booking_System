package com.example.utmspace_hostelbookingsystem;

public class Booking {
    // Primary Key
    private String bookingId;        // Document ID

    // Foreign Keys
    private String uid;              // Foreign Key → Users.uid
    private String roomId;           // Foreign Key → Rooms.roomId

    // 从 Users 继承的快照数据
    private String name;
    private String matricNumber;
    private String phone;
    private String email;            // 添加 email
    private String programme;        // 添加 programme
    private String currentSemester;  // 添加 currentSemester

    // 从 Rooms 继承的快照数据
    private String roomType;
    private double price;
    private String status;
    private String location;

    // Booking specific fields
    private String checkInDate;
    private String leaseDuration;
    private String bookingStatus;
    private String rejectReason;
    private String paymentMethod;
    private long paymentTimestamp;
    private long createdAt;
    private int duration;            // 添加学期数 (1 or 2)
    private double pricePerSemester; // 添加每学期价格

    public Booking() {}

    // ========== Getters ==========
    public String getBookingId() { return bookingId; }
    public String getUid() { return uid; }
    public String getRoomId() { return roomId; }

    // Users 继承字段
    public String getName() { return name; }
    public String getMatricNumber() { return matricNumber; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getProgramme() { return programme; }
    public String getCurrentSemester() { return currentSemester; }

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
    public int getDuration() { return duration; }
    public double getPricePerSemester() { return pricePerSemester; }

    // ========== Setters ==========
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setUid(String uid) { this.uid = uid; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    // Users 继承字段
    public void setName(String name) { this.name = name; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setProgramme(String programme) { this.programme = programme; }
    public void setCurrentSemester(String currentSemester) { this.currentSemester = currentSemester; }

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
    public void setDuration(int duration) { this.duration = duration; }
    public void setPricePerSemester(double pricePerSemester) { this.pricePerSemester = pricePerSemester; }

    /**
     * 获取显示价格
     * 根据学期数计算总价格
     */
    public String getDisplayPrice() {
        double finalPrice = calculateTotalPrice();
        return String.format("RM %.2f", finalPrice);
    }

    /**
     * 获取总价格（数字）
     */
    public double getTotalPrice() {
        return calculateTotalPrice();
    }

    /**
     * 计算总价格的核心方法
     */
    private double calculateTotalPrice() {
        // 优先使用 duration + pricePerSemester (更可靠)
        if (duration > 1 && pricePerSemester > 0) {
            return pricePerSemester * duration;
        }

        // 方法2: 使用 leaseDuration 判断
        if (leaseDuration != null) {
            if (leaseDuration.equalsIgnoreCase("2 Semesters") ||
                    leaseDuration.contains("2") ||
                    leaseDuration.equalsIgnoreCase("2 Semesters")) {
                return price * 2;
            }
        }

        // 默认返回单学期价格
        return price;
    }

    /**
     * 获取状态显示文本（带中文支持）
     */
    public String getStatusDisplayText() {
        if (bookingStatus == null) return "Pending";

        switch (bookingStatus.toLowerCase()) {
            case "pending": return "Pending";
            case "approved": return "Approved";
            case "rejected": return "Rejected";
            case "paid": return "Paid";
            default: return bookingStatus;
        }
    }

    /**
     * 判断是否可以取消
     */
    public boolean isCancellable() {
        return "Pending".equalsIgnoreCase(bookingStatus);
    }

    /**
     * 判断是否可以支付
     */
    public boolean isPayable() {
        return "Approved".equalsIgnoreCase(bookingStatus);
    }

    /**
     * 判断是否已支付
     */
    public boolean isPaid() {
        return "Paid".equalsIgnoreCase(bookingStatus);
    }

    /**
     * 判断是否被拒绝
     */
    public boolean isRejected() {
        return "Rejected".equalsIgnoreCase(bookingStatus);
    }

    /**
     * 获取房间显示名称
     */
    public String getRoomDisplayName() {
        if (roomId != null && !roomId.isEmpty()) {
            return roomId;
        }
        if (location != null && !location.isEmpty()) {
            return location;
        }
        return "N/A";
    }

    /**
     * 获取学生显示名称
     */
    public String getStudentDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        if (matricNumber != null && !matricNumber.isEmpty()) {
            return matricNumber;
        }
        return "N/A";
    }

    /**
     * 获取格式化日期
     */
    public String getFormattedCreatedDate() {
        if (createdAt > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(createdAt));
        }
        return "N/A";
    }
}