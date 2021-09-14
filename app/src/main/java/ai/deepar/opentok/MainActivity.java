package ai.deepar.opentok;

import ai.deepar.ar.ARErrorType;
import ai.deepar.ar.AREventListener;
import ai.deepar.ar.CameraResolutionPreset;
import ai.deepar.ar.DeepAR;
import ai.deepar.ar.DeepARImageFormat;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import android.widget.Toast;
import android.view.View;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.appcompat.app.AppCompatActivity;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, AREventListener, EasyPermissions.PermissionCallbacks  {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Default camera lens value, change to CameraSelector.LENS_FACING_BACK to initialize with back camera
    private int defaultLensFacing = CameraSelector.LENS_FACING_FRONT;
    private int lensFacing = defaultLensFacing;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ByteBuffer[] buffers;
    private int currentBuffer = 0;
    private static final int NUMBER_OF_BUFFERS=2;

    private DeepAR deepAR;
    private SurfaceView surfaceView;
    private boolean callInProgress;

    private Session session;
    private Publisher publisher;

    private VideoCustomCapturer videoCustomCapturer;
    private static final int PERMISSIONS_REQUEST_CODE = 124;

    public boolean isCallInProgress() {
        return callInProgress;
    }

    public void setCallInProgress(boolean callInProgress) {
        this.callInProgress = callInProgress;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if(!ApplicationConfig.isValid()) {
            finishWithMessage("Invalid ApplicationConfig. " + ApplicationConfig.getDescription());
            return;
        }

        deepAR = new DeepAR(this);
        deepAR.setLicenseKey(ApplicationConfig.DEEPAR_LICENSEKEY);
        deepAR.initialize(this, this);

        setContentView(R.layout.activity_main);

        setCallInProgress(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ": " + perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        finishWithMessage("onPermissionsDenied: " + requestCode + ": " + perms);
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        if (EasyPermissions.hasPermissions(this, perms)) {
            setup();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.request_permission), PERMISSIONS_REQUEST_CODE, perms);
        }
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        requestPermissions();
    }

    void setup() {
        Log.i(TAG, "setup");
        setupCamera();
        setupView();

        final Button btn = findViewById(R.id.startCall);

        btn.setOnClickListener(v -> {
            if (isCallInProgress()) {
                setCallInProgress(false);
                disconnectSession();
                btn.setText(R.string.connect);
            } else {
                setCallInProgress(true);
                connectSession();
                btn.setText(R.string.disconnect);
            }
        });
    }

    private void connectSession() {
        Log.i(TAG, "connectSession");
        session = new Session.Builder(this, ApplicationConfig.API_KEY, ApplicationConfig.SESSION_ID)
                 .build();
        session.setSessionListener(sessionListener);
        session.connect(ApplicationConfig.TOKEN);
    }

    private void disconnectSession() {
        Log.i(TAG, "disconnectSession");
        session.disconnect();
    }

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.i(TAG, "onConnected: Connected to session " + session.getSessionId());

            setCallInProgress(true);

            videoCustomCapturer = new VideoCustomCapturer(surfaceView);
            publisher = new Publisher.Builder(MainActivity.this)
                    .capturer(videoCustomCapturer)
                    .build();

            publisher.setPublisherListener(publisherListener);
            publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

            session.publish(publisher);
        }

        @Override
        public void onDisconnected(Session session) {
            Log.i(TAG, "onDisconnected: disconnected from session " + session.getSessionId());
            MainActivity.this.session = null;
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.i(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
        }
    };

    private PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            Log.i(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            Log.i(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            finishWithMessage("PublisherKit error: " + opentokError.getMessage());
        }
    };

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    /*
        get interface orientation from
        https://stackoverflow.com/questions/10380989/how-do-i-get-the-current-orientation-activityinfo-screen-orientation-of-an-a/10383164
     */
    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }
        return orientation;
    }

    private void setupView() {
        Log.i(TAG, "setupView");
        surfaceView = findViewById(R.id.surface);

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deepAR.onClick();
            }
        });

        surfaceView.getHolder().addCallback(this);

        // Surface might already be initialized, so we force the call to onSurfaceChanged
        surfaceView.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
    }

    private void setupCamera() {
        Log.i(TAG, "setupCamera");
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        CameraResolutionPreset cameraPreset = CameraResolutionPreset.P640x480;
        int width;
        int height;
        int orientation = getScreenOrientation();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation ==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            width = cameraPreset.getWidth();
            height =  cameraPreset.getHeight();
        } else {
            width = cameraPreset.getHeight();
            height = cameraPreset.getWidth();
        }
        buffers = new ByteBuffer[NUMBER_OF_BUFFERS];
        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            buffers[i] = ByteBuffer.allocateDirect(width * height * 3);
            buffers[i].order(ByteOrder.nativeOrder());
            buffers[i].position(0);
        }

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(width, height)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                //image.getImageInfo().getTimestamp();
                byte[] byteData;
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                byteData = new byte[ySize + uSize + vSize];

                //U and V are swapped
                yBuffer.get(byteData, 0, ySize);
                vBuffer.get(byteData, ySize, vSize);
                uBuffer.get(byteData, ySize + vSize, uSize);

                buffers[currentBuffer].put(byteData);
                buffers[currentBuffer].position(0);
                if(deepAR != null) {
                    deepAR.receiveFrame(buffers[currentBuffer],
                            image.getWidth(), image.getHeight(),
                            image.getImageInfo().getRotationDegrees(),
                            lensFacing == CameraSelector.LENS_FACING_FRONT,
                            DeepARImageFormat.YUV_420_888,
                            image.getPlanes()[1].getPixelStride()
                    );
                }
                currentBuffer = ( currentBuffer + 1 ) % NUMBER_OF_BUFFERS;
                image.close();
            }
        });

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis);
    }

    @Override
    public void initialized() {
        Log.i(TAG, "initialized");
        deepAR.switchEffect("mask", ApplicationConfig.MASK);
        deepAR.startCapture();
    }

    @Override
    public void frameAvailable(Image image) {
        if (image != null) {
            if(videoCustomCapturer != null){
                videoCustomCapturer.sendFrame(image);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectSession();
        deepAR.release();
    }

    @Override
    public void screenshotTaken(Bitmap bitmap) {

    }

    @Override
    public void videoRecordingStarted() {

    }

    @Override
    public void videoRecordingFinished() {

    }

    @Override
    public void videoRecordingFailed() {

    }

    @Override
    public void videoRecordingPrepared() {

    }

    @Override
    public void shutdownFinished() {

    }

    @Override
    public void faceVisibilityChanged(boolean b) {

    }

    @Override
    public void imageVisibilityChanged(String s, boolean b) {

    }

    @Override
    public void error(ARErrorType arErrorType, String s) {

    }

    @Override
    public void effectSwitched(String s) {
        Log.i(TAG, "effectSwitched - "+ s);
    }

    public void onToggleButtonClicked(View view) {
        Log.i(TAG, "onToggleButtonClicked - ");
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.unmask);
        boolean checked = toggleButton.isChecked();
        Log.i(TAG, "onToggleButtonClicked - checked - "+ checked);
        if (checked){
            deepAR.switchEffect("mask", ApplicationConfig.MASK);
        }else{
            deepAR.switchEffect("mask", "");
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If we are using on screen rendering we have to set surface view where DeepAR will render
        deepAR.setRenderSurface(holder.getSurface(), width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        if (deepAR != null) {
            deepAR.setRenderSurface(null, 0, 0);
        }
    }
}