package ais.hskl.tobi;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    private static final int CAMERA_PERMISSION_CODE = 42;
    private BoundingBoxView boundingBoxView;
    private Switch showDebugInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        TobiNetwork tobi = new TobiNetwork(this);

        this.boundingBoxView = findViewById(R.id.boundingBoxView);
        this.boundingBoxView.setTobiNetwork(tobi);

        this.showDebugInfo = findViewById(R.id.debug);
        this.showDebugInfo.setOnClickListener((v) ->
                MainActivity.this.boundingBoxView.showDebugInfo(MainActivity.this.showDebugInfo.isChecked()));

        Button minDetectionScore = findViewById(R.id.min_detection_score);
        minDetectionScore.setText(getResources().getString(R.string.min_detection_score, (int)(tobi.getMinDetectionScore() * 100)));
        MinDetectionScoreDialog minDetectionScoreDialog = new MinDetectionScoreDialog(this, minDetectionScore, tobi);
        minDetectionScore.setOnClickListener((v) -> minDetectionScoreDialog.show());
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        this.boundingBoxView.setupPreview();
    }

    @Override
    protected void onPause(){
        //Doing stuff when pausing the application before actually calling the super method... otherwise would be stupid
        this.boundingBoxView.releaseCamera();

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        switch(requestCode){
            case CAMERA_PERMISSION_CODE:
                if(grantResults != null || grantResults.length > 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.boundingBoxView.setupPreview();
                }
                else {
                    Toast.makeText(this, "Die App benötigt die Berechtigung auf Ihre Kamera zuzugreifen!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            default:
                //Another one bites the dust
                break;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }
}
