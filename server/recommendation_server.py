#!/usr/bin/env python3
"""
Federated Learning Server for Recommendation Model
"""

from typing import Any, Callable, Dict, List, Optional, Tuple
import flwr as fl
import tensorflow as tf
import numpy as np
import json
import os

def main() -> None:
    print("="*60)
    print("RECOMMENDATION MODEL FEDERATED LEARNING SERVER")
    print("="*60)
    
    # Create strategy for recommendation model
    strategy = fl.server.strategy.FedAvgAndroid(
        fraction_fit=1.0,           # Use all available clients for training
        fraction_evaluate=1.0,      # Use all available clients for evaluation
        min_fit_clients=2,          # Minimum 2 clients required for training
        min_evaluate_clients=2,     # Minimum 2 clients required for evaluation
        min_available_clients=2,    # Minimum 2 clients must be available
        evaluate_fn=None,           # No server-side evaluation
        on_fit_config_fn=fit_config,
        on_evaluate_config_fn=evaluate_config,
        initial_parameters=None,    # Start with random weights
    )

    print("Starting Flower server for recommendation model federated learning...")
    print(f"Server will run on 0.0.0.0:8080")
    print(f"Minimum clients required: {strategy.min_available_clients}")
    print(f"Training rounds: 10")
    
    # Start Flower server
    fl.server.start_server(
        server_address="0.0.0.0:8080",
        config=fl.server.ServerConfig(num_rounds=10),
        strategy=strategy,
    )

def fit_config(server_round: int):
    """Return training configuration dict for each round.
    
    Adjusts training parameters based on the round number.
    """
    if server_round < 3:
        # Early rounds: gentle learning
        config = {
            "batch_size": 16,
            "local_epochs": 3,
            "learning_rate": 0.001,
        }
    elif server_round < 7:
        # Middle rounds: standard training
        config = {
            "batch_size": 16,
            "local_epochs": 5,
            "learning_rate": 0.0005,
        }
    else:
        # Final rounds: fine-tuning
        config = {
            "batch_size": 16,
            "local_epochs": 2,
            "learning_rate": 0.0001,
        }
    
    print(f"Round {server_round + 1}: batch_size={config['batch_size']}, "
          f"local_epochs={config['local_epochs']}, lr={config['learning_rate']}")
    
    return config

def evaluate_config(server_round: int):
    """Return evaluation configuration dict for each round."""
    config = {
        "batch_size": 16,
        "local_epochs": 1,  # No training during evaluation
    }
    return config

if __name__ == "__main__":
    main()
