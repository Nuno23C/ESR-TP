import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/* tipos de mensagem :
 * pedir conteudo -> ask_content
 * enviar conteudo -> response_content
 * 
 * caso tenha o conteudo -> content_on_me
 * 
 */



public class oNode {
    private ServerSocket serverSocket; // tcp
    private Socket clientSocket;
    
    public static final int ASK_CONTENT = 1; // pedir o video
    public static final int NOT_CONTENT = 2; 
    public String nodeId;
    public List<String> neighbors;
    
    
    public oNode(int port) {
        try {
            serverSocket = new ServerSocket(port);
            neighbors = new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendContentRequest() {
        // pedir o video aos vizinhos 
        for(String neighbor: neighbors) {
            Packet packet = new Packet(ASK_CONTENT, neighbor, nodeId); // como ir buscar os ips ???
        }

    }


    public void init(String ficheiro) {
        carregarVizinhos(ficheiro);
        // Lógica para aceitar conexões e interagir com vizinhos
        new Thread(this::sendContentRequest).start();

        // Lógica para se conectar a outros nós
        new Thread(this::conectarAOutrosNos).start();
    }

    private void carregarVizinhos(String ficheiro) {
        try (Scanner scanner = new Scanner(new File(ficheiro))) {
            while (scanner.hasNextLine()) {
                String id = scanner.nextLine();
                if(id.equals(nodeId) ) {
                    String nodes = scanner.nextLine();
                    while(!nodes.trim().isEmpty()) {
                        neighbors.add(nodes);
                        if (scanner.hasNextLine()) {
                            nodes = scanner.nextLine();
                        } else {
                            break;  // Terminar o loop se não houver mais linhas
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receberMessages() {
        try {
            while (true) {
                clientSocket = serverSocket.accept();
                // Lógica para tratar a conexão recebida
                // Exemplo: criar uma thread para lidar com as mensagens do cliente
                new Thread(() -> tratarCliente(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void conectarAOutrosNos() {
        // Lógica para se conectar a outros nós
        // Exemplo: usar Socket para se conectar a outros nós e init a comunicação
    }

    private void tratarCliente(Socket clientSocket) {
        // Lógica para lidar com mensagens recebidas do cliente
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("java oNode <ficheiro>");
            System.exit(1);
        }

        
        int port = 8080; 
        String ficheiro = args[0];
        
        oNode node = new oNode(port);
        node.init(ficheiro);
    }
}


