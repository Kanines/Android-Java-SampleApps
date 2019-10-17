package com.sampleapps;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class TextRecognitionService extends Service {
    private static final String TAG = "HelloService";

    private boolean isRunning = false;
    private CameraSource cameraSource;

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {

        Log.i(TAG, "Service onStartCommand");

        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();

            try {
                cameraSource.start();
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }

            //Creating new thread for my service
            //Always write your long running tasks in a separate thread, to avoid ANR
            new Thread(new Runnable() {
                @Override
                public void run() {

                    textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                        @Override
                        public void release() {
                        }

                        @Override
                        public void receiveDetections(Detector.Detections<TextBlock> detections) {

                            final SparseArray<TextBlock> items = detections.getDetectedItems();
                            if (items.size() != 0) {
                                MainActivity.recognizedTextView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        for (int i = 0; i < items.size(); ++i) {
                                            TextBlock item = items.valueAt(i);
                                            stringBuilder.append(item.getValue());
                                            stringBuilder.append("\n");
                                        }
                                        MainActivity.recognizedTextView.setText(stringBuilder.toString());

                                        if(stringBuilder.toString().equals("BACK\n")) {
                                            sendBroadcast(new Intent(BroadcastActions.FinishActivity.toString()));
                                            stringBuilder.append("done");
                                        }
                                        else if(stringBuilder.toString().equals("VRCAMERA\n")) {
                                            sendBroadcast(new Intent(BroadcastActions.VrCameraActivity.toString()));
                                            stringBuilder.append("done");
                                        }
                                        else if(stringBuilder.toString().equals("COMPASS\n")) {
                                            sendBroadcast(new Intent(BroadcastActions.CompassActivity.toString()));
                                            stringBuilder.append("done");
                                        }
                                        else if(stringBuilder.toString().equals("MP3\n")) {
                                            sendBroadcast(new Intent(BroadcastActions.Mp3PlayerRecorderActivity.toString()));
                                            stringBuilder.append("done");
                                        }
                                        else if(stringBuilder.toString().equals("HOME\n")) {
                                            sendBroadcast(new Intent(BroadcastActions.ExitApp.toString()));
                                            stringBuilder.append("done");
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }).start();
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        Log.i(TAG, "Service onDestroy");
    }
}
