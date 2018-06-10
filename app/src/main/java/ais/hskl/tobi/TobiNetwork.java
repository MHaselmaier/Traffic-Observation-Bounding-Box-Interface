package ais.hskl.tobi;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TobiNetwork
{
    static
    {
        System.loadLibrary("tensorflow_inference");
    }

    private static final String MODEL_FILE = "file:///android_asset/tobi-mobile.pb";
    private static final String INPUT_NODE = "image_tensor";
    private static final String[] OUTPUT_NODES = {"num_detections", "detection_boxes", "detection_scores", "detection_classes"};

    private TensorFlowInferenceInterface inferenceInterface;
    private float minDetectionScore = 0.7f;
    private String[] detectedClassStrings;

    private BlockingQueue<PredictionInput> currentInput = new ArrayBlockingQueue<>(1);

    private Thread predictionThread = new Thread()
    {
        private Handler handler = new Handler();

        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    PredictionInput input = TobiNetwork.this.currentInput.take();

                    Bitmap bitmap = input.image;
                    int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                    bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                    byte[] image = new byte[1 * bitmap.getHeight() * bitmap.getWidth() * 3];
                    for (int i = 0; pixels.length > i; ++i)
                    {
                        image[i * 3 + 0] = (byte)((pixels[i] & 0xff0000) >> 16);
                        image[i * 3 + 1] = (byte)((pixels[i] & 0xff00) >> 8);
                        image[i * 3 + 2] = (byte)(pixels[i] & 0xff);
                    }

                    TobiNetwork.this.inferenceInterface.feed(INPUT_NODE, image, input.dimensions);
                    TobiNetwork.this.inferenceInterface.run(OUTPUT_NODES);

                    float[] num_detections = new float[1];
                    TobiNetwork.this.inferenceInterface.fetch(OUTPUT_NODES[0], num_detections);

                    if (0 < num_detections[0])
                    {
                        float[] detection_boxes = new float[400];
                        TobiNetwork.this.inferenceInterface.fetch(OUTPUT_NODES[1], detection_boxes);

                        float[] detection_scores = new float[100];
                        TobiNetwork.this.inferenceInterface.fetch(OUTPUT_NODES[2], detection_scores);

                        float[] detection_classes = new float[100];
                        TobiNetwork.this.inferenceInterface.fetch(OUTPUT_NODES[3], detection_classes);

                        DetectedObject[] detectedObjects = generateDetectedObjects(detection_boxes, detection_scores, detection_classes);
                        //this.handler.post(() -> input.handler.handleDetectedObjects(detectedObjects));
                        input.handler.handleDetectedObjects(bitmap, detectedObjects);
                    }

                    //this.handler.post(() -> input.handler.handleDetectedObjects(new DetectedObject[0]));
                    input.handler.handleDetectedObjects(bitmap, new DetectedObject[0]);
                }
                catch (InterruptedException e) {}
            }
        }
    };

    public TobiNetwork(Context context)
    {
        this.inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
        this.detectedClassStrings = context.getResources().getStringArray(R.array.detection_classes);
        this.predictionThread.setPriority(Thread.MAX_PRIORITY);
        this.predictionThread.start();
    }

    public float getMinDetectionScore()
    {
        return this.minDetectionScore;
    }

    public void setMinDetectionScore(float minDetectionScore)
    {
        this.minDetectionScore = minDetectionScore;
    }

    public void predict(DetectedObjectHandler handler, Bitmap image, long... dimensions)
    {
        this.currentInput.offer(new PredictionInput(handler, image, dimensions));
    }

    private DetectedObject[] generateDetectedObjects(float[] detection_boxes, float[] detection_scores, float[] detection_classes)
    {
        List<DetectedObject> detectedObjects = new ArrayList<>();
        for (int i = 0; detection_scores.length > i; ++i)
        {
            if (this.minDetectionScore <= detection_scores[i])
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

    private static class PredictionInput
    {
        public final DetectedObjectHandler handler;
        public final Bitmap image;
        public final long[] dimensions;

        public PredictionInput(DetectedObjectHandler handler, Bitmap image, long... dimensions)
        {
            this.handler = handler;
            this.image = image;
            this.dimensions = dimensions;
        }
    }

    public interface DetectedObjectHandler
    {
        public void handleDetectedObjects(Bitmap bitmap, DetectedObject[] detectedObjects);
    }
}