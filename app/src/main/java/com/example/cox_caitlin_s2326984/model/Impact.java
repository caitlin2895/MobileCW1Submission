package com.example.cox_caitlin_s2326984.model;

//holds the <impact> block of a situationRecord. Every field is optional in the feed so "not given" is -1 (numbers) or null (Strings)
public class Impact {
    private int delaySeconds = -1;
    private int lanesRestricted = -1;
    private int operationalLanes = -1;
    private double capacityRemaining = -1;
    private String trafficConstrictionType;

    public Impact() {
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    // The setters take the text from the XML and convert it to a number (Lab 2 Task 3),
    // like setBolt() in Thing processes its text. Stays -1 ("not given") if it isn't a number
    public void setDelaySeconds(String delaySeconds) {
        try {
            this.delaySeconds = (int) Double.parseDouble(delaySeconds.trim()); // e.g. "600.0"
        } catch (NumberFormatException e) {
            this.delaySeconds = -1;
        }
    }

    public int getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(String lanesRestricted) {
        try {
            this.lanesRestricted = Integer.parseInt(lanesRestricted.trim());
        } catch (NumberFormatException e) {
            this.lanesRestricted = -1;
        }
    }

    public int getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(String operationalLanes) {
        try {
            this.operationalLanes = Integer.parseInt(operationalLanes.trim());
        } catch (NumberFormatException e) {
            this.operationalLanes = -1;
        }
    }

    public double getCapacityRemaining() {
        return capacityRemaining;
    }

    public void setCapacityRemaining(String capacityRemaining) {
        try {
            this.capacityRemaining = Double.parseDouble(capacityRemaining.trim());
        } catch (NumberFormatException e) {
            this.capacityRemaining = -1;
        }
    }

    public String getTrafficConstrictionType() {
        return trafficConstrictionType;
    }

    public void setTrafficConstrictionType(String trafficConstrictionType) {
        this.trafficConstrictionType = trafficConstrictionType;
    }

    // Delay in minutes, or -1 if the feed didn't give one
    public int getDelayMinutes() {
        if (delaySeconds < 0) {
            return -1;
        }
        return delaySeconds / 60;
    }

    @Override
    public String toString() {
        return "Impact{" +
                "constriction='" + trafficConstrictionType + '\'' +
                ", delayMinutes=" + getDelayMinutes() +
                ", lanesRestricted=" + lanesRestricted +
                ", operationalLanes=" + operationalLanes +
                ", capacityRemaining=" + capacityRemaining +
                '}';
    }
}
