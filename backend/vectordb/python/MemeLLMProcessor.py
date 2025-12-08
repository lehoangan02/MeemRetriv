import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from ner import GLiNER_Person_Entity_Prediction

class MemeLLMProcessor:
    def __init__(self, model_name="Qwen/Qwen2.5-1.5B-Instruct"):
        if torch.backends.mps.is_available() and torch.backends.mps.is_built():
            self.device = "mps"
            self.dtype = torch.float16
        elif torch.cuda.is_available():
            self.device = "cuda"
            self.dtype = torch.float16
        else:
            self.device = "cpu"
            self.dtype = torch.float32

        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForCausalLM.from_pretrained(model_name, dtype=self.dtype)
        self.model.to(self.device)

        self.ner_model = GLiNER_Person_Entity_Prediction()

    def process_query(self, query):
        celebrities = self.ner_model.predict_person_entities(query, threshold=0.5)
        celebrity_text = celebrities[0] if celebrities else "unknown"

        # Extract caption if present in quotes
        import re
        caption_match = re.search(r'"(.*?)"', query)
        caption_text = caption_match.group(1) if caption_match else ""

        system_prompt = f"""
You are a data processing assistant. 
Generate JSON with keys: celebrity, caption, text. 
- celebrity: use the detected celebrity if any
- caption: exact text in quotes from the input
- text: rewrite the rest of the description generically (replace celebrity with 'a man'/'a woman', keep actions)
Return ONLY valid JSON.

Detected celebrity: {celebrity_text}
Caption: {caption_text}
Input: {query}
"""

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": query}
        ]

        text = self.tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        model_inputs = self.tokenizer([text], return_tensors="pt").to(self.device)

        generated_ids = self.model.generate(
            **model_inputs,
            max_new_tokens=256,
            temperature=0.1
        )

        generated_ids = [
            output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
        ]

        response = self.tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]
        return response

if __name__ == "__main__":
    processor = MemeLLMProcessor()
    query = """
Meme about Leonardo DiCaprio holding a glass of wine and smirking. The caption reads: "When you realize you've been acting for over 20 years and still haven't won an Oscar."
"""
    result = processor.process_query(query)
    print(result)
