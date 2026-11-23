/*  Mobile Platform Development coursework - 1st diet 26/27
    Traffic Scotland app: downloads the incidents and roadworks DATEX II feeds,
    shows them in lists, a details screen, a search screen and on a map,
    and refreshes the data automatically
*/

//
// Name                 Caitlin Cox
// Student ID           S2326984
// Programme of Study   Software Development
//

package com.example.cox_caitlin_s2326984;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.cox_caitlin_s2326984.fragments.DetailsFragment;
import com.example.cox_caitlin_s2326984.fragments.IncidentListFragment;
import com.example.cox_caitlin_s2326984.fragments.MapFragment;
import com.example.cox_caitlin_s2326984.fragments.RoadworksListFragment;
import com.example.cox_caitlin_s2326984.model.Situation;
import com.example.cox_caitlin_s2326984.viewmodel.TrafficViewModel;
import com.example.cox_caitlin_s2326984.fragments.SearchFragment;


public class MainActivity extends AppCompatActivity implements OnClickListener,
        IncidentListFragment.IncidentSelectedListener,
        RoadworksListFragment.RoadworksSelectedListener,
        DetailsFragment.DetailsBackListener, SearchFragment.SearchSelectedListener {
    private TrafficViewModel viewModel; // shared with every Fragment, holds all the data

    // top bar buttons that switch Fragment (MPD_5b_Fragments)
    private Button frButton1;   // Incidents
    private Button frButton2;   // Roadworks
    private Button frButton3;   // Search
    private Button frButton4;   // Map of everything
    private Fragment fr1;       // IncidentListFragment
    private Fragment fr2;       // RoadworksListFragment
    private Fragment fr3;       // DetailsFragment
    private Fragment fr4;       // SearchFragment
    private Fragment fr5;       // MapFragment

    private Fragment fr;        // the screen picked with the top bar, so "back" can return to it

    // AUTO UPDATE - how often the feeds are downloaded again
    private final static long UPDATE_INTERVAL = 60 * 60 * 1000; // 1 hour in milliseconds
//    private final static long UPDATE_INTERVAL = 60 * 1000; // demo: 1 minute
    // handler on the UI thread, used to run autoUpdate after UPDATE_INTERVAL
    private Handler timerHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //ViewModelProvider gets a ViewModel to use by this activity
        ViewModelProvider provider = new ViewModelProvider(this);
        //creates the ViewModel object if it doesn't exist yet, reconnects to it if it does (e.g. after a rotation)
        viewModel = provider.get(TrafficViewModel.class);

        //read the Traffic Scotland API credentials from the manifest metadata
        //(the real values are in local.properties, which is not uploaded to GitHub)
        Bundle metaData = null;
        try {
            metaData = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
                    .metaData;
            viewModel.setCredentials(
                    metaData.getString("com.example.cox_caitlin_S2326984.TS_CLIENT_ID"),
                    metaData.getString("com.example.cox_caitlin_S2326984.TS_CLIENT_KEY")
            );
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e); // can't happen - this is our own package
        }

        // buttons to switch between the screens
        frButton1 = (Button) findViewById(R.id.frbutton1);
        frButton2 = (Button) findViewById(R.id.frbutton2);
        frButton3 = (Button) findViewById(R.id.frbutton3);
        frButton4 = (Button) findViewById(R.id.frbutton4);
        frButton1.setOnClickListener(this);
        frButton2.setOnClickListener(this);
        frButton3.setOnClickListener(this);
        frButton4.setOnClickListener(this);

        // create one of each Fragment (MapFragment is re-created each time it is opened)
        fr1 = new IncidentListFragment();
        fr2 = new RoadworksListFragment();
        fr3 = new DetailsFragment();
        fr4 = new SearchFragment();
        fr5 = new MapFragment();
        fr = fr1; //pick the incident list first

        // only on first start - after a rotation Android puts back
        // whichever Fragment was already showing
        if (savedInstanceState == null) {
            //Set fr as the current Fragment to show
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.fragholder, fr); //put fragment fr into Frame
            transaction.commit(); //update the view
        }

        //  AUTO UPDATE 1 - fetch the data as soon as the app starts
        //  (skipped after a rotation, as the ViewModel still has the data)
        if (viewModel.getIncidents().getValue() == null){
            viewModel.startDownload();
        }
        //  AUTO UPDATE 2: schedule the next update using the handler postDelayed()
        timerHandler.postDelayed(autoUpdate, UPDATE_INTERVAL);
    }

    // top bar buttons: pick the Fragment to show, then swap it into the Frame
    public void onClick(View v) { //handle buttons onClicks
        if (v == frButton1) {
            fr = fr1;
            Log.d("MyTag", "Incidents list selected");
        }
        else if (v == frButton2) {
            fr = fr2;
            Log.d("MyTag", "Roadworks list selected");
        }     else if (v == frButton3) {
            fr = fr4;
            Log.d("MyTag", "Search selected");
        }     else if (v == frButton4) {
            // map of ALL incidents and roadworks
            viewModel.setMapShowsAll(true);
            // a new MapFragment each time, so its view (and map) is rebuilt in the right mode
            fr5 = new MapFragment();
            fr = fr5;
            Log.d("MyTag", "Map of everything selected");
        }
        //update fragment view
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragholder, fr);
        transaction.commit();
    }

    // callback from either list Fragment or the search results - the user tapped a situation
    @Override
    public void onSituationSelected(Situation situation) { //Get data from fragment!
        Log.d("MyTag", "Situation selected: " + situation.getId());
        viewModel.selectSituation(situation); //pass data on to ViewModel
        //show the details Fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragholder, fr3);
        transaction.commit();
    }

    // callback from SearchFragment - the user pressed "Show results on map"
    // (the map reads the search results straight from the ViewModel)
    @Override
    public void onShowMap() {
        Log.d("MyTag", "Map of search results selected");
        // map of the search results only
        viewModel.setMapShowsAll(false);
        // a new MapFragment each time, so its view (and map) is rebuilt in the right mode
        fr5 = new MapFragment();
        //show the map Fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragholder, fr5);
        transaction.commit();
    }

    // callback from DetailsFragment or MapFragment - the user pressed a back button
    @Override
    public void onBackToList() {
        //put the screen picked with the top bar (list or search) back in the Frame
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragholder, fr);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop the timer when the activity is destroyed (e.g. app closed or screen rotated)
        // otherwise the old activity would keep downloading in the background
        timerHandler.removeCallbacks(autoUpdate);
    }

    //runnable posted to the UI handler with postDelayed()
    //each time it runs it starts a new download and then posts itself again, so the data keeps updating every UPDATE_INTERVAL
    private Runnable autoUpdate = new Runnable() {
        @Override
        public void run() {
            Log.d("AutoUpdate", "timer fired - updating data");
            viewModel.startDownload();
            timerHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    // generated by Android Studio - not used by the app, just calls the default behaviour
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}