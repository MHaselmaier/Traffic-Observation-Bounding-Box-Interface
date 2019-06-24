package ais.hskl.tobi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity implements GpsHandler.SpeedChangedListener
{
    private BoundingBoxView boundingBoxView;
    private Switch showDebugInfo;
    private Switch enableGps;
    private GpsHandler gpsHandler;
    private TobiNetwork tobi;
    private MediaPlayer speedLimitExceededSound;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.speedLimitExceededSound = MediaPlayer.create(this, R.raw.speed_limit_exceeded);
        this.speedLimitExceededSound.setAudioStreamType(AudioManager.STREAM_MUSIC);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        this.gpsHandler = new GpsHandler(this, this);
        this.tobi = new TobiNetwork(this);

        this.boundingBoxView = findViewById(R.id.boundingBoxView);
        this.boundingBoxView.setTobiNetwork(this.tobi);

        this.showDebugInfo = findViewById(R.id.debug);
        this.showDebugInfo.setOnClickListener((v) ->
                this.boundingBoxView.showDebugInfo(this.showDebugInfo.isChecked()));

        this.enableGps = findViewById(R.id.enable_gps);
        this.enableGps.setOnClickListener((v) ->
        {
            if (this.enableGps.isChecked())
            {
                LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

                if (null != manager && manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                {
                    this.gpsHandler.start();
                }
                else
                {
                    RequestGpsEnable gpsEnableDialog = new RequestGpsEnable(this, this.gpsHandler, this.enableGps);
                    gpsEnableDialog.show();
                }
            }
            else
            {
                this.gpsHandler.stop();
            }
        });

        Button minDetectionScore = findViewById(R.id.min_detection_score);
        minDetectionScore.setText(getString(R.string.min_detection_score, (int)(this.tobi.getMinDetectionScore() * 100)));
        MinDetectionScoreDialog minDetectionScoreDialog = new MinDetectionScoreDialog(this, minDetectionScore, this.tobi);
        minDetectionScore.setOnClickListener((v) -> minDetectionScoreDialog.show());
    }

    protected boolean isDebugModeEnabled()
    {
        return this.showDebugInfo.isChecked();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA))
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, Constants.CAMERA_PERMISSION_CODE);
        }

        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) ||
                PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION))
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, Constants.GPS_PERMISSION_CODE);
        }
        else if (this.enableGps.isChecked()) //enabled in settings
        {
            this.gpsHandler.start();
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.TOBI_SHARED_PREFERENCES, MODE_PRIVATE);
        this.showDebugInfo.setChecked(sharedPreferences.getBoolean(Constants.SHOW_DEBUG, false));
        this.boundingBoxView.showDebugInfo(showDebugInfo.isChecked());
        this.tobi.setMinDetectionScore(sharedPreferences.getFloat(Constants.DETECTION_SCORE, 0.7f));
        Button minDetectionScore = findViewById(R.id.min_detection_score);
        minDetectionScore.setText(getString(R.string.min_detection_score, (int)(this.tobi.getMinDetectionScore() * 100)));

        this.boundingBoxView.setupPreview();
    }

    @Override
    protected void onPause()
    {
        this.gpsHandler.stop();
        SharedPreferences sharedPrefs = getSharedPreferences(Constants.TOBI_SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(Constants.SHOW_DEBUG,showDebugInfo.isChecked());
        editor.putFloat(Constants.DETECTION_SCORE, this.tobi.getMinDetectionScore());
        editor.apply();

        this.boundingBoxView.releaseCamera();

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
        case Constants.CAMERA_PERMISSION_CODE:
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                this.boundingBoxView.setupPreview();
            }
            else
            {
                Toast.makeText(this, "Die App benötigt die Berechtigung auf Ihre Kamera zuzugreifen!", Toast.LENGTH_LONG).show();
                finish();
            }
            break;
        case Constants.GPS_PERMISSION_CODE:
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                gpsHandler.start();
            }
            break;
        default:
            //Another one bites the dust
            break;
        }
    }
    
    @Override
    public void onSpeedChanged(int speed, double latitude, double longitude)
    {
        Constants.SIGNS lastSign = this.boundingBoxView.getLastSpeedSign();
        if (null == lastSign) return;

        int maxSpeed;
        switch (lastSign)
        {
        case SPEED_LIMIT_30:
            maxSpeed = 30;
            break;
        case SPEED_LIMIT_50:
            maxSpeed = 50;
            break;
        case SPEED_LIMIT_60:
            maxSpeed = 60;
            break;
        case SPEED_LIMIT_70:
            maxSpeed = 70;
            break;
        case SPEED_LIMIT_80:
            maxSpeed = 80;
            break;
        case SPEED_LIMIT_100:
            maxSpeed = 100;
            break;
        case SPEED_LIMIT_120:
            maxSpeed = 120;
            break;
        default:
            maxSpeed = Integer.MAX_VALUE;
            break;
        }

        if (maxSpeed < speed)
            this.speedLimitExceededSound.start();
    }

    private static class RequestGpsEnable extends AlertDialog.Builder
    {
        private RequestGpsEnable(@NonNull  Context context, GpsHandler gpsHandler, Switch enableGps)
        {
            super(context);

            super.setMessage(context.getString(R.string.enable_gps_dialog_message));
            super.setPositiveButton(context.getString(R.string.accept), (v,w) ->
            {
                Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(settings);
            });
            super.setNegativeButton(context.getString(R.string.cancel), (v,w) ->
            {
                gpsHandler.stop();
                enableGps.setChecked(false);
            });
        }
    }
}
