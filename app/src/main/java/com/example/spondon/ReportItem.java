package com.example.spondon;

public class ReportItem {
    private int id;
    private String category;
    private String title;
    private String description;
    private String location;
    private String status;
    private String imageUri;
    private double latitude;
    private double longitude;
    private String firebaseId;
    private String createdAt;

    public ReportItem(int id, String category, String title, String description,
                      String location, String status, String imageUri,
                      double latitude, double longitude, String firebaseId, String createdAt) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.description = description;
        this.location = location;
        this.status = status;
        this.imageUri = imageUri;
        this.latitude = latitude;
        this.longitude = longitude;
        this.firebaseId = firebaseId;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getImageUri() { return imageUri; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getFirebaseId() { return firebaseId; }
    public String getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}
