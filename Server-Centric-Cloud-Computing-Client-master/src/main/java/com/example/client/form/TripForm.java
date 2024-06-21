package com.example.client.form;

public class TripForm {

    private int tripId;
    private Integer userId;  // Change from int to Integer to allow null
    private String location;
    private String startdate;
    private String enddate;

    private String weather;

    // Constructor with parameters
    public TripForm(int tripId, Integer userId, String location, String startdate, String enddate, String weather) {
        this.tripId = tripId;
        this.userId = userId;
        this.location = location;
        this.startdate = startdate;
        this.enddate = enddate;
        this.weather = weather;
    }

    // Default constructor with no parameters
    public TripForm() {
    }

    // Getters and setters

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartDate() {
        return startdate;
    }

    public void setStartDate(String startdate) {
        this.startdate = startdate;
    }

    public String getEndDate() {
        return enddate;
    }

    public void setEndDate(String enddate) {
        this.enddate = enddate;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }
}
