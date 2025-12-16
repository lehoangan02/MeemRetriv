import requests

url = "http://localhost:8081/searchByText"
# meme_description = """
# A two-panel meme.
# Top panel: Bill Gates, with text saying “Perfectly healthy” and “Gives billions to cure disease.”
# Bottom panel: Steve Jobs speaking on a stage, with text saying “Keeps billions” and “Dies of cancer.”
# """
# meme_description = """
# Jon Snow
# """
# meme_description = """
# A two-panel meme featuring actors from Game of Thrones.
# Top panel: Sophie Turner looking serious, with text saying "Only a fool would trust Littlefinger."
# Bottom panel: Sean Bean and Michelle Fairley standing together, smiling broadly and looking at each other.
# """
meme_description = """
A two-panel meme featuring characters from Game of Thrones.
Top panel: Sansa Stark looking serious, with text saying "Only a fool would trust Littlefinger."
Bottom panel: Ned Stark and Catelyn Stark standing together, smiling broadly and looking at each other.
"""
headers = {
    "Content-Type": "text/plain",
    "Connection": "close" 
}

response = requests.post(url, data=meme_description.encode('utf-8'), headers=headers)

print(response.text)