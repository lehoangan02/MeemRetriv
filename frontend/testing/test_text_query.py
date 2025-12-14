import requests

url = "http://localhost:8081/searchByText"
meme_description = """
A two-panel meme.
Top panel: Bill Gates, with text saying “Perfectly healthy” and “Gives billions to cure disease.”
Bottom panel: Steve Jobs speaking on a stage, with text saying “Keeps billions” and “Dies of cancer.”
"""

headers = {
    "Content-Type": "text/plain",
    "Connection": "close" 
}

response = requests.post(url, data=meme_description.encode('utf-8'), headers=headers)

print(response.text)