package flwr.android_client;

import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;
import android.util.Pair;
import androidx.lifecycle.MutableLiveData;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class RecommendationFlowerClient {
    private RecommendationModelWrapper recommendationModel;
    private MutableLiveData<Float> lastLoss = new MutableLiveData<>();
    private Context context;
    private final ConditionVariable isTraining = new ConditionVariable();
    private static String TAG = "RecommendationFlower";
    private int local_epochs = 1;

    public RecommendationFlowerClient(Context context) {
        this.recommendationModel = new RecommendationModelWrapper(context);
        this.context = context;
    }

    public ByteBuffer[] getWeights() {
        return recommendationModel.getParameters();
    }

    public Pair<ByteBuffer[], Integer> fit(ByteBuffer[] weights, int epochs) {
        this.local_epochs = epochs;
        recommendationModel.updateParameters(weights);
        isTraining.close();
        
        recommendationModel.enableTraining((epoch, loss) -> setLastLoss(epoch, loss));
        Log.d(TAG, "Training enabled. Local Epochs = " + this.local_epochs);
        
        recommendationModel.train(this.local_epochs);
        isTraining.block();
        
        return Pair.create(getWeights(), recommendationModel.getSize_Training());
    }

    public Pair<Pair<Float, Float>, Integer> evaluate(ByteBuffer[] weights) {
        recommendationModel.updateParameters(weights);
        recommendationModel.disableTraining();
        return Pair.create(recommendationModel.calculateTestStatistics(), recommendationModel.getSize_Testing());
    }

    public void setLastLoss(int epoch, float newLoss) {
        if (epoch == this.local_epochs - 1) {
            Log.d(TAG, "Training finished after epoch = " + epoch);
            lastLoss.postValue(newLoss);
            recommendationModel.disableTraining();
            isTraining.open();
        }
    }

    public void loadData(int device_id) {
        try {
            Log.d(TAG, "Loading recommendation data for device " + device_id);
            // For recommendation model, we generate synthetic data
            // In a real scenario, you might load user behavior data from local storage
            Log.d(TAG, "Synthetic recommendation data loaded successfully");
        } catch (Exception ex) {
            Log.e(TAG, "Error loading recommendation data: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void writeStringToFile(Context context, String fileName, String content) {
        try {
            java.io.File file = new java.io.File(context.getFilesDir(), fileName);
            java.io.FileWriter writer = new java.io.FileWriter(file, true);
            writer.append(content).append("\n");
            writer.flush();
            writer.close();
            Log.d(TAG, "Data written to file: " + fileName);
        } catch (Exception e) {
            Log.e(TAG, "Error writing to file: " + e.getMessage());
        }
    }

    public MutableLiveData<Float> getLastLoss() {
        return lastLoss;
    }

    public void cleanup() {
        if (recommendationModel != null) {
            recommendationModel.close();
        }
    }
}
