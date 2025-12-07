import requests
from image_to_base64 import image_to_base64

image_path = "image_3889.jpg"
encoded_image = image_to_base64(image_path)

url = "http://localhost:8081/uploadImageBase64"

json_data = {
    "filename": "image_3889.jpg",
    "imageBase64": encoded_image
}

response = requests.post(url, json=json_data)
print(response.status_code)
print(response.text)