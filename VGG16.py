import tensorflow as tf
import os
import random
import cv2
import numpy as np
from tqdm import tqdm
from tensorflow.keras.applications import VGG16
from tensorflow.keras.layers import Dense, Dropout, GlobalAveragePooling2D
from tensorflow.keras.models import Model
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau


def augment_by_cropping(input_root, output_root, crop_ratio=0.8):
    for brand in tqdm(os.listdir(input_root)):
        brand_path = os.path.join(input_root, brand)
        if not os.path.isdir(brand_path):
            continue

        output_dir = os.path.join(output_root, brand)
        os.makedirs(output_dir, exist_ok=True)

        for fname in os.listdir(brand_path):
            if not fname.lower().endswith((".jpg", ".jpeg", ".png")):
                continue
            img_path = os.path.join(brand_path, fname)
            img = cv2.imread(img_path)
            if img is None:
                continue
            h, w = img.shape[:2]
            ch, cw = int(h * crop_ratio), int(w * crop_ratio)
            variants = [
                (0, 0), (h - ch, 0), (0, w - cw), (h - ch, w - cw),
                ((h - ch)//2, (w - cw)//2)
            ]
            for i, (y, x) in enumerate(variants):
                crop = img[y:y+ch, x:x+cw]
                resized = cv2.resize(crop, (224, 224))
                cv2.imwrite(os.path.join(output_dir, f"{fname[:-4]}_crop{i}.jpg"), resized)

augment_by_cropping(
    input_root=r"E:\!_ModeleAutKopia\nowe_dane\train",
    output_root=r"E:\!_ModeleAutKopia\nowe_dane\train_aug"
)

# ==============================
# parametry
# ==============================
img_size = (224, 224)
batch_size = 8
epochs = 20
dataset_path = r"E:\!_ModeleAutKopia\nowe_dane"

# ==============================
# Warstwy do augmentacji
# ==============================
data_augmentation = tf.keras.Sequential([
    tf.keras.layers.RandomFlip("horizontal_and_vertical"),
    tf.keras.layers.RandomRotation(0.15),
    tf.keras.layers.RandomTranslation(0.1, 0.1),
    tf.keras.layers.RandomContrast(0.3),
])
# tf.keras.layers.RandomZoom(0.2),
# ==============================
# wczytanie etykiet
# ==============================
def get_filepaths_labels(root_path, brand2idx=None):
    filepaths, brand_indices = [], []

    brands = sorted([
        b for b in os.listdir(root_path)
        if os.path.isdir(os.path.join(root_path, b))
    ])

    if brand2idx is None:
        brand2idx = {b: i for i, b in enumerate(brands)}

    for b in brands:
        brand_path = os.path.join(root_path, b)
        for img_file in os.listdir(brand_path):
            if img_file.lower().endswith((".jpg", ".jpeg", ".png")):
                filepaths.append(os.path.join(brand_path, img_file))
                brand_indices.append(brand2idx[b])

    combined = list(zip(filepaths, brand_indices))
    random.seed(42)
    random.shuffle(combined)
    if combined:
        filepaths, brand_indices = map(list, zip(*combined))

    return filepaths, brand_indices, brand2idx

# ==============================
# polaczenie zwyklych zdjec i zdjec po agumetnacji
# ==============================
train_path = os.path.join(dataset_path, "train")
train_aug_path = os.path.join(dataset_path, "train_aug")

train_files, train_brand_idx, brand2idx = get_filepaths_labels(train_path)
aug_files, aug_brand_idx, _ = get_filepaths_labels(train_aug_path, brand2idx)

train_files += aug_files
train_brand_idx += aug_brand_idx

val_files, val_brand_idx, _ = get_filepaths_labels(os.path.join(dataset_path, "val"), brand2idx)

num_brands = len(brand2idx)
print(f"[INFO] Found {num_brands} brands")
print(f"[INFO] Train images: {len(train_files)}, Validation: {len(val_files)}")

# ==============================
# Przetwarzanie
# ==============================
def process_image(file_path, brand_idx, augment=False):
    image = tf.io.read_file(file_path)
    image = tf.image.decode_jpeg(image, channels=3)
    image = tf.image.resize(image, img_size)
    image = image / 255.0
    if augment: 
        image = data_augmentation(image)
    brand_label = tf.one_hot(brand_idx, num_brands)
    return image, brand_label

# ==============================
# Dataset 
# ==============================
train_dataset = tf.data.Dataset.from_tensor_slices((train_files, train_brand_idx))
train_dataset = train_dataset.map(lambda f, b: process_image(f, b, augment=True), num_parallel_calls=tf.data.AUTOTUNE)
train_dataset = train_dataset.shuffle(512).batch(batch_size).prefetch(tf.data.AUTOTUNE)

val_dataset = tf.data.Dataset.from_tensor_slices((val_files, val_brand_idx))
val_dataset = val_dataset.map(lambda f, b: process_image(f, b, augment=False), num_parallel_calls=tf.data.AUTOTUNE)
val_dataset = val_dataset.batch(batch_size).prefetch(tf.data.AUTOTUNE)

base_model = VGG16(weights="imagenet", include_top=False, input_shape=(224, 224, 3))
for layer in base_model.layers:
    layer.trainable = False

x = base_model.output
x = GlobalAveragePooling2D()(x)
x = Dense(1024, activation="relu")(x)
x = Dropout(0.5)(x)
x = Dense(512, activation="relu")(x)
x = Dropout(0.4)(x)
brand_output = Dense(num_brands, activation="softmax")(x)

model = Model(inputs=base_model.input, outputs=brand_output)

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-4),
    loss="categorical_crossentropy",
    metrics=["accuracy"]
)

