import sys
import os
import multiprocessing
import re

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
        import time
        import re
        import json

        # 1. NER Prediction
        start_time = time.time()
        
        # Initialize variables to ensure safety
        celebrities = []
        helper_prompt = ""
        
        try:
            # Get the mapping string (helper_prompt) and the list
            celebrities, helper_prompt, character_names, name_to_actor = self.ner_model.predict_person_entities(query, threshold=0.5)
            celebrities = celebrities + character_names
            print("NER PROMPT: ", helper_prompt)
            print("CELEBRITIES: ", celebrities)
        except Exception as e:
            print(f"[WARNING] NER failed: {e}")
        
        print(f"NER Time: {(time.time() - start_time) * 1000:.2f} ms")

        # 2. Regex Caption Extraction
        caption_match = re.search(r'"(.*?)"', query)
        hint_caption = caption_match.group(1) if caption_match else "None detected"

        # 3. LLM Extraction
        start_time = time.time()

        # --- CHANGE 1: STATIC SYSTEM PROMPT ---
        system_prompt = """
        You are a smart database assistant. Extract structured JSON from the meme description.

        ### ONE-SHOT EXAMPLE (STRICTLY FOLLOW THIS PATTERN):
        User Input: "Elon Musk smoking a joint. Text says 'To the moon'."
        Context Celebrities: ["Elon Musk"]
        Output JSON:
        {
            "celebrities": ["Elon Musk"],
            "caption": "To the moon",
            "text": "A person smoking a joint."
        }

        ### GUIDELINES:
        1. "celebrities": Output the list of real names provided in the context.
        2. "caption": 
           - Extract text found inside quotes.
           - Extract text explicitly described as "saying", "reads", or "text on image".
           - Join multiple captions with " | ".
        3. "text" (Visual Description): 
           - Describe the visual action.
           - CRITICAL: REPLACE ALL NAMES (Celebrities/Characters) with generic terms like "a person".
           - The "text" field must NOT contain proper names.
        """

        # --- CHANGE 2: INJECT CONTEXT INTO USER MESSAGE ---
        # We explicitly put the helper prompt right next to the query so the 1.5B model can't miss it.
        user_payload = f"""
        ### CONTEXT / KNOWLEDGE BASE:
        {helper_prompt}

        ### HINTS:
        Text detected in quotes: "{hint_caption}"

        ### CELEBRITIES (Use these for the 'celebrities' list):
        {', '.join(celebrities) if celebrities else 'None detected'}

        ### USER INPUT:
        {query}

        ### INSTRUCTION:
        Generate the JSON. Remember to replace names in the "text" field with "a person".
        """

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_payload}
        ]

        # Debug: Print exactly what goes into the model
        # print("--- DEBUG PROMPT ---")
        # print(user_payload)
        # print("--------------------")

        text = self.tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        model_inputs = self.tokenizer([text], return_tensors="pt").to(self.device)

        generated_ids = self.model.generate(
            **model_inputs,
            max_new_tokens=256,
            temperature=0.1, 
            do_sample=False
        )

        generated_ids = [
            output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
        ]

        response = self.tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]

        print(f"LLM Time: {(time.time() - start_time) * 1000:.2f} ms")
        
        # Cleanup JSON
        try:
            json_start = response.find('{')
            json_end = response.rfind('}') + 1
            if json_start != -1 and json_end != -1:
                response = response[json_start:json_end]
        except:
            pass
        # final process to replace all character names with "a person" in the "text" field
        # # get the descriptive text from the response
        # text = json.loads(response).get("text", "")
        # # replace names in the text
        # names_to_replace = character_names + celebrities
        
        # cleaned_text = self.__replace_names_in_text(text, names_to_replace)
        # # reconstruct the response JSON
        # response_json = json.loads(response)
        # response_json["text"] = cleaned_text
        # response = json.dumps(response_json)

        print("Maps of character names to actors:", name_to_actor)
        
        response = self.__replace_celebrities_with_actors(response, name_to_actor)
        return response


    def __replace_celebrities_with_actors(self, response, name_to_actor):
        import json

        try:
            data = json.loads(response)
        except:
            return response

        celebrities = data.get("celebrities", [])
        if not celebrities or not name_to_actor:
            return response

        data["celebrities"] = [
            name_to_actor.get(name, name)
            for name in celebrities
        ]

        return json.dumps(data, ensure_ascii=False)


    
    def __replace_names_in_text(self, text, names):
        print("Names to replace:", names)
        if not names:
            return text
        pattern = re.compile(r"\b(" + "|".join(re.escape(n) for n in names) + r")\b", re.IGNORECASE)
        return pattern.sub("a person", text)

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
        query = """Top panel: Bill Gates, with text saying “Perfectly healthy” and “Gives billions to cure disease.”
        Bottom panel: Steve Jobs speaking on a stage, with text saying “Keeps billions” and “Dies of cancer.”
        """
        query = """
        A two-panel meme featuring characters from Game of Thrones.
        Top panel: Sansa Stark looking serious, with text saying "Only a fool would trust Littlefinger."
        Bottom panel: Ned Stark and Catelyn Stark standing together, smiling broadly and looking at each other.
        """
    result = processor.process_query(query)
    print("asdfasf")
    print(result)