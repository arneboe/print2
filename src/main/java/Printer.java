import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.image.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.Buffer;

public class Printer {

    private EscPos escpos;
    private OutputStream printer;
    private Style style = new Style()
                        .setJustification(EscPosConst.Justification.Left_Default)
                        .setBold(false)
                        .setUnderline(Style.Underline.None_Default)
                        .setFontName(Style.FontName.Font_B);

    private final int maxImgWidth = 380;

    /*
    @param filename to to dev/usb/lpX
     */
    public Printer(String filename) throws FileNotFoundException {
        printer = new FileOutputStream(filename);
        escpos = new EscPos(printer);
    }

    public void setStyle(Style style)
    {
        this.style = style;
    }

    public void close()
    {
        try {
            escpos.close();
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void printLn(String line)
    {
        try {
            escpos.writeLF(style, line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printImage(BufferedImage img)
    {
        RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper();
        final int threshold = otsuTreshold(img);
        Bitonal algorithm = new BitonalThreshold(threshold);
        BufferedImage scaledImg = scaleImage(img);
        EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(scaledImg), algorithm);
        try {
            escpos.write(imageWrapper, escposImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage scaleImage(BufferedImage input)
    {
        if(input.getWidth() < maxImgWidth) {
            //return input;
        }

        final int targetWidth = maxImgWidth;
        final double scale = input.getWidth() / (double)targetWidth;
        final int targetHeight = (int) Math.ceil(input.getHeight() / scale);

        BufferedImage img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.drawImage(input, 0, 0, targetWidth, targetHeight, 0, 0, input.getWidth(),
                input.getHeight(), null);
        g.dispose();
        try {
            ImageIO.write(img, "png", new File("/home/arne/test.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    private int otsuTreshold(BufferedImage original) {

        int[] histogram = imageHistogram(original);
        int total = original.getHeight() * original.getWidth();

        float sum = 0;
        for(int i=0; i<256; i++) sum += i * histogram[i];

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 0;

        for(int i=0 ; i<256 ; i++) {
            wB += histogram[i];
            if(wB == 0) continue;
            wF = total - wB;

            if(wF == 0) break;

            sumB += (float) (i * histogram[i]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if(varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }

        return threshold;

    }

    private int[] imageHistogram(BufferedImage input) {

        int[] histogram = new int[256];

        for(int i=0; i<histogram.length; i++) histogram[i] = 0;

        for(int i=0; i<input.getWidth(); i++) {
            for(int j=0; j<input.getHeight(); j++) {
                int brigh = getBrightness(i, j, input);
                histogram[brigh]++;
            }
        }

        return histogram;

    }

    private int getBrightness(int x, int y, BufferedImage img)
    {
        if(x >= img.getWidth() || y >= img.getHeight() || x < 0 || y < 0) return 0;
        Color c = new Color(img.getRGB(x, y));
        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();
        return (int)((0.21 * red) + (0.71 * green) + (0.07 * blue));
    }


}
