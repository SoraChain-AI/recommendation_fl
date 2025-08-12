# Recommendation Model Federated Learning for Android

This project implements federated learning for a recommendation model, specifically designed for Android devices. It allows multiple Android clients to collaboratively train a recommendation model without sharing raw user data.

## 🏗️ Project Structure

```
recommendation_fl/
├── client/                          # Android client application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/flwr/android_client/
│   │   │   │   ├── RecommendationMainActivity.java      # Main UI
│   │   │   │   ├── RecommendationFlowerClient.java      # FL client logic
│   │   │   │   ├── RecommendationFlowerWorker.java      # Background worker
│   │   │   │   └── RecommendationModelWrapper.java      # TensorFlow Lite wrapper
│   │   │   ├── assets/
│   │   │   │   └── recommendation_model/                 # TFLite model
│   │   │   └── res/                                      # UI resources
│   │   └── build.gradle                                  # Build configuration
├── server/                          # Python FL server
│   └── recommendation_server.py      # FL server implementation
├── requirements.txt                  # Python dependencies
└── README.md                        # This file
```

## 🚀 Features

- **Federated Learning**: Collaborative training across multiple Android devices
- **Recommendation Model**: TensorFlow Lite model for user behavior recommendations
- **Privacy-Preserving**: No raw user data leaves the device
- **Real-time Training**: Background training with WorkManager
- **Synthetic Data**: Generates realistic training data for demonstration

## 📱 Android Client Features

- **User Interface**: Simple interface to configure server connection
- **Background Processing**: Training runs in background using WorkManager
- **Model Management**: Loads and manages TensorFlow Lite recommendation model
- **Data Generation**: Creates synthetic user behavior data for training
- **Progress Monitoring**: Real-time status updates and logging

## 🖥️ Server Features

- **Adaptive Training**: Adjusts training parameters based on round number
- **Client Management**: Handles multiple Android clients
- **Federated Averaging**: Implements FedAvg strategy for model aggregation
- **Configurable Rounds**: Supports configurable number of training rounds

## 🛠️ Setup Instructions

### Prerequisites

1. **Android Studio** (latest version)
2. **Python 3.8+** with pip
3. **Android device or emulator** (API level 24+)

### 1. Install Python Dependencies

```bash
cd server
pip install -r ../requirements.txt
```

### 2. Build Android App

```bash
cd client
./gradlew assembleDebug
```

### 3. Install APK

```bash
# Install on connected device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Start FL Server

```bash
cd server
python recommendation_server.py
```

### 5. Configure and Start Client

1. Open the "Recommendation FL Client" app
2. Enter server IP (use `10.0.2.2` for Android emulator)
3. Enter server port (`8080`)
4. Enter data slice number (unique for each client)
5. Click "Start" to begin federated learning

## 🔧 Configuration

### Server Configuration

- **Port**: 8080 (configurable in `recommendation_server.py`)
- **Minimum Clients**: 2 (configurable)
- **Training Rounds**: 10 (configurable)
- **Strategy**: FedAvg with adaptive parameters

### Client Configuration

- **Model Path**: `assets/recommendation_model/recommendation.tflite`
- **Training Data**: 100 synthetic samples (configurable)
- **Features**: 10-dimensional user behavior vectors
- **Output**: Rating predictions (0-5 scale)

## 📊 Model Architecture

The recommendation model processes:
- **Device Features**: Device ID, OS, gender
- **Behavior Features**: App usage, screen time, battery drain
- **Usage Patterns**: Apps installed, data usage, age
- **Output**: Compatibility rating (0-5 scale)

## 🔄 Federated Learning Process

1. **Client Registration**: Android clients connect to FL server
2. **Model Distribution**: Server sends initial model weights
3. **Local Training**: Each client trains on local synthetic data
4. **Weight Aggregation**: Server aggregates model updates
5. **Model Update**: Server distributes improved model
6. **Evaluation**: Clients evaluate model performance
7. **Iteration**: Process repeats for specified rounds

## 🧪 Testing

### Single Client Test

1. Start server: `python recommendation_server.py`
2. Install and run Android app
3. Configure connection (IP: `10.0.2.2`, Port: `8080`)
4. Start federated learning
5. Monitor logs and progress

### Multi-Client Test

1. Start server
2. Install app on multiple devices/emulators
3. Configure each with unique data slice numbers
4. Start all clients simultaneously
5. Observe collaborative training

## 📝 Logging and Debugging

- **Client Logs**: Check Android logcat with tag "RecommendationFlower"
- **Server Logs**: Monitor Python console output
- **Training Progress**: View in-app status and log display
- **Model Performance**: Track loss and MAE metrics

## 🔒 Privacy and Security

- **Local Training**: All training happens on device
- **Weight Sharing**: Only model weights are shared (no raw data)
- **Secure Communication**: gRPC over HTTP/2
- **Data Isolation**: Each client maintains separate data

## 🚧 Limitations and Considerations

- **Synthetic Data**: Current implementation uses generated data
- **Model Size**: Limited by TensorFlow Lite constraints
- **Network Dependency**: Requires stable internet connection
- **Battery Usage**: Training can be resource-intensive

## 🔮 Future Enhancements

- **Real User Data**: Integrate with actual user behavior data
- **Advanced Models**: Support for larger, more complex models
- **Heterogeneous FL**: Handle different client capabilities
- **Privacy Techniques**: Implement differential privacy
- **Model Compression**: Optimize model size for mobile

## 📚 References

- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [Android WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Federated Learning](https://ai.googleblog.com/2017/04/federated-learning-collaborative.html)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

## 📄 License

This project follows the same license as the SoraChain Framework.
