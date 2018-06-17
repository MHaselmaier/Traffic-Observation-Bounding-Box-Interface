package ais.hskl.tobi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class BoundingBoxView extends ConstraintLayout implements TextureView.SurfaceTextureListener
{
    private Context context;
    private int cameraID;
    private Camera camera;
    private TextureView preview;
    private TextureView boundingBox;
    private SignsView signs;

    private TobiNetwork tobi;
    private boolean showDebugInfo;

    private String[] detectedClasses;

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

    public BoundingBoxView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        this.detectedClasses = this.context.getResources().getStringArray(R.array.detection_classes);

        this.preview = findViewById(R.id.textureBackground);
        this.preview.setSurfaceTextureListener(this);
        this.boundingBox = findViewById(R.id.textureForeground);
        this.signs = findViewById(R.id.signs);

        if (this.preview.isAvailable())
        {
            setupPreview();
        }
    }

    public void setTobiNetwork(TobiNetwork tobi)
    {
        this.tobi = tobi;
    }

    public void showDebugInfo(boolean showDebugInfo)
    {
        this.showDebugInfo = showDebugInfo;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        setupPreview();
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

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        Bitmap bitmap = this.preview.getBitmap();
        byte[] image = getImageData(bitmap);

        long start = System.nanoTime();
        TobiNetwork.DetectedObject[] objects = this.tobi.predict(image, bitmap.getHeight(), bitmap.getWidth());
        long end = System.nanoTime();
        Log.d("ok", "Detected Objects in: " + (end - start) / 1_000_000_000f + "s");
        Log.d("ok", "DetectedObjects: " + objects.length);

        drawBitmapWithBoundingBoxes(bitmap, objects);
        this.signs.updateSigns(objects);
    }

    public void setupPreview()
    {
        setupCameraInstance();
        if (null != this.camera)
        {
            try
            {
                this.camera.setPreviewTexture(this.preview.getSurfaceTexture());
                this.camera.startPreview();
            }
            catch (IOException ioe)
            {
                Toast.makeText(this.context, "Es trat ein Fehler beim erstellen der Preview auf!", Toast.LENGTH_LONG).show();
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
                    String detectedClassString = this.detectedClasses[object.getDetectedClass().ordinal()];
                    canvas.drawText((int)(object.getScore() * 100) + "% " + detectedClassString, rect[LEFT] * bitmap.getWidth(), rect[BOTTOM] * bitmap.getHeight() + 30, fontPaint);
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
            Toast.makeText(this.context, "Es konnte nicht auf die Kamera zugegriffen werden!", Toast.LENGTH_LONG).show();
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
        WindowManager windowManager = (WindowManager)this.context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
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
