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
data_dir = "C:\\Users\\rladb\\git\\aglined_faces"
image_size = (128, 128)  # 모델 입력 이미지 크기
categories = ['2-19', '20-60', '61+']  # 나이대 클래스

# 데이터 및 레이블 리스트 초기화
data = []
labels = []

# 파일 탐색 및 이미지 로드
for file in os.listdir(data_dir):
    if file.endswith(".jpg"):
        file_path = os.path.join(data_dir, file)

        # 파일 이름에서 나이 정보 추출
        parts = file.split('A')
        if len(parts) == 2:
            age_part = parts[1].split('.')[0]  # .jpg 제거
            if age_part.isdigit():
                age = int(age_part)

                # 나이대 분류
                if age <= 19:
                    label = 0  # 2-19
                elif age <= 60:
                    label = 1  # 20-60
                else:
                    label = 2  # 61+

                # 이미지 로드 및 전처리
                img = cv2.imread(file_path)
                if img is None:
                    print(f"Warning: Could not load image {file_path}")
                    continue
                img = cv2.resize(img, image_size)
                data.append(img)
                labels.append(label)

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
for layer in base_model.layers[-20:]:  # 마지막 20개 레이어를 학습 가능하게 변경
    layer.trainable = True

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
