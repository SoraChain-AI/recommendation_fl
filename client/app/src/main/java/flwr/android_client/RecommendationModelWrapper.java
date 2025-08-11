package flwr.android_client;

import android.content.Context;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RecommendationModelWrapper {
    private static final String TAG = "RecommendationModel";
    private static final String MODEL_PATH = "recommendation_model/recommendation.tflite";
    
    private Interpreter tflite;
    private Context context;
    private boolean isTrainingEnabled = false;
    private int trainingSize = 0;
    private int testingSize = 0;
    
    // Training data storage
    private float[][] trainingFeatures;
    private float[] trainingLabels;
    private float[][] testingFeatures;
    private float[] testingLabels;
    
    // Model parameters (weights)
    private ByteBuffer[] modelParameters;
    
    public RecommendationModelWrapper(Context context) {
        this.context = context;
        loadModel();
        generateSyntheticData();
    }
    
    private void loadModel() {
        try {
            File modelFile = new File(context.getFilesDir(), MODEL_PATH);
            if (!modelFile.exists()) {
                // Copy from assets if not exists
                FileUtil.copyAsset(context, MODEL_PATH, modelFile);
            }
            
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tflite = new Interpreter(modelFile, options);
            
            Log.d(TAG, "Model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading model: " + e.getMessage());
        }
    }
    
    private void generateSyntheticData() {
        // Generate synthetic recommendation training data
        Random random = new Random(42); // Fixed seed for reproducibility
        
        int numSamples = 100;
        int numFeatures = 10;
        
        trainingFeatures = new float[numSamples][numFeatures];
        trainingLabels = new float[numSamples];
        testingFeatures = new float[numSamples/4][numFeatures];
        testingLabels = new float[numSamples/4];
        
        // Generate synthetic user behavior data
        for (int i = 0; i < numSamples; i++) {
            // Device features (categorical encoded)
            trainingFeatures[i][0] = random.nextInt(5); // device_id
            trainingFeatures[i][1] = random.nextInt(2); // os_id
            trainingFeatures[i][2] = random.nextInt(2); // gender_id
            
            // Numerical features (normalized)
            trainingFeatures[i][3] = random.nextFloat(); // age (normalized)
            trainingFeatures[i][4] = random.nextFloat(); // app_usage_time (normalized)
            trainingFeatures[i][5] = random.nextFloat(); // screen_time (normalized)
            trainingFeatures[i][6] = random.nextFloat(); // battery_drain (normalized)
            trainingFeatures[i][7] = random.nextFloat(); // apps_installed (normalized)
            trainingFeatures[i][8] = random.nextFloat(); // data_usage (normalized)
            trainingFeatures[i][9] = random.nextInt(5); // behavior_class
            
            // Generate synthetic rating (0-5 scale)
            float rating = 0.0f;
            rating += trainingFeatures[i][4] * 0.25f; // app usage weight
            rating += trainingFeatures[i][5] * 0.20f; // screen time weight
            rating += trainingFeatures[i][6] * 0.15f; // battery drain weight
            rating += trainingFeatures[i][7] * 0.15f; // apps installed weight
            rating += trainingFeatures[i][8] * 0.25f; // data usage weight
            rating *= 5.0f; // Scale to 0-5
            
            trainingLabels[i] = rating;
            
            // Add some noise for realistic training
            trainingLabels[i] += (random.nextFloat() - 0.5f) * 0.5f;
            trainingLabels[i] = Math.max(0.0f, Math.min(5.0f, trainingLabels[i]));
        }
        
        // Split into training and testing
        for (int i = 0; i < numSamples/4; i++) {
            testingFeatures[i] = trainingFeatures[i];
            testingLabels[i] = trainingLabels[i];
        }
        
        trainingSize = numSamples;
        testingSize = numSamples/4;
        
        Log.d(TAG, "Generated " + trainingSize + " training samples and " + testingSize + " testing samples");
    }
    
    public ByteBuffer[] getParameters() {
        // For federated learning, we need to extract weights from the model
        // This is a simplified version - in practice you'd need to extract actual weights
        if (modelParameters == null) {
            modelParameters = new ByteBuffer[1];
            // Create a dummy parameter buffer for demonstration
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            buffer.order(ByteOrder.nativeOrder());
            modelParameters[0] = buffer;
        }
        return modelParameters;
    }
    
    public void updateParameters(ByteBuffer[] weights) {
        // Update model parameters (weights) from federated learning
        this.modelParameters = weights;
        Log.d(TAG, "Model parameters updated from federated learning");
    }
    
    public void train(int epochs) {
        if (!isTrainingEnabled) {
            Log.w(TAG, "Training not enabled");
            return;
        }
        
        Log.d(TAG, "Starting training for " + epochs + " epochs");
        
        // Simple training loop for demonstration
        // In practice, you'd implement proper gradient descent
        for (int epoch = 0; epoch < epochs; epoch++) {
            float totalLoss = 0.0f;
            
            for (int i = 0; i < trainingSize; i++) {
                // Forward pass
                float prediction = predict(trainingFeatures[i]);
                float loss = (prediction - trainingLabels[i]) * (prediction - trainingLabels[i]);
                totalLoss += loss;
            }
            
            float avgLoss = totalLoss / trainingSize;
            Log.d(TAG, "Epoch " + (epoch + 1) + "/" + epochs + ", Loss: " + avgLoss);
        }
        
        Log.d(TAG, "Training completed");
    }
    
    public void enableTraining(TrainingCallback callback) {
        this.isTrainingEnabled = true;
        Log.d(TAG, "Training enabled");
    }
    
    public void disableTraining() {
        this.isTrainingEnabled = false;
        Log.d(TAG, "Training disabled");
    }
    
    public int getSize_Training() {
        return trainingSize;
    }
    
    public int getSize_Testing() {
        return testingSize;
    }
    
    public Pair<Float, Float> calculateTestStatistics() {
        float totalLoss = 0.0f;
        float totalMae = 0.0f;
        
        for (int i = 0; i < testingSize; i++) {
            float prediction = predict(testingFeatures[i]);
            float loss = (prediction - testingLabels[i]) * (prediction - testingLabels[i]);
            float mae = Math.abs(prediction - testingLabels[i]);
            
            totalLoss += loss;
            totalMae += mae;
        }
        
        float avgLoss = totalLoss / testingSize;
        float avgMae = totalMae / testingSize;
        
        return new Pair<>(avgLoss, avgMae);
    }
    
    private float predict(float[] features) {
        if (tflite == null) {
            Log.e(TAG, "Model not loaded");
            return 0.0f;
        }
        
        try {
            // Prepare input tensor
            TensorBuffer inputBuffer = TensorBuffer.createFixedSize(
                new int[]{1, features.length}, org.tensorflow.lite.DataType.FLOAT32
            );
            inputBuffer.loadArray(features);
            
            // Prepare output tensor
            TensorBuffer outputBuffer = TensorBuffer.createFixedSize(
                new int[]{1, 1}, org.tensorflow.lite.DataType.FLOAT32
            );
            
            // Run inference
            Map<Integer, Object> inputs = new HashMap<>();
            inputs.put(0, inputBuffer.getBuffer());
            
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, outputBuffer.getBuffer());
            
            tflite.runForMultipleInputsOutputs(inputs, outputs);
            
            // Get prediction
            float prediction = outputBuffer.getFloatArray()[0];
            return Math.max(0.0f, Math.min(5.0f, prediction)); // Clamp to 0-5 range
            
        } catch (Exception e) {
            Log.e(TAG, "Error during prediction: " + e.getMessage());
            return 0.0f;
        }
    }
    
    public interface TrainingCallback {
        void onEpochComplete(int epoch, float loss);
    }
    
    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
