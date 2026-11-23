package com.example.cox_caitlin_s2326984.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cox_caitlin_s2326984.R;
import com.example.cox_caitlin_s2326984.model.GeoPoint;
import com.example.cox_caitlin_s2326984.model.Situation;
import com.example.cox_caitlin_s2326984.model.SituationLocation;
import com.example.cox_caitlin_s2326984.viewmodel.TrafficViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

// Map screen (Lab 7 / MPD_06c_Maps). It has two uses, picked by a flag in the ViewModel:
//  - "Map" button on the top bar        -> ALL incidents and roadworks
//  - "Show results on map" on Search    -> only the results of the last search (road or distance)
// Each situation gets a marker at its main point
public class MapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private TextView mapTitle;
    private Button mapBackButton;
    private TrafficViewModel viewModel;
    private LatLng firstPoint;          // where the camera goes
    private boolean cameraMoved;        // only move the camera once, not on every auto update

    // the "back" callback is the same one DetailsFragment uses - MainActivity already
    // implements it and onBackToList() puts the search screen back (MPD_6a_Fragments_commUI)
    private DetailsFragment.DetailsBackListener listener; //reference to main activity

    public MapFragment() {} // Required empty public constructor

    //Stores reference to the main activity for the interface's callback
    @Override
    public void onAttach(Context context) { //context is a ref to creator
        super.onAttach(context);
        if (context instanceof DetailsFragment.DetailsBackListener)
            listener = (DetailsFragment.DetailsBackListener) context;
        else
            throw new RuntimeException(context.toString()
                    + " must implement DetailsBackListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        // handle the fragment's own widgets
        mapTitle = (TextView) v.findViewById(R.id.mapTitle);
        mapBackButton = (Button) v.findViewById(R.id.mapBackButton);
        mapBackButton.setOnClickListener(this);
        cameraMoved = false;

        //ViewModelProvider gets the ViewModel - getActivity() so this Fragment
        //reconnects to the SAME ViewModel that MainActivity created
        ViewModelProvider provider = new ViewModelProvider(getActivity());
        viewModel = provider.get(TrafficViewModel.class);

        // "Back to search" only makes sense when we came from the Search screen
        // (setVisibility() from MPD_02a_Layouts)
        if (viewModel.getMapShowsAll()) {
            mapBackButton.setVisibility(View.GONE);
        } else {
            mapBackButton.setVisibility(View.VISIBLE);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used (Lab 7).
        // The map fragment is INSIDE this Fragment, so it is found with getChildFragmentManager()
        // instead of getSupportFragmentManager() as in the lab's MapsActivity
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return v;
    }

    // called when the map is ready to be used (MPD_06c_Maps)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true); // + / - buttons, as in Lab 7 Task 4

        //This uses MutableLiveData to "connect" directly to observed data
        if (viewModel.getMapShowsAll()) {
            // redraw whenever either list changes (start-up download or auto update)
            viewModel.getIncidents().observe(this, list -> showAll());
            viewModel.getRoadworks().observe(this, list -> showAll());
        } else {
            viewModel.getSearchResults().observe(this, this::showResults);
        }
    }

    // "Map" button: every incident AND every roadworks
    private void showAll() {
        ArrayList<Situation> incidents = viewModel.getIncidents().getValue();
        ArrayList<Situation> roadworks = viewModel.getRoadworks().getValue();
        if (incidents == null || roadworks == null) {
            mapTitle.setText("The data is still loading - please wait");
            return;
        }

        mMap.clear(); // remove any old markers
        firstPoint = null;
        int shownIncidents = addMarkers(incidents);
        int shownRoadworks = addMarkers(roadworks);
        mapTitle.setText(shownIncidents + " incidents and " + shownRoadworks
                + " roadworks - tap a marker for details");
        moveCamera();
    }

    // "Show results on map": only the results of the last search (road or distance)
    private void showResults(ArrayList<Situation> results) {
        mMap.clear(); // remove any old markers
        firstPoint = null;
        int shown = addMarkers(results);

        // tell the user what they are looking at
        if (results.size() == 0) {
            mapTitle.setText("No search results to show - search for a road first");
        } else if (shown == 0) {
            mapTitle.setText("None of the results " + viewModel.getSearchDescription()
                    + " have map coordinates");
        } else {
            mapTitle.setText(shown + " of " + results.size() + " results "
                    + viewModel.getSearchDescription() + " - tap a marker for details");
        }
        moveCamera();
    }

    // adds one marker per situation that has coordinates, returns how many were added
    private int addMarkers(ArrayList<Situation> situations) {
        int shown = 0;
        for (Situation s : situations) {
            SituationLocation location = s.getLocation();
            if (location != null) {
                GeoPoint point = location.getMainPoint();
                if (point != null) {
                    String road = s.getRoadName();
                    if (road == null) {
                        road = "Location not given";
                    }
                    // marker title says what it is, e.g. "Accident - M8"
                    LatLng position = new LatLng(point.getLatitude(), point.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(position)
                            .title(s.getDisplayType() + " - " + road)
                            .snippet(s.getComment()));
                    if (firstPoint == null) {
                        firstPoint = position;
                    }
                    shown++;
                }
            }
        }
        return shown;
    }

    // move the camera to the first marker (MPD_06c_Maps) - only the first time,
    // so an auto update doesn't jump the map away from where the user is looking
    private void moveCamera() {
        if (firstPoint != null && !cameraMoved) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(firstPoint));
            cameraMoved = true;
        }
    }

    // "Back to search" button
    public void onClick(View aview) {
        if (aview == mapBackButton) {
            //SEND BACK TO MAIN ACTIVITY VIA CALLBACK
            listener.onBackToList();
        }
    }
}