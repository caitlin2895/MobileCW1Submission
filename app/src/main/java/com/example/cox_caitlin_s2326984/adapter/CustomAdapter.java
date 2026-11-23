package com.example.cox_caitlin_s2326984.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.cox_caitlin_s2326984.R;

// Custom adapter for the incidents and roadworks lists
// (ListView_custom example, MPD_03a_MoreWidgets slides 18-22):
// each row is an icon (ImageView) + the text (TextView), plus a colour bar
// used for the roadworks duration colour coding
public class CustomAdapter extends BaseAdapter {
    String[] itemList;   //storage for textual data
    int[] pics;          //storage for icons ids
    int[] colours;       //colour of the bar on each row (0 = no colour bar)
    LayoutInflater inflter; //turns layout_listview.xml into a row View

    public CustomAdapter(Context applicationContext,
                         String[] items, int[] images, int[] barColours) {
        this.itemList = items;
        this.pics = images;
        this.colours = barColours;
        inflter = (LayoutInflater.from(applicationContext));
    }

    // number of rows in the list
    @Override
    public int getCount() {
        return itemList.length;
    }

    @Override
    public Object getItem(int i) {
        return itemList[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    // builds (or reuses) the row View for position i
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        CustomAdapter.WidgetsHolder holder = null;
        if (view == null) { //1st time, create view and bound to widgets
            view = inflter.inflate(R.layout.layout_listview, null);
            holder = new CustomAdapter.WidgetsHolder();
            holder.text = (TextView) view.findViewById(R.id.textView);
            holder.pic = (ImageView) view.findViewById(R.id.icon);
            holder.bar = (View) view.findViewById(R.id.colourBar);
            view.setTag(holder); //store bound objects into the view
        }
        else holder = (CustomAdapter.WidgetsHolder) view.getTag();

        holder.text.setText(itemList[i]);
        holder.pic.setImageResource(pics[i]);
        // colour bar: hidden (but still taking its space, so icons line up) when there is no colour
        if (colours[i] == 0) {
            holder.bar.setVisibility(View.INVISIBLE);
        } else {
            holder.bar.setVisibility(View.VISIBLE);
            holder.bar.setBackgroundColor(colours[i]);
        }
        return view; //return the ready-made view to be displayed
    }

    static class WidgetsHolder { //preserve objects bound to widgets
        TextView text;
        ImageView pic;
        View bar;
    }
}