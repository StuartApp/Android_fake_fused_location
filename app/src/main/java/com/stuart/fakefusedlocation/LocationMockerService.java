package com.stuart.fakefusedlocation;

import android.app.AppOpsManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

public class LocationMockerService extends Service {

    private static final double DEFAULT_LATITUDE = 41.3965032;
    private static final double DEFAULT_LONGITUDE = 2.160502;

    private final Handler mHandler = new Handler();

    private static boolean sStarted;

    private GoogleApiClient mGoogleApiClient;

    private double mLatitude = DEFAULT_LATITUDE;
    private double mLongitude = DEFAULT_LONGITUDE;

    public static boolean isStarted() {
        return sStarted;
    }

    public LocationMockerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        int fineLocationPermission = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED
                && coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Enable location permissions for this app",
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        if (!isMockLocationEnabled()) {
            Toast.makeText(this, "Select this as mock location app at developer settings",
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        sStarted = true;

        GoogleApiClient.ConnectionCallbacks connectionCallback =
                new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    @SuppressWarnings("MissingPermission")
                    public void onConnected(@Nullable Bundle bundle) {
                        LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, true)
                                .setResultCallback(new ResultCallbacks<Status>() {
                                    @Override
                                    public void onSuccess(@NonNull Status status) {
                                        startMockingLocations();
                                    }

                                    @Override
                                    public void onFailure(@NonNull Status status) {
                                    }
                                });
                    }

                    @Override
                    public void onConnectionSuspended(int reason) {
                    }
                };

        GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            }
        };

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(connectionCallback)
                .addOnConnectionFailedListener(connectionFailedListener)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onDestroy() {
        super.onDestroy();
        sStarted = false;

        stopMockingLocations();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, false)
                    .setResultCallback(new ResultCallbacks<Status>() {
                        @Override
                        public void onSuccess(@NonNull Status status) {
                            mGoogleApiClient.disconnect();
                        }

                        @Override
                        public void onFailure(@NonNull Status status) {
                            mGoogleApiClient.disconnect();
                        }
                    });
        }

        if (mGoogleApiClient != null &&  mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (intent.getData() != null) {
                Uri uri = intent.getData();
                if ("start".equals(uri.getHost())) {
                    try {
                        mLatitude = Double.parseDouble(uri.getQueryParameter("lat"));
                        mLongitude = Double.parseDouble(uri.getQueryParameter("lon"));
                    } catch (NumberFormatException | NullPointerException e) {
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

    @SuppressWarnings("MissingPermission")
    private void startMockingLocations() {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(mLatitude);
        location.setLongitude(mLongitude);
        location.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        location.setAccuracy(10f);
        location.setAltitude(100.0);

        LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, location)
                .setResultCallback(new ResultCallbacks<Status>() {
                    @Override
                    public void onSuccess(@NonNull Status status) {
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                    }
                });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startMockingLocations();
            }
        }, 5000);
    }

    private void stopMockingLocations() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private boolean isMockLocationEnabled() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int mockLocationMode = opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(), BuildConfig.APPLICATION_ID);
                return (mockLocationMode == AppOpsManager.MODE_ALLOWED);
            } else {
                return !android.provider.Settings.Secure
                        .getString(getContentResolver(), "mock_location").equals("0");
            }
        } catch (SecurityException e) {
            return false;
        }
    }
}
