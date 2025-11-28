import os
import json
from collections import defaultdict

def build_indexes(root_dir):
    model_to_pieces = defaultdict(set)
    piece_to_models = defaultdict(set)

    # Loop through all LEGO models
    for model in os.listdir(root_dir):
        model_path = os.path.join(root_dir, model)
        if not os.path.isdir(model_path):
            continue

        # Loop through piece folders
        for piece in os.listdir(model_path):
            piece_path = os.path.join(model_path, piece)
            if not os.path.isdir(piece_path):
                continue

            # Add relationships (dedupe automatically with set)
            model_to_pieces[model].add(piece)
            piece_to_models[piece].add(model)

    # Convert sets → sorted lists for JSON
    model_to_pieces = {k: sorted(list(v)) for k, v in model_to_pieces.items()}
    piece_to_models = {k: sorted(list(v)) for k, v in piece_to_models.items()}

    return model_to_pieces, piece_to_models


def save_json(data, filename):
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


# ===== RUN HERE =====

ROOT = r"extracted"

model_to_pieces, piece_to_models = build_indexes(ROOT)

save_json(model_to_pieces, "model_to_pieces.json")
save_json(piece_to_models, "piece_to_models.json")

print("Done!")
print("Saved: model_to_pieces.json and piece_to_models.json")
