package ais.hskl.tobi

import android.content.Context
import android.hardware.camera2.CameraManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class MainActivity : AppCompatActivity() {

    private var MODEL_FILE = "file:///android_asset/tobi.pb"
    private var INPUT_NODE = "image_tensor"
    private var OUTPUT_NODES = arrayOf("num_detections", "detection_boxes", "detection_scores", "detection_classes")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var cameraManager = getSystemService(Context.CAMERA_SERVICE)
        if(cameraManager is CameraManager){
            var cameraIds = cameraManager.cameraIdList
            var characteristics = cameraManager.getCameraCharacteristics(cameraIds[0])
        }

        System.loadLibrary("tensorflow_inference")

        initializeTensorFlow()
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
