import os
import cv2
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Dense, Flatten, Dropout
from tensorflow.keras.applications import MobileNetV2
from sklearn.model_selection import train_test_split
from tensorflow.keras.utils import to_categorical

data_dir = "C:\\Users\\rladb\\OneDrive\\바탕 화면\\코딩공부\\git\\JZP\\AI\\training\\All-Age-Faces_Dataset\\aglined_faces"

for file in os.listdir(data_dir):
    if file.endswith(".jpg"):
        file_path = os.path.join(data_dir, file)
        print(f"Processing file: {file_path}")
        img = cv2.imread(file_path)
        if img is None:
            print(f"Failed to load image: {file_path}")
        else:
            print(f"Loaded image successfully: {file_path}, Shape: {img.shape}")
