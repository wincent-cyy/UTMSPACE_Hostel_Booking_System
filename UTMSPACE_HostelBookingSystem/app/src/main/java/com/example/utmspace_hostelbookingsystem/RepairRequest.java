package com.example.utmspace_hostelbookingsystem;

public class RepairRequest {
    private String documentId;
    private String requestId;
    private String issueType;      // 改为 issueType (原来是 itemName)
    private String priority;       // 改为 priority (原来是 urgency)
    private String description;
    private String status;
    private String roomId;
    private String roomType;       // 添加 roomType
    private String uid;
    private String name;           // 改为 name (原来是 staffName)
    private long createdAt;
    private long updatedAt;
    private String proofImage;
    private String completionPhoto;
    private String availableTime;  // 添加 availableTime
    private String contactPerson;  // 添加 contactPerson

    public RepairRequest() {}

    // Getters
    public String getDocumentId() { return documentId; }
    public String getIssueType() { return issueType; }
    public String getPriority() { return priority; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getRoomId() { return roomId; }
    public String getRoomType() { return roomType; }
    public String getUid() { return uid; }
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public String getProofImage() { return proofImage; }
    public String getAvailableTime() { return availableTime; }
    public String getContactPerson() { return contactPerson; }
    public String getCompletionPhoto() { return completionPhoto; }
    public String getRequestId() {return requestId;}

    // Setters
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setProofImage(String proofImage) { this.proofImage = proofImage; }
    public void setAvailableTime(String availableTime) { this.availableTime = availableTime; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public void setCompletionPhoto(String completionPhoto) { this.completionPhoto = completionPhoto; }
    public void setRequestId(String requestId) {this.requestId = requestId;}
}