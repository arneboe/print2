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

    public void printImage(BufferedImage img, boolean dither)
    {
        RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper();
        int threshold = 127;
        img = scaleImage(img); //copies the img (important, otherwise dithering would break the image
        if(dither) {
            threshold = otsuTreshold(img);
            atkinsonDither(img, threshold);
        }
        Bitonal algorithm = new BitonalThreshold(threshold);
        EscPosImage escposImage = new EscPosImage(new CoffeeImageImpl(img), algorithm);
        try {
            escpos.write(imageWrapper, escposImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**dithers the given img using the given threshold */
    private void atkinsonDither(BufferedImage img, final int threshold) {
        final double w1 = 1.0/8.0;
        final int h = img.getHeight();
        final int w = img.getWidth();
        for (int y=0; y<h; y++){
            for (int x=0; x<w; x++){
                int oldpixel = getBrightness(x,y, img);
                int newpixel = oldpixel < threshold? 0 : 255;
                setPixel(x, y, newpixel, img);
                final double quant_error = oldpixel - newpixel;
                setPixel(x+1,y, (int)(getBrightness(x+1,y, img) + w1 * quant_error), img);
                setPixel(x+2,y, (int)(getBrightness(x+2,y, img) + w1 * quant_error), img);
                setPixel(x-1,y+1, (int)(getBrightness(x-1,y+1, img) + w1 * quant_error), img);
                setPixel(x,y+1, (int)(getBrightness(x,y+1, img) + w1 * quant_error), img);
                setPixel(x+1,y+1, (int)(getBrightness(x+1,y+1, img) + w1 * quant_error), img);
                setPixel(x,y+2, (int)(getBrightness(x,y+2, img) + w1 * quant_error), img);
            }
        }
    }

    private void setPixel(int x, int y, int brightness, BufferedImage img)
    {
        if(x >= img.getWidth() || y >= img.getHeight() || x < 0 || y < 0) return;
        if(brightness < 0) brightness = 0;
        if(brightness > 255) brightness = 255;
        Color c = new Color(brightness, brightness, brightness);
        img.setRGB(x, y, c.getRGB());
    }

    /** returns a scaled copy of the image  */
    private BufferedImage scaleImage(BufferedImage input)
    {
        final int targetWidth = input.getWidth() < maxImgWidth? input.getWidth() : maxImgWidth;
        final double scale = input.getWidth() / (double)targetWidth;
        final int targetHeight = (int) Math.ceil(input.getHeight() / scale);

        BufferedImage img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.drawImage(input, 0, 0, targetWidth, targetHeight, 0, 0, input.getWidth(),
                input.getHeight(), null);
        g.dispose();
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
        Color c = null;
        if(img.getType() == BufferedImage.TYPE_INT_ARGB) {
            c = new Color(img.getRGB(x, y), true);
        }
        else {
            c = new Color(img.getRGB(x, y));
        }
        int red = c.getRed();
        int green = c.getGreen();
        int blue = c.getBlue();
        // calc luminance in range 0.0 to 255; using SRGB luminance constants
        double luminance = (0.21 * red) + (0.71 * green) + (0.07 * blue);
        //scale by alpha if the pixel has an alpha component
        if(img.getType() == BufferedImage.TYPE_INT_ARGB) {
            luminance *= c.getAlpha()/255.0;
        }

        return (int)luminance;
    }


}
