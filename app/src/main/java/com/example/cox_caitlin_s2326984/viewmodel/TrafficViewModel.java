package com.example.cox_caitlin_s2326984.viewmodel;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cox_caitlin_s2326984.model.GeoPoint;
import com.example.cox_caitlin_s2326984.model.Situation;
import com.example.cox_caitlin_s2326984.model.SituationLocation;
import com.example.cox_caitlin_s2326984.network.FeedDownloader;
import com.example.cox_caitlin_s2326984.parser.TrafficXmlParser;

import java.io.IOException;
import java.util.ArrayList;


// ViewModel (MPD_ViewModel) shared by MainActivity and all the Fragments.
// It downloads and parses the feeds on a worker thread, holds the results,
// and does the road / distance searches, so the data survives screen rotation
public class TrafficViewModel extends ViewModel {

    // observable data - the Fragments observe these and update their widgets automatically
    private final MutableLiveData<ArrayList<Situation>> incidents;
    private final MutableLiveData<ArrayList<Situation>> roadworks;
    private final MutableLiveData<String> status;
    // the situation the user tapped in a list - kept here so it survives rotation
    private final MutableLiveData<Situation> selectedSituation;

    //DOWNLOADING FROM TRAFFIC SCOTLAND
    private String clientId = "";
    private String clientKey = "";
    // the two DATEX II feeds: incidents (unplanned events) and current roadworks
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    final String roadworksURL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    // stops a second download starting (e.g. auto update + Refresh) while one is running
    private boolean downloadInProgress = false;

    // results filled in by the worker thread, then handed to the LiveData on the UI thread
    private ArrayList<Situation> downloadedIncidents;
    private ArrayList<Situation> downloadedRoadworks;
    private String downloadError;

    // handler to pass the results from the worker thread back to the ui thread
    private Handler updateUIHandler = null;
    private final static int MESSAGE_UPDATE_STATUS = 1;  // msg.obj is a progress message
    private final static int MESS_DOWNLOAD_COMPLETE = 2; // both feeds done, msg.obj is the summary

    // search results - kept here so they survive rotation and the map can reuse them
    // (used by BOTH the road search and the distance search)
    private final MutableLiveData<ArrayList<Situation>> searchResults;
    private final MutableLiveData<String> searchMessage;
    private String lastSearch;   // what the user last typed, to put back in the box
    private String searchDescription = "";   // e.g. "on M8" or "within 10 km of Glasgow"

    // which map to show: true = every incident and roadworks ("Map" button),
    // false = only the search results ("Show results on map")
    // kept here so it survives rotation, like the other data
    private boolean mapShowsAll = false;

    // ---- search by distance ----
    // fixed places to measure from - the spec allows "fixed lat/long coordinates for simplicity"
    // (same order in all three arrays)
    public static final String[] PLACE_NAMES = {"Glasgow", "Edinburgh", "Aberdeen",
            "Dundee", "Inverness", "Stirling", "Perth"};
    private static final double[] PLACE_LATS = {55.8666, 55.9533, 57.1497,
            56.4620, 57.4778, 56.1165, 56.3950};
    private static final double[] PLACE_LONGS = {-4.2502, -3.1883, -2.0943,
            -2.9707, -4.2247, -3.9369, -3.4308};
    private int placeIndex = 0;              // which place the last distance search used
    private boolean distanceSearch = false;  // true if the current results came from a distance search

    public TrafficViewModel() {
        incidents = new MutableLiveData<>();
        roadworks = new MutableLiveData<>();
        status = new MutableLiveData<>();
        searchResults = new MutableLiveData<>();
        searchMessage = new MutableLiveData<>();
        status.setValue("Loading...");
        selectedSituation = new MutableLiveData<>();
        createUpdateUiHandler();
    }

    // getters return LiveData (read only) so only the ViewModel can change the values
    public LiveData<ArrayList<Situation>> getIncidents() {
        return incidents;
    }

    public LiveData<ArrayList<Situation>> getRoadworks() {
        return roadworks;
    }

    public LiveData<String> getStatus() {
        return status;
    }

    public LiveData<Situation> getSelectedSituation() {
        return selectedSituation;
    }

    public LiveData<ArrayList<Situation>> getSearchResults() {
        return searchResults;
    }

