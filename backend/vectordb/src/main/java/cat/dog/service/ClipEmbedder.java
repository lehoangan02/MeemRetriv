package cat.dog.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Service responsible for generating MobileCLIP embeddings for images and text.
 * This abstracts the Python interop logic away from the repository layer.
 */
public class ClipEmbedder {

    private static final String VENV_DIR_NAME = ".venv";
    private static final String CHECKPOINT_FILE = "./mobileclip_s0.pt"; // Ensure this file exists in project root!
    private static final String PYTHON_EXEC;

    // Static block to determine OS-specific paths for Python inside venv
    static {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "Scripts" + File.separator + "python.exe";
        } else {
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "bin" + File.separator + "python3";
        }
    }

    public static void main(String[] args) {
        ClipEmbedder embedder = new ClipEmbedder();

        // 1. Text Embedding Example -> Saves .npy
        String textQuery = "A funny minion meme";
        String textNpy = "text_query_vector.npy";
        System.out.println("Embedding text: \"" + textQuery + "\" -> " + textNpy);
        
        embedder.embedText(textQuery, textNpy);

        // 2. Image Embedding Example -> Saves .npy
        String imagePath = "./image_3889.jpg";
        String imageNpy = "image_3889_vector.npy";
        System.out.println("Embedding image: " + imagePath + " -> " + imageNpy);
        
        embedder.embedImage(imagePath, imageNpy);
        
        System.out.println("Done. Generated .npy files for inspection.");
    }

    /**
     * Overload for text embedding without saving to file.
     */
    public String embedText(String textQuery) {
        return embedText(textQuery, null);
    }

    /**
     * Generates a normalized vector embedding for the given text.
     * @param textQuery The text to embed.
     * @param saveNpyPath Optional path to save the vector as a .npy file (can be null).
     * @return JSON string representation of the vector.
     */
    public String embedText(String textQuery, String saveNpyPath) {
        String safeText = textQuery.replace("'", "\\'");
        
        // Resolve Checkpoint Path
        File ckptFile = new File(CHECKPOINT_FILE);
        if (!ckptFile.exists()) {
            System.err.println("ClipEmbedder Error: Model checkpoint not found at " + ckptFile.getAbsolutePath());
            return null;
        }
        String ckptPath = ckptFile.getAbsolutePath().replace("\\", "\\\\");

        // Inject NPY saving logic if path is provided
        String npyLogic = "";
        if (saveNpyPath != null) {
            String safePath = saveNpyPath.replace("\\", "\\\\");
            npyLogic = "    np.save('" + safePath + "', features.cpu().numpy().astype(np.float32))\n";
        }
        
        String pythonScript = 
            "import torch, mobileclip, json\n" +
            "import numpy as np\n" +
            "try:\n" +
            "    # FIXED: Load from specific checkpoint file to ensure deterministic weights\n" +
            "    checkpoint_path = '" + ckptPath + "'\n" +
            "    model, _, _ = mobileclip.create_model_and_transforms('mobileclip_s0', pretrained=checkpoint_path)\n" +
            "    model.eval()\n" + 
            "    tokenizer = mobileclip.get_tokenizer('mobileclip_s0')\n" +
            "    \n" +
            "    text_tensor = tokenizer(['" + safeText + "'])\n" +
            "    with torch.no_grad():\n" +
            "        features = model.encode_text(text_tensor)\n" +
            "        # CRITICAL: Normalize vector to match database format\n" +
            "        features /= features.norm(dim=-1, keepdim=True)\n" +
            "    \n" +
            npyLogic +
            "    print('VECTOR_START|' + json.dumps(features.cpu().numpy().flatten().tolist()))\n" +
            "except Exception as e:\n" +
            "    print(f'ERROR|{e}')";

        return executePython(pythonScript);
    }

    /**
     * Overload for image embedding without saving to file.
     */
    public String embedImage(String imagePath) {
        return embedImage(imagePath, null);
    }

    /**
     * Generates a normalized vector embedding for the image at the given path.
     * @param imagePath Absolute or relative path to the image file.
     * @param saveNpyPath Optional path to save the vector as a .npy file (can be null).
     * @return JSON string representation of the vector.
     */
    public String embedImage(String imagePath, String saveNpyPath) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("ClipEmbedder Error: Image file does not exist at " + imagePath);
            return null;
        }
        
        // Resolve Paths
        String absImagePath = imageFile.getAbsolutePath().replace("\\", "\\\\");
        
        File ckptFile = new File(CHECKPOINT_FILE);
        if (!ckptFile.exists()) {
            System.err.println("ClipEmbedder Error: Model checkpoint not found at " + ckptFile.getAbsolutePath());
            return null;
        }
        String ckptPath = ckptFile.getAbsolutePath().replace("\\", "\\\\");

        // Inject NPY saving logic if path is provided
        String npyLogic = "";
        if (saveNpyPath != null) {
            String safePath = saveNpyPath.replace("\\", "\\\\");
            npyLogic = "    np.save('" + safePath + "', features.cpu().numpy().astype(np.float32))\n";
        }

        String pythonScript = 
            "import torch, mobileclip, json\n" +
            "import numpy as np\n" +
            "from PIL import Image\n" +
            "try:\n" +
            "    # FIXED: Load from specific checkpoint file to ensure deterministic weights\n" +
            "    checkpoint_path = '" + ckptPath + "'\n" +
            "    model, _, preprocess = mobileclip.create_model_and_transforms('mobileclip_s0', pretrained=checkpoint_path)\n" +
            "    model.eval()\n" + 
            "    image = Image.open('" + absImagePath + "').convert('RGB')\n" +
            "    image_tensor = preprocess(image).unsqueeze(0)\n" +
            "    \n" +
            "    with torch.no_grad():\n" +
            "        features = model.encode_image(image_tensor)\n" +
            "        # CRITICAL: Normalize vector to match database format\n" +
            "        features /= features.norm(dim=-1, keepdim=True)\n" +
            "    \n" +
            npyLogic +
            "    print('VECTOR_START|' + json.dumps(features.cpu().numpy().flatten().tolist()))\n" +
            "except Exception as e:\n" +
            "    print(f'ERROR|{e}')";

        return executePython(pythonScript);
    }

    private String executePython(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXEC, "-c", script);
            pb.redirectErrorStream(true); // Merge stderr into stdout
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Uncomment for debugging python output
                // System.out.println("PYTHON: " + line); 
                
                if (line.startsWith("VECTOR_START|")) {
                    return line.split("\\|")[1];
                } else if (line.startsWith("ERROR|")) {
                    System.err.println("ClipEmbedder Python Error: " + line.substring(6));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}