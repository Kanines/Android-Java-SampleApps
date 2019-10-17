package com.sampleapps;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class PermissionsHandler {

    private Activity activity;
    private boolean permissionsStatus = true;
    private static final int REQUEST_PERMISSION_CODE = 1;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
    };

    public PermissionsHandler(Activity activity) {

        this.activity = activity;
    }

    public void verifyRequiredPermissions() {

        for (String permission : REQUIRED_PERMISSIONS) {

            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsStatus = false;
                ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_PERMISSION_CODE);
                return;
            }
        }
    }

    public boolean isPermissionGranted() {
        return permissionsStatus;
    }

    public void setPermissionsStatus(boolean status) {
        permissionsStatus = status;
    }
}
