package com.example.cox_caitlin_s2326984.model;

// One <situationRecord> inside a situation: its type, severity, comment and the
// <validity>, <impact> and <groupOfLocations> blocks (each in its own class)
public class SituationRecord {

    private String id;
    private String version;
    private String recordType;          // xsi:type, e.g. "Accident"
    private String detailType;          // e.g. <accidentType>, <roadMaintenanceType> value
    private String creationTime;        // not read from the feed at the moment
    private String versionTime;         // not read from the feed at the moment
    private String probability;
    private String severity;
    private String roadworksDuration;
    private String roadworksScale;
    private String comment = "";        // generalPublicComment text

    private Validity validity = new Validity();
    private Impact impact = new Impact();
    private SituationLocation location = new SituationLocation();


    public SituationRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getDetailType() {
        return detailType;
    }

    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getVersionTime() {
        return versionTime;
    }

    public void setVersionTime(String versionTime) {
        this.versionTime = versionTime;
    }

    public String getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = probability;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRoadworksDuration() {
        return roadworksDuration;
    }

    public void setRoadworksDuration(String roadworksDuration) {
        this.roadworksDuration = roadworksDuration;
    }

    public String getRoadworksScale() {
        return roadworksScale;
    }

    public void setRoadworksScale(String roadworksScale) {
        this.roadworksScale = roadworksScale;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // The feed can split a comment over more than one <value>, so add each part
    public void addToComment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (comment.isEmpty()) {
            comment = text.trim();
        } else {
            comment = comment + " " + text.trim();
        }
    }

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public Impact getImpact() {
        return impact;
    }

    public void setImpact(Impact impact) {
        this.impact = impact;
    }

    public SituationLocation getLocation() {
        return location;
    }

    public void setLocation(SituationLocation location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "SituationRecord{" +
                "id='" + id + '\'' +
                ", type='" + recordType + '\'' +
                ", detailType='" + detailType + '\'' +
                ", comment='" + comment + '\'' +
                ", roadworksDuration='" + roadworksDuration + '\'' +
                ",\n   " + validity +
                ",\n   " + impact +
                ",\n   " + location +
                '}';
    }
}
