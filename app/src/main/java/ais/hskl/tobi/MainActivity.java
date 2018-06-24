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
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity
{
    private static final String TOBI_NETWORK_KEY = "tobi";
    private static final int CAMERA_PERMISSION_CODE = 42;
    private BoundingBoxView boundingBoxView;
    private Switch showDebugInfo;
    private static final String FILENAME = "dataFile";
    private static final String VAL_KEY = "key_detectionScore";

    private TobiNetwork tobi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        this.tobi = new TobiNetwork(this);

        this.boundingBoxView = findViewById(R.id.boundingBoxView);
        this.boundingBoxView.setTobiNetwork(this.tobi);

        this.showDebugInfo = findViewById(R.id.debug);
        this.showDebugInfo.setOnClickListener((v) ->
                MainActivity.this.boundingBoxView.showDebugInfo(MainActivity.this.showDebugInfo.isChecked()));

        Button minDetectionScore = findViewById(R.id.min_detection_score);
        MinDetectionScoreDialog minDetectionScoreDialog = new MinDetectionScoreDialog(this, minDetectionScore, this.tobi);
        minDetectionScore.setOnClickListener((v) -> minDetectionScoreDialog.show());
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        this.tobi.setMinDetectionScore(getSharedPreferences(FILENAME, MODE_PRIVATE).getFloat(VAL_KEY, 0.7f));
        Button minDetectionScore = findViewById(R.id.min_detection_score);
        minDetectionScore.setText(getResources().getString(R.string.min_detection_score, (int)(this.tobi.getMinDetectionScore() * 100)));
    }

    @Override
    protected void onPause(){
        //Doing stuff when pausing the application before actually calling the super method... otherwise would be stupid
        SharedPreferences sharedPrefs = getSharedPreferences(FILENAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putFloat(VAL_KEY, this.tobi.getMinDetectionScore());
        editor.commit();

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
