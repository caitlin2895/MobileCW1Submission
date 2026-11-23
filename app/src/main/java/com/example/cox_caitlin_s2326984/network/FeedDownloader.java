package com.example.cox_caitlin_s2326984.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

// Downloads a DATEX II feed into a String so the parser can work on it.
// Network access is not allowed on the UI thread, so this must be called from a background thread
public class FeedDownloader {
    private String clientId;
    private String clientKey;

    // the Traffic Scotland API credentials, sent with every request
    public FeedDownloader(String clientId, String clientKey) {
        this.clientId = clientId;
        this.clientKey = clientKey;
    }

    // reads the whole feed at feedUrl line by line and returns it as one String
    public String download(String feedUrl) throws IOException
    {
        // StringBuilder instead of "result = result + inputLine", as the roadworks feed is
        // very large and adding to a String each time made the download much slower
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        String inputLine = "";

        URL url = new URL(feedUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        // add the authorisation header: "clientId:clientKey" Base64 encoded (same as the starter code)
        urlConnection.setRequestProperty("Authorization",
                Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
        try {
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            Log.e("FeedDownloader", "Error downloading " + feedUrl + ": " + e);
            throw e;   // pass the error on to the ViewModel's download thread
        } finally {
            urlConnection.disconnect(); // always close the connection, even if it failed
        }

        Log.d("FeedDownloader", "Downloaded " + result.length() + " characters from " + feedUrl);
        return result.toString();
    }
}
