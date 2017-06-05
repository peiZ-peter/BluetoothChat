package edu.scu.bluetoothchat;

/**
 * Author info.
 */

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends AppCompatActivity {
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        setTitle(R.string.about);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PackageManager manager = getPackageManager();
        PackageInfo info = null;

        try {
            info = manager.getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = info == null ? getString(R.string.unknown): info.versionName;
        String msg = String.format(getString(R.string.version_info), version);

        TextView ver = (TextView) findViewById(R.id.version_info);
        ver.setText(msg);

        Button button = (Button)findViewById(R.id.onesbutton);
        button.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                count++;
                if (count % 5 == 0){
                    Toast.makeText(getApplicationContext(), "+1s", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
