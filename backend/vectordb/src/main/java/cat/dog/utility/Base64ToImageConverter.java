package cat.dog.utility;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

public class Base64ToImageConverter {

    private static final String FOLDER = "received_images";
    private static final String FILENAME = "query_image.png";

    public static boolean saveBase64AsPng(String imageBase64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

            File folder = new File(FOLDER);
            if (!folder.exists()) folder.mkdir();

            InputStream is = new ByteArrayInputStream(imageBytes);
            BufferedImage bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) return false;

            File file = new File(folder, FILENAME);
            ImageIO.write(bufferedImage, "png", file);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
