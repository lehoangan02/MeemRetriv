import os
import glob
import argparse
import warnings
import torch
import numpy as np
from PIL import Image
from tqdm import tqdm

# Suppress annoying warnings from timm/mobileclip dependencies
warnings.filterwarnings("ignore", category=FutureWarning)
warnings.filterwarnings("ignore", category=UserWarning)

# CORRECTED IMPORT: Use 'mobileclip' instead of 'mobile_clip'
try:
    import mobileclip
except ImportError:
    raise ImportError(
        "The 'mobileclip' library is not installed.\n"
        "Please install it using: pip install git+https://github.com/apple/ml-mobileclip.git"
    )

def get_device(device_arg):
    """Selects the best available device."""
    if device_arg:
        return device_arg
    
    if torch.cuda.is_available():
        return "cuda"
    # Check for Apple Silicon (MPS) acceleration
    elif torch.backends.mps.is_available():
        return "mps"
    else:
        return "cpu"

def main():
    parser = argparse.ArgumentParser(description="Embed images using MobileCLIP and save as .npy")
    parser.add_argument("--input_dir", type=str, required=True, help="Directory containing input images")
    parser.add_argument("--output_dir", type=str, default=None, help="Directory to save .npy files (defaults to input_dir)")
    parser.add_argument("--model_name", type=str, default="mobileclip_s0", choices=["mobileclip_s0", "mobileclip_s1", "mobileclip_s2", "mobileclip_b"], help="MobileCLIP model variant")
    parser.add_argument("--pretrained", type=str, default="./../backend/vectordb/models/mobileclip_s0.pt", help="Path to pretrained checkpoint (e.g., mobileclip_s0.pt). Optional if model auto-downloads.")
    parser.add_argument("--device", type=str, default=None, help="Device to use (cuda, mps, cpu). Defaults to best available.")
    
    args = parser.parse_args()

    # handle output directory
    if args.output_dir is None:
        args.output_dir = args.input_dir
    os.makedirs(args.output_dir, exist_ok=True)

    device = get_device(args.device)
    print(f"Loading model: {args.model_name} on {device}...")
    
    # Load model and preprocessing transform
    try:
        model, _, preprocess = mobileclip.create_model_and_transforms(
            args.model_name, 
            pretrained=args.pretrained
        )
    except Exception as e:
        print(f"\nError loading model: {e}")
        print("-" * 50)
        print("Troubleshooting Tip:")
        print(f"1. Download the checkpoint manually (e.g., {args.model_name}.pt).")
        print("   You can find links in the official Apple repository: https://github.com/apple/ml-mobileclip")
        print(f"2. Run the script again with: --pretrained /path/to/{args.model_name}.pt")
        print("-" * 50)
        return

    model = model.to(device)
    model.eval()

    image_files = []
    
    print(f"Scanning for images in {args.input_dir} (recursive)...")
    
    # Use os.walk for robust recursive search instead of glob
    valid_exts = {'.jpg', '.jpeg', '.png', '.bmp', '.webp'}
    
    for root, _, files in os.walk(args.input_dir):
        for file in files:
            # Check extension case-insensitively
            if os.path.splitext(file)[1].lower() in valid_exts:
                image_files.append(os.path.join(root, file))

    # Remove duplicates
    image_files = sorted(list(set(image_files)))
    
    if not image_files:
        print(f"No images found in {args.input_dir}")
        return

    print(f"Found {len(image_files)} images. Starting embedding...")

    for img_path in tqdm(image_files):
        try:
            # Load and preprocess image
            image = Image.open(img_path).convert('RGB')
            image_tensor = preprocess(image).unsqueeze(0).to(device)

            # Generate embedding
            with torch.no_grad():
                features = model.encode_image(image_tensor)

                features /= features.norm(dim=-1, keepdim=True)
            
            # Convert to numpy
            features_np = features.cpu().numpy().astype(np.float32)
            
            # Construct output filename (Flat structure)
            # e.g., /path/to/subdir/cat.jpg -> /output_dir/cat.npy
            base_name = os.path.splitext(os.path.basename(img_path))[0]
            output_path = os.path.join(args.output_dir, f"{base_name}.npy")
            
            # Save
            np.save(output_path, features_np)

        except Exception as e:
            print(f"Failed to process {img_path}: {e}")

    print("Processing complete.")

if __name__ == "__main__":
    main()