package com.stuart.fakefusedlocation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String SHARE_PREFERENCES_NAME = "fakeFusedLocation.MapActivity";

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_ZOOM = "zoom";

    @Nullable
    private LocationMockerService mService;

    @Nullable
    private ServiceConnection mServiceConnection;

    @Nullable
    private GoogleMap mMap;

    private boolean mIsMapCentered;

    private Button mStartStopButton;

    private boolean mStartServiceOnResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.act_map_title);
        setContentView(R.layout.activity_map);

        NotificationsHelper.createNotificationChannels(this);

        mStartStopButton = findViewById(R.id.act_map_start_stop_button);
        mStartStopButton.setOnClickListener(view -> {
            if (mService == null) {
                startLocationMockerService();
            } else {
                stopLocationMockerService();
            }
        });

        updateButtons();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindLocationMockerService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindLocationMockerService();

        if (mMap != null) {
            LatLng target = mMap.getCameraPosition().target;
            SharedPreferences preferences = getSharedPreferences(
                    SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);
            preferences.edit()
                    .putString(KEY_LATITUDE, String.valueOf(target.latitude))
                    .putString(KEY_LONGITUDE, String.valueOf(target.longitude))
                    .putFloat(KEY_ZOOM, mMap.getCameraPosition().zoom)
                    .apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mStartServiceOnResume) {
            mStartServiceOnResume = false;
            startLocationMockerService();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnCameraMoveListener(() -> {
            mIsMapCentered = true;
            if (mService != null) {
                sendLatLngToLocationMockerService();
            }
        });

        updateButtons();
        centerMap();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE
                && PermissionHelper.hasRuntimePermissions(this)) {
            mStartServiceOnResume = true;
        }
    }

    private void updateButtons() {
        if (mMap == null) {
            mStartStopButton.setVisibility(View.GONE);
        } else {
            mStartStopButton.setVisibility(View.VISIBLE);

            if (mService == null) {
                mStartStopButton.setText(R.string.act_map_start_mocking);
            } else {
                mStartStopButton.setText(R.string.act_map_stop_mocking);
            }
        }
    }

    private void bindLocationMockerService() {
        if (mServiceConnection == null) {
            mServiceConnection = new LocationMockerServiceConnection();
            Intent intent = new Intent(MapActivity.this, LocationMockerService.class);
            bindService(intent, mServiceConnection, 0);
        }
    }

    private void unbindLocationMockerService() {
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }

    private void startLocationMockerService() {
        if (!PermissionHelper.hasRuntimePermissions(MapActivity.this)) {
            PermissionHelper.requestRuntimePermissions(
                    MapActivity.this, PERMISSIONS_REQUEST_CODE);
        } else if (!PermissionHelper.hasMockLocationPermissions(MapActivity.this)) {
            MockLocationsPermissionDialog dialog = new MockLocationsPermissionDialog();
            dialog.show(getSupportFragmentManager(), null);
        } else {
            sendLatLngToLocationMockerService();
        }
    }

    private void stopLocationMockerService() {
        Intent intent = new Intent(MapActivity.this, LocationMockerService.class);
        stopService(intent);
    }

    private void sendLatLngToLocationMockerService() {
        if (mMap != null) {
            Intent intent = new Intent(MapActivity.this, LocationMockerService.class);
            LatLng target = mMap.getCameraPosition().target;
            intent.putExtra("lat", target.latitude);
            intent.putExtra("lon", target.longitude);
            startService(intent);
        }
    }

    private void centerMap() {
        if (mMap != null && !mIsMapCentered) {
            SharedPreferences preferences = getSharedPreferences(
                    SHARE_PREFERENCES_NAME, Context.MODE_PRIVATE);

            LatLng latLng;
            if (mService != null) {
                latLng = new LatLng(mService.getLatitude(), mService.getLongitude());
            } else {
                try {
                    latLng = new LatLng(
                            Double.parseDouble(preferences.getString(KEY_LATITUDE, "41.3965032")),
                            Double.parseDouble(preferences.getString(KEY_LONGITUDE, "2.160502")));
                } catch (NumberFormatException e) {
                    latLng = null;
                }
            }

            float zoom = preferences.getFloat(KEY_ZOOM, 15);

            if (latLng != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
                mIsMapCentered = true;
            }
        }
    }

    private class LocationMockerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((LocationMockerService.Binder) iBinder).getService();
            updateButtons();
            centerMap();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            updateButtons();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            unbindLocationMockerService();
            bindLocationMockerService();
        }
    }
}
