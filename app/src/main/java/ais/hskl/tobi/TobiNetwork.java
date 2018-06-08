package ais.hskl.tobi;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TobiNetwork
{
    private static final String MODEL_FILE = "file:///android_asset/tobi-mobile.pb";
    private static final String INPUT_NODE = "image_tensor";
    private static final String[] OUTPUT_NODES = {"num_detections", "detection_boxes", "detection_scores", "detection_classes"};

    private static final float MIN_SCORE = 0.7f;

    private TensorFlowInferenceInterface inferenceInterface;
    private String[] detectedClassStrings;

    public TobiNetwork(Context context)
    {
        System.loadLibrary("tensorflow_inference");
        this.inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
        this.detectedClassStrings = context.getResources().getStringArray(R.array.detection_classes);
    }

    public DetectedObject[] predict(byte[] image, long... dimensions)
    {
        this.inferenceInterface.feed(INPUT_NODE, image, dimensions);
        this.inferenceInterface.run(OUTPUT_NODES);

        float[] num_detections = new float[1];
        this.inferenceInterface.fetch(OUTPUT_NODES[0], num_detections);

        if (0 < num_detections[0])
        {
            float[] detection_boxes = new float[400];
            this.inferenceInterface.fetch(OUTPUT_NODES[1], detection_boxes);

            float[] detection_scores = new float[100];
            this.inferenceInterface.fetch(OUTPUT_NODES[2], detection_scores);

            float[] detection_classes = new float[100];
            this.inferenceInterface.fetch(OUTPUT_NODES[3], detection_classes);

            return generateDetectedObjects(detection_boxes, detection_scores, detection_classes);
        }

        return new DetectedObject[0];
    }

    private DetectedObject[] generateDetectedObjects(float[] detection_boxes, float[] detection_scores, float[] detection_classes)
    {
        List<DetectedObject> detectedObjects = new ArrayList<>();
        for (int i = 0; detection_scores.length > i; ++i)
        {
            if (MIN_SCORE <= detection_scores[i])
            {
                float[] box = Arrays.copyOfRange(detection_boxes, i * 4, i * 4 + 4);
                detectedObjects.add(new DetectedObject(box, detection_scores[i], this.detectedClassStrings[(int)detection_classes[i]]));
            }
        }
        return detectedObjects.toArray(new DetectedObject[0]);
    }

    public static class DetectedObject
    {
        private float[] box;
        private float score;
        private String detectedClass;

        private DetectedObject(float[] box, float score, String detectedClass)
        {
            this.box = box;
            this.score = score;
            this.detectedClass = detectedClass;
        }

        public  float[] getBox()
        {
            return this.box;
        }

        public float getScore()
        {
            return this.score;
        }

        public String getDetectedClass()
        {
            return this.detectedClass;
        }
    }
}