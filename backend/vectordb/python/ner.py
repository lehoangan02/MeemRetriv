import os
import multiprocessing
import torch

os.environ["TOKENIZERS_PARALLELISM"] = "false"
os.environ["OMP_NUM_THREADS"] = "1"
os.environ["MKL_NUM_THREADS"] = "1"
os.environ["USE_TF"] = "0"

multiprocessing.set_start_method("spawn", force=True)

from gliner import GLiNER
from actor import CharacterActorResolver

class GLiNER_Person_Entity_Prediction:
    def __init__(self):
        # Detect device: CUDA -> MPS -> CPU
        self.device = torch.device("cuda" if torch.cuda.is_available() else "mps" if torch.backends.mps.is_available() else "cpu")
        # self.device = "cpu"
        self.model = GLiNER.from_pretrained("urchade/gliner_medium-v2.1").to(self.device)

    def predict_person_entities(self, text, threshold=0.5):
        # We define what we want AND what we want to ignore
        # The model will assign "A woman" to "woman" or "person", 
        # and "Leonardo DiCaprio" to "celebrity".
        labels = ["celebrity", "famous person", "unknown person", "character"]
        
        entities = self.model.predict_entities(text, labels, threshold=threshold)
        
        # Filter: Only keep entities that were predicted as 'celebrity'
        celebrities = [
            entity["text"] 
            for entity in entities 
            if entity["label"] == "celebrity"
        ]
        character_names = [
            entity["text"] 
            for entity in entities 
            if entity["label"] in ["character", "famous person"]
        ]
        # save the character names to a file for debugging
        with open("character_names.txt", "w") as f:
            for name in character_names:
                f.write(name + "\n")
        # replace character names with celebrities if they exist
        resolver = CharacterActorResolver()
        name_to_actor = {}

        for name in character_names:
            actor = resolver.resolve(name)
            if actor:
                name_to_actor[name] = actor

        prompt = "These are the celebrities correseponding to the characters: "
        for name, actor in name_to_actor.items():
            prompt += f"{actor} is the celebrity/actor for {name}. "


        return celebrities, prompt, character_names, name_to_actor

if __name__ == "__main__":
    model = GLiNER_Person_Entity_Prediction()
    # text = """
    # Meme about Leonardo DiCaprio holding a glass of wine and smirking. The caption reads: "When you realize you've been acting for over 20 years and still haven't won an Oscar."
    # """
    # text = """
    # A woman hands a gift to a man, while another woman takes a photo.
    # """
    text = """
    A two-panel meme.
    Top panel: Bill Gates, with text saying “Perfectly healthy” and “Gives billions to cure disease.”
    Bottom panel: Steve Jobs speaking on a stage, with text saying “Keeps billions” and “Dies of cancer.”
    """
    text = """
    A two-panel meme featuring characters from Game of Thrones.
    Top panel: Sansa Stark looking serious, with text saying "Only a fool would trust Littlefinger."
    Bottom panel: Ned Stark and Catelyn Stark standing together, smiling broadly and looking at each other.
    """
    celebrities, prompt, character_names, name_to_actor = model.predict_person_entities(text, threshold=0.5)
    print("Entities recognized as Person:", celebrities)
    print("Characters recognized as Person:", character_names)
    print("Mapping of character names to actors:", name_to_actor)
