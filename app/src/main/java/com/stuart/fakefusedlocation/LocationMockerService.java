package com.stuart.fakefusedlocation;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;

import static java.util.Objects.requireNonNull;

public class LocationMockerService extends Service {

    private static final int NOTIFICATION_ID = 1;

    private static final double DEFAULT_LATITUDE = 41.3965032;
    private static final double DEFAULT_LONGITUDE = 2.160502;

    private final Handler mHandler = new Handler();

    private double mLatitude = DEFAULT_LATITUDE;
    private double mLongitude = DEFAULT_LONGITUDE;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!PermissionHelper.hasRuntimePermissions(this)) {
            Toast.makeText(this, R.string.permission_msg_location,
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        if (!PermissionHelper.hasMockLocationPermissions(this)) {
            Toast.makeText(this, R.string.permission_msg_mock_location,
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        LocationServices.getFusedLocationProviderClient(this).setMockMode(true)
                .addOnSuccessListener(aVoid -> startMockingLocations());

        startForeground(NOTIFICATION_ID, NotificationsHelper.buildForegroundNotification(this));
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onDestroy() {
        super.onDestroy();

        stopMockingLocations();

        LocationServices.getFusedLocationProviderClient(this).setMockMode(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (intent.getData() != null) {
                Uri uri = intent.getData();
                if ("start".equals(uri.getHost())) {
                    try {
                        mLatitude = Double.parseDouble(
                                requireNonNull(uri.getQueryParameter("lat")));
                        mLongitude = Double.parseDouble(
                                requireNonNull(uri.getQueryParameter("lon")));
                    } catch (NumberFormatException e) {
                        mLatitude = DEFAULT_LATITUDE;
                        mLongitude = DEFAULT_LONGITUDE;
                    }
                } else {
                    stopSelf();
                }
            } else if (intent.hasExtra("lat") && intent.hasExtra("lon")) {
                mLatitude = intent.getDoubleExtra("lat", DEFAULT_LATITUDE);
                mLongitude = intent.getDoubleExtra("lon", DEFAULT_LONGITUDE);
            } else {
                stopSelf();
            }
        else {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    @SuppressWarnings("MissingPermission")
    private void startMockingLocations() {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(mLatitude);
        location.setLongitude(mLongitude);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        location.setAccuracy(10f);
        location.setAltitude(100.0);

        LocationServices.getFusedLocationProviderClient(this).setMockLocation(location);

        mHandler.postDelayed(this::startMockingLocations, 5000);
    }

    private void stopMockingLocations() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public class Binder extends android.os.Binder {

        public LocationMockerService getService() {
            return LocationMockerService.this;
        }

    }
}
