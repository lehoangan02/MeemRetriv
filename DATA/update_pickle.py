import pickle
import os

# Configuration
input_file = 'celebrity_clip_vectors.pkl'
output_file = 'celebrity_clip_vectors_updated.pkl'
new_prefix = "./../../DATA/"

print(f"Loading {input_file}...")
with open(input_file, 'rb') as f:
    data = pickle.load(f)

print(f"Loaded {len(data)} records. Processing paths...")

# Counter to track changes
count = 0

for item in data:
    original_path = item['path']
    
    # 1. Normalize slashes: Replace backslashes (\) with forward slashes (/)
    # Example: ./celebrity_images_by_name\\Aaron... -> ./celebrity_images_by_name/Aaron...
    clean_path = original_path.replace('\\', '/')
    
    # 2. Remove the leading './' if it exists to prepare for the new prefix
    if clean_path.startswith('./'):
        clean_path = clean_path[2:]  # Remove first 2 chars
    
    # 3. Construct the new path
    # Final result: ./../../DATA/celebrity_images_by_name/Aaron_Eckhart/00000.jpg
    new_path = new_prefix + clean_path
    
    # Update the dictionary
    item['path'] = new_path
    count += 1

# Print a verification sample (First 3 items)
print("\n--- Verification Sample (First 3) ---")
for i in range(3):
    print(f"Original (from memory): {data[i]['path'].replace(new_prefix, './')}") # Approximate look back
    print(f"Updated:  {data[i]['path']}")
    print("-" * 30)

# Save the updated data
print(f"\nSaving updated data to {output_file}...")
with open(output_file, 'wb') as f:
    pickle.dump(data, f)

print("Done! You can rename the new file to the original name if the paths look correct.")