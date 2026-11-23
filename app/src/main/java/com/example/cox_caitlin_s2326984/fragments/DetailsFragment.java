package com.example.cox_caitlin_s2326984.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cox_caitlin_s2326984.R;
import com.example.cox_caitlin_s2326984.model.GeoPoint;
import com.example.cox_caitlin_s2326984.model.Impact;
import com.example.cox_caitlin_s2326984.model.Situation;
import com.example.cox_caitlin_s2326984.model.SituationLocation;
import com.example.cox_caitlin_s2326984.model.SituationRecord;
import com.example.cox_caitlin_s2326984.model.Validity;
import com.example.cox_caitlin_s2326984.viewmodel.TrafficViewModel;

// Details screen - full details of the incident or roadworks the user selected
public class DetailsFragment extends Fragment implements View.OnClickListener {

    private Button backButton;
    private LinearLayout headerBand;
    private TextView headerType;
    private TextView headerRoad;
    private TextView severityText;
    private TextView commentText;
    private TextView whenText;
    private TextView impactText;
    private TextView locationText;
    private TextView moreText;
    // section headings, so a whole section can be hidden when it has nothing in it
    private TextView severityHeading;
    private TextView commentHeading;
    private TextView whenHeading;
    private TextView impactHeading;
    private TextView locationHeading;
    private TextView moreHeading;
    private TrafficViewModel viewModel;

    // callback to tell MainActivity to go back to the list (MPD_6a_Fragments_commUI)
    public interface DetailsBackListener { //interface definition
        void onBackToList(); //callback method
    }
    private DetailsBackListener listener; //reference to main activity

    public DetailsFragment() {} // Required empty public constructor

