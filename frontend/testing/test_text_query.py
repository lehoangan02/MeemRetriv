import requests

url = "http://localhost:8081/searchByText"
query = "funny minion meme"

response = requests.post(url, data=query, headers={"Content-Type": "text/plain"})
print(response.text)
