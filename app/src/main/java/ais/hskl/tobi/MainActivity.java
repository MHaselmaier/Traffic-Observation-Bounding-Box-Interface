package ais.hskl.tobi;


import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class MainActivity extends AppCompatActivity {

    private String MODEL_FILE = "file:///android_asset/tobi.pb";
    private String INPUT_NODE = "image_tensor";
    private String[] OUTPUT_NODES = new String[]{"num_detections", "detection_boxes", "detection_scores", "detection_classes"};
    private static final int CAMERA_PERMISSION_CODE = 42;

    @Override
     protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new BoundingBoxView(this, (TextureView) findViewById(R.id.textureBackground), (TextureView) findViewById(R.id.textureForeground));

        //System.loadLibrary("tensorflow_inference");

        //initializeTensorFlow();
    }

    @Override
    protected void onResume(){
        super.onResume();

        /*
         if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
              //If permissions has been revoked -> Close application
         }
         */
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
                }else{
                    Toast.makeText(this, "Die App benötigt die Berechtigung auf Ihre Kamera zuzugreifen!", Toast.LENGTH_LONG).show();
                }
                break;

            default:
                //Another one bites the dust
                break;
        }

    }

    private void initializeTensorFlow(){
        TensorFlowInferenceInterface tensorface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
        int imageWidth = 1360;
        int imageHeight = 800;

        //Why would you multiply by 1?
        ByteBuffer imageData = ByteBuffer.allocate(imageWidth * imageHeight * 3);
        tensorface.feed(INPUT_NODE, imageData, 1, imageWidth, imageHeight, 3);

        FloatBuffer floatBuffer = FloatBuffer.allocate(100);
        tensorface.run(OUTPUT_NODES);
        tensorface.fetch(OUTPUT_NODES[0], floatBuffer);

        //One could use Lambdas as they are part of java 8. Guess what? Androids like 'Uh noo, the minimal version has to be SDK 24'. Well fuck you too.

        float[] data = floatBuffer.array();

        //See what I did there? ++i!!!! #Optimization #Matze #nohomo
        for(int i = 0; i < data.length; ++i){
            Log.d("MainActivity::TEST::", String.valueOf(data[i]));
        }

    }
/*

    private fun initializeTensorFlow() {
        var inferenceInterface = TensorFlowInferenceInterface(getAssets(), MODEL_FILE)

        var imageWidth = 1360
        var imageHeight = 800
        var imageData = ByteBuffer.allocate(1 * imageWidth * imageHeight * 3)
        inferenceInterface.feed(INPUT_NODE, imageData, 1, imageWidth.toLong(), imageHeight.toLong(), 3)

        var floatBuffer = FloatBuffer.allocate(100)
        //inferenceInterface.readNodeIntoFloatBuffer(OUTPUT_NODES[0], floatBuffer)
        inferenceInterface.run(OUTPUT_NODES)
        inferenceInterface.fetch(OUTPUT_NODES[0], floatBuffer)

        floatBuffer.array().forEach { Log.d("test", "$it") }
    }

    */
}
