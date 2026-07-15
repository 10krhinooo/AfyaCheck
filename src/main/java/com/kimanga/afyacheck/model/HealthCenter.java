//package com.kimanga.afyacheck.model;
//
//import jakarta.persistence.*;
//
//
//@Entity
//@Table(name = "health_centers")
//public class HealthCenter {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false)
//    private String name;
//
//    @Column(nullable = false)
//    private String address;
//
//    @Column(nullable = false)
//    private Double latitude;
//
//    @Column(nullable = false)
//    private Double longitude;
//
//    private String type; // HOSPITAL, CLINIC, PHARMACY, URGENT_CARE, etc.
//    private String phone;
//    private String hours;
//    private String website;
//
//    // Additional fields you might want
//    private Boolean emergencyServices;
//    private String specialties;
//
//    // constructors
//    public HealthCenter() {}
//
//    public HealthCenter(String name, String address, Double latitude, Double longitude,
//                        String type, String phone) {
//        this.name = name;
//        this.address = address;
//        this.latitude = latitude;
//        this.longitude = longitude;
//        this.type = type;
//        this.phone = phone;
//    }
//
//    // getters and setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getAddress() { return address; }
//    public void setAddress(String address) { this.address = address; }
//
//    public Double getLatitude() { return latitude; }
//    public void setLatitude(Double latitude) { this.latitude = latitude; }
//
//    public Double getLongitude() { return longitude; }
//    public void setLongitude(Double longitude) { this.longitude = longitude; }
//
//    public String getType() { return type; }
//    public void setType(String type) { this.type = type; }
//
//    public String getPhone() { return phone; }
//    public void setPhone(String phone) { this.phone = phone; }
//
//    public String getHours() { return hours; }
//    public void setHours(String hours) { this.hours = hours; }
//
//    public String getWebsite() { return website; }
//    public void setWebsite(String website) { this.website = website; }
//
//    public Boolean getEmergencyServices() { return emergencyServices; }
//    public void setEmergencyServices(Boolean emergencyServices) { this.emergencyServices = emergencyServices; }
//
//    public String getSpecialties() { return specialties; }
//    public void setSpecialties(String specialties) { this.specialties = specialties; }
//}
