#v3에 과적합방지 데이터증강 옵티마이저 학습률을 좀더 미세하게 조정에 BatchNormalization추가
import os
import cv2
import numpy as np
import tensorflow as tf
import keras.saving
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Dense, Flatten, Dropout, BatchNormalization
from tensorflow.keras.applications import MobileNetV2
from sklearn.model_selection import train_test_split
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.callbacks import EarlyStopping
from tensorflow.keras.optimizers import Adam

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
                img = cv2.imread(file_path, cv2.IMREAD_GRAYSCALE)  # 흑백 이미지로 로드
                if img is None:
                    print(f"Warning: Could not load image {file_path}")
                    continue

                img = cv2.resize(img, image_size)  # 크기 조정
                img = np.expand_dims(img, axis=-1)  # (128, 128) → (128, 128, 1)
                img = cv2.merge([img, img, img])  # (128, 128, 1) → (128, 128, 3) (MobileNetV2 입력 맞춤)
               
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

# 📌 데이터 증강 설정
datagen = ImageDataGenerator(
    rotation_range=15,  # 회전
    width_shift_range=0.1,  # 좌우 이동
    height_shift_range=0.1,  # 상하 이동
    shear_range=0.1,  # 기울이기 변형
    zoom_range=0.1,  # 확대/축소
    horizontal_flip=True,  # 좌우 반전
    fill_mode="nearest"
)

# 📌 MobileNetV2 백본 모델 (사전 학습된 모델 사용)
base_model = MobileNetV2(input_shape=image_size + (3,), include_top=False, weights="imagenet")

# MobileNetV2의 일부 레이어 동결 해제 (미세 조정)
for layer in base_model.layers[:-40]:  # 앞부분 40개 레이어 동결
    layer.trainable = False
for layer in base_model.layers[-40:]:  # 뒷부분 40개 레이어만 학습 가능
    layer.trainable = True

# 모델 구조 추가
x = Flatten()(base_model.output)
x = Dense(128, activation="relu")(x)
x = BatchNormalization()(x)  # 추가
x = Dropout(0.5)(x)
output = Dense(len(categories), activation="softmax")(x)

model = Model(inputs=base_model.input, outputs=output)

# 📌 최적화된 Adam 옵티마이저 설정 (학습률 0.0001)
model.compile(optimizer=Adam(learning_rate=0.0001), loss="categorical_crossentropy", metrics=["accuracy"])

# 📌 Early Stopping 설정 (검증 데이터 성능 개선이 없으면 학습 중단)
early_stopping = EarlyStopping(monitor="val_loss", patience=5, restore_best_weights=True)

# 📌 모델 학습 (데이터 증강 적용)
history = model.fit(
    datagen.flow(train_X, train_y, batch_size=32),
    validation_data=(val_X, val_y),
    epochs=40,
    callbacks=[early_stopping]  # Early Stopping 추가
)

# 모델 저장
keras.saving.save_model(model, 'age_classification_model_40_v4.keras')

print("Model saved to age_classification_model_40_v4.keras")
