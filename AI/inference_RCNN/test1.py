import keras.saving
import cv2
import numpy as np

# ✅ 저장된 모델 로드
model_path = "C:\\Users\\rladb\\OneDrive\\바탕 화면\\코딩공부\\git\JZP\\age_classification_model_40_v4.keras"  # 🔹 경로 확인 필수!
model = keras.saving.load_model(model_path)
print("✅ Model loaded successfully!")

# ✅ 이미지 전처리 함수
def preprocess_image(image_path):
    image_size = (128, 128)  # 모델의 입력 크기
    img = cv2.imread(image_path)

    if img is None:
        print(f"❌ Error: Cannot load image at {image_path}")
        return None

    img = cv2.resize(img, image_size)  # 크기 조정
    img = img.astype("float32") / 255.0  # 정규화
    img = np.expand_dims(img, axis=0)  # 모델 입력 차원 맞추기 (batch 차원 추가)

    return img

# ✅ 예측할 이미지 경로
image_path = "C:\\Users\\rladb\\test_image\\20.jpg"  # 🔹 실제 경로 확인 필수!

# ✅ 이미지 전처리 및 예측
processed_img = preprocess_image(image_path)

if processed_img is not None:
    predictions = model.predict(processed_img)  # 모델 예측 실행
    predicted_class = np.argmax(predictions, axis=1)[0]  # 가장 높은 확률의 클래스 찾기

    # ✅ 연령대 카테고리 출력
    categories = ['2-19', '20-60', '61+']
    print(f"🎯 Predicted Age Group: {categories[predicted_class]}")
