from fastapi import FastAPI, UploadFile, File
import tensorflow as tf
import numpy as np
from PIL import Image
import json
import io

app = FastAPI()

model = tf.keras.models.load_model("resnet50_model_file.keras", compile=False)

with open("label_mapping.json", "r") as f:
    label_mapping = json.load(f)

@app.get("/health")
def health():
    return {"status": "ok", "model": "ResNet50", "accuracy": "98%"}

@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    image_bytes = await file.read()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    image = image.resize((224, 224))

    img_array = np.array(image, dtype=np.float32)
    img_array = tf.keras.applications.resnet50.preprocess_input(img_array)
    img_array = np.expand_dims(img_array, axis=0)

    predictions = model.predict(img_array)
    predicted_class = int(np.argmax(predictions[0]))
    confidence = float(np.max(predictions[0]))
    label = label_mapping[str(predicted_class)]

    all_predictions = {
        label_mapping[str(i)]: round(float(p) * 100, 2)
        for i, p in enumerate(predictions[0])
    }

    return {
        "label": label,
        "confidence": round(confidence * 100, 2),
        "predictions": all_predictions
    }