    public LiveData<String> getSearchMessage() {
        return searchMessage;
    }

    public String getLastSearch() {
        return lastSearch;
    }

    public String getSearchDescription() {
        return searchDescription;
    }

    public boolean isDistanceSearch() {
        return distanceSearch;
    }

    // set by MainActivity just before it opens the map
    public void setMapShowsAll(boolean showAll) {
        mapShowsAll = showAll;
    }

    public boolean getMapShowsAll() {
        return mapShowsAll;
    }

    // searches BOTH incidents and roadworks for a road name, ignoring case,
    // e.g. "m8" finds "M8" (trim / toLowerCase / contains: String methods from Lab 2 Task 2)
    public void searchByRoad(String road) {
        lastSearch = road;
        distanceSearch = false;
        searchDescription = "on " + road.trim();
        String searchText = road.trim().toLowerCase();
        ArrayList<Situation> results = new ArrayList<>();

        if (searchText.equals("")) {
            searchMessage.setValue("Please enter a road name, e.g. M8");
            searchResults.setValue(results);
            return;
        }
        if (incidents.getValue() == null || roadworks.getValue() == null) {
            searchMessage.setValue("The data is still loading - please try again in a moment");
            searchResults.setValue(results);
            return;
        }

        int incidentCount = 0;
        int roadworksCount = 0;
        for (Situation s : incidents.getValue()) {
            if (matchesRoad(s, searchText)) {
                results.add(s);
                incidentCount++;
            }
        }
        for (Situation s : roadworks.getValue()) {
            if (matchesRoad(s, searchText)) {
                results.add(s);
                roadworksCount++;
            }
        }

        if (results.size() == 0) {
            searchMessage.setValue("No incidents or roadworks found on " + road.trim());
        } else {
            searchMessage.setValue(incidentCount + " incidents and " + roadworksCount
                    + " roadworks found on " + road.trim());
        }
        searchResults.setValue(results);
    }

    // true if the road name contains the search text;
    // if the feed gave no road name, look in the comment instead
    private boolean matchesRoad(Situation s, String searchText) {
        String road = s.getRoadName();
        if (road != null) {
            return road.toLowerCase().contains(searchText);
        }
        String comment = s.getComment();
        return comment != null && comment.toLowerCase().contains(searchText);
    }

    // searches BOTH incidents and roadworks for everything within a distance (in km)
    // of one of the fixed places. The distance is typed by the user, so it is
    // converted from text to a number as in Lab 2 Task 3
    public void searchByDistance(int place, String distanceText) {
        ArrayList<Situation> results = new ArrayList<>();

        double maxKm;
        try {
            maxKm = Double.parseDouble(distanceText.trim());
        } catch (NumberFormatException e) {
            maxKm = -1;
        }
        if (maxKm <= 0) {
            searchMessage.setValue("Please enter a distance in km, e.g. 10");
            searchResults.setValue(results);
            return;
        }
        if (incidents.getValue() == null || roadworks.getValue() == null) {
            searchMessage.setValue("The data is still loading - please try again in a moment");
            searchResults.setValue(results);
            return;
        }

        placeIndex = place;
        distanceSearch = true;
        searchDescription = "within " + distanceText.trim() + " km of " + PLACE_NAMES[place];

        int incidentCount = 0;
        int roadworksCount = 0;
        for (Situation s : incidents.getValue()) {
            double km = getDistanceKm(s);
            if (km >= 0 && km <= maxKm) {
                results.add(s);
                incidentCount++;
            }
        }
        for (Situation s : roadworks.getValue()) {
            double km = getDistanceKm(s);
            if (km >= 0 && km <= maxKm) {
                results.add(s);
                roadworksCount++;
            }
        }

        if (results.size() == 0) {
            searchMessage.setValue("No incidents or roadworks found " + searchDescription);
        } else {
            searchMessage.setValue(incidentCount + " incidents and " + roadworksCount
                    + " roadworks found " + searchDescription);
        }
        searchResults.setValue(results);
    }

