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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;

import java.io.IOException;


@SuppressWarnings("deprecation")
public class BoundingBoxView implements TextureView.SurfaceTextureListener
{
    private Activity activity;
    private int cameraID;
    private Camera camera;
    private TextureView preview;
    private TextureView boundingBox;
    private FlexboxLayout signs;

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

    public BoundingBoxView(Activity activity, TextureView preview, TextureView boundingBox, FlexboxLayout signs, TobiNetwork tobi)
    {
        this.activity = activity;
        this.preview = preview;
        this.preview.setSurfaceTextureListener(this);
        this.boundingBox = boundingBox;
        this.boundingBox.setOpaque(true);
        this.signs = signs;
        this.tobi = tobi;

        this.detectedClasses = activity.getResources().getStringArray(R.array.detection_classes);

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

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        Bitmap bitmap = this.preview.getBitmap();
        byte[] image = getImageData(bitmap);

        long start = System.nanoTime();
        TobiNetwork.DetectedObject[] objects = this.tobi.predict(image, 1, bitmap.getHeight(), bitmap.getWidth(), 3);
        long end = System.nanoTime();
        Log.d("ok", "Detected Objects in: " + (end - start) / 1_000_000_000f + "s");
        Log.d("ok", "DetectedObjects: " + objects.length);

        drawBitmapWithBoundingBoxes(bitmap, objects);
        addDetectedSignsToView(objects);
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
                    String detectedClassString = this.detectedClasses[object.getDetectedClass().ordinal()];
                    canvas.drawText((int)(object.getScore() * 100) + "% " + detectedClassString, rect[LEFT] * bitmap.getWidth(), rect[BOTTOM] * bitmap.getHeight() + 30, fontPaint);
                }
            }
        }
        this.boundingBox.unlockCanvasAndPost(canvas);
    }

    private void addDetectedSignsToView(TobiNetwork.DetectedObject[] detectedObjects)
    {
        for (TobiNetwork.DetectedObject detectedObject: detectedObjects)
        {
            ImageView sign = new ImageView(this.activity);
            sign.setImageResource(mapDetectedClassToImageResource(detectedObject.getDetectedClass()));

            int size = this.activity.getResources().getDimensionPixelSize(R.dimen.sign_size);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(size, size);
            sign.setLayoutParams(layoutParams);

            this.signs.addView(sign);
        }
    }

    private int mapDetectedClassToImageResource(Constants.SIGNS detectedClass)
    {
        switch (detectedClass)
        {
        case SPEED_LIMIT_30:
            //return R.drawable.SPEED_LIMIT_30;
        case SPEED_LIMIT_50:
            //return R.drawable.SPEED_LIMIT_50;
        case SPEED_LIMIT_60:
            return R.drawable.speed_limit_60;
        case SPEED_LIMIT_70:
            //return R.drawable.SPEED_LIMIT_70;
        case SPEED_LIMIT_80:
            //return R.drawable.SPEED_LIMIT_80;
        case END_SPEED_LIMIT_80:
            //return R.drawable.END_SPEED_LIMIT_80;
        case SPEED_LIMIT_100:
            //return R.drawable.SPEED_LIMIT_100;
        case SPEED_LIMIT_120:
            //return R.drawable.SPEED_LIMIT_120;
        case NO_OVERTAKING:
            return R.drawable.no_overtaking;
        case NO_OVERTAKING_TRUCK:
            return R.drawable.no_overtaking_truck;
        case RIGHT_OF_WAY:
            return R.drawable.right_of_way;
        case MAJOR_ROAD:
            return R.drawable.major_road;
        case GIVE_WAY:
            return R.drawable.give_way;
        case STOP:
            return R.drawable.stop;
        case RESTRICTION_ALL:
            return R.drawable.restriction_all;
        case RESTRICTION_TRUCK:
            return R.drawable.restriction_truck;
        case RESTRICTION_ENTRY:
            return R.drawable.restriction_entry;
        case DANGER:
            return R.drawable.danger;
        case CURVE_LEFT:
            return R.drawable.curve_left;
        case CURVE_RIGHT:
            return R.drawable.curve_right;
        case DOUBLE_CURVE:
            return R.drawable.double_curve;
        case UNEVEN_ROAD:
            return R.drawable.uneven_road;
        case SLIPPERY_ROAD:
            return R.drawable.slippery_road;
        case NARROW_ROAD:
            return R.drawable.narrow_road;
        case CONSTRUCTION:
            return R.drawable.construction;
        case TRAFFIC_LIGHT:
            return R.drawable.traffic_light;
        case PEDESTRIAN:
            return R.drawable.pedestrian;
        case CHILDREN:
            return R.drawable.children;
        case BIKE:
            return R.drawable.bike;
        case SNOW_ICE:
            return R.drawable.snow_ice;
        case ANIMALS:
            return R.drawable.animals;
        case END_RESTRICTION_ALL:
            return R.drawable.end_restriction_all;
        case RIGHT:
            return R.drawable.right;
        case LEFT:
            return R.drawable.left;
        case STRAIGHT:
            return R.drawable.straight;
        case RIGHT_OR_STRAIGHT:
            return R.drawable.right_or_straight;
        case LEFT_OR_STRAIGHT:
            return R.drawable.left_or_straight;
        case PASS_RIGHT:
            return R.drawable.pass_right;
        case PASS_LEFT:
            return R.drawable.pass_left;
        case ROUND_ABOUT:
            return R.drawable.round_about;
        case END_NO_OVERTAKING:
            return R.drawable.end_no_overtaking;
        case END_NO_OVERTAKING_TRUCKS:
            return R.drawable.end_no_overtaking_truck;
        }
        return -1;
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
