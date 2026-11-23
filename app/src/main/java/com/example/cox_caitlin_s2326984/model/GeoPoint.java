package com.example.cox_caitlin_s2326984.model;

//one lat/long point taken from a <pointCoordinates> block in the feed
//Linear situations have two of these ("from" and "to"); point situations have one
public class GeoPoint {
    // 0 means "not found" - every point in Scotland has non-zero lat/long
    private double latitude = 0;
    private double longitude = 0;
    private String name;   // e.g. junction / place name, like "J14 Provan"

    public GeoPoint(){ }

    public double getLatitude() {
        return latitude;
    }

    // Takes the text from the XML and converts it to a number (Lab 2 Task 3),
    // in the setter like setBolt() in Thing. Stays 0 ("not found") if it isn't a number
    public void setLatitude(String latitude) {
        try {
            this.latitude = Double.parseDouble(latitude.trim());
        } catch (NumberFormatException e) {
            this.latitude = 0;
        }
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        try {
            this.longitude = Double.parseDouble(longitude.trim());
        } catch (NumberFormatException e) {
            this.longitude = 0;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // True only when both coordinates were found in the XML
    public boolean hasCoordinates() {
        return latitude != 0 && longitude != 0;
    }

    @Override
    public String toString() {
        return "GeoPoint{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
