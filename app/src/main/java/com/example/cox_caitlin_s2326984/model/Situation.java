package com.example.cox_caitlin_s2326984.model;
import com.example.cox_caitlin_s2326984.R;

import java.util.ArrayList;

// One <situation> from the feed - an incident or a roadworks.
// It holds one or more SituationRecords; the first one holds the main details
public class Situation {

    // The two possible values for feedType
    public static final String INCIDENT = "Incident";
    public static final String ROADWORKS = "Roadworks";

    private String id;
    private String version;
    private String feedType;   // INCIDENT or ROADWORKS - which feed it came from
    private String overallSeverity;
    private ArrayList<SituationRecord> records = new ArrayList<>();

    public Situation(String feedType) {
        this.feedType = feedType;
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

    public String getFeedType() {
        return feedType;
    }

    public void setFeedType(String feedType) {
        this.feedType = feedType;
    }

    public String getOverallSeverity() {
        return overallSeverity;
    }

    public void setOverallSeverity(String overallSeverity) {
        this.overallSeverity = overallSeverity;
    }

    public ArrayList<SituationRecord> getRecords() {
        return records;
    }

    public void addRecord(SituationRecord record) {
        records.add(record);
    }

    // The first record holds the main information (null if there are none)
    public SituationRecord getMainRecord() {
        if (records.isEmpty()) {
            return null;
        }
        return records.get(0);
    }
    // ---- Shortcut getters for the list / search / map screens ----

    // xsi:type of the main record, e.g. "Accident" or "MaintenanceWorks"
    public String getType() {
        if (getMainRecord() == null) {
            return "Unknown";
        }
        return getMainRecord().getRecordType();
    }

    // the type with a space before each capital, e.g. "MaintenanceWorks" -> "Maintenance Works"
    public String getDisplayType() {
        String type = getType();
        if (type == null) {
            return "Unknown";
        }
        String result = "";
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (i > 0 && c >= 'A' && c <= 'Z') {
                result = result + " ";
            }
            result = result + c;
        }
        return result;
    }

    // icon for this type of situation, shown in the lists
    // (Font Awesome Free icons, CC BY 4.0)
    public int getIconId() {
        String type = getType();
        if (type == null) {
            return R.drawable.ic_closure;
        }
        if (type.equals("Accident")) {
            return R.drawable.ic_crash;
        }
        if (type.equals("AbnormalTraffic") || type.equals("VehicleObstruction")) {
            return R.drawable.ic_car;
        }
        if (type.equals("WeatherRelatedRoadConditions") || type.equals("PoorEnvironmentConditions")) {
            return R.drawable.ic_weather;
        }
        if (type.equals("GeneralNetworkManagement") || type.equals("ReroutingManagement")) {
            return R.drawable.ic_route;
        }
        if (type.equals("MaintenanceWorks")) {
            return R.drawable.ic_roadworks;
        }
        if (type.equals("ConstructionWorks")) {
            return R.drawable.ic_construction;
        }
        if (type.equals("GeneralObstruction") || type.equals("AnimalPresenceObstruction") || type.equals("EnvironmentalObstruction") || type.equals("InfrastructureDamageObstruction")) {
            return R.drawable.ic_obstruction;
        }
        // road / lane closures and anything else
        return R.drawable.ic_closure;
    }

    // the generalPublicComment text ("" if there is no record)
    public String getComment() {
        if (getMainRecord() == null) {
            return "";
        }
        return getMainRecord().getComment();
    }

    // road number, e.g. "M8" (null if none was found)
    public String getRoadName() {
        if (getMainRecord() == null) {
            return null;
        }
        return getMainRecord().getLocation().getRoadName();
    }

    public Validity getValidity() {
        if (getMainRecord() == null) {
            return null;
        }
        return getMainRecord().getValidity();
    }

    public SituationLocation getLocation() {
        if (getMainRecord() == null) {
            return null;
        }
        return getMainRecord().getLocation();
    }

    @Override
    public String toString() {
        String result = "Situation{" +
                "feedType='" + feedType + '\'' +
                ", id='" + id + '\'' +
                ", severity='" + overallSeverity + '\'' +
                ", records=" + records.size() +
                '}';
        for (SituationRecord r : records) {
            result = result + "\n  " + r.toString();
        }
        return result;
    }
}
