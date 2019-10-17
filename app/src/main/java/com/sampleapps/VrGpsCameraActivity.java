package com.sampleapps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class VrGpsCameraActivity extends GvrActivity implements GvrView.StereoRenderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "VRCamMtMMainAc";
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private static Camera camera = null;

    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;
    private int mProgram;
    private int mPositionHandle;

    private int mTextureCoordHandle;
    private Intent intentService;

    private LocationManager locationManager;
    private Geocoder geocoder;
    private List<Address> addresses;
    private String address = "";
    private Location currentLocation;
    private double latitude;
    private double longitude;
    private TextView streetNameTextView;
    private TextView infoTextView;
    private ImageView streetPreviewImageView;


    // Street View Preview size
    private int svpWidth = 200;
    private int svpHeight = 200;

    // Default GPS localization - Galeria Bałtycka
    private double defaultLat = 54.3825891;
    private double defaultLon = 18.5985922;


    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    static float squareVertices[] = { // in counterclockwise order:
            -1.0f, -1.0f,   // left - mid
            1.0f, -1.0f,    // right - mid
            -1.0f, 1.0f,    // left - top
            1.0f, 1.0f,     // right - top

    };

    private short drawOrder[] = {0, 2, 1, 1, 2, 3}; // order to draw vertices
    //private short drawOrder2[] = {2, 0, 3, 3, 0, 1}; // order to draw vertices

    static float textureVertices[] = {
            0.0f, 1.0f,  // left-bottom
            1.0f, 1.0f,  // right-bottom
            0.0f, 0.0f,  // left-top
            1.0f, 0.0f   // right-top
    };

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private int texture;
    private GvrView cardboardView;
    private SurfaceTexture surface;
    private float[] mView;
    private float[] mCamera;

    public void startCamera(int texture) {
        surface = new SurfaceTexture(texture);
        surface.setOnFrameAvailableListener(this);

        camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        camera.setParameters(params);
        try {
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (IOException ioe)
        {
            Log.i(TAG, "CAM LAUNCH FAILED");
        }
    }

    static private int createTexture() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
       // GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
       //         GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(com.sampleapps.R.layout.activity_camera);
        }catch (Exception ex)
        {
            System.out.println(ex.toString());
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Load default GPS localization
        latitude = defaultLat;
        longitude = defaultLon;

        cardboardView = (GvrView) findViewById(com.sampleapps.R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setGvrView(cardboardView);

        geocoder = new Geocoder(this, Locale.getDefault());

        streetNameTextView = (TextView) findViewById(com.sampleapps.R.id.streetNameTextView);
        streetNameTextView.setText("");
        infoTextView = (TextView) findViewById(com.sampleapps.R.id.infoTextView);
        infoTextView.setText("");
        streetPreviewImageView = (ImageView) findViewById(com.sampleapps.R.id.streetPreviewImageView);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        registerLocationListener();

        mCamera = new float[16];
        mView = new float[16];

        processLocation();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        intentService = new Intent(this, TextRecognitionService.class);
        startService(intentService);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        intentService = new Intent(this, TextRecognitionService.class);
        startService(intentService);
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f);

        ByteBuffer bb = ByteBuffer.allocateDirect(squareVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareVertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        int vertexShader = Shaders.loadVertexShader();
        int fragmentShader = Shaders.loadFragmentShader();

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        texture = createTexture();
        startCamera(texture);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);


        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, vertexBuffer);


        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, textureVerticesBuffer);


        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);

        Matrix.multiplyMM(mView, 0, eye.getEyeView(), 0, mCamera, 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    private void registerLocationListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 0.1f, locationListenerGPS);
    }

    private LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentLocation = location;
            processLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void processLocation() {
        if (currentLocation != null) {
            latitude = currentLocation.getLatitude();
            longitude = currentLocation.getLongitude();
            infoTextView.setText("");
        } else {

            infoTextView.setText("nie odnaleziono GPS\nskorzystano z domyślnej lokalizacji");
        }

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            address = addresses.get(0).getAddressLine(0);
        } catch (IOException ex) {
            // It is unnecessary to handle this exception
        }

        streetNameTextView.setText(address);

        String StreetViewImageUrl = "https://maps.googleapis.com/maps/api/streetview?size=" +
                Integer.toString(svpWidth)  + "x" + Integer.toString(svpHeight) + "&location=";
        StreetViewImageUrl += Double.toString(latitude) + "," + Double.toString(longitude);
        StreetViewImageUrl += "&key=AIzaSyCqJ8sKDStTK9ZwFbI1tV1FzAlWdL8csMo";

        Picasso.with(this).load(StreetViewImageUrl).into(target);
    }

    private Target target = new Target() {
        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            streetPreviewImageView.setImageBitmap(bitmap);
        }
    };
}
