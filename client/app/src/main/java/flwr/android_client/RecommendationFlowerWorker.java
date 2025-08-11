package flwr.android_client;

import static android.content.Context.NOTIFICATION_SERVICE;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.Log;
import android.util.Pair;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import flwr.android_client.FlowerServiceGrpc.FlowerServiceStub;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class RecommendationFlowerWorker extends Worker {

    private ManagedChannel channel;
    public RecommendationFlowerClient rfc;
    private StreamObserver<ClientMessage> UniversalRequestObserver;
    private static final String TAG = "RecommendationFlower";
    String serverIp = "00:00:00";
    String serverPort = "0000";
    String dataslice = "1";
    public static String start_time;
    public static String end_time;
    public static String workerStartTime = "";
    public static String workerEndTime = "";
    public static String workerEndReason = "worker ended";

    public String getTime() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedDateTime = sdf.format(new Date());
            return formattedDateTime;
        }
        return "";
    }
    
    private NotificationManager notificationManager;
    private static String PROGRESS = "PROGRESS";

    public RecommendationFlowerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        RecommendationFlowerWorker worker = this;
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        rfc = new RecommendationFlowerClient(context.getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        Data checkData = getInputData();
        serverIp = checkData.getString("server");
        serverPort = checkData.getString("port");
        dataslice = checkData.getString("dataslice");

        setForegroundAsync(createForegroundInfo("Recommendation FL Progress"));
        
        try {
            workerStartTime = getTime();
            boolean resultConnect = connect();
            if(resultConnect) {
                Log.d(TAG, "Connected to server successfully");
                loadData();
                CompletableFuture<Void> future = runGrpc();
                future.get();
                workerEndTime = getTime();
                workerEndReason = "worker completed successfully";
                return Result.success();
            } else {
                Log.e(TAG, "Failed to connect to server");
                workerEndTime = getTime();
                workerEndReason = "connection failed";
                return Result.failure();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in recommendation FL worker: " + e.getMessage());
            workerEndTime = getTime();
            workerEndReason = "error: " + e.getMessage();
            return Result.failure();
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (channel != null) {
            channel.shutdown();
        }
        if (rfc != null) {
            rfc.cleanup();
        }
    }

    public boolean connect() {
        try {
            channel = ManagedChannelBuilder.forAddress(serverIp, Integer.parseInt(serverPort))
                    .usePlaintext()
                    .build();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Connection error: " + e.getMessage());
            return false;
        }
    }

    public void loadData() {
        try {
            int deviceId = Integer.parseInt(dataslice);
            rfc.loadData(deviceId);
        } catch (Exception e) {
            Log.e(TAG, "Error loading data: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> runGrpc() {
        return CompletableFuture.runAsync(() -> {
            try {
                FlowerServiceStub asyncStub = FlowerServiceGrpc.newStub(channel);
                CountDownLatch latch = new CountDownLatch(1);
                ProgressUpdater progressUpdater = new ProgressUpdater();
                
                FlowerServiceRunnable runnable = new FlowerServiceRunnable();
                runnable.run(asyncStub, this, latch, progressUpdater, getApplicationContext());
                
                latch.await();
            } catch (Exception e) {
                Log.e(TAG, "gRPC error: " + e.getMessage());
            }
        });
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
        String id = "recommendation_fl_worker";
        String title = "Recommendation Federated Learning";
        String text = "Training recommendation model with FL";

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), id)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        return new ForegroundInfo(1, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        String channelId = "recommendation_fl_channel";
        String channelName = "Recommendation FL Channel";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

    public class ProgressUpdater {
        public void setProgress() {
            // Update progress for recommendation FL
        }
    }

    private static class FlowerServiceRunnable {
        protected Throwable failed;

        public void run(FlowerServiceStub asyncStub, RecommendationFlowerWorker worker, CountDownLatch latch, ProgressUpdater progressUpdater, Context context) {
            try {
                join(asyncStub, worker, latch, progressUpdater, context);
            } catch (Exception e) {
                Log.e(TAG, "Error in FlowerServiceRunnable: " + e.getMessage());
                latch.countDown();
            }
        }

        public void writeStringToFile(Context context, String fileName, String content) {
            try {
                File file = new File(context.getFilesDir(), fileName);
                FileWriter writer = new FileWriter(file, true);
                writer.append(content).append("\n");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error writing to file: " + e.getMessage());
            }
        }

        private void join(FlowerServiceStub asyncStub, RecommendationFlowerWorker worker, CountDownLatch latch, ProgressUpdater progressUpdater, Context context) {
            asyncStub.join(ClientMessage.newBuilder().build(), new StreamObserver<ServerMessage>() {
                @Override
                public void onNext(ServerMessage msg) {
                    try {
                        handleMessage(msg, worker, progressUpdater, context);
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling message: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "gRPC error: " + t.getMessage());
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "gRPC stream completed");
                    latch.countDown();
                }
            });
        }

        private void handleMessage(ServerMessage message, RecommendationFlowerWorker worker, ProgressUpdater progressUpdater, Context context) {
            try {
                if (message.hasJoinIns()) {
                    Log.d(TAG, "Received join instruction");
                    // Handle join instruction
                } else if (message.hasFitIns()) {
                    Log.d(TAG, "Received fit instruction");
                    handleFitInstruction(message.getFitIns(), worker, context);
                } else if (message.hasEvaluateIns()) {
                    Log.d(TAG, "Received evaluate instruction");
                    handleEvaluateInstruction(message.getEvaluateIns(), worker, context);
                } else if (message.hasReconnectIns()) {
                    Log.d(TAG, "Received reconnect instruction");
                    // Handle reconnect
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling message: " + e.getMessage());
            }
        }

        private void handleFitInstruction(FitIns fitIns, RecommendationFlowerWorker worker, Context context) {
            try {
                // Extract weights and config from fitIns
                ByteBuffer[] weights = extractWeights(fitIns.getParameters());
                int epochs = extractEpochs(fitIns.getConfig());
                
                // Perform local training
                Pair<ByteBuffer[], Integer> result = worker.rfc.fit(weights, epochs);
                
                // Send fit result back
                ClientMessage fitRes = createFitResult(result.getFirst(), result.getSecond());
                // Send response back to server
                
            } catch (Exception e) {
                Log.e(TAG, "Error in fit instruction: " + e.getMessage());
            }
        }

        private void handleEvaluateInstruction(EvaluateIns evaluateIns, RecommendationFlowerWorker worker, Context context) {
            try {
                // Extract weights from evaluateIns
                ByteBuffer[] weights = extractWeights(evaluateIns.getParameters());
                
                // Perform local evaluation
                Pair<Pair<Float, Float>, Integer> result = worker.rfc.evaluate(weights);
                
                // Send evaluate result back
                ClientMessage evalRes = createEvaluateResult(result.getFirst().first, result.getFirst().second, result.getSecond());
                // Send response back to server
                
            } catch (Exception e) {
                Log.e(TAG, "Error in evaluate instruction: " + e.getMessage());
            }
        }

        private ByteBuffer[] extractWeights(Parameters parameters) {
            // Extract weights from protobuf parameters
            // This is a simplified version
            ByteBuffer[] weights = new ByteBuffer[1];
            weights[0] = ByteBuffer.allocateDirect(1024);
            return weights;
        }

        private int extractEpochs(Config config) {
            // Extract epochs from config
            return 5; // Default value
        }

        private ClientMessage createFitResult(ByteBuffer[] weights, int trainingSize) {
            // Create fit result message
            return ClientMessage.newBuilder().build();
        }

        private ClientMessage createEvaluateResult(float loss, float mae, int testingSize) {
            // Create evaluate result message
            return ClientMessage.newBuilder().build();
        }
    }
}
