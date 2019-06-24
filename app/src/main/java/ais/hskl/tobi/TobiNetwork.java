package ais.hskl.tobi;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TobiNetwork implements Serializable
{
    static
    {
        System.loadLibrary("tensorflow_inference");
    }

    private static final String MODEL_FILE = "file:///android_asset/tobi-mobile.pb";
    private static final String INPUT_NODE = "image_tensor";
    private static final String[] OUTPUT_NODES = {"num_detections", "detection_boxes", "detection_scores", "detection_classes"};

    private static final long INPUT_SHAPE_COLOR_CHANNELS = 3;
    private static final long INPUT_SHAPE_NUMBER_OF_IMAGES = 1;

    private TensorFlowInferenceInterface inferenceInterface;
    private float minDetectionScore = 0.0f;

    public TobiNetwork(Context context)
    {
        this.inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    public float getMinDetectionScore()
    {
        return this.minDetectionScore;
    }

    public void setMinDetectionScore(float minDetectionScore)
    {
        this.minDetectionScore = minDetectionScore;
    }

    public DetectedObject[] predict(byte[] image, long bitmapWidth, long bitmapHeight)
    {
        this.inferenceInterface.feed(INPUT_NODE, image, INPUT_SHAPE_NUMBER_OF_IMAGES, bitmapHeight, bitmapWidth, INPUT_SHAPE_COLOR_CHANNELS);
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
            if (this.minDetectionScore <= detection_scores[i])
            {
                float[] box = Arrays.copyOfRange(detection_boxes, i * 4, i * 4 + 4);
                if (1e-5 > Math.abs(box[0] - box[2]) || 1e-5 > Math.abs(box[1] - box[3]))
                {
                    // Ignoring boxes that are too small -> some kind of error
                    continue;
                }

                detectedObjects.add(new DetectedObject(box, detection_scores[i], (int)detection_classes[i]-1));
            }
        }
        return detectedObjects.toArray(new DetectedObject[0]);
    }

    public static class DetectedObject
    {
        private float[] box;
        private float score;
        private Constants.SIGNS detectedClass;

        private DetectedObject(float[] box, float score, int detectedClass)
        {
            this.box = box;
            this.score = score;
            this.detectedClass = Constants.SIGNS.values()[detectedClass];
        }

        public  float[] getBox()
        {
            return this.box;
        }

        public float getScore()
        {
            return this.score;
        }

        public Constants.SIGNS getDetectedClass()
        {
            return this.detectedClass;
        }
    }
}