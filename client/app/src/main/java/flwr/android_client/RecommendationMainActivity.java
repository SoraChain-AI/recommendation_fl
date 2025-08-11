package flwr.android_client;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.UUID;

public class RecommendationMainActivity extends AppCompatActivity {
    private static final String TAG = "RecommendationMain";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private EditText serverIpEditText;
    private EditText serverPortEditText;
    private EditText dataSliceEditText;
    private Button startButton;
    private Button stopButton;
    private TextView statusTextView;
    private TextView logTextView;
    
    private UUID currentWorkId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        checkPermissions();
    }

    private void initializeViews() {
        serverIpEditText = findViewById(R.id.server_ip);
        serverPortEditText = findViewById(R.id.server_port);
        dataSliceEditText = findViewById(R.id.data_slice);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        statusTextView = findViewById(R.id.status_text);
        logTextView = findViewById(R.id.log_text);
        
        // Set default values
        serverIpEditText.setText("10.0.2.2"); // Default for Android emulator
        serverPortEditText.setText("8080");
        dataSliceEditText.setText("1");
        
        startButton.setOnClickListener(v -> startFederatedLearning());
        stopButton.setOnClickListener(v -> stopFederatedLearning());
        
        updateStatus("Ready to start recommendation federated learning");
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void startFederatedLearning() {
        String serverIp = serverIpEditText.getText().toString().trim();
        String serverPort = serverPortEditText.getText().toString().trim();
        String dataSlice = dataSliceEditText.getText().toString().trim();
        
        if (serverIp.isEmpty() || serverPort.isEmpty() || dataSlice.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int port = Integer.parseInt(serverPort);
            int slice = Integer.parseInt(dataSlice);
            
            if (port <= 0 || port > 65535) {
                Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (slice <= 0) {
                Toast.makeText(this, "Invalid data slice number", Toast.LENGTH_SHORT).show();
                return;
            }
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create work data
        Data inputData = new Data.Builder()
                .putString("server", serverIp)
                .putString("port", serverPort)
                .putString("dataslice", dataSlice)
                .build();
        
        // Create work request
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RecommendationFlowerWorker.class)
                .setInputData(inputData)
                .build();
        
        // Start the work
        WorkManager.getInstance(this).enqueue(workRequest);
        currentWorkId = workRequest.getId();
        
        // Update UI
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        updateStatus("Recommendation FL started - connecting to " + serverIp + ":" + serverPort);
        addLog("Started recommendation federated learning");
        
        Log.d(TAG, "Started recommendation FL with server: " + serverIp + ":" + serverPort + ", slice: " + dataSlice);
    }

    private void stopFederatedLearning() {
        if (currentWorkId != null) {
            WorkManager.getInstance(this).cancelWorkById(currentWorkId);
            currentWorkId = null;
            
            // Update UI
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            updateStatus("Recommendation FL stopped");
            addLog("Stopped recommendation federated learning");
            
            Log.d(TAG, "Stopped recommendation FL");
        }
    }

    private void updateStatus(String status) {
        statusTextView.setText(status);
        Log.d(TAG, "Status: " + status);
    }

    private void addLog(String message) {
        String timestamp = java.time.LocalDateTime.now().toString();
        String logEntry = timestamp + ": " + message + "\n";
        logTextView.append(logEntry);
        
        // Keep only last 100 lines
        String[] lines = logTextView.getText().toString().split("\n");
        if (lines.length > 100) {
            StringBuilder sb = new StringBuilder();
            for (int i = lines.length - 100; i < lines.length; i++) {
                sb.append(lines[i]).append("\n");
            }
            logTextView.setText(sb.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentWorkId != null) {
            WorkManager.getInstance(this).cancelWorkById(currentWorkId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted");
            } else {
                Log.w(TAG, "Storage permission denied");
                Toast.makeText(this, "Storage permission required for logging", Toast.LENGTH_LONG).show();
            }
        }
    }
}
