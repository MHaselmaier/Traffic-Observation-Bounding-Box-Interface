package ais.hskl.tobi

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast

import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class MainActivity : AppCompatActivity() {

    private var MODEL_FILE = "file:///android_asset/tobi.pb"
    private var INPUT_NODE = "image_tensor"
    private var OUTPUT_NODES = arrayOf("num_detections", "detection_boxes", "detection_scores", "detection_classes")

    private var camera: Camera? = null
    private var cameraId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)){

            Toast.makeText(this, "Could not find any camera device", Toast.LENGTH_LONG).show()

        }else{

            this.cameraId = findPrimaryCamera()

            if(this.cameraId < 0){

                Toast.makeText(this, "Could not find any primary camera", Toast.LENGTH_LONG).show()

            }
        }

        //System.loadLibrary("tensorflow_inference")

        //initializeTensorFlow()
    }

    var pictureCallback: Camera.PictureCallback = object: Camera.PictureCallback{
        override fun onPictureTaken(byteData: ByteArray?, cameraObject: Camera?) {

            findViewById<TextView>(R.id.testText).text = byteData?.size.toString()

            if(byteData != null){

                var bitmap = BitmapFactory.decodeByteArray(byteData, 0, byteData.size)

                    var data:IntArray = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(data,0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    Log.i("MainActivity", "Data: " + data[10])
            }
        }

    }

    override fun onResume() {
        super.onResume()

        if(safeCameraOpen(this.cameraId)) {

            //Initialize fake SurfaceView
            var surfaceView = SurfaceView(this)

            try {

                this.camera?.setPreviewDisplay(surfaceView.holder)

            } catch (e: Exception) {

                Log.e("MainActivity", "Exception while setting preview display surface")

            }

            this.camera?.startPreview()

            // camera.parameters

            this.camera?.takePicture(null, null, pictureCallback)
        }
    }

    public override fun onPause() {
        releaseCamera()
        super.onPause()
    }

    private fun safeCameraOpen(id: Int): Boolean{

        var isOpen: Boolean = false

        try{
            releaseCamera()
            this.camera = Camera.open(id)

            if(this.camera != null)
                isOpen = true
        }catch(e: Exception){

            Log.e("MainActivity", "Could not safely open camera")
            Log.e("MainActivity", e.message)

        }

        return isOpen
    }

    private fun releaseCamera(){

            this.camera?.stopPreview()
            this.camera?.release()

    }

    private fun findPrimaryCamera(): Int {
        var cameraId = -1


        for (index in 0..Camera.getNumberOfCameras()){
            var info = CameraInfo()
            Camera.getCameraInfo(index, info)

            if(info.facing == CameraInfo.CAMERA_FACING_BACK){
                cameraId = index
                break
            }
        }

        return cameraId
    }

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
}
