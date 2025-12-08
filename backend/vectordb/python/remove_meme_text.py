import os
import cv2
import torch
import easyocr
import numpy as np
import requests
from tqdm import tqdm


class MemeTextCleaner:
    LAMA_MODEL_URL = "https://github.com/Sanster/models/releases/download/add_big_lama/big-lama.pt"
    MODELS_DIR = "models"
    MODEL_PATH = os.path.join(MODELS_DIR, "big-lama.pt")
    VALID_EXT = {".png", ".jpg", ".jpeg", ".webp"}

    def __init__(self, gpu=True):
        self.device = self._select_device(gpu)
        print(f"Using device: {self.device}")

        self._download_model()
        self.lama_model = self._load_lama_model()
        self.reader = easyocr.Reader(['en'], gpu=(self.device == "cuda"))

    def _select_device(self, gpu_enabled):
        if not gpu_enabled:
            return "cpu"
        if torch.cuda.is_available():
            return "cuda"
        if torch.backends.mps.is_available() and torch.backends.mps.is_built():
            return "mps"
        return "cpu"

    def _download_model(self):
        os.makedirs(self.MODELS_DIR, exist_ok=True)

        if os.path.exists(self.MODEL_PATH):
            return

        print(f"Downloading LaMa model to {self.MODEL_PATH}...")
        response = requests.get(self.LAMA_MODEL_URL, stream=True)

        with open(self.MODEL_PATH, "wb") as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)

        print("Download complete.")

    def _load_lama_model(self):
        model = torch.jit.load(self.MODEL_PATH, map_location=self.device)

        if self.device == "mps":
            model = model.to(torch.device("mps"))
        else:
            model = model.to(self.device)

        model.eval()
        return model

    def _preprocess(self, img_np, mask_np):
        img_t = torch.from_numpy(img_np).float().div(255.0)
        mask_t = torch.from_numpy(mask_np).float().div(255.0)
        img_t = img_t.permute(2, 0, 1).unsqueeze(0)
        mask_t = mask_t.unsqueeze(0).unsqueeze(0)

        _, _, h, w = img_t.shape
        pad_h = (8 - h % 8) % 8
        pad_w = (8 - w % 8) % 8

        if pad_h > 0 or pad_w > 0:
            img_t = torch.nn.functional.pad(img_t, (0, pad_w, 0, pad_h), mode="reflect")
            mask_t = torch.nn.functional.pad(mask_t, (0, pad_w, 0, pad_h), mode="reflect")

        return img_t, mask_t, h, w

    def clean_file(self, input_path, output_path):
        img = cv2.imread(input_path)
        if img is None:
            raise ValueError(f"Could not load image: {input_path}")

        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        results = self.reader.readtext(img)

        h, w, _ = img.shape
        mask = np.zeros((h, w), dtype=np.uint8)

        for (bbox, text, prob) in results:
            pts = np.array([bbox], dtype=np.int32)
            cv2.fillPoly(mask, pts, (255))

        kernel = np.ones((9, 9), np.uint8)
        dilated_mask = cv2.dilate(mask, kernel, iterations=2)

        with torch.no_grad():
            img_t, mask_t, oh, ow = self._preprocess(img, dilated_mask)

            img_t = img_t.to(self.device)
            mask_t = mask_t.to(self.device)

            result_t = self.lama_model(img_t, mask_t).cpu()

            result_t = result_t[:, :, :oh, :ow]
            result_np = result_t[0].permute(1, 2, 0).numpy()
            result_np = (np.clip(result_np, 0, 1) * 255).astype(np.uint8)
            result_bgr = cv2.cvtColor(result_np, cv2.COLOR_RGB2BGR)

        cv2.imwrite(output_path, result_bgr)

    def clean_folder(self, input_folder, output_folder):
        os.makedirs(output_folder, exist_ok=True)
        input_files = [
            f for f in os.listdir(input_folder)
            if os.path.splitext(f)[1].lower() in self.VALID_EXT
        ]

        cleaned_files = set(os.listdir(output_folder))
        files_to_process = [f for f in input_files if f not in cleaned_files]

        print(f"Found {len(input_files)} total images.")
        print(f"{len(cleaned_files)} already cleaned.")
        print(f"Processing {len(files_to_process)} remaining images...\n")

        for filename in tqdm(files_to_process, desc="Cleaning images"):
            try:
                self.clean_file(
                    os.path.join(input_folder, filename),
                    os.path.join(output_folder, filename)
                )
            except Exception as e:
                print(f"❌ Error on {filename}: {e}")

        print("\n✅ Finished! Cleaned images saved to:", output_folder)


import argparse

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    cleaner = MemeTextCleaner(gpu=True)
    cleaner.clean_file(input_path=args.input, output_path=args.output)
