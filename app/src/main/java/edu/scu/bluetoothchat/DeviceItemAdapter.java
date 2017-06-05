package edu.scu.bluetoothchat;

/**
 * Adapter for device items.
 */

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;

public class DeviceItemAdapter extends ArrayAdapter<BluetoothDevice> {

    private final LayoutInflater mInflater;
    private int mResource;
    private String dir;
    private String cur;

    public DeviceItemAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = LayoutInflater.from(context);
        mResource = resource;
        cur = context.getFilesDir().toString() + "/";
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        TextView name = (TextView) convertView.findViewById(R.id.device_name);
        TextView info = (TextView) convertView.findViewById(R.id.device_info);

        BluetoothDevice device = getItem(position);
        name.setText(device.getName());
        info.setText(device.getAddress());
        dir = cur + device.getAddress() +".jpg";
        File f = new File(dir);

        if(f.exists()) {
            Drawable dw = Drawable.createFromPath(dir);
            dw.setBounds(0, 0, 110, 110);
            name.setCompoundDrawables(dw, null, null, null);
        }

        return convertView;
    }
}
