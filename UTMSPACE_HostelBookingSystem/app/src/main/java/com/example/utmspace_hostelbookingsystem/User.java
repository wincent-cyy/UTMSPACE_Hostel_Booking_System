package com.example.utmspace_hostelbookingsystem;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String phone;
    private String gender;
    private String emergencyContact;
    private String profilePictureBase64;

    public User() {}

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getProfilePictureBase64() { return profilePictureBase64; }

    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGender(String gender) { this.gender = gender; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public void setProfilePictureBase64(String profilePictureBase64) { this.profilePictureBase64 = profilePictureBase64; }
}