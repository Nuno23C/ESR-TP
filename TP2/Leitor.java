import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class Leitor {
    public static HashMap<String,HashMap<String,String>> readFile(String path) throws IOException {
        HashMap<String,HashMap<String,String>> topologia = new HashMap<>();
        File file = new File(path);
        Scanner scan = new Scanner(file);
        String temp = scan.nextLine();
        while (scan.hasNextLine()) {
            if (!temp.startsWith(" ")) {
                String host = temp.substring(0, temp.indexOf(":"));
                HashMap<String,String> map = new HashMap<>();
                while (scan.hasNextLine() && (temp = scan.nextLine()).startsWith(" ")) {
                    temp = temp.stripIndent();
                    String a[] = temp.split(":");
                    map.put(a[0], a[1]);
                }
                topologia.put(host, map);
            }
        }
        scan.close();
        return topologia;
    }
}
