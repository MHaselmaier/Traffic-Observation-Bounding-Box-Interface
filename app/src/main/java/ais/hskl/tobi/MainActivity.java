package ais.hskl.tobi;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
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

public class MainActivity extends AppCompatActivity implements GpsHandler.SpeedChangedListener{
    private static final String TOBI_NETWORK_KEY = "tobi";
    private static final int CAMERA_PERMISSION_CODE = 42;
    private static final int GPS_PERMISSION_CODE = 43;
    private BoundingBoxView boundingBoxView;
    private Switch showDebugInfo;
    private Switch enableGps;
    private GpsHandler gpsHandler;
    private TobiNetwork tobi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        this.gpsHandler = new GpsHandler(this, this);
        this.tobi = new TobiNetwork(this);

        this.boundingBoxView = findViewById(R.id.boundingBoxView);
        this.boundingBoxView.setTobiNetwork(tobi);

        this.showDebugInfo = findViewById(R.id.debug);
        this.showDebugInfo.setOnClickListener((v) ->
                MainActivity.this.boundingBoxView.showDebugInfo(MainActivity.this.showDebugInfo.isChecked()));

        this.enableGps = findViewById(R.id.enable_gps);
        this.enableGps.setOnClickListener((v) ->
        {
            if(this.enableGps.isChecked())
            {
                final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
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
        minDetectionScore.setText(getResources().getString(R.string.min_detection_score, (int)(this.tobi.getMinDetectionScore() * 100)));
        MinDetectionScoreDialog minDetectionScoreDialog = new MinDetectionScoreDialog(this, minDetectionScore, this.tobi);
        minDetectionScore.setOnClickListener((v) -> minDetectionScoreDialog.show());
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISSION_CODE);
        }else
        {
            if(this.enableGps.isChecked()) //enabled in settings
            {
                this.gpsHandler.start();
            }
        }
        this.boundingBoxView.setupPreview();
    }

    @Override
    protected void onPause(){
        //Doing stuff when pausing the application before actually calling the super method... otherwise would be stupid
        this.gpsHandler.stop();
        this.boundingBoxView.releaseCamera();

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        switch(requestCode){
            case CAMERA_PERMISSION_CODE:
                if(grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.boundingBoxView.setupPreview();
                }
                else {
                    Toast.makeText(this, "Die App benötigt die Berechtigung auf Ihre Kamera zuzugreifen!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case GPS_PERMISSION_CODE:
                if(grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    gpsHandler.start();
                }

            default:
                //Another one bites the dust
                break;
        }
    }

    @Override
    public void onSpeedChanged(float speed, double latitude, double longitude) {

    }

    public static class RequestGpsEnable extends AlertDialog.Builder
    {

        public RequestGpsEnable(@NonNull  Context context, GpsHandler gpsHandler, Switch enableGps) {
            super(context);

            super.setMessage(getContext().getResources().getString(R.string.enable_gps_dialog_message));
            super.setPositiveButton(getContext().getResources().getString(R.string.accept), (v,w) ->
            {
                Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getContext().startActivity(settings);
            });

            super.setNegativeButton(getContext().getResources().getString(R.string.cancel), (v,w) ->
            {
                gpsHandler.stop();
                enableGps.setChecked(false);
            });


        }


    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }
}
