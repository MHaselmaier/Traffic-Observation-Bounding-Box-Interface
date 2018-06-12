package ais.hskl.tobi;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 42;

    private BoundingBoxView boundingBoxView;
    private GpsHandler gpsHandler;
    private TobiNetwork tobi;

    private Switch showDebugInfo;

    @Override
     protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // this.tobi = new TobiNetwork(this);
        this.gpsHandler = new GpsHandler(this, (speed, lng, lat) -> {
            //Log.i("GPS SPEED", "" + speed);
            Toast.makeText(this, String.format("Sp:%f,Lng:%f,Lat:%f", speed, lng, lat), Toast.LENGTH_LONG).show();


        });

        //this.showDebugInfo = findViewById(R.id.debug);
        /*this.showDebugInfo.setOnClickListener((v) -> {
            if (null != MainActivity.this.boundingBoxView)
            {
                MainActivity.this.boundingBoxView.showDebugInfo(MainActivity.this.showDebugInfo.isChecked());
            }
        });


        Button minDetectionScore = findViewById(R.id.min_detection_score);
        minDetectionScore.setText(getResources().getString(R.string.min_detection_score, (int)(this.tobi.getMinDetectionScore() * 100)));
        MinDetectionScoreDialog minDetectionScoreDialog = new MinDetectionScoreDialog(this, minDetectionScore, this.tobi);
        minDetectionScore.setOnClickListener((v) -> minDetectionScoreDialog.show());

        */
    }

    @Override
    protected void onResume(){
        super.onResume();

         if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
             // setupBoundingBoxView();
         }
         else {
             ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
         }
    }

    @Override
    protected void onPause(){
        //Doing stuff when pausing the application before actually calling the super method... otherwise would be stupid

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        switch(requestCode){
            case CAMERA_PERMISSION_CODE:
                if(grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //Start using the camera. All needed permissions are granted
                    setupBoundingBoxView();
                }else{
                    Toast.makeText(this, "Die App benötigt die Berechtigung auf Ihre Kamera zuzugreifen!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            default:
                //Another one bites the dust
                break;
        }

    }

    private void setupBoundingBoxView()
    {
        if (null == this.boundingBoxView)
        {
            this.boundingBoxView = new BoundingBoxView(this, findViewById(R.id.textureBackground), findViewById(R.id.textureForeground), this.tobi);
            this.boundingBoxView.showDebugInfo(this.showDebugInfo.isChecked());
        }
    }
}
