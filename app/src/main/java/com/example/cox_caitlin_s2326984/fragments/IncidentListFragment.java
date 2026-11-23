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

// First screen: the list of incidents (MPD_5b_Fragments + Lab 4 Task 3 ListView)
// The Fragment is the View - it only shows data, it never downloads or parses
public class IncidentListFragment extends Fragment
        implements View.OnClickListener, AdapterView.OnItemClickListener {

    private Button refreshButton;
    private TextView statusText;
    private ListView listView;
    private TrafficViewModel viewModel;
    private ArrayList<Situation> incidentList = new ArrayList<>(); // the incidents shown, same order as the rows

    // callback to send the tapped incident to MainActivity (MPD_6a_Fragments_commUI)
    public interface IncidentSelectedListener { //interface definition
        void onSituationSelected(Situation situation); //callback method
    }
    private IncidentSelectedListener listener; //reference to main activity

    public IncidentListFragment() {} // Required empty public constructor

    //Stores reference to the main activity for the interface's callback
    @Override
    public void onAttach(Context context) { //context is a ref to creator
        super.onAttach(context);
        if (context instanceof IncidentSelectedListener)
            listener = (IncidentSelectedListener) context;
        else
            throw new RuntimeException(context.toString()
                    + " must implement IncidentSelectedListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_incident_list, container, false);

        // handle the fragment's own widgets
        refreshButton = (Button) v.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(this);
        statusText = (TextView) v.findViewById(R.id.statusText);
        listView = (ListView) v.findViewById(R.id.incidentList);
        listView.setOnItemClickListener(this);

        //ViewModelProvider gets the ViewModel - getActivity() so this Fragment
        //reconnects to the SAME ViewModel that MainActivity created
        ViewModelProvider provider = new ViewModelProvider(getActivity());
        viewModel = provider.get(TrafficViewModel.class);

        //This uses MutableLiveData to "connect" directly to observed data
        viewModel.getStatus().observe(this, statusText::setText);
        viewModel.getIncidents().observe(this, this::showIncidents);

        return v;
    }

    // called by the observer every time the incidents list changes
    private void showIncidents(ArrayList<Situation> incidents) {
        incidentList = incidents;

        // arrays for the CustomAdapter (same order as the list items), as in ListView_custom
        String[] values = new String[incidents.size()];
        int[] imgs = new int[incidents.size()];
        int[] colours = new int[incidents.size()];   // all 0 = no colour bar for incidents

        for (int i = 0; i < incidents.size(); i++) {
            Situation s = incidents.get(i);
            // some incidents (e.g. Abnormal Traffic) have no road number in the feed
            String where = s.getRoadName();
            if (where == null) {
                where = "Location not given";
            }
            // generalPublicComment - the spec asks for type + comment in the list
            String comment = s.getComment();
            if (comment == null || comment.equals("")) {
                comment = "No description given";
            }
            values[i] = s.getDisplayType() + " - " + where + "\n" + comment;
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

    // a row was tapped - position is the same index in incidentList
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Situation s = incidentList.get(position);
        //DATA READY, SEND BACK TO MAIN ACTIVITY VIA CALLBACK
        listener.onSituationSelected(s);
    }
}