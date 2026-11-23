package com.example.cox_caitlin_s2326984.model;

import java.util.ArrayList;

// The <groupOfLocations> block of a situationRecord: where the situation is.
// Linear locations have a "from" and a "to" point, Point locations have a single point
public class SituationLocation {
    private String locationType;        // "Linear" or "Point"
    private GeoPoint from;              // start of a stretch of road
    private GeoPoint to;                // end of a stretch of road
    private GeoPoint point;             // a single point
    private GeoPoint displayPoint;      // <locationForDisplay> - the point the feed suggests for maps
    private String roadName;            // road number, e.g. "M8"
    private String direction;           // tpegDirection, e.g. "northBound"
    // every place descriptor found, as "type: value", e.g. "linkName: M8"
    private ArrayList<String> descriptors = new ArrayList<>();


    public SituationLocation() {}

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public GeoPoint getFrom() {
        return from;
    }

    public void setFrom(GeoPoint from) {
        this.from = from;
    }

    public GeoPoint getTo() {
        return to;
    }

    public void setTo(GeoPoint to) {
        this.to = to;
    }

    public GeoPoint getPoint() {
        return point;
    }

    public void setPoint(GeoPoint point) {
        this.point = point;
    }

    public GeoPoint getDisplayPoint() {
        return displayPoint;
    }

    public void setDisplayPoint(GeoPoint displayPoint) {
        this.displayPoint = displayPoint;
    }

    public String getRoadName() {
        return roadName;
    }

    // Further String processing in the setter, like setBolt() in Thing (Lab 3):
    // splits the text at spaces (like reverseWords() in Lab 2) and keeps only the first
    // road number found, e.g. "M8", "A9" or "A720" (M, A or B followed by a whole number).
    // If the text has no road number, roadName stays null.
    public void setRoadName(String text) {
        this.roadName = null;
        if (text == null) {
            return;
        }
        String[] words = text.split(" ");
        for (String word : words) {
            // remove punctuation stuck to the word, e.g. "M8," or "(A9)"
            word = word.replace(",", "").replace(".", "").replace(":", "")
                    .replace(";", "").replace("(", "").replace(")", "");
            if (word.length() >= 2 && word.length() <= 5
                    && (word.charAt(0) == 'M' || word.charAt(0) == 'A' || word.charAt(0) == 'B')) {
                try {
                    // skip the 1st character, parse the rest for an integer (same as setBolt())
                    int number = Integer.parseInt(word.substring(1));
                    if (number > 0) {
                        this.roadName = word;
                        return;
                    }
                } catch (NumberFormatException e) {
                    // not a road number, try the next word
                }
            }
        }
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public ArrayList<String> getDescriptors() {
        return descriptors;
    }

    public void addDescriptor(String type, String value) {
        descriptors.add(type + ": " + value);
    }

    // True when we have both ends of a stretch of road (draw as a line on the map)
    public boolean isLinear() {
        return from != null && to != null && from.hasCoordinates() && to.hasCoordinates();
    }

    // Best single point for map markers / distance search (null if none found)
    public GeoPoint getMainPoint() {
        if (displayPoint != null && displayPoint.hasCoordinates()) {
            return displayPoint;
        }
        if (point != null && point.hasCoordinates()) {
            return point;
        }
        if (from != null && from.hasCoordinates()) {
            return from;
        }
        if (to != null && to.hasCoordinates()) {
            return to;
        }
        return null;
    }

    @Override
    public String toString() {
        return "SituationLocation{" +
                "type='" + locationType + '\'' +
                ", road='" + roadName + '\'' +
                ", direction='" + direction + '\'' +
                ", from=" + from +
                ", to=" + to +
                ", point=" + point +
                ", displayPoint=" + displayPoint +
                ", descriptors=" + descriptors +
                '}';
    }
}
