package ais.hskl.tobi;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.IOException;

public class BoundingBoxView implements TextureView.SurfaceTextureListener
{
    private Activity activity;
    private int cameraID;
    private Camera camera;
    private TextureView preview;
    private TextureView boundingBox;

    public BoundingBoxView(Activity activity, TextureView preview, TextureView boundingBox)
    {
        this.activity = activity;
        this.preview = preview;
        this.preview.setSurfaceTextureListener(this);
        this.boundingBox = boundingBox;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
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
                Toast.makeText(this.activity, "Es trat ein Fehler beim erstellen der Preview auf!", Toast.LENGTH_LONG);
                Log.e(BoundingBoxView.class.getSimpleName(),ioe.toString());
            }
        }
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
            Toast.makeText(this.activity, "Es konnte nicht auf die Kamera zugegriffen werden!", Toast.LENGTH_LONG);
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
