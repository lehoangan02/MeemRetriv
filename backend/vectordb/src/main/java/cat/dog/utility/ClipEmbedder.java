package cat.dog.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for generating MobileCLIP embeddings for images and text.
 * This abstracts the Python interop logic away from the repository layer.
 */
public class ClipEmbedder {

    private static final String VENV_DIR_NAME = ".venv";
    private static final String CHECKPOINT_FILE = "./models/mobileclip_s0.pt"; // Ensure this file exists in project root!
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

        // 1. Text Embedding Example -> Saves .npy
        String textQuery = "A funny minion meme";
        String textNpy = "text_query_vector.npy";
        System.out.println("Embedding text: \"" + textQuery + "\" -> " + textNpy);
        
        ClipEmbedder.embedText(textQuery, textNpy);

        // 2. Image Embedding Example -> Saves .npy
        String imagePath = "./image_3889.jpg";
        String imageNpy = "image_3889_vector.npy";
        System.out.println("Embedding image: " + imagePath + " -> " + imageNpy);
        
        ClipEmbedder.embedImage(imagePath, imageNpy);
        
        System.out.println("Done. Generated .npy files for inspection.");
    }

    /**
     * Overload for text embedding without saving to file.
     */
    public static String embedText(String textQuery) {
        return embedText(textQuery, null);
    }

    /**
     * Generates a normalized vector embedding for the given text.
     * @param textQuery The text to embed.
     * @param saveNpyPath Optional path to save the vector as a .npy file (can be null).
     * @return JSON string representation of the vector.
     */
    public static String embedText(String textQuery, String saveNpyPath) {
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
    public static String embedImage(String imagePath) {
        return embedImage(imagePath, null);
    }
    public static List<String> embedImageBatch(List<String> imagePaths) {
        List<String> results = new ArrayList<>();
        File ckptFile = new File(CHECKPOINT_FILE);
        
        if (!ckptFile.exists()) {
            System.err.println("ClipEmbedder Error: Model checkpoint not found at " + ckptFile.getAbsolutePath());
            // Fill with nulls to match size
            for (int i = 0; i < imagePaths.size(); i++) results.add(null);
            return results;
        }

        File tempInputFile = null;
        try {
            // 1. Write paths to temp file to avoid CLI arguments limit
            tempInputFile = File.createTempFile("batch_req", ".json");
            String jsonContent = imagePaths.stream()
                .map(path -> "\"" + path.replace("\\", "\\\\") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
                
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempInputFile))) {
                writer.write(jsonContent);
            }

            String ckptPath = ckptFile.getAbsolutePath().replace("\\", "\\\\");
            String inputJsonPath = tempInputFile.getAbsolutePath().replace("\\", "\\\\");

            // 2. Python Script
            // We print "BATCH_RES|..." for success or "BATCH_ERR|..." for failure
            // ensuring we print exactly one line per input item.
            String pythonScript = 
                "import torch, mobileclip, json, os, sys\n" +
                "import numpy as np\n" +
                "from PIL import Image\n" +
                "\n" +
                "def get_device():\n" +
                "    if torch.cuda.is_available(): return 'cuda'\n" +
                "    if torch.backends.mps.is_available(): return 'mps'\n" +
                "    return 'cpu'\n" +
                "\n" +
                "try:\n" +
                "    device = get_device()\n" +
                "    checkpoint_path = '" + ckptPath + "'\n" +
                "    # Load Model Once\n" +
                "    model, _, preprocess = mobileclip.create_model_and_transforms('mobileclip_s0', pretrained=checkpoint_path)\n" +
                "    model.to(device)\n" +
                "    model.eval()\n" +
                "\n" +
                "    with open('" + inputJsonPath + "', 'r') as f:\n" +
                "        image_paths = json.load(f)\n" +
                "\n" +
                "    # Loop and ensure output order matches input order\n" +
                "    for img_path in image_paths:\n" +
                "        try:\n" +
                "            if not os.path.exists(img_path):\n" +
                "                raise FileNotFoundError(f'{img_path} not found')\n" +
                "\n" +
                "            image = Image.open(img_path).convert('RGB')\n" +
                "            image_tensor = preprocess(image).unsqueeze(0).to(device)\n" +
                "\n" +
                "            with torch.no_grad():\n" +
                "                features = model.encode_image(image_tensor)\n" +
                "                features /= features.norm(dim=-1, keepdim=True)\n" +
                "\n" +
                "            # Success: Print Vector\n" +
                "            vec_str = json.dumps(features.cpu().numpy().flatten().tolist())\n" +
                "            print(f'BATCH_RES|{vec_str}', flush=True)\n" +
                "\n" +
                "        except Exception as inner_e:\n" +
                "            # Failure: Print Error Marker\n" +
                "            # We print the error to stderr for logs, but BATCH_ERR to stdout for Java logic\n" +
                "            sys.stderr.write(f'Skipping {img_path}: {inner_e}\\n')\n" +
                "            print('BATCH_ERR|NULL', flush=True)\n" +
                "\n" +
                "except Exception as e:\n" +
                "    # Fatal script error\n" +
                "    print(f'FATAL|{e}', flush=True)";

            // 3. Execute and Parse
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXEC, "-c", pythonScript);
            // We do NOT redirectErrorStream(true) here because we want to separate 
            // the JSON data (stdout) from the noise/logs (stderr) if possible, 
            // but for simplicity in this helper we often merge them. 
            // Let's merge them but parse carefully.
            pb.redirectErrorStream(true); 
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("BATCH_RES|")) {
                    // Success: Add vector string
                    results.add(line.substring("BATCH_RES|".length()));
                } else if (line.startsWith("BATCH_ERR|")) {
                    // Failure: Add null to maintain index alignment
                    results.add(null);
                } else if (line.startsWith("FATAL|")) {
                    System.err.println("Python Critical Error: " + line);
                } else {
                    // Debug logs from python (optional)
                    // System.out.println("[PyLog] " + line);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempInputFile != null && tempInputFile.exists()) {
                tempInputFile.delete();
            }
        }
        
        // Safety: Ensure result list is same size as input list
        // (In case Python crashed halfway through)
        while (results.size() < imagePaths.size()) {
            results.add(null);
        }

        return results;
    }

    /**
     * Generates a normalized vector embedding for the image at the given path.
     * @param imagePath Absolute or relative path to the image file.
     * @param saveNpyPath Optional path to save the vector as a .npy file (can be null).
     * @return JSON string representation of the vector.
     */
    public static String embedImage(String imagePath, String saveNpyPath) {
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

    private static String executePython(String script) {
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