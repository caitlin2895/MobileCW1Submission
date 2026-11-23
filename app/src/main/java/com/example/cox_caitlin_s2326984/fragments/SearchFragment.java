package com.example.cox_caitlin_s2326984.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cox_caitlin_s2326984.R;
import com.example.cox_caitlin_s2326984.adapter.CustomAdapter;
import com.example.cox_caitlin_s2326984.model.Situation;
import com.example.cox_caitlin_s2326984.viewmodel.TrafficViewModel;

import java.util.ArrayList;

// Search screen: find incidents AND roadworks
//  - on a road (e.g. "M8"), or
//  - within a distance (km) of a chosen place (Spinner, MPD_03a_MoreWidgets)
// The filtering is done in the ViewModel, so the map can reuse the results
public class SearchFragment extends Fragment
        implements View.OnClickListener, AdapterView.OnItemClickListener {

    private EditText roadInput;
    private Button searchButton;
    private Button mapButton;
    private Spinner placeSpinner;
    private EditText distanceInput;
    private Button nearButton;
    private TextView resultsText;
    private ListView listView;
    private TrafficViewModel viewModel;
    private ArrayList<Situation> resultsList = new ArrayList<>(); // the results shown, same order as the rows

    // callbacks to MainActivity (MPD_6a_Fragments_commUI)
    public interface SearchSelectedListener { //interface definition
        void onSituationSelected(Situation situation); //callback method - a result was tapped
        void onShowMap(); //callback method - "Show results on map" was pressed
    }
    private SearchSelectedListener listener; //reference to main activity

    public SearchFragment() {} // Required empty public constructor

    //Stores reference to the main activity for the interface's callback
    @Override
    public void onAttach(Context context) { //context is a ref to creator
        super.onAttach(context);
        if (context instanceof SearchSelectedListener)
            listener = (SearchSelectedListener) context;
        else
            throw new RuntimeException(context.toString()
                    + " must implement SearchSelectedListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        // handle the fragment's own widgets
        roadInput = (EditText) v.findViewById(R.id.roadInput);
        searchButton = (Button) v.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(this);
        mapButton = (Button) v.findViewById(R.id.mapButton);
        mapButton.setOnClickListener(this);
        placeSpinner = (Spinner) v.findViewById(R.id.placeSpinner);
        distanceInput = (EditText) v.findViewById(R.id.distanceInput);
        nearButton = (Button) v.findViewById(R.id.nearButton);
        nearButton.setOnClickListener(this);
        resultsText = (TextView) v.findViewById(R.id.resultsText);
        listView = (ListView) v.findViewById(R.id.searchList);
        listView.setOnItemClickListener(this);

        //ViewModelProvider gets the ViewModel - getActivity() so this Fragment
        //reconnects to the SAME ViewModel that MainActivity created
        ViewModelProvider provider = new ViewModelProvider(getActivity());
        viewModel = provider.get(TrafficViewModel.class);

        // fill the Spinner with the fixed places from a String[] array (MPD_03a_MoreWidgets)
        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_spinner_item, TrafficViewModel.PLACE_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        placeSpinner.setAdapter(adapter);

        // put the last search back in the box (e.g. after "Back to list" or a rotation)
        if (viewModel.getLastSearch() != null) {
            roadInput.setText(viewModel.getLastSearch());
        }

        //This uses MutableLiveData to "connect" directly to observed data
        viewModel.getSearchMessage().observe(this, resultsText::setText);
        viewModel.getSearchResults().observe(this, this::showResults);

        return v;
    }

    // called by the observer every time the search results change
    private void showResults(ArrayList<Situation> results) {
        resultsList = results;

        // arrays for the CustomAdapter (same order as the list items), as in ListView_custom
        String[] values = new String[results.size()];
        int[] imgs = new int[results.size()];
        int[] colours = new int[results.size()];   // 0 = no colour bar (incidents)

        for (int i = 0; i < results.size(); i++) {
            Situation s = results.get(i);
            String where = s.getRoadName();
            if (where == null) {
                where = "Location not given";
            }
            String comment = s.getComment();
            if (comment == null || comment.equals("")) {
                comment = "No description given";
            }
            String text = s.getDisplayType() + " - " + where + "\n" + comment;

            // distance search: also show how far away it is, to 1 decimal place
            if (viewModel.isDistanceSearch()) {
                double km = viewModel.getDistanceKm(s);
                text = text + "\n" + (Math.round(km * 10) / 10.0) + " km away";
            }

            // roadworks also get their dates and the same duration colour as the roadworks list
            if (s.getFeedType().equals(Situation.ROADWORKS) && s.getValidity() != null) {
                int days = s.getValidity().getDurationDays();
                text = text + "\n" + s.getValidity().getFormattedStart() + " to "
                        + s.getValidity().getFormattedEnd();
                if (days < 0) {
                    colours[i] = getResources().getColor(android.R.color.darker_gray, null);
                } else if (days < 7) {
                    colours[i] = getResources().getColor(android.R.color.holo_green_light, null);
                } else if (days <= 30) {
                    colours[i] = getResources().getColor(android.R.color.holo_orange_light, null);
                } else {
                    colours[i] = getResources().getColor(android.R.color.holo_red_light, null);
                }
            }
            values[i] = text;
            imgs[i] = s.getIconId();
        }

        CustomAdapter customAdapter = new CustomAdapter(getActivity(), values, imgs, colours);
        // Assign adapter to ListView
        listView.setAdapter(customAdapter);
    }

    // the three buttons: search by road, search by distance, show results on map
    public void onClick(View aview) {
        if (aview == searchButton) {
            //pass the text typed by the user on to the ViewModel
            viewModel.searchByRoad(roadInput.getText().toString());
        } else if (aview == nearButton) {
            //pass the chosen place (its position in the Spinner) and the distance typed
            viewModel.searchByDistance(placeSpinner.getSelectedItemPosition(),
                    distanceInput.getText().toString());
        } else if (aview == mapButton) {
            // only open the map when there is something to show on it
            if (resultsList.size() == 0) {
                resultsText.setText("Search first, then press Show results on map");
            } else {
                //SEND BACK TO MAIN ACTIVITY VIA CALLBACK
                listener.onShowMap();
            }
        }
    }

    // a result was tapped - position is the same index in resultsList
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Situation s = resultsList.get(position);
        //DATA READY, SEND BACK TO MAIN ACTIVITY VIA CALLBACK
        listener.onSituationSelected(s);
    }
}