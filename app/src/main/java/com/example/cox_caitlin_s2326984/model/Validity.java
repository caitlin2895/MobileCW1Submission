package com.example.cox_caitlin_s2326984.model;
import android.util.Log;

//holds the <validity> block: status plus overallStartTime / overallEndTime
//the raw date Strings are broken down into year / month / day / hour / minute numbers so the app can work out durations (used for the colour-coded roadworks)
public class Validity {
    private String validityStatus;

    private String startTime;          // raw text from the feed
    private int startYear, startMonth, startDay, startHour, startMinute;
    private boolean hasStart = false;  // true once a start date has been read correctly

    private String endTime;
    private int endYear, endMonth, endDay, endHour, endMinute;
    private boolean hasEnd = false;    // false when the feed gives no end date

    /**
     * Default constructor
     */
    public Validity() {
    }

    public String getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(String validityStatus) {
        this.validityStatus = validityStatus;
    }

    public String getStartTime() {
        return startTime;
    }

    // Stores the start date text, e.g. "2026-11-08T21:05:00Z", and also extracts the year, month, day, hour and minute numbers
    public void setStartTime(String startTime) {
        this.startTime = startTime;
        try {
            startYear = Integer.parseInt(startTime.substring(0, 4));
            startMonth = Integer.parseInt(startTime.substring(5, 7));
            startDay = Integer.parseInt(startTime.substring(8, 10));
            startHour = Integer.parseInt(startTime.substring(11, 13));
            startMinute = Integer.parseInt(startTime.substring(14, 16));
            hasStart = true;
        } catch (NumberFormatException e) {
            Log.e("Validity", "Error parsing start time: " + startTime);
            hasStart = false;
        } catch (StringIndexOutOfBoundsException e) {
            Log.e("Validity", "Start time too short: " + startTime);
            hasStart = false;
        }
    }

    public String getEndTime() {
        return endTime;
    }

    // Same as setStartTime(), for the end date
    public void setEndTime(String endTime) {
        this.endTime = endTime;
        try {
            endYear = Integer.parseInt(endTime.substring(0, 4));
            endMonth = Integer.parseInt(endTime.substring(5, 7));
            endDay = Integer.parseInt(endTime.substring(8, 10));
            endHour = Integer.parseInt(endTime.substring(11, 13));
            endMinute = Integer.parseInt(endTime.substring(14, 16));
            hasEnd = true;
        } catch (NumberFormatException e) {
            Log.e("Validity", "Error parsing end time: " + endTime);
            hasEnd = false;
        } catch (StringIndexOutOfBoundsException e) {
            Log.e("Validity", "End time too short: " + endTime);
            hasEnd = false;
        }
    }

    public boolean hasStart() {
        return hasStart;
    }

    public boolean hasEnd() {
        return hasEnd;
    }


    // How long the situation lasts, in hours (-1 if start or end is missing).
    // Turns each date into "days since 1 Jan 2000", then takes the difference
    public int getDurationHours() {
        if (!hasStart || !hasEnd) {
            return -1;
        }
        int startDays = daysSince2000(startYear, startMonth, startDay);
        int endDays = daysSince2000(endYear, endMonth, endDay);

        int startMinutes = startDays * 24 * 60 + startHour * 60 + startMinute;
        int endMinutes = endDays * 24 * 60 + endHour * 60 + endMinute;

        return (endMinutes - startMinutes) / 60;
    }

    // How long the situation lasts, in whole days (-1 if start or end is missing)
    public int getDurationDays() {
        int hours = getDurationHours();
        if (hours < 0) {
            return -1;
        }
        return hours / 24;
    }

    // Counts the days from 1 Jan 2000 to the given date
    private int daysSince2000(int year, int month, int day) {
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int days = 0;

        // whole years (a leap year has 366 days - every 4th year is correct from 2000 to 2099)
        for (int y = 2000; y < year; y++) {
            if (y % 4 == 0) {
                days = days + 366;
            } else {
                days = days + 365;
            }
        }
        // whole months in this year
        for (int m = 1; m < month; m++) {
            days = days + daysInMonth[m - 1];
            if (m == 2 && year % 4 == 0) {
                days = days + 1;   // 29th of February
            }
        }
        // days in this month
        days = days + day;
        return days;
    }

    // "2026-11-08T21:05:00Z" -> "08/11/2026 21:05"
    public String getFormattedStart() {
        if (!hasStart) {
            return "Unknown";
        }
        return formatDate(startTime);
    }

    // same format as getFormattedStart(), or "Not specified" if there is no end date
    public String getFormattedEnd() {
        if (!hasEnd) {
            return "Not specified";
        }
        return formatDate(endTime);
    }

    // picks the parts out of "yyyy-MM-ddTHH:mm..." and puts them in UK order
    private String formatDate(String date) {
        String year = date.substring(0, 4);
        String month = date.substring(5, 7);
        String day = date.substring(8, 10);
        String time = date.substring(11, 16);
        return day + "/" + month + "/" + year + " " + time;
    }

    @Override
    public String toString() {
        return "Validity{" +
                "status='" + validityStatus + '\'' +
                ", start=" + getFormattedStart() +
                ", end=" + getFormattedEnd() +
                ", durationHours=" + getDurationHours() +
                '}';
    }
}