print("\n[INFO] === Etap 1\n")
history1 = model.fit(train_dataset, validation_data=val_dataset, epochs=epochs)

# ==============================
# Fine-tuning
# ==============================
print("\n[INFO] === Etap 2\n")

fine_tune_at = 12  # ostatnie warstwy VGG16
for layer in base_model.layers[fine_tune_at:]:
    layer.trainable = True

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-3),
    loss="categorical_crossentropy",
    metrics=["accuracy"]
)

callbacks = [ 
    EarlyStopping(patience=6, restore_best_weights=True),
    ReduceLROnPlateau(factor=0.5, patience=3, min_lr=1e-7)
]

history2 = model.fit(
    train_dataset,
    validation_data=val_dataset,
    epochs=epochs,
    callbacks=callbacks
)

# ==============================
# Zapis modelu
# ==============================
save_path = r"E:\!_Model_aktualny\vgg16_finetuned.keras"
model.save(save_path)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

tflite_path = r"E:\!_Model_aktualny\vgg16.tflite"
with open(tflite_path, "wb") as f:
    f.write(tflite_model)

print(f"model zapisany w: {save_path}")
print(f"model tflite zapisany w: {tflite_path}")

print("\nGenrowanie raportu")

def get_history_metrics(h1, h2, key_names, default_lr_1=1e-3):
    combined_data = {}
    
    # Pobieranie metryk (accuracy, loss)
    for key in key_names:
        data1 = h1.history.get(key, [])
        data2 = h2.history.get(key, [])
        combined_data[key] = data1 + data2

    # Pobieranie Learning Rate
    lr_key_1 = 'learning_rate' if 'learning_rate' in h1.history else 'lr'
    if lr_key_1 in h1.history:
        lr1 = h1.history[lr_key_1]
    else:
        lr1 = [default_lr_1] * len(h1.history['loss'])
        
    lr_key_2 = 'learning_rate' if 'learning_rate' in h2.history else 'lr'
    lr2 = h2.history.get(lr_key_2, [])
    
    if not lr2:
        lr2 = [1e-5] * len(h2.history['loss'])

    combined_data['learning_rate'] = lr1 + lr2
    return combined_data

metrics_to_save = ['accuracy', 'loss', 'val_accuracy', 'val_loss']

try:
    # Pobieranie danych
    data = get_history_metrics(history1, history2, metrics_to_save)
    df = pd.DataFrame(data)

    df.insert(0, 'epoch', range(1, len(df) + 1))

    len_phase1 = len(history1.history['loss'])
    df.insert(1, 'phase', ['Base Training'] * len_phase1 + ['Fine-tuning'] * (len(df) - len_phase1))

    excel_path = os.path.join(save_dir, "training_summary.xlsx")
    df.to_excel(excel_path, index=False)

    print(f"zapisano w: {excel_path}")

except Exception as e:
    print(f"blad zapisu: {e}")
    