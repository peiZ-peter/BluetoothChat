package edu.scu.bluetoothchat;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static edu.scu.bluetoothchat.ConnectionManager.deviceName;
import static edu.scu.bluetoothchat.R.id.imageView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private final int RESULT_CODE_BTDEVICE = 0;
    private final int RESULT_CODE_UPLOAD_ME = 1;
    private final int RESULT_CODE_UPLOAD_OTHER = 2;
    public static String deviceAddr = null;

    private ConnectionManager mConnectionManager;
    private EditText mMessageEditor;
    private ImageButton mSendBtn;
    private ListView mMessageListView;
    private MenuItem mConnectionMenuItem;

    private FileOutputStream out;
    private BufferedWriter writer;
    private Vibrator vibrator;



    private final static int MSG_SENT_DATA = 0;
    private final static int MSG_RECEIVE_DATA = 1;
    private final static int MSG_UPDATE_UI = 2;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_SENT_DATA: {

                        byte[] data = (byte[]) msg.obj;
                        boolean suc = msg.arg1 == 1;
                        if (data != null && suc) {
                            ChatMessage chatMsg = new ChatMessage();
                            chatMsg.messageSender = ChatMessage.MSG_SENDER_ME;
                            chatMsg.messageContent = new String(data);

                            String saveMessage = chatMsg.messageSender + " " + chatMsg.messageContent +"\r\n";

                            out = openFileOutput(deviceAddr, Context.MODE_APPEND);
                            out.write(saveMessage.getBytes());


                            Log.d("logged: ", deviceAddr);
                            Log.d("logged: ", saveMessage);

                            MessageAdapter adapter = (MessageAdapter) mMessageListView.getAdapter();
                            adapter.add(chatMsg);
                            adapter.notifyDataSetChanged();

                            out.close();

                            mMessageEditor.setText("");
                        }
                    }
                    break;

                    case MSG_RECEIVE_DATA: {

                        byte[] data = (byte[]) msg.obj;
                        if (data != null) {

                            ChatMessage chatMsg = new ChatMessage();
                            chatMsg.messageSender = ChatMessage.MSG_SENDER_OTHERS;
                            chatMsg.messageContent = new String(data);

                            String saveMessage = chatMsg.messageSender + " " + chatMsg.messageContent +"\r\n";

                            out = openFileOutput(deviceAddr, Context.MODE_APPEND);
                            out.write(saveMessage.getBytes());

                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(getApplicationContext())
                                            .setSmallIcon(R.drawable.cat)
                                            .setContentTitle(deviceName)
                                            .setContentText(chatMsg.messageContent)
                                            .setAutoCancel(true)
                                            .setCategory(Intent.CATEGORY_LAUNCHER);
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            mBuilder.setContentIntent(pendingIntent);
                            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE) ;
                            mNotificationManager.notify(0, mBuilder.build());


                            MessageAdapter adapter = (MessageAdapter) mMessageListView.getAdapter();
                            adapter.add(chatMsg);
                            adapter.notifyDataSetChanged();
                            vibrator.vibrate(333);

                            out.close();
                        }

                    }
                    break;

                    case MSG_UPDATE_UI: {
                        updateUI();
                    }
                    break;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "oncreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);


        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                final InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(drawer.getWindowToken(), 0);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

        try {


            //If bluetooth is not opened, ask for opening
            BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!BTAdapter.isEnabled()) {
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(i);
                return;
            }

            //Check permissions
            int hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }


            mMessageEditor = (EditText) findViewById(R.id.msg_editor);
            mMessageEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {


                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEND) {

                        sendMessage();
                        return true;
                    }
                    return false;
                }
            });

            mSendBtn = (ImageButton) findViewById(R.id.send_btn);
            mSendBtn.setOnClickListener(mSendClickListener);


            mMessageListView = (ListView) findViewById(R.id.message_list);
            MessageAdapter adapter = new MessageAdapter(this, R.layout.me_list_item, R.layout.others_list_item);
            mMessageListView.setAdapter(adapter);

            mConnectionManager = new ConnectionManager(mConnectionListener);
            deviceAddr = mConnectionManager.startListen();

            //Check if device is discoverable
            if (BTAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivity(i);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {

        Log.d(TAG,"on resume");
        super.onResume();


        try {
            String addr = "me.jpg";
            File f=new File(getFilesDir(), addr);
            ImageView iv = (ImageView)findViewById(imageView);
            if(f.exists()) {
                String cur = getFilesDir() + "/" + addr;
                Drawable dw = Drawable.createFromPath(cur);
                iv.setBackground(dw);
            } else{
                iv.setBackgroundResource(R.drawable.cat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



        try {
            mMessageListView = (ListView) findViewById(R.id.message_list);
            MessageAdapter adapter = new MessageAdapter(this, R.layout.me_list_item, R.layout.others_list_item);
            mMessageListView.setAdapter(adapter);
            if (deviceAddr != null) {
                setTitle(deviceName);
                FileInputStream in = openFileInput(deviceAddr);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String readMessage = reader.readLine();
                while (readMessage != null) {
                    Log.d("read: ", readMessage);
                    Log.d("read: ", deviceAddr);
                    String[] readMessageStrs = readMessage.split(" ");
                    ChatMessage readChat = new ChatMessage();
                    if(readMessageStrs[0].equals("9")){
                        readMessage = reader.readLine();
                        continue;
                    }
                    readChat.messageSender = Integer.parseInt(readMessageStrs[0]);
                    readChat.messageContent = readMessageStrs[1];
                    adapter.add(readChat);
                    adapter.notifyDataSetChanged();
                    readMessage = reader.readLine();
                }
                in.close();


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            super.onDestroy();
            mHandler.removeMessages(MSG_UPDATE_UI);
            mHandler.removeMessages(MSG_SENT_DATA);
            mHandler.removeMessages(MSG_RECEIVE_DATA);

            if (mConnectionManager != null) {
                mConnectionManager.disconnect();
                mConnectionManager.stopListen();
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    private ConnectionManager.ConnectionListener mConnectionListener = new ConnectionManager.ConnectionListener() {

        @Override
        public void onConnectStateChange(int oldState, int State) {

            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        @Override
        public void onListenStateChange(int oldState, int State) {

            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        @Override
        public void onSendData(boolean suc, byte[] data) {

            mHandler.obtainMessage(MSG_SENT_DATA, suc?1:0, 0, data).sendToTarget();
        }

        @Override
        public void onReadData(byte[] data) {

            mHandler.obtainMessage(MSG_RECEIVE_DATA,  data).sendToTarget();

        }

    };

    private View.OnClickListener mSendClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            sendMessage();
        }
    };

    private void sendMessage() {
        String content = mMessageEditor.getText().toString();
        if(content != null) {
            content = content.trim();
            if(content.length() > 0) {
                boolean ret = mConnectionManager.sendData(content.getBytes());
                if(!ret) {
                    Toast.makeText(MainActivity.this, R.string.send_fail, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        mConnectionMenuItem = menu.findItem(R.id.connect_menu);
        updateUI();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.connect_menu) {
            //if select connect menu button
            if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
                mConnectionManager.disconnect();

            }
            else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
                mConnectionManager.disconnect();

            }
            else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
                Intent i = new Intent(this, DeviceListActivity.class);
                startActivityForResult(i, RESULT_CODE_BTDEVICE);
            }
            return true;
        }

        else return false;

    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Intent reset = new Intent(this, DeleteActivity.class);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        switch (item.getItemId()){

            case R.id.about_menu: {
                //if selected about menu
                Intent i = new Intent(this, AboutActivity.class);
                startActivity(i);
            }
            return true;

            case R.id.delete_record: {
                //if selected delete record
                if(deviceAddr != null) {
                    try {
                        out = openFileOutput(deviceAddr, Context.MODE_PRIVATE);
                        out.write("9 9\r\n".getBytes());
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), R.string.delete_successful, Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, DeleteActivity.class);
                    startActivity(i);
                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
            return true;

            case R.id.upload_self_pic: {
                Intent intent = new Intent(Intent.ACTION_PICK,null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, RESULT_CODE_UPLOAD_ME);
            }
            return true;

            case R.id.default_self_pic: {
                File f=new File(getFilesDir(), "me.jpg");
                f.delete();
                Toast.makeText(this, R.string.restore_self, Toast.LENGTH_SHORT).show();
                startActivity(reset);
            }
            return true;

            case R.id.upload_other_pic: {
                if(deviceAddr != null) {
                    Intent intent = new Intent(Intent.ACTION_PICK, null);
                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                    startActivityForResult(intent, RESULT_CODE_UPLOAD_OTHER);
                }
                else {
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }

            }
            return true;

            case R.id.default_other_pic: {
                if(deviceAddr != null) {
                    String name = deviceAddr + ".jpg";
                    File f = new File(getFilesDir(), name);
                    f.delete();
                    Toast.makeText(this, R.string.restore_other, Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }

                startActivity(reset);

            }
            return true;

            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult, requestCode="+requestCode+" resultCode="+resultCode );
        if(requestCode == RESULT_CODE_BTDEVICE && resultCode == RESULT_OK) {
            //get device address and connect
            deviceAddr = data.getStringExtra("DEVICE_ADDR");
            Log.d("deviceaddr", deviceAddr);
            mConnectionManager.connect(deviceAddr);
            Log.d(TAG, "ffff");
        }

        if(requestCode == RESULT_CODE_UPLOAD_ME && resultCode == RESULT_OK) {
            //save uploaded picture to local
            if(data != null) {
                try {
                    File f = new File(getFilesDir(), "me.jpg");
                    f.delete();

                    Bitmap bitmap = getBitmap(data.getData());

                    if (bitmap != null) {
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, bos);
                        bos.flush();
                        bos.close();
                    }

                    if(f.exists()){
                        Toast.makeText(this, R.string.uploaded, Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(this, R.string.not_uploaded, Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (requestCode == RESULT_CODE_UPLOAD_OTHER && resultCode == RESULT_OK) {
            //save uploaded pictures to local
            if (data != null) {
                try {
                    if (deviceAddr != null) {
                        String addr = deviceAddr + ".jpg";
                        File f = new File(getFilesDir(),  addr);
                        f.delete();

                        Bitmap bitmap = getBitmap(data.getData());
                        if(bitmap != null) {
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            bos.flush();
                            bos.close();
                        }
                        if(f.exists()) {
                            Toast.makeText(this, R.string.uploaded, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(this, R.string.not_uploaded, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }

    private Bitmap getBitmap(Uri uri) {
        String picturePath = null;
        Bitmap bitmap = null;

        if (uri != null) {
            Log.d("uri", "not null");
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
        }
        if (picturePath != null) {
            bitmap = BitmapFactory.decodeFile(picturePath);
        }

        return bitmap;
    }

    private void updateUI()
    {
        if(mConnectionManager == null) {
            return;
        }

        if(mConnectionMenuItem == null) {
            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);

            return;
        }

        Log.d(TAG, "current BT ConnectState="+mConnectionManager.getState(mConnectionManager.getCurrentConnectState())
                +" ListenState="+mConnectionManager.getState(mConnectionManager.getCurrentListenState()));

        if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
            mConnectionMenuItem.setTitle(R.string.disconnect);

            mMessageEditor.setEnabled(true);
            mSendBtn.setEnabled(true);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
            mConnectionMenuItem.setTitle(R.string.cancel);

            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
            mConnectionMenuItem.setTitle(R.string.connect);

            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);
        }
    }
}
