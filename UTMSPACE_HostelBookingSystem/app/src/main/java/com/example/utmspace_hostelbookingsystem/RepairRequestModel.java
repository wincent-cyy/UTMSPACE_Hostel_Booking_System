package com.example.utmspace_hostelbookingsystem;

public class RepairRequestModel {
    private String documentId;
    private String requestId;
    private String roomId;
    private String roomType;           // 添加房间类型
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String issueType;
    private String description;
    private String status;
    private long createdAt;
    private long startedAt;
    private long completedAt;
    private long lastUpdated;
    private String priority;
    private String proofImage;
    private String itemName;
    private String urgency;
    private String staffName;
    private String availableTime;      // 添加可用时间
    private String contactPerson;      // 添加联系人
    private String completionPhoto;    // 添加完成证明图片

    public RepairRequestModel() {
        // Default constructor required for Firestore
    }

    // ========== Getters ==========
    public String getDocumentId() { return documentId; }
    public String getRequestId() { return requestId; }
    public String getRoomId() { return roomId; }
    public String getRoomType() { return roomType; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getIssueType() { return issueType; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getStartedAt() { return startedAt; }
    public long getCompletedAt() { return completedAt; }
    public long getLastUpdated() { return lastUpdated; }
    public String getPriority() { return priority; }
    public String getProofImage() { return proofImage; }
    public String getItemName() { return itemName; }
    public String getUrgency() { return urgency; }
    public String getStaffName() { return staffName; }
    public String getAvailableTime() { return availableTime; }
    public String getContactPerson() { return contactPerson; }
    public String getCompletionPhoto() { return completionPhoto; }

    // ========== Setters ==========
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setProofImage(String proofImage) { this.proofImage = proofImage; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public void setAvailableTime(String availableTime) { this.availableTime = availableTime; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public void setCompletionPhoto(String completionPhoto) { this.completionPhoto = completionPhoto; }
}