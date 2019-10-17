package com.sampleapps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static TextView recognizedTextView;

    private ListView activitiesListView;
    private List<Class> activities = new ArrayList<Class>();
    private List<String> activitiesNames = new ArrayList<String>();
    private Intent intentService;
    private PermissionsHandler permissionsHandler;
    private IntentFilter intentFilter;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        permissionsHandler.setPermissionsStatus(true);
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsHandler.setPermissionsStatus(false);
                break;
            }
        }
        if (permissionsHandler.isPermissionGranted()) {
            startTextRecognitionService();
        } else {
            permissionsHandler.verifyRequiredPermissions();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.sampleapps.R.layout.activity_main);

        activitiesListView = (ListView) findViewById(com.sampleapps.R.id.listView);
        recognizedTextView = (TextView) findViewById(com.sampleapps.R.id.recognizedTextView);
        recognizedTextView.setText("");

        activities.add(VrGpsCameraActivity.class);
        activities.add(CompassActivity.class);
        activities.add(Mp3PlayerRecorderActivity.class);

        for (Class c : activities)
            activitiesNames.add(c.getSimpleName());

        activitiesListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, activitiesNames));
        activitiesListView.setOnItemClickListener(MainActivity.this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastActions.FinishActivity.toString());
        intentFilter.addAction(BroadcastActions.VrCameraActivity.toString());
        intentFilter.addAction(BroadcastActions.CompassActivity.toString());
        intentFilter.addAction(BroadcastActions.Mp3PlayerRecorderActivity.toString());
        intentFilter.addAction(BroadcastActions.ExitApp.toString());

        registerReceiver(bReceiver, intentFilter);

        permissionsHandler = new PermissionsHandler(this);
        permissionsHandler.verifyRequiredPermissions();

        if (permissionsHandler.isPermissionGranted()) {
            startTextRecognitionService();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(intentService);
        try {
            unregisterReceiver(bReceiver);
        } catch (IllegalArgumentException e) {
            // It is unnecessary to handle this exception
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Handler delayHandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                registerReceiver(bReceiver, intentFilter);
            }
        };
        delayHandler.postDelayed(runnable, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(bReceiver);
        } catch (IllegalArgumentException e) {
            // It is unnecessary to handle this exception
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        intent.setClass(this, activities.get(position));
        intent.putExtra("position", position);
        intent.putExtra("id", id);

        startActivity(intent);
    }

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastActions.FinishActivity.toString())) {
                finish();
            } else if (intent.getAction().equals(BroadcastActions.VrCameraActivity.toString())) {
                Intent nIntent = new Intent();
                nIntent.setClass(MainActivity.this, VrGpsCameraActivity.class);

                startActivity(nIntent);
            } else if (intent.getAction().equals(BroadcastActions.CompassActivity.toString())) {

                Intent nIntent = new Intent();
                nIntent.setClass(MainActivity.this, CompassActivity.class);
                startActivity(nIntent);

            } else if (intent.getAction().equals(BroadcastActions.Mp3PlayerRecorderActivity.toString())) {

                Intent nIntent = new Intent();
                nIntent.setClass(MainActivity.this, Mp3PlayerRecorderActivity.class);
                startActivity(nIntent);

            } else if (intent.getAction().equals(BroadcastActions.ExitApp.toString())) {
                MainActivity.this.moveTaskToBack(true);
            }
        }
    };

    private void startTextRecognitionService() {
        intentService = new Intent(this, TextRecognitionService.class);
        startService(intentService);
    }
}
