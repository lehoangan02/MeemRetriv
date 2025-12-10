import os
import pickle
import torch
import numpy as np
from PIL import Image
from tqdm import tqdm
import warnings

# Suppress warnings
warnings.filterwarnings("ignore", category=FutureWarning)
warnings.filterwarnings("ignore", category=UserWarning)

try:
    import mobileclip
except ImportError:
    raise ImportError("Please install mobileclip: pip install git+https://github.com/apple/ml-mobileclip.git")

# --- CONFIGURATION ---
CELEB_DATASET_DIR = "./../DATA/celebrity_images_by_name" 
MEME_FACES_DIR = "./../DATA/extracted_faces"
CELEB_EMBEDDINGS_FILE = "celeb_embeddings_mobileclip.pkl" 
MEME_EMBEDDINGS_FILE = "meme_face_embeddings_mobileclip.pkl"

# MobileCLIP specific config
MODEL_NAME = "mobileclip_s0"
PRETRAINED_PATH = "./../backend/vectordb/models/mobileclip_s0.pt"

# --- DEVICE SETUP ---
def get_device():
    if torch.cuda.is_available():
        return "cuda"
    elif torch.backends.mps.is_available():
        return "mps"
    else:
        return "cpu"

DEVICE = get_device()

def load_mobileclip_model():
    print(f"Loading {MODEL_NAME} model onto {DEVICE}...")
    try:
        model, _, preprocess = mobileclip.create_model_and_transforms(MODEL_NAME, pretrained=PRETRAINED_PATH)
    except Exception as e:
        print(f"Loading generic model (auto-download)... Error: {e}")
        model, _, preprocess = mobileclip.create_model_and_transforms(MODEL_NAME)

    model = model.to(DEVICE)
    model.eval()
    return model, preprocess

# Helper to save data
def save_embeddings(data, filename):
    with open(filename, 'wb') as f:
        pickle.dump(data, f)
    print(f"Saved {len(data)} items to {filename}")

def get_image_paths(directory):
    image_paths = []
    valid_exts = {'.jpg', '.jpeg', '.png', '.bmp', '.webp'}
    for root, dirs, files in os.walk(directory):
        for file in files:
            if os.path.splitext(file)[1].lower() in valid_exts:
                image_paths.append(os.path.join(root, file))
    return image_paths

# ======================================================
#      UPDATED: RETURNS LIST OF DICTIONARIES
# ======================================================

def generate_mobileclip_embeddings(paths, description, model, preprocess):
    """
    Processes images and returns a LIST of dictionaries:
    [{'label': 'Name', 'path': '...', 'vector': array}, ...]
    """
    embeddings_list = []
    
    BATCH_SIZE = 32
    
    with tqdm(total=len(paths), desc=description, unit="img") as pbar:
        for i in range(0, len(paths), BATCH_SIZE):
            batch_paths = paths[i:i + BATCH_SIZE]
            
            batch_tensors = []
            valid_paths_in_batch = []

            # Preprocess images
            for p in batch_paths:
                try:
                    img = Image.open(p).convert("RGB")
                    tensor = preprocess(img)
                    batch_tensors.append(tensor)
                    valid_paths_in_batch.append(p)
                except Exception as e:
                    print(f"Skipping {p}: {e}")
                    continue
            
            if not batch_tensors:
                pbar.update(len(batch_paths))
                continue

            try:
                input_tensor = torch.stack(batch_tensors).to(DEVICE)
                
                with torch.no_grad():
                    image_features = model.encode_image(input_tensor)
                
                image_features /= image_features.norm(dim=-1, keepdim=True)
                numpy_features = image_features.cpu().numpy().astype(np.float32)
                
                for path, feature in zip(valid_paths_in_batch, numpy_features):
                    
                    label = os.path.basename(os.path.dirname(path))
                    
                    # --- CHANGE HERE: Adjust path level ---
                    # Converts "./../DATA/..." -> "./../../DATA/..."
                    saved_path = path.replace("./../DATA", "./../../DATA")
                    
                    entry = {
                        "label": label,
                        "path": saved_path, 
                        "vector": feature
                    }
                    embeddings_list.append(entry)

            except Exception as e:
                print(f"Batch processing error: {e}")
            
            pbar.update(len(batch_paths))

    return embeddings_list


# ======================================================
#                PROCESS FUNCTIONS
# ======================================================
def process_celebs_mobile(model, preprocess):
    print(f"\n--- Encoding Celebrity Images ---")
    paths = get_image_paths(CELEB_DATASET_DIR)
    celeb_data = generate_mobileclip_embeddings(paths, "Encoding Celebs", model, preprocess)
    save_embeddings(celeb_data, CELEB_EMBEDDINGS_FILE)

def process_meme_faces_mobile(model, preprocess):
    print(f"\n--- Encoding Meme Faces ---")
    paths = get_image_paths(MEME_FACES_DIR)
    meme_data = generate_mobileclip_embeddings(paths, "Encoding Memes", model, preprocess)
    save_embeddings(meme_data, MEME_EMBEDDINGS_FILE)

# ======================================================
#                   MAIN
# ======================================================
if __name__ == "__main__":
    if not os.path.exists(CELEB_DATASET_DIR) or not os.path.exists(MEME_FACES_DIR):
        print("❌ Please check input directories.")
    else:
        model, preprocess = load_mobileclip_model()
        process_celebs_mobile(model, preprocess)
        process_meme_faces_mobile(model, preprocess)