    //Stores reference to the main activity for the interface's callback
    @Override
    public void onAttach(Context context) { //context is a ref to creator
        super.onAttach(context);
        if (context instanceof DetailsBackListener)
            listener = (DetailsBackListener) context;
        else
            throw new RuntimeException(context.toString()
                    + " must implement DetailsBackListener!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_details, container, false);

        // handle the fragment's own widgets
        backButton = (Button) v.findViewById(R.id.backButton);
        backButton.setOnClickListener(this);
        headerBand = (LinearLayout) v.findViewById(R.id.headerBand);
        headerType = (TextView) v.findViewById(R.id.headerType);
        headerRoad = (TextView) v.findViewById(R.id.headerRoad);
        severityText = (TextView) v.findViewById(R.id.severityText);
        commentText = (TextView) v.findViewById(R.id.commentText);
        whenText = (TextView) v.findViewById(R.id.whenText);
        impactText = (TextView) v.findViewById(R.id.impactText);
        locationText = (TextView) v.findViewById(R.id.locationText);
        moreText = (TextView) v.findViewById(R.id.moreText);
        severityHeading = (TextView) v.findViewById(R.id.severityHeading);
        commentHeading = (TextView) v.findViewById(R.id.commentHeading);
        whenHeading = (TextView) v.findViewById(R.id.whenHeading);
        impactHeading = (TextView) v.findViewById(R.id.impactHeading);
        locationHeading = (TextView) v.findViewById(R.id.locationHeading);
        moreHeading = (TextView) v.findViewById(R.id.moreHeading);

        //ViewModelProvider gets the ViewModel - getActivity() so this Fragment
        //reconnects to the SAME ViewModel that MainActivity created
        ViewModelProvider provider = new ViewModelProvider(getActivity());
        viewModel = provider.get(TrafficViewModel.class);

        //This uses MutableLiveData to "connect" directly to observed data
        viewModel.getSelectedSituation().observe(this, this::showDetails);

        return v;
    }

    // called by the observer when a situation has been selected
    private void showDetails(Situation s) {
        SituationRecord r = s.getMainRecord();

        // ---- header band: type and road, coloured by feed type ----
        headerType.setText(s.getDisplayType());
        if (s.getFeedType().equals(Situation.INCIDENT)) {
            headerBand.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, null));
        } else {
            headerBand.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark, null));
        }

        String road = notGiven(s.getRoadName());
        if (r != null && r.getLocation().getDirection() != null) {
            road = road + " (" + readable(r.getLocation().getDirection()) + ")";
        }
        headerRoad.setText(road);

        // a situation with no record has nothing else to show
        if (r == null) {
            showSection(severityHeading, severityText, null);
            showSection(commentHeading, commentText, null);
            showSection(whenHeading, whenText, null);
            showSection(impactHeading, impactText, null);
            showSection(locationHeading, locationText, null);
            showSection(moreHeading, moreText, null);
            return;
        }

        // ---- severity ----
        String severity = s.getOverallSeverity();
        if (severity == null) {
            severity = r.getSeverity();
        }
        showSection(severityHeading, severityText, readable(severity));

        // ---- description (generalPublicComment) ----
        showSection(commentHeading, commentText, s.getComment());

        // ---- when (validity) ----
        Validity validity = r.getValidity();
        String when = "Start: " + validity.getFormattedStart()
                + "\nEnd: " + validity.getFormattedEnd();
        if (validity.getDurationHours() >= 0) {
            when = when + "\nDuration: " + validity.getDurationDays() + " days "
                    + (validity.getDurationHours() % 24) + " hours";
        }
        if (validity.getValidityStatus() != null) {
            when = when + "\nStatus: " + readable(validity.getValidityStatus());
        }
        showSection(whenHeading, whenText, when);

        // ---- impact (-1 means the feed didn't give it) ----
        Impact impact = r.getImpact();
        String impactInfo = "";
        if (impact.getDelayMinutes() >= 0) {
            impactInfo = impactInfo + "Delay: " + impact.getDelayMinutes() + " minutes\n";
        }
        if (impact.getLanesRestricted() >= 0) {
            impactInfo = impactInfo + "Lanes restricted: " + impact.getLanesRestricted() + "\n";
        }
        if (impact.getOperationalLanes() >= 0) {
            impactInfo = impactInfo + "Lanes open: " + impact.getOperationalLanes() + "\n";
        }
        if (impact.getCapacityRemaining() >= 0) {
            impactInfo = impactInfo + "Capacity remaining: " + (int) impact.getCapacityRemaining() + "%\n";
        }
        if (impact.getTrafficConstrictionType() != null) {
            impactInfo = impactInfo + "Restriction: " + readable(impact.getTrafficConstrictionType()) + "\n";
        }

        // section is hidden if none of the impact values were in the feed
        showSection(impactHeading, impactText, impactInfo.trim());

        // ---- location ----
        SituationLocation location = r.getLocation();
        String locationInfo = "";
        if (location.getLocationType() != null) {
            locationInfo = locationInfo + "Type: " + readable(location.getLocationType()) + "\n";
        }
        if (location.isLinear()) {
            // a stretch of road: show both ends, each with its name and coordinates
            GeoPoint from = location.getFrom();
            String fromName = from.getName();
            if (fromName == null) {
                fromName = "Start of section";
            }
            locationInfo = locationInfo + "From: " + fromName + "\n    "
                    + from.getLatitude() + ", " + from.getLongitude() + "\n";

            GeoPoint to = location.getTo();
            String toName = to.getName();
            if (toName == null) {
                toName = "End of section";
            }
            locationInfo = locationInfo + "To: " + toName + "\n    "
                    + to.getLatitude() + ", " + to.getLongitude() + "\n";
        } else {
            // a single point
            GeoPoint point = location.getMainPoint();
            if (point != null) {
                if (point.getName() != null) {
                    locationInfo = locationInfo + "At: " + point.getName() + "\n";
                }
                locationInfo = locationInfo + "Coordinates: " + point.getLatitude()
                        + ", " + point.getLongitude() + "\n";
            }
        }
        showSection(locationHeading, locationText, locationInfo.trim());

        // ---- more information (type-specific fields) ----
        String more = "";
        if (r.getDetailType() != null) {
            more = more + "Cause: " + readable(r.getDetailType()) + "\n";
        }
        if (r.getRoadworksDuration() != null) {
            more = more + "Roadworks duration: " + readable(r.getRoadworksDuration()) + "\n";
        }
        if (r.getRoadworksScale() != null) {
            more = more + "Roadworks scale: " + readable(r.getRoadworksScale()) + "\n";
        }
        if (r.getProbability() != null) {
            more = more + "Probability: " + readable(r.getProbability()) + "\n";
        }
        showSection(moreHeading, moreText, more.trim());
    }

    // shows a section's heading and text, or hides both (View.GONE, so no blank gap
    // is left) when there is nothing to show - setVisibility() is from MPD_02a_Layouts
    private void showSection(TextView heading, TextView text, String value) {
        if (value == null || value.equals("")) {
            heading.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
        } else {
            heading.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            text.setText(value);
        }
    }

    // turns a raw word from the feed into readable text,
    // e.g. "definedByValidityTimeSpec" -> "Scheduled", "queuingTraffic" -> "Queuing traffic"
    private String readable(String text) {
        if (text == null || text.equals("")) {
            return text;
        }
        // words that need their own wording
        if (text.equals("definedByValidityTimeSpec")) {
            return "Scheduled";
        }
        if (text.equals("Linear")) {
            return "Stretch of road";
        }
        if (text.equals("Point")) {
            return "Single point";
        }
        if (text.equals("northBound")) {
            return "Northbound";
        }
        if (text.equals("southBound")) {
            return "Southbound";
        }
        if (text.equals("eastBound")) {
            return "Eastbound";
        }
        if (text.equals("westBound")) {
            return "Westbound";
        }
        if (text.equals("bothWays")) {
            return "Both directions";
        }
        // anything else: capitalise the 1st letter (as in Lab 2 Task 2), then put a space
        // before each capital letter and make it lower case (same loop as getDisplayType())
        String result = text.substring(0, 1).toUpperCase();
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                result = result + " " + text.substring(i, i + 1).toLowerCase();
            } else {
                result = result + c;
            }
        }
        return result;
    }

    // shows "Not given" instead of null or a blank line
    private String notGiven(String text) {
        if (text == null || text.equals("")) {
            return "Not given";
        }
        return text;
    }

    // "Back" button: return to the screen the user came from
    public void onClick(View aview) {
        if (aview == backButton) {
            //SEND BACK TO MAIN ACTIVITY VIA CALLBACK
            listener.onBackToList();
        }
    }
}