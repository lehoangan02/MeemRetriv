import pandas as pd

df = pd.read_csv("celeb_mapping.csv")

def update_path(path):
    parts = path.split("/")  # ['./Aaron_Eckhart', '00000.jpg']
    celebrity_name = parts[1]
    filename = parts[2]
    return f"./../../DATA/celebrity_images_by_name/{celebrity_name}/{filename}"

df["file_path"] = df["file_path"].apply(update_path)
df.to_csv("updated_file.csv", index=False)
