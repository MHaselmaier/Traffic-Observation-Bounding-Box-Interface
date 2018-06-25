package ais.hskl.tobi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("deprecation")
public class BoundingBoxView extends ConstraintLayout implements TextureView.SurfaceTextureListener
{
    private Context context;
    private int cameraID;
    private Camera camera;
    private TextureView preview;
    private TextureView boundingBox;

    private AtomicBoolean isProcessing = new AtomicBoolean(false);

    private TobiNetwork tobi;
    private boolean showDebugInfo;

    private String[] detectedClasses;
    private Drawable[] signs;
    private long[] signsTiming;
    private int signSize;

    private volatile Constants.SIGNS lastSpeedSign = null;
    private MediaPlayer dangerSignSound;


    private static final int BATCH_SIZE = 1;
    private static final int COLOR_CHANNELS = 3;

    private static final int DETECTION_BOX_COLOR = Color.RED;

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
        this.dangerSignSound = MediaPlayer.create(context, R.raw.danger_sound);
        this.dangerSignSound.setAudioStreamType(AudioManager.STREAM_MUSIC);

        loadSigns();
    }

    private void loadSigns()
    {
        this.signSize = this.context.getResources().getDimensionPixelSize(R.dimen.sign_size);

        Constants.SIGNS[] definedSigns = Constants.SIGNS.values();
        this.signs = new Drawable[definedSigns.length];
        this.signsTiming = new long[definedSigns.length];
        for (int i = 0; this.signs.length > i; ++i)
        {
            this.signs[i] = this.context.getResources().getDrawable(Constants.SIGNS.mapSignToImageResource(definedSigns[i]));
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        this.detectedClasses = this.context.getResources().getStringArray(R.array.detection_classes);

        this.preview = findViewById(R.id.textureBackground);
        this.preview.setSurfaceTextureListener(this);
        this.boundingBox = findViewById(R.id.textureForeground);
        this.boundingBox.setOpaque(false);
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
        return false;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
    }

    public Constants.SIGNS getLastSpeedSign()
    {
        return this.lastSpeedSign;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        if(this.isProcessing.compareAndSet(false, true)) {
            AsyncTask.execute(() ->
            {
                long start = System.nanoTime();

                Bitmap bitmap = Bitmap.createBitmap(BoundingBoxView.this.preview.getBitmap());
                byte[] image = getImageData(bitmap);

                TobiNetwork.DetectedObject[] objects = this.tobi.predict(image, bitmap.getWidth(), bitmap.getHeight());

                this.lastSpeedSign = findSpeedSignInPredictionResult(objects, this.lastSpeedSign);

                if(findDangerSignInPredictionResult(objects) && ((MainActivity) context).isDebugModeEnabled())
                    this.dangerSignSound.start();

                Canvas canvas = this.boundingBox.lockCanvas();
                if (null != canvas)
                {
                    //clear canvas before drawing the new boxes
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    drawBoundingBoxes(canvas, objects);
                    drawSigns(canvas, objects);
                    drawFPS(canvas, start);

                    this.boundingBox.unlockCanvasAndPost(canvas);
                }

                this.isProcessing.set(false);

                long end = System.nanoTime();
                Log.d("ok", "Detected Objects in: " + (end - start) / 1_000_000_000f + "s");
            });
        }
    }

    private Constants.SIGNS findSpeedSignInPredictionResult(TobiNetwork.DetectedObject[] detectedObjects, Constants.SIGNS lastSpeedSign)
    {
        for(TobiNetwork.DetectedObject detectedObject : detectedObjects){
            Constants.SIGNS objectSign = detectedObject.getDetectedClass();

            switch(objectSign)
            {
                case SPEED_LIMIT_30:
                case SPEED_LIMIT_50:
                case SPEED_LIMIT_60:
                case SPEED_LIMIT_70:
                case SPEED_LIMIT_80:
                case SPEED_LIMIT_100:
                case SPEED_LIMIT_120:
                    return objectSign;

                case END_SPEED_LIMIT_80:
                case END_RESTRICTION_ALL:
                    return objectSign;
            }
        }

        return lastSpeedSign;
    }

    private boolean findDangerSignInPredictionResult(TobiNetwork.DetectedObject[] detectedObjects)
    {
        for(TobiNetwork.DetectedObject detectedObject : detectedObjects)
        {
            Constants.SIGNS objectSign = detectedObject.getDetectedClass();
            if(objectSign == Constants.SIGNS.DANGER)
                return true;
        }

        return false;
    }

    public void setupPreview()
    {
        if (null != this.preview && this.preview.isAvailable())
        {
            setupCameraInstance();
            if (null != this.camera) {
                try {
                    this.camera.setPreviewTexture(this.preview.getSurfaceTexture());
                    this.camera.startPreview();
                } catch (IOException ioe) {
                    Toast.makeText(this.context, "Es trat ein Fehler beim erstellen der Preview auf!", Toast.LENGTH_LONG).show();
                    Log.e(BoundingBoxView.class.getSimpleName(), ioe.toString());
                }
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

    private void drawBoundingBoxes(Canvas canvas, TobiNetwork.DetectedObject[] detectedObjects)
    {
        Paint boxPaint = new Paint();
        boxPaint.setColor(DETECTION_BOX_COLOR);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5);

        Paint fontPaint = new Paint();
        fontPaint.setColor(DETECTION_BOX_COLOR);
        fontPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fontPaint.setStrokeWidth(1);
        fontPaint.setTextSize(30);

        for (TobiNetwork.DetectedObject object : detectedObjects)
        {
            float[] rect = object.getBox();
            canvas.drawRect(rect[LEFT] * canvas.getWidth(), rect[TOP] * canvas.getHeight(), rect[RIGHT] * canvas.getWidth(), rect[BOTTOM] * canvas.getHeight(), boxPaint);
            if (this.showDebugInfo)
            {
                String detectedClassString = this.detectedClasses[object.getDetectedClass().ordinal()];
                canvas.drawText((int)(object.getScore() * 100) + "% " + detectedClassString, rect[LEFT] * canvas.getWidth(), rect[BOTTOM] * canvas.getHeight() + 30, fontPaint);
            }
        }
    }

    private void drawSigns(Canvas canvas, TobiNetwork.DetectedObject[] detectedObjects)
    {
        updateSignsTiming(detectedObjects);

        int count = 0;
        for (int i = 0; this.signsTiming.length > i; ++i)
        {
            if (SystemClock.uptimeMillis() < this.signsTiming[i])
            {
                this.signs[i].setBounds(count * this.signSize, canvas.getHeight() - this.signSize, (count + 1) * this.signSize, canvas.getHeight());
                this.signs[i].draw(canvas);
                ++count;
            }
        }
    }

    private void drawFPS(Canvas canvas, long start)
    {
        if (this.showDebugInfo)
        {
            Paint fontPaint = new Paint();
            fontPaint.setColor(DETECTION_BOX_COLOR);
            fontPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            fontPaint.setStrokeWidth(1);
            fontPaint.setTextSize(30);

            float fps = 1_000_000_000f / (System.nanoTime() - start);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f FPS", fps), canvas.getWidth() - 105, 30, fontPaint);
        }
    }

    private void updateSignsTiming(TobiNetwork.DetectedObject[] detectedObjects)
    {
        for (TobiNetwork.DetectedObject detectedObject: detectedObjects)
        {
            this.signsTiming[detectedObject.getDetectedClass().ordinal()] = SystemClock.uptimeMillis() + 3000;
        }
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

    public void releaseCamera()
    {
        if (null != this.camera)
        {
            this.camera.stopPreview();
            this.camera.release();
        }
    }
}
