package ais.hskl.tobi

import android.content.Context
import android.hardware.camera2.CameraManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var cameraManager = getSystemService(Context.CAMERA_SERVICE)
        if(cameraManager is CameraManager){
            var cameraIds = cameraManager.cameraIdList
            var characteristics = cameraManager.getCameraCharacteristics(cameraIds[0])
        }
    }
}
