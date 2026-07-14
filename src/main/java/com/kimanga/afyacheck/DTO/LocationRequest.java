package com.kimanga.afyacheck.DTO;

public class LocationRequest {
    private Double latitude;
    private Double longitude;
    private Double radius = 5.0; // default 5km
    private String type; // filter by type

    // getters and setters
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getRadius() { return radius; }
    public void setRadius(Double radius) { this.radius = radius; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}