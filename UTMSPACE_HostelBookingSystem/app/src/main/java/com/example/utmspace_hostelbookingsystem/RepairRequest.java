package com.example.utmspace_hostelbookingsystem;

public class RepairRequest {
    private String documentId;
    private String itemName;
    private String urgency;
    private String description;
    private String status;
    private String roomId;
    private String uid;           // ✅ 改为 uid（不是 userId）
    private String staffName;
    private long createdAt;
    private long updatedAt;
    private String proofImage;     // 存储 Base64 图片

    public RepairRequest() {}

    // Getters
    public String getDocumentId() { return documentId; }
    public String getItemName() { return itemName; }
    public String getUrgency() { return urgency; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getRoomId() { return roomId; }
    public String getUid() { return uid; }           // ✅ 改为 getUid
    public String getStaffName() { return staffName; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public String getProofImage() { return proofImage; }

    // Setters
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setUid(String uid) { this.uid = uid; }           // ✅ 改为 setUid
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setProofImage(String proofImage) { this.proofImage = proofImage; }
}