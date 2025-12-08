package cat.dog.utility;

import ai.onnxruntime.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.*;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LamaInpaint {

    private OrtEnvironment env;
    private OrtSession session;

    public LamaInpaint(String modelPath) throws Exception {
        env = OrtEnvironment.getEnvironment();
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
        System.out.println("Loaded ONNX LaMa model");
    }

    private String generateMask(String imagePath) throws Exception {
        String maskPath = imagePath + "_mask.png";

        ProcessBuilder pb = new ProcessBuilder(
                "python3", "generate_mask.py",
                imagePath,
                maskPath
        );

        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            br.lines().forEach(System.out::println);
        }

        p.waitFor();
        return maskPath;
    }

    // SINGLE IMAGE CLEANING
    public void cleanSingleImage(String inputPath, String outputPath) throws Exception {

        String maskPath = generateMask(inputPath);

        Mat img = Imgcodecs.imread(inputPath);
        Mat mask = Imgcodecs.imread(maskPath, Imgcodecs.IMREAD_GRAYSCALE);

        if (img.empty()) throw new RuntimeException("Cannot read: " + inputPath);

        // Normalize + prepare
        Mat imgFloat = new Mat();
        img.convertTo(imgFloat, CvType.CV_32FC3, 1.0 / 255);

        Mat maskFloat = new Mat();
        mask.convertTo(maskFloat, CvType.CV_32FC1, 1.0 / 255);

        // Resize to CHW
        float[] imgCHW = new float[(int) (imgFloat.total() * imgFloat.channels())];
        imgFloat.get(0, 0, imgCHW);

        float[] maskCHW = new float[(int) maskFloat.total()];
        maskFloat.get(0, 0, maskCHW);

        // ONNX inputs
        long[] shapeImg = {1, 3, img.rows(), img.cols()};
        long[] shapeMask = {1, 1, img.rows(), img.cols()};

        OnnxTensor inputImg = OnnxTensor.createTensor(env, FloatBuffer.wrap(imgCHW), shapeImg);
        OnnxTensor inputMask = OnnxTensor.createTensor(env, FloatBuffer.wrap(maskCHW), shapeMask);

        Map<String, OnnxTensor> inputMap = Map.of(
                "image", inputImg,
                "mask", inputMask
        );

        OrtSession.Result output = session.run(inputMap);
        float[][][][] result = (float[][][][]) output.get(0).getValue();

        // Convert back to OpenCV Mat
        Mat out = new Mat(img.rows(), img.cols(), CvType.CV_32FC3);
        float[] outData = new float[img.rows() * img.cols() * 3];

        int idx = 0;
        for (int c = 0; c < 3; c++)
            for (int y = 0; y < img.rows(); y++)
                for (int x = 0; x < img.cols(); x++)
                    outData[(y * img.cols() + x) * 3 + c] = result[0][c][y][x];

        out.put(0, 0, outData);

        Mat finalImg = new Mat();
        out.convertTo(finalImg, CvType.CV_8UC3, 255);

        Imgcodecs.imwrite(outputPath, finalImg);

        System.out.println("Saved cleaned image: " + outputPath);
    }

    // BATCH FOLDER CLEANING
    public void cleanFolder(String inputFolder, String outputFolder) throws Exception {
        File in = new File(inputFolder);
        File out = new File(outputFolder);
        if (!out.exists()) out.mkdirs();

        List<File> images = List.of(in.listFiles())
                .stream()
                .filter(f -> f.getName().matches(".*\\.(png|jpg|jpeg|webp)"))
                .collect(Collectors.toList());

        for (File file : images) {
            System.out.println("Cleaning " + file.getName());
            cleanSingleImage(file.getAbsolutePath(),
                    outputFolder + "/" + file.getName());
        }
    }
}
