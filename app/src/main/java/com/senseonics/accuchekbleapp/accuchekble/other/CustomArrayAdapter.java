package com.senseonics.accuchekbleapp.accuchekble.other;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.senseonics.accuchekbleapp.accuchekble.R;

import java.util.ArrayList;

/**
 * Created by Shiva on 01-11-2017.
 */

public class CustomArrayAdapter extends ArrayAdapter<BluetoothDevice> {
    ArrayList<BluetoothDevice> devicesList = new ArrayList<BluetoothDevice>();

    public CustomArrayAdapter(Context context, int textViewResourceId, ArrayList<BluetoothDevice> objects) {
        super(context, textViewResourceId, objects);
        devicesList = objects;
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(android.R.layout.simple_list_item_2, null);
        TextView deviceName = (TextView) v.findViewById(android.R.id.text1);
        TextView deviceAddress = (TextView) v.findViewById(android.R.id.text2);
        deviceName.setText(devicesList.get(position).getName());
        deviceAddress.setText(devicesList.get(position).getAddress());
        return v;
    }
}
