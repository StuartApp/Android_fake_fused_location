package com.stuart.fakefusedlocation;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private static final double LOCATION_A_LAT = 41.407300;
    private static final double LOCATION_A_LON = 2.167330;

    private static final double LOCATION_B_LAT = 41.411378;
    private static final double LOCATION_B_LON = 2.173584;

    private boolean mServiceStarted;

    private Button mStartStopButton;
    private Button mLocationAButton;
    private Button mLocationBButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartStopButton = (Button) findViewById(R.id.act_main_start_stop_button);
        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LocationMockerService.class);
                if (!LocationMockerService.isStarted()) {
                    intent.putExtra("lat", LOCATION_A_LAT);
                    intent.putExtra("lon", LOCATION_A_LON);
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }
        });

        mLocationAButton = (Button) findViewById(R.id.act_main_location_a_button);
        mLocationAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LocationMockerService.class);
                intent.putExtra("lat", LOCATION_A_LAT);
                intent.putExtra("lon", LOCATION_A_LON);
                startService(intent);
            }
        });

        mLocationBButton = (Button) findViewById(R.id.act_main_location_b_button);
        mLocationBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LocationMockerService.class);
                intent.putExtra("lat", LOCATION_B_LAT);
                intent.putExtra("lon", LOCATION_B_LON);
                startService(intent);
            }
        });

        updateButtons();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(MainActivity.this, LocationMockerService.class);
        bindService(intent, mServiceConnection, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConnection);
    }

    private void updateButtons() {
        if (mServiceStarted) {
            mStartStopButton.setText("Stop service");
            mLocationAButton.setEnabled(true);
            mLocationBButton.setEnabled(true);
        } else {
            mStartStopButton.setText("Start service");
            mLocationAButton.setEnabled(false);
            mLocationBButton.setEnabled(false);
        }
    }

    private final ServiceConnection mServiceConnection =  new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mServiceStarted = true;
            updateButtons();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mServiceStarted = false;
            updateButtons();
        }
    };
}
