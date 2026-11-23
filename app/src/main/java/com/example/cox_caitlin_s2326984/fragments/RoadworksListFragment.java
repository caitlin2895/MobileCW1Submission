package com.example.cox_caitlin_s2326984.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cox_caitlin_s2326984.R;
import com.example.cox_caitlin_s2326984.adapter.CustomAdapter;
import com.example.cox_caitlin_s2326984.model.Situation;
import com.example.cox_caitlin_s2326984.viewmodel.TrafficViewModel;

import java.util.ArrayList;

// Second screen: the list of roadworks (same as IncidentListFragment,
// but it observes getRoadworks() instead of getIncidents())
public class RoadworksListFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private Button refreshButton;
    private TextView statusText;
    private ListView listView;
    private TrafficViewModel viewModel;
    private ArrayList<Situation> roadworksList = new ArrayList<>(); // the roadworks shown, same order as the rows

    // callback to send the tapped roadworks to MainActivity (MPD_6a_Fragments_commUI)
    public interface RoadworksSelectedListener { //interface definition
        void onSituationSelected(Situation situation); //callback method
    }
    private RoadworksSelectedListener listener; //reference to main activity

    public RoadworksListFragment() {} // Required empty public constructor

    //Stores reference to the main activity for the interface's callback
    @Override
    public void onAttach(Context context) { //context is a ref to creator
        super.onAttach(context);
        if (context instanceof RoadworksSelectedListener)
            listener = (RoadworksSelectedListener) context;
        else
            throw new RuntimeException(context.toString()
                    + " must implement RoadworksSelectedListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_roadworks_list, container, false);

        // handle the fragment's own widgets
        refreshButton = (Button) v.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(this);
        statusText = (TextView) v.findViewById(R.id.statusText);
        listView = (ListView) v.findViewById(R.id.roadworksList);
        listView.setOnItemClickListener(this);

        //ViewModelProvider gets the ViewModel - getActivity() so this Fragment
        //reconnects to the SAME ViewModel that MainActivity created
        ViewModelProvider provider = new ViewModelProvider(getActivity());
        viewModel = provider.get(TrafficViewModel.class);

        //This uses MutableLiveData to "connect" directly to observed data
        viewModel.getStatus().observe(this, statusText::setText);
        viewModel.getRoadworks().observe(this, this::showRoadworks);

        return v;
    }

    // called by the observer every time the roadworks list changes
    private void showRoadworks(ArrayList<Situation> roadworks) {
        roadworksList = roadworks;

        // arrays for the CustomAdapter (same order as the list items), as in ListView_custom
        String[] values = new String[roadworks.size()];
        int[] imgs = new int[roadworks.size()];
        int[] colours = new int[roadworks.size()];

        for (int i = 0; i < roadworks.size(); i++) {
            Situation s = roadworks.get(i);
            String where = s.getRoadName();
            if (where == null) {
                where = "Location not given";
            }
            // generalPublicComment - the spec asks for type + comment in the list
            String comment = s.getComment();
            if (comment == null || comment.equals("")) {
                comment = "No description given";
            }
            // start and end dates
            String dates = "Dates not given";
            int days = -1;
            if (s.getValidity() != null) {
                dates = s.getValidity().getFormattedStart() + " to "
                        + s.getValidity().getFormattedEnd();
                days = s.getValidity().getDurationDays();   // -1 if there is no end date
            }

            // colour coding by duration: green = under a week, orange = up to a month,
            // red = longer, grey = no end date given (holo colours as in the Lab 4 snippets)
            if (days < 0) {
                colours[i] = getResources().getColor(android.R.color.darker_gray, null);
            } else if (days < 7) {
                colours[i] = getResources().getColor(android.R.color.holo_green_light, null);
                dates = dates + " (" + days + " days)";
            } else if (days <= 30) {
                colours[i] = getResources().getColor(android.R.color.holo_orange_light, null);
                dates = dates + " (" + days + " days)";
            } else {
                colours[i] = getResources().getColor(android.R.color.holo_red_light, null);
                dates = dates + " (" + days + " days)";
            }

            values[i] = s.getDisplayType() + " - " + where + "\n" + comment + "\n" + dates;
            imgs[i] = s.getIconId();
        }

        CustomAdapter customAdapter = new CustomAdapter(getActivity(), values, imgs, colours);
        // Assign adapter to ListView
        listView.setAdapter(customAdapter);
    }

    // "Refresh data" button: download both feeds again now
    public void onClick(View aview) {
        if (aview == refreshButton) {
            viewModel.startDownload();
        }
    }

    // a row was tapped - position is the same index in roadworksList
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Situation s = roadworksList.get(position);
        //DATA READY, SEND BACK TO MAIN ACTIVITY VIA CALLBACK
        listener.onSituationSelected(s);
    }
}