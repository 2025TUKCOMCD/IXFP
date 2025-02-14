import os
import cv2
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Dense, Flatten, Dropout
from tensorflow.keras.applications import MobileNetV2
from sklearn.model_selection import train_test_split
from tensorflow.keras.utils import to_categorical

# 데이터 경로 설정
data_dir = "C:\Users\rladb\OneDrive\바탕 화면\코딩공부\git\JZP\AI\training\All-Age-Faces_Dataset"
image_size = (128, 128)  # 모델 입력 이미지 크기
categories = ['2-19', '20-60', '61+']  # 나이대 클래스

# 데이터 및 레이블 리스트 초기화
data = []
labels = []

# 파일 구조를 탐색하여 이미지 로드 및 전처리
for root, dirs, files in os.walk(data_dir):
    for file in files:
        if file.endswith(".jpg"):  # PNG 파일만 처리
            # 이미지 파일 경로
            file_path = os.path.join(root, file)
            
            # 파일 이름에서 나이대 정보 추출 (예: 17111302_S001_L01_E01_C1.png)
            # 예시에서 번호에 따라 나이대를 정한다면 이를 추출
            # 여기서는 S 번호 (사람 ID)로 가정
            try:
                person_id = int(file.split('_')[0])  # 파일 이름에서 ID 추출
                # 나이대 분류 (임의로 설정한 조건)
                if person_id < 20000000:  # 예: ID가 20000000보다 작으면 2-19
                    label = 0
                elif person_id < 40000000:  # 20000000 <= ID < 40000000이면 20-60
                    label = 1
                else:  # 그 외는 61+
                    label = 2
                
                # 이미지 로드 및 전처리
                img = cv2.imread(file_path)
                if img is not None:
                    img = cv2.resize(img, image_size)
                    data.append(img)
                    labels.append(label)
            except Exception as e:
                print(f"Error processing file {file_path}: {e}")

# NumPy 배열로 변환
data = np.array(data, dtype="float32") / 255.0  # 픽셀 값을 [0, 1] 범위로 정규화
labels = np.array(labels)

# 데이터를 훈련 및 검증 세트로 분할
train_X, val_X, train_y, val_y = train_test_split(data, labels, test_size=0.2, random_state=42)

# 레이블을 원-핫 인코딩
train_y = to_categorical(train_y, num_classes=len(categories))
val_y = to_categorical(val_y, num_classes=len(categories))

# MobileNetV2 백본 모델
base_model = MobileNetV2(input_shape=image_size + (3,), include_top=False, weights="imagenet")
base_model.trainable = False  # 사전 학습된 가중치를 고정

# 모델 구조 추가
x = Flatten()(base_model.output)
x = Dense(128, activation="relu")(x)
x = Dropout(0.5)(x)
output = Dense(len(categories), activation="softmax")(x)

model = Model(inputs=base_model.input, outputs=output)

# 모델 컴파일
model.compile(optimizer="adam", loss="categorical_crossentropy", metrics=["accuracy"])

# 모델 학습
history = model.fit(
    train_X, train_y,
    validation_data=(val_X, val_y),
    epochs=10,
    batch_size=32
)

# 모델 저장
model_save_path = "age_classification_model.h5"
model.save(model_save_path)
print(f"Model saved to {model_save_path}")