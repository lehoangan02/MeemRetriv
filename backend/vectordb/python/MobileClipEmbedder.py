import torch
import numpy as np
from PIL import Image
import warnings
import os

# Suppress warnings
warnings.filterwarnings("ignore", category=FutureWarning)
warnings.filterwarnings("ignore", category=UserWarning)

try:
    import mobileclip
except ImportError:
    raise ImportError(
        "The 'mobileclip' library is not installed.\n"
        "pip install git+https://github.com/apple/ml-mobileclip.git"
    )

class MobileCLIPEmbedder:
    def __init__(self, model_name="mobileclip_s0", pretrained_path=None, device=None):
        """
        Initializes the MobileCLIP model and tokenizer once.

        Args:
            model_name (str): Variant (e.g., "mobileclip_s0", "mobileclip_s1").
            pretrained_path (str): Path to local .pt file. If None, tries to download/load default.
            device (str): "cuda", "mps", "cpu", or None (auto-detect).
        """
        self.device = self._get_device(device)
        self.model_name = model_name
        
        print(f"Initializing {model_name} on {self.device}...")

        try:
            # 1. Load Model and Image Transforms
            self.model, _, self.preprocess = mobileclip.create_model_and_transforms(
                self.model_name, 
                pretrained=pretrained_path
            )
            
            # 2. Load Tokenizer (Required for text)
            self.tokenizer = mobileclip.get_tokenizer(self.model_name)

            self.model = self.model.to(self.device)
            self.model.eval()
            print("MobileCLIP (Model & Tokenizer) loaded successfully.")
            
        except Exception as e:
            raise RuntimeError(f"Failed to load MobileCLIP model: {e}")

    def _get_device(self, device_arg):
        if device_arg:
            return device_arg
        if torch.cuda.is_available():
            return "cuda"
        elif torch.backends.mps.is_available():
            return "mps"
        return "cpu"

    def get_embedding(self, image_input, normalize=True):
        """
        Generates an embedding for a single image.
        (Kept as 'get_embedding' to maintain backward compatibility with your server)
        """
        try:
            # Handle input types (File path vs PIL Object)
            if isinstance(image_input, str):
                if not os.path.exists(image_input):
                    raise FileNotFoundError(f"Image not found: {image_input}")
                image = Image.open(image_input).convert('RGB')
            elif isinstance(image_input, Image.Image):
                image = image_input.convert('RGB')
            else:
                raise ValueError("Input must be a file path string or PIL Image object.")

            # Preprocess
            image_tensor = self.preprocess(image).unsqueeze(0).to(self.device)

            # Inference
            with torch.no_grad():
                features = self.model.encode_image(image_tensor)
                
                if normalize:
                    features /= features.norm(dim=-1, keepdim=True)

            # Return numpy array
            return features.cpu().numpy().astype(np.float32)

        except Exception as e:
            print(f"Error embedding image: {e}")
            return None

    def get_text_embedding(self, text_input, normalize=True):
        """
        Generates an embedding for a text string.

        Args:
            text_input (str): The text to embed (e.g., "a photo of a cat").
            normalize (bool): Whether to L2 normalize (Recommended).

        Returns:
            np.ndarray: The text embedding (shape: [1, 512]).
        """
        try:
            if not isinstance(text_input, str):
                raise ValueError(f"Text input must be a string, got {type(text_input)}")

            # Tokenize (Tokenizer expects a list of strings usually, or handles single str)
            # We wrap in list to be safe: [text_input]
            text_tensor = self.tokenizer([text_input]).to(self.device)

            with torch.no_grad():
                features = self.model.encode_text(text_tensor)
                
                if normalize:
                    features /= features.norm(dim=-1, keepdim=True)

            return features.cpu().numpy().astype(np.float32)

        except Exception as e:
            print(f"Error embedding text: {e}")
            return None