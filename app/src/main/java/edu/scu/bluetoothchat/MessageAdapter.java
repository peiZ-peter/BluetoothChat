package edu.scu.bluetoothchat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
/**
 * Adapter for message.
 */
public class MessageAdapter extends ArrayAdapter<ChatMessage> {

    private final LayoutInflater mInflater;
    private int mResourceMe;
    private int mResourceOthers;
    private File fMe;
    private File fOther;
    private String dirMe;
    private String dirOther;

    public MessageAdapter(Context context, int resourceMe, int resourceOthers) {
        super(context, 0);
        mInflater = LayoutInflater.from(context);
        mResourceMe = resourceMe;
        mResourceOthers = resourceOthers;
        fMe =new File(context.getFilesDir(), "me.jpg");
        dirMe = context.getFilesDir().toString() + "/me.jpg";
        String devAddr = MainActivity.deviceAddr + ".jpg";
        if (devAddr != null) {
            fOther = new File(context.getFilesDir(), devAddr);
            dirOther = context.getFilesDir().toString() + "/" + devAddr;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        ChatMessage message = getItem(position);
        convertView = mInflater.inflate(message.messageSender == ChatMessage.MSG_SENDER_ME ? mResourceMe:mResourceOthers, parent, false);

        TextView name = (TextView) convertView.findViewById(R.id.message_content);
        name.setText(message.messageContent);

        if(message.messageSender == ChatMessage.MSG_SENDER_ME) {
            ImageView iv = (ImageView) convertView.findViewById(R.id.me_image);
            if(fMe.exists()) {
                Log.d("file:", "exists");
                Drawable dw = Drawable.createFromPath(dirMe);
                if (dw == null) Log.d("dw", " is null");
                else Log.d("dw", " is not null");
                iv.setBackground(dw);
            }
            else{
                Log.d("file:", "not exists");
            }
        }

        if(message.messageSender ==  ChatMessage.MSG_SENDER_OTHERS) {
            ImageView iv = (ImageView) convertView.findViewById(R.id.other_image);
            if(fOther.exists()) {
                Drawable dw = Drawable.createFromPath(dirOther);
                if(dw != null) iv.setBackground(dw);
            }
        }



        return convertView;
    }
}
