import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Stack;

public class FileLinePrinter {

    private Stack<String> words = new Stack<>();
    private Printer printer;

    public FileLinePrinter(Printer printer, String filename) throws FileNotFoundException {
        this.printer = printer;
        File f = new File(filename);

        Scanner s = new Scanner(f);
        while (s.hasNextLine()) {
            words.add(s.nextLine());
        }
        s.close();
        System.out.println("Loaded " + words.size() + " words from file");
    }

    public void print(int num)
    {
        for(int i = 0; i < num; i++) {
            if (words.empty()) {
                System.out.println("Words empty, nothing to print");
                return;
            }
            printer.printLn(words.pop());
        }
    }

    public void printAll()
    {
        while(!words.empty())
        {
            printer.printLn(words.pop());
        }
    }
}
