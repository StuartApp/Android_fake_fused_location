package com.stuart.fakefusedlocation;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class PermissionHelper {

    public static boolean hasRuntimePermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestRuntimePermissions(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                requestCode);
    }

    public static boolean hasMockLocationPermissions(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) context
                        .getSystemService(Context.APP_OPS_SERVICE);
                int mockLocationMode = opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(), BuildConfig.APPLICATION_ID);
                return (mockLocationMode == AppOpsManager.MODE_ALLOWED);
            } else {
                String allowMockLocation = Settings.Secure.getString(
                        context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION);
                return !allowMockLocation.equals("0");
            }
        } catch (SecurityException e) {
            return false;
        }
    }

    private PermissionHelper() {
        // No instance
    }
}
