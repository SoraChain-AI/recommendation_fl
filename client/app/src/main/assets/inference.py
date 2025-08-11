#!/usr/bin/env python3
"""
Simple inference script for the trained recommendation model.
"""

import pickle
import json
import numpy as np
from sklearn.preprocessing import MinMaxScaler

def load_model_and_mappings():
    """Load the trained model and feature mappings."""
    with open('recommendation_model/recommendation_model.pkl', 'rb') as f:
        model = pickle.load(f)
    
    with open('feature_mappings.json', 'r') as f:
        mappings = json.load(f)
    
    return model, mappings

def predict_rating(user_features, model, mappings):
    """Predict rating for a user."""
    # Process user features
    device_id = mappings['device_mapping'].get(user_features['device_model'], 0)
    os_id = mappings['os_mapping'].get(user_features['os'], 0)
    gender_id = mappings['gender_mapping'].get(user_features['gender'], 0)
    
    # Scale numerical features
    scaler = MinMaxScaler()
    scaler.scale_ = np.array(mappings['scaler_params']['scale_'])
    scaler.min_ = np.array(mappings['scaler_params']['min_'])
    
    numerical_values = [
        user_features['app_usage_time'],
        user_features['screen_time'],
        user_features['battery_drain'],
        user_features['apps_installed'],
        user_features['data_usage'],
        user_features['age']
    ]
    
    scaled_numerical = scaler.transform([numerical_values])[0]
    
    # Create feature vector (must match training feature order exactly)
    features = [
        device_id, os_id, gender_id, user_features['age'],
        *scaled_numerical, user_features['behavior_class']
    ]
    
    # Make prediction
    prediction = model.predict([features])[0]
    return prediction

# Example usage
if __name__ == "__main__":
    model, mappings = load_model_and_mappings()
    
    # Example user features
    user_features = {
        'device_model': 'Google Pixel 5',
        'os': 'Android',
        'gender': 'Male',
        'age': 30,
        'app_usage_time': 300,  # minutes per day
        'screen_time': 6.0,      # hours per day
        'battery_drain': 1500,   # mAh per day
        'apps_installed': 50,
        'data_usage': 1000,      # MB per day
        'behavior_class': 3
    }
    
    rating = predict_rating(user_features, model, mappings)
    print(f"Predicted rating: {rating:.2f}")
    print(f"Features used: {len(features)} features")
    print(f"Feature vector: {features}")