    // distance in km from the place chosen in the last distance search to a situation's
    // main point, or -1 if the situation has no coordinates.
    // Location.distanceBetween() is part of Android (android.location) - it puts the
    // distance in metres into results[0]
    public double getDistanceKm(Situation s) {
        SituationLocation location = s.getLocation();
        if (location == null) {
            return -1;
        }
        GeoPoint point = location.getMainPoint();
        if (point == null) {
            return -1;
        }
        float[] results = new float[1];
        Location.distanceBetween(PLACE_LATS[placeIndex], PLACE_LONGS[placeIndex],
                point.getLatitude(), point.getLongitude(), results);
        return results[0] / 1000.0;
    }

    // called by MainActivity when a list or search item is tapped (like setAmount() in ExchangeViewModel)
    public void selectSituation(Situation situation) {
        selectedSituation.setValue(situation);
    }

    // the Activity reads the keys from the manifest and passes them in
    public void setCredentials(String clientId, String clientKey) {
        this.clientId = clientId;
        this.clientKey = clientKey;
    }

    // called from the UI thread (button press or auto update timer)
    public void startDownload() {
        if (downloadInProgress) {
            Log.d("AutoUpdate", "download already running - skipped");
            return;
        }
        downloadInProgress = true;
        status.setValue("Downloading data, please wait...");
        // Run network access on a separate thread;
        new Thread(new Task()).start();
    }

    // create handler object linked to the UI thread's looper so handleMessage()
    // runs on the UI thread and is allowed to call setValue() on the LiveData
    private void createUpdateUiHandler() {
        if (updateUIHandler == null) {
            updateUIHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Log.d("UI thread", "I am the UI thread");
                    if (msg.what == MESSAGE_UPDATE_STATUS) {
                        status.setValue((String) msg.obj);
                    } else if (msg.what == MESS_DOWNLOAD_COMPLETE) {
                        // only replace the lists if that download worked, otherwise keep the old data
                        if (downloadedIncidents != null) {
                            incidents.setValue(downloadedIncidents);
                        }
                        if (downloadedRoadworks != null) {
                            roadworks.setValue(downloadedRoadworks);
                        }
                        status.setValue((String) msg.obj);
                        downloadInProgress = false; // ready for the next download
                    }
                }
            };
        }
    }

    // Need separate thread to access the internet resource over network
    private class Task implements Runnable {
        @Override
        public void run() {
            Log.d("MyTask", "in run");
            FeedDownloader downloader = new FeedDownloader(clientId, clientKey);
            TrafficXmlParser parser = new TrafficXmlParser();
            downloadedIncidents = null;
            downloadedRoadworks = null;
            downloadError = null;

            try {
                // 1. Unplanned events (incidents)
                sendStatus("Downloading incidents...");
                String incidentXml = downloader.download(unplannedEventsURL);
                downloadedIncidents = parser.parseData(incidentXml, Situation.INCIDENT);

                // 2. Roadworks
                sendStatus("Downloading roadworks (this can take a while)...");
                String roadworksXml = downloader.download(roadworksURL);
                downloadedRoadworks = parser.parseData(roadworksXml, Situation.ROADWORKS);
            } catch (IOException e) {
                Log.e("MyTask", "Download failed: " + e);
                downloadError = e.toString();
            }

            // Lab 3 test step: iterate through the collections and log each item
            if (downloadedIncidents != null) {
                for (Situation s : downloadedIncidents) {
                    Log.d("MyTag", s.toString());
                }
            }
            if (downloadedRoadworks != null) {
                for (Situation s : downloadedRoadworks) {
                    Log.d("MyTag", s.toString());
                }
            }

            // tell the UI thread the download has finished (whether it worked or not)
            // build message object
            Message message = new Message();
            message.what = MESS_DOWNLOAD_COMPLETE;
            message.obj = buildStatus();
            // send message to the main thread Handler
            updateUIHandler.sendMessage(message);
        }

        // send a short status update to the UI thread while the download is in progress
        private void sendStatus(String text) {
            Message message = new Message();
            message.what = MESSAGE_UPDATE_STATUS;
            message.obj = text;
            updateUIHandler.sendMessage(message);
        }

        // short summary line shown above the list when the download finishes
        private String buildStatus() {
            if (downloadError != null) {
                // friendly message for the user - the technical error is in Logcat
                return "Could not connect to Traffic Scotland. "
                        + "Check your internet connection and press Refresh data to try again.";
            }
            return downloadedIncidents.size() + " incidents, "
                    + downloadedRoadworks.size() + " roadworks";
        }
    }
}