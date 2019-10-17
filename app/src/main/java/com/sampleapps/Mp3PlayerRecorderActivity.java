package com.sampleapps;

import android.Manifest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.content.DialogInterface;

public class Mp3PlayerRecorderActivity extends AppCompatActivity implements View.OnClickListener {

    Button StartButton;
    Button StopButton;
    Button ReloadButton;
    Button StartRecButton;
    Button StopRecButton;
    Button VolumeUpButton;
    Button VolumeDownButton;
    RadioButton RecordingIndicator;
    MediaPlayer mediaPlayer;
    MediaRecorder mediaRecorder;
    TextView currentFile;
    TextView volume;
    String dirPath = "./storage/emulated/0/Music/";

    VisualizerView  mVisualizerView;
    private Visualizer mVisualizer;

    ListView songsList;

    static List<MediaPlayer> mediaPlayers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.sampleapps.R.layout.activity_mp3_player_recorder);

        StartButton = (Button) findViewById(com.sampleapps.R.id.startButton);
        StopButton = (Button) findViewById(com.sampleapps.R.id.stopButton);
        ReloadButton = (Button) findViewById(com.sampleapps.R.id.reloadButton);
        StartRecButton = (Button) findViewById(com.sampleapps.R.id.startRecButton);
        StopRecButton = (Button) findViewById(com.sampleapps.R.id.stopRecButton);
        VolumeUpButton = (Button) findViewById(com.sampleapps.R.id.volumeUp);
        VolumeDownButton = (Button) findViewById(com.sampleapps.R.id.volumeDown);
        RecordingIndicator = (RadioButton) findViewById(com.sampleapps.R.id.recordingIndicator);
        songsList = (ListView) findViewById(com.sampleapps.R.id.listView);
        currentFile = (TextView) findViewById(com.sampleapps.R.id.currentFileTextBox);
        volume = (TextView) findViewById(com.sampleapps.R.id.volumeTextBox);


        mVisualizerView = (VisualizerView) findViewById(com.sampleapps.R.id.myvisualizerview);

        StartButton.setOnClickListener(this);
        StopButton.setOnClickListener(this);
        ReloadButton.setOnClickListener(this);
        StartRecButton.setOnClickListener(this);
        StopRecButton.setOnClickListener(this);
        VolumeUpButton.setOnClickListener(this);
        VolumeDownButton.setOnClickListener(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        reloadList();

        songsList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                if(arg0 != null)
                    currentFile.setText((String) arg0.getItemAtPosition(position));
            }
        });
    }

    @Override
    public void onClick(View v) {
        if(currentFile.getText() == "none")
            return;

        if(v == StartButton) {
            mediaPlayer = new MediaPlayer();
            mediaPlayers.add(mediaPlayer);
            String filePath = dirPath + currentFile.getText();
            File file = new File(filePath);


            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            //mediaPlayer = MediaPlayer.create(this, R.raw.test);




            try {
                mediaPlayer.setDataSource(file.getAbsolutePath());

                int intValue = Integer.parseInt(volume.getText().toString());
                float volumeValue = (float) intValue * 0.1f;

                mediaPlayer.prepare();
                mediaPlayer.setVolume(volumeValue, volumeValue);

                setupVisualizerFxAndUI();

                mVisualizer.setEnabled(true);

            } catch (IOException e) {
                Log.e("dupa", e.getMessage().toString());
            }
            //mediaPlayer.setVolume(volumeValue, volumeValue);

            mediaPlayer
                    .setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            mVisualizer.setEnabled(false);
                        }
                    });
            mediaPlayer.start();


        }

        if(v == StopButton){

            if(mediaPlayers.size() > 0){
                //MediaPlayer lastMediaPlayer = mediaPlayers.get(mediaPlayers.size() - 1);
                MediaPlayer lastMediaPlayer = mediaPlayers.get(0);
                lastMediaPlayer.stop();
                lastMediaPlayer.release();
                lastMediaPlayer = null;
                //mediaPlayers.remove(mediaPlayers.size() - 1);
                mediaPlayers.remove(0);
            }
        }

        if(v == ReloadButton){
            reloadList();
        }

        if(v == StartRecButton){

            File dir = new File(dirPath);
            File recordFile = new File(dirPath);

            try {
                recordFile = File.createTempFile("recordedFile", ".3gpp", dir);
            } catch (IOException e) {
                Log.e("dupa2", e.getMessage().toString());
                e.printStackTrace();
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioSamplingRate(8000);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            mediaRecorder.setOutputFile(recordFile.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                Log.e("dupa3", e.getMessage().toString());
                e.printStackTrace();
            }
            mediaRecorder.start();
            RecordingIndicator.setChecked(true);
        }

        if(v == StopRecButton)
        {
            mediaRecorder.stop();

            mediaRecorder.release();
            RecordingIndicator.setChecked(false);
        }

        if(v == VolumeUpButton)
        {
            int currentVolume = Integer.parseInt(volume.getText().toString());
            if (currentVolume < 10) {
                currentVolume += 1;
                volume.setText(Integer.toString(currentVolume));
            }
        }

        if(v == VolumeDownButton)
        {
            int currentVolume = Integer.parseInt(volume.getText().toString());
            if (currentVolume > 0) {
                currentVolume -= 1;
                volume.setText(Integer.toString(currentVolume));
            }
        }
    }

    void reloadList()
    {
        File dir = new File(dirPath);
        File[] fileList = dir.listFiles();
        if(fileList == null)
        {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage("Files not found in: \n\"" + dirPath + "\"");
            alertBuilder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            AlertDialog filesNotFoundAlert = alertBuilder.create();
            filesNotFoundAlert.show();
            return;
        }

        String[] theNamesOfFiles = new String[fileList.length];

        for (int i = 0; i < theNamesOfFiles.length; i++) {
            theNamesOfFiles[i] = fileList[i].getName();
        }
        songsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, theNamesOfFiles));
    }

    private void setupVisualizerFxAndUI() {

        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] bytes, int samplingRate) {
                        mVisualizerView.updateVisualizer(bytes);
                    }

                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] bytes, int samplingRate) {
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, false);
    }
}
