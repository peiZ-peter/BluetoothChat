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

import java.util.Timer;
import java.util.TimerTask;

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
        final String[] strs = {"I'm angry!"
                ,"吼啊！"
                ,"这是坠吼的！"
                ,"你这样子啊是不行的！"
                ,"苟利国家生死以，岂因祸福避趋之？"
                ,"中央已经撅腚了！"
                ,"我实在也不是谦虚！"
                ,"Apply for professor."
                ,"Excited!"
                ,"Big mistake!"
                ,"闷声发大财！"
                ,"Too young, too simple, sometimes naive!"
                ,"西方的哪一个国家我没去过？"
                ,"You mean I'm a dictatorship?"
                ,"Time flies very fast."
                ,"我有必要告诉你们一点人生的经验！"
                ,"写程序也要按照基本法！"
                ,"今天算是得罪了你们一下！"
                ,"另请高明吧！"
                ,"很惭愧，就做了一点微小的工作。",
                "识得唔识得啊？"
                ,"无可奉告。"
                ,"滋瓷！"
                ,"图样图森破，上来拿衣服。"
                ,"苟"
                ,"一颗赛艇"};



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
        final Toast toast = Toast.makeText(getApplicationContext(), "+1s", Toast.LENGTH_SHORT);
        button.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                count++;
                if (count % 5 != 0){
                    toast.setText("+1s");
                    showMyToast(toast, 1000);
                } else{
                    toast.setText(strs[(int)(Math.random()*strs.length)]);
                    showMyToast(toast, 1000);
                }
            }
        });
    }
    private void showMyToast(final Toast toast, final int cnt) {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                toast.show();
            }
        }, 0, 3000);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                toast.cancel();
                timer.cancel();
            }
        }, cnt );
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
