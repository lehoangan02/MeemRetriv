import pickle

# Load the pickle file
with open('celebrity_clip_vectors.pkl', 'rb') as f:
    data = pickle.load(f)

# Inspect the structure
print(f"Type: {type(data)}")
print(f"Keys/Length: {len(data)}")

# Print sample data
if isinstance(data, dict):
    for key, value in list(data.items())[:3]:  # First 3 items
        print(f"Key: {key}, Value shape: {value.shape if hasattr(value, 'shape') else len(value)}")
else:
    print(f"Data: {data[:3]}")  # First 3 items