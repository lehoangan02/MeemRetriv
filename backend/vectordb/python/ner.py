import os
import multiprocessing

os.environ["TOKENIZERS_PARALLELISM"] = "false"
os.environ["OMP_NUM_THREADS"] = "1"
os.environ["MKL_NUM_THREADS"] = "1"
os.environ["USE_TF"] = "0"

multiprocessing.set_start_method("spawn", force=True)

from gliner import GLiNER

class GLiNER_Person_Entity_Prediction:
    def __init__(self):
        self.model = GLiNER.from_pretrained("urchade/gliner_medium-v2.1")

    def predict_person_entities(self, text, threshold=0.5):
        labels = ["Person"]
        entities = self.model.predict_entities(text, labels, threshold=threshold)
        print("Predicted Entities:", entities)
        return [entity["text"] for entity in entities]

if __name__ == "__main__":
    model = GLiNER_Person_Entity_Prediction()
    text = """
    Meme about Leonardo DiCaprio holding a glass of wine and smirking. The caption reads: "When you realize you've been acting for over 20 years and still haven't won an Oscar."
    """
    results = model.predict_person_entities(text, threshold=0.5)
    print("Entities recognized as Person:", results)
