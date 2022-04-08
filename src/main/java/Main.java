import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args)  {

        try {
            Printer printer = new Printer("/dev/usb/lp3");
            BufferedImage img = ImageIO.read(new File("/home/arne/Downloads/dog.jpg"));
            printer.printImage(img, true);
            //printer.printImage(img, false);
            //FileLinePrinter linePrinter = new FileLinePrinter(printer, "/home/arne/Downloads/porn_search_terms.txt");
            //linePrinter.printAll();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("DONE");
    }
}
