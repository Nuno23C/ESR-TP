import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopologyParser {

    public static void main(String[] args) {
        String inputFile = "topologia.txt"; // Substitua pelo nome do seu ficheiro de topologia
        String outputFile = "neighbors.txt";

        Map<String, List<String>> topology = parseTopology(inputFile);

        createNeighborsFile(topology, outputFile);
    }

    private static Map<String, List<String>> parseTopology(String inputFile) {
        Map<String, List<String>> topology = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            String currentNode = null;
            List<String> currentNeighbors = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.matches(".*\\{.*")) { // vai buscar o node 
                    Matcher matcher = Pattern.compile("node (\\w+) \\{").matcher(line);
                    if (matcher.matches()) {
                        currentNode = matcher.group(1);
                        currentNeighbors = new ArrayList<>();
                    } 
                } else if (line.matches(".*interface-peer.*")) { // encontramos os vizinhos
                    Matcher matcher = Pattern.compile("interface-peer \\{e\\d (\\w+)\\}").matcher(line);
                    if (matcher.matches()) {
                        String neighbor = matcher.group(1);
                        currentNeighbors.add(neighbor);
                    }
                } else if (line.matches(".*\\}.*")) {
                    // Fim do bloco de nó, adiciona à topologia
                    if (currentNode != null) {
                        topology.put(currentNode, currentNeighbors);
                        currentNode = null;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return topology;
    }
    // topology: Node1: [Node3, Node4]
    private static void createNeighborsFile(Map<String, List<String>> topology, String outputFile) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            for (Map.Entry<String, List<String>> entry : topology.entrySet()) {
                String node = entry.getKey();
                List<String> neighbors = entry.getValue();

                // escreve ID do node e vizinhos 
                writer.println(node);
                neighbors.forEach(writer::println);
                writer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
