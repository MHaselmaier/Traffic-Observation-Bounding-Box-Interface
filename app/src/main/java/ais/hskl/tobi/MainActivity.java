package ais.hskl.tobi;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 42;

    private BoundingBoxView boundingBoxView;

    @Override
     protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        LocationListener listener = new LocationListener(){

            @Override
            public void onLocationChanged(Location location) {
                Toast.makeText(MainActivity.this, "Speed: " + location.getSpeed(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                0, listener);
    }

    @Override
    protected void onResume(){
        super.onResume();

         if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
              setupBoundingBoxView();
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
            TobiNetwork tobi = new TobiNetwork(this);
            this.boundingBoxView = new BoundingBoxView(this, (TextureView) findViewById(R.id.textureBackground), (TextureView) findViewById(R.id.textureForeground), tobi);
        }
    }
}
