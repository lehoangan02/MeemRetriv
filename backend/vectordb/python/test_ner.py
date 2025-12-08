from gliner import GLiNER

class GLiNER_Person_Entity_Prediction:
    def __init__(self):
        # Initialize GLiNER with the base model
        self.model = GLiNER.from_pretrained("urchade/gliner_medium-v2.1")

    def predict_person_entities(self, text):
        # Labels for entity prediction
        labels = ["Person"]

        # Perform entity prediction
        entities = self.model.predict_entities(text, labels, threshold=0.5)

        return entities

# Example usage
if __name__ == "__main__":
    model = GLiNER_Person_Entity_Prediction()
    text = """
    Meme about Leonardo DiCaprio holding a glass of wine and smirking. The caption reads: "When you realize you've been acting for over 20 years and still haven't won an Oscar."
    """
    # Perform entity prediction
    entities = model.predict_person_entities(text)

    for entity in entities:
        print(entity["text"], "=>", entity["label"])