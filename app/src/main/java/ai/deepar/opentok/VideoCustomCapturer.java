package ai.deepar.opentok;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Image;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import com.opentok.android.BaseVideoCapturer;
import java.nio.ByteBuffer;

public class VideoCustomCapturer extends BaseVideoCapturer {

    private static final String TAG = VideoCustomCapturer.class.getSimpleName();

    private boolean capturing = false;
    private View contentView;
    private int fps = 15;
    private int width = 0;
    private int height = 0;
    private int[] frame;
    private Bitmap bmp;
    private Canvas canvas;
    private Handler mHandler = new Handler();

    private Runnable newFrame = new Runnable() {

        @Override
        public void run() {
            if (capturing) {
                int width = contentView.getWidth();
                int height = contentView.getHeight();
                if (frame == null || VideoCustomCapturer.this.width != width ||
                        VideoCustomCapturer.this.height != height) {
                    VideoCustomCapturer.this.width = width;
                    VideoCustomCapturer.this.height = height;
                    if (bmp != null) {
                        bmp.recycle();
                        bmp = null;
                    }
                    bmp = Bitmap.createBitmap(width,
                            height, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(bmp);
                    frame = new int[width * height];
                }
                canvas.save();
                canvas.translate(-contentView.getScrollX(), - contentView.getScrollY());
                contentView.draw(canvas);
                bmp.getPixels(frame, 0, width, 0, 0, width, height);
                provideIntArrayFrame(frame, ARGB, width, height, 0, false);
                canvas.restore();

                mHandler.postDelayed(newFrame, 1000 / fps);
            }
        }
    };

    public void sendFrame(Image image) {
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();

        int width = rowStride / pixelStride;
        provideBufferFrame(buffer, ABGR, width, image.getHeight(),0, false);
    }

    public VideoCustomCapturer(View view) {
        Log.i(TAG, "VideoCustomCapturer constructed");
        this.contentView = view;
    }

    @Override
    public void init() {
    }

    @Override
    public int startCapture() {
        Log.i(TAG, "startCapture");
        capturing = true;
        return 0;
    }

    @Override
    public int stopCapture() {
        Log.i(TAG, "stopCapture");
        capturing = false;
        mHandler.removeCallbacks(newFrame);
        return 0;
    }

    @Override
    public boolean isCaptureStarted() {
        Log.i(TAG, "isCaptureStarted");
        return capturing;
    }

    @Override
    public CaptureSettings getCaptureSettings() {
        CaptureSettings settings = new CaptureSettings();
        settings.fps = fps;
        settings.width = width;
        settings.height = height;
        settings.format = ARGB;
        return settings;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }
}