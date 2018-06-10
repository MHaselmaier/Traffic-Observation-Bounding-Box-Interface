package ais.hskl.tobi;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Stack;


@SuppressWarnings("deprecation")
public class BoundingBoxView implements TextureView.SurfaceTextureListener, TobiNetwork.DetectedObjectHandler
{
    private Activity activity;
    private int cameraID;
    private Camera camera;
    private TextureView preview;
    private TextureView boundingBox;

    private TobiNetwork tobi;
    private boolean showDebugInfo;

    private static final int BATCH_SIZE = 1;
    private static final int COLOR_CHANNELS = 3;

    private static final int COLOR_FRAME_SIZE = 3;
    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;

    private static final int TOP = 0;
    private static final int LEFT = 1;
    private static final int BOTTOM = 2;
    private static final int RIGHT = 3;

    public BoundingBoxView(Activity activity, TextureView preview, TextureView boundingBox, TobiNetwork tobi)
    {
        this.activity = activity;
        this.preview = preview;
        this.preview.setSurfaceTextureListener(this);
        this.boundingBox = boundingBox;
        this.boundingBox.setOpaque(true);
        this.tobi = tobi;

        if (this.preview.isAvailable())
        {
            setupPreview(this.preview.getSurfaceTexture());
        }
    }

    public void showDebugInfo(boolean showDebugInfo)
    {
        this.showDebugInfo = showDebugInfo;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        setupPreview(surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        if (null != this.camera)
        {
            this.camera.stopPreview();
            this.camera.release();
            return true;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
    }

    Bitmap bitmap;

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        bitmap = this.preview.getBitmap();
        byte[] image = getImageData(bitmap);

        this.tobi.predict(this, this.preview.getBitmap(), 1, bitmap.getHeight(), bitmap.getWidth(), 3);
    }

    long start;
    @Override
    public void handleDetectedObjects(Bitmap bitmap, TobiNetwork.DetectedObject[] detectedObjects)
    {
        long end = System.nanoTime();
        Log.d("ok", ((end - start) / 1_000_000_000f) + "s");
        start = end;
        drawBitmapWithBoundingBoxes(bitmap, detectedObjects);
    }

    private void setupPreview(SurfaceTexture surface)
    {
        setupCameraInstance();
        if (null != this.camera)
        {
            try
            {
                this.camera.setPreviewTexture(surface);
                this.camera.startPreview();
            }
            catch (IOException ioe)
            {
                Toast.makeText(this.activity, "Es trat ein Fehler beim erstellen der Preview auf!", Toast.LENGTH_LONG).show();
                Log.e(BoundingBoxView.class.getSimpleName(),ioe.toString());
            }
        }
    }

    private byte[] getImageData(Bitmap bitmap)
    {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        byte[] image = new byte[BATCH_SIZE * bitmap.getHeight() * bitmap.getWidth() * COLOR_CHANNELS];
        for (int i = 0; pixels.length > i; ++i)
        {
            image[i * COLOR_FRAME_SIZE + RED] = (byte)((pixels[i] & 0xff0000) >> 16);
            image[i * COLOR_FRAME_SIZE + GREEN] = (byte)((pixels[i] & 0xff00) >> 8);
            image[i * COLOR_FRAME_SIZE + BLUE] = (byte)(pixels[i] & 0xff);
        }

        return image;
    }

    private void drawBitmapWithBoundingBoxes(Bitmap bitmap, TobiNetwork.DetectedObject[] objects)
    {
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5);

        Paint fontPaint = new Paint();
        fontPaint.setColor(Color.RED);
        fontPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fontPaint.setStrokeWidth(1);
        fontPaint.setTextSize(30);

        Canvas canvas = this.boundingBox.lockCanvas();
        if (null != canvas)
        {
            canvas.drawBitmap(bitmap, 0, 0, null);
            for (TobiNetwork.DetectedObject object : objects) {
                float[] rect = object.getBox();
                canvas.drawRect(rect[LEFT] * bitmap.getWidth(), rect[TOP] * bitmap.getHeight(), rect[RIGHT] * bitmap.getWidth(), rect[BOTTOM] * bitmap.getHeight(), boxPaint);
                if (this.showDebugInfo)
                {
                    canvas.drawText((int)(object.getScore() * 100) + "% " + object.getDetectedClass(), rect[LEFT] * bitmap.getWidth(), rect[BOTTOM] * bitmap.getHeight() + 30, fontPaint);
                }
            }
        }
        this.boundingBox.unlockCanvasAndPost(canvas);

    }

    private void setupCameraInstance()
    {
        try
        {
            this.cameraID = getPrimaryCameraID();
            this.camera = Camera.open(this.cameraID);
            setupCameraOrientation();
            setupCameraAutoFocus();
        }
        catch(Exception e)
        {
            Toast.makeText(this.activity, "Es konnte nicht auf die Kamera zugegriffen werden!", Toast.LENGTH_LONG).show();
            Log.e(BoundingBoxView.class.getSimpleName(), e.toString());
        }
    }

    private int getPrimaryCameraID()
    {
        for (int i = 0; Camera.getNumberOfCameras() > i; ++i)
        {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                return i;
            }
        }
        return -1;
    }

    private void setupCameraOrientation()
    {
        int rotation = this.activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation)
        {
        case Surface.ROTATION_0:
            degrees = 0;
            break;
        case Surface.ROTATION_90:
            degrees = 90;
            break;
        case Surface.ROTATION_180:
            degrees = 180;
            break;
        case Surface.ROTATION_270:
            degrees = 270;
            break;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(this.cameraID, info);
        this.camera.setDisplayOrientation((info.orientation - degrees + 360) % 360);
    }

    private void setupCameraAutoFocus()
    {
        Camera.Parameters parameters = this.camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        this.camera.setParameters(parameters);
    }
}
