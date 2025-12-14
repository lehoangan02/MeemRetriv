import sys
import os
import multiprocessing

# --- CRITICAL: These must be set BEFORE importing torch or transformers ---
os.environ["TOKENIZERS_PARALLELISM"] = "false"
os.environ["OMP_NUM_THREADS"] = "1"
os.environ["MKL_NUM_THREADS"] = "1"
os.environ["USE_TF"] = "0"

# Set start method before other imports, but wrapped in try/except 
# to prevent errors if it was already set by another module
try:
    multiprocessing.set_start_method("spawn", force=True)
except RuntimeError:
    pass
# --------------------------------------------------------------------------

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

        # --- VRAM-friendly model loading ---
        self.model = None
        if self.device == "cuda":
            from transformers import BitsAndBytesConfig
            try:
                bnb_config = BitsAndBytesConfig(load_in_8bit=True)
                self.model = AutoModelForCausalLM.from_pretrained(
                    model_name,
                    device_map="auto",
                    quantization_config=bnb_config
                )
                print("[INFO] 8-bit quantization loaded successfully on CUDA.")
            except Exception as e:
                print(f"[WARNING] 8-bit quantization failed: {e}")
                print("[INFO] Falling back to CPU offload to save GPU memory.")
                self.model = AutoModelForCausalLM.from_pretrained(
                    model_name,
                    device_map="auto",
                    offload_folder="./offload",
                    dtype=self.dtype
                )
        else:
            self.model = AutoModelForCausalLM.from_pretrained(model_name, dtype=self.dtype)
            self.model.to(self.device)  # safe to move only if no device_map used

        # --- Do NOT call self.model.to() if device_map="auto" is used ---
        self.ner_model = GLiNER_Person_Entity_Prediction()

    def process_query(self, query):
        # NOTE: Since we are running in the same process now, 
        # ensure ner.py isn't trying to spawn new processes unnecessarily

        # Count time taken to predict person entities by the NER model
        import time
        start_time = time.time()
        celebrities = self.ner_model.predict_person_entities(query, threshold=0.5)
        end_time = time.time()
        print(f"Time taken to predict person entities: {(end_time - start_time) * 1000:.2f} ms")
        if celebrities:
            celebrities_text = ", ".join(celebrities)  # joins all names with comma
        else:
            celebrities_text = "unknown"

        # Count time taken to extract caption from the query by the LLM
        start_time = time.time()

        # Extract caption if present in quotes
        import re
        caption_match = re.search(r'"(.*?)"', query)
        caption_text = caption_match.group(1) if caption_match else ""

        system_prompt = f"""
        You are a data processing assistant for a meme retrieval system.
        Your job is to split the user query into three fields:

        - "celebrities": list of detected celebrity names
        - "caption": the meme caption text
        - "text": a generic visual description of the meme

        CAPTION VS DESCRIPTION RULES:
        1. If the user includes text inside quotation marks → that exact text is the caption.
        2. If there are no quotes:
        - If the sentence describes a visual scene (contains words like: meme, man, woman, guy, people, image, photo, picture, holding, standing, pointing, sitting, smirking, looking) → treat it as visual description, not caption.
        - If the sentence reads like commentary, a joke, a statement, or typical meme text → treat the entire input as the caption.
        3. If the query explicitly includes both a description and a caption (e.g., "caption:" or "the caption reads") → separate them accordingly.
        4. Never rewrite or modify the caption. Always keep caption text exactly as the user wrote it.
        5. Never invent a caption or description. Only separate what the user gave.

        GENERATION RULES:
        - "celebrities": use detected names if any
        - "caption": determined using the rules above
        - "text": rewrite only the visual description generically.  
        Replace each detected celebrity with "a person" while preserving the number and order.
        - Return ONLY valid JSON without explanations.

        Detected celebrities: {celebrities_text}
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

        end_time = time.time()
        print(f"Time taken to extract caption and description: {(end_time - start_time) * 1000:.2f} ms")
        
        return response

if __name__ == "__main__":
    processor = MemeLLMProcessor()
    # query = """
    # Meme about Leonardo DiCaprio holding a glass of wine and smirking. The caption reads: "When you realize you've been acting for over 20 years and still haven't won an Oscar."
    # """
    # query = """
    # Meme about Tom Hanks and Leonardo DiCaprio having a coffee together. The caption reads: "Actors just want to chill."
    # """
    # query = """
    # A woman is handing a gift to a man, while another woman is taking a photo. The caption reads: "Memories captured forever."
    # """
    # query = """
    # Obama giving Obama a medal.
    # """
    if len(sys.argv) > 1:
        query = sys.argv[1]
    else:
        query = "PERFECTLY HEALTHY GIVES BILLIONS TO CURE DISEASE KEEPS BILLIONS DIES OF CANCER"
    result = processor.process_query(query)
    print(result)