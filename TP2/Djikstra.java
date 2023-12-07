import java.util.HashMap;
import java.util.LinkedList;


public class Djikstra {
    public static HashMap<String,LinkedList<String>> route(HashMap<String,HashMap<String,String>> topologia,HashMap<String,HashMap<String,Long>> pesos) {
        HashMap<String,LinkedList<String>> n = new HashMap<>();

        LinkedList<String> pontosChave = new LinkedList<>();
        // Ver quantos percursos existem para verificar
        for (String host : topologia.keySet())
            if (host.equals("RP") || host.contains("Server")) pontosChave.add(host);
        // Criação de nós para criar grafos
        HashMap<String,HashMap<String,Node>> nodes = new HashMap();
        for (String host: pontosChave) 
            nodes.put(host,new HashMap<>());

        for (String host: topologia.keySet())
            for (String no: pontosChave)
                nodes.get(no).put(host, new Node(host));

        HashMap<String,Path> graph = new HashMap<>();
        for (String no: pontosChave) graph.put(no, new Path());

        // Adicionar os Nos aos grafos
        for (String host: pontosChave) {
            for (String node: nodes.get(host).keySet()) {
                Node noTemp = nodes.get(host).get(node);
                for (String key: topologia.get(node).keySet())
                    noTemp.addDestination(nodes.get(host).get(key), pesos.get(node).get(key));
                for (String no: pontosChave) 
                    graph.get(no).addNode(noTemp);
            }
        }

        // Calcular os grafos diferentes
        for (String no: pontosChave)
            graph.get(no).calculatePath(graph.get(no), nodes.get(no).get(no));

        
        for (String noInicial: pontosChave) {
            for (Node node: graph.get(noInicial).getNodes()) {
                if ((node.getName().contains("C") && noInicial.equals("RP")) || (node.getName().equals("RP") && noInicial.contains("Server"))) {
                    LinkedList<Node> sp = (LinkedList) node.getShortestPath();
                    switch (sp.size()) {
                        case 2:
                            String before = sp.get(0).getName();
                            String now = sp.get(1).getName();
                            String after = node.getName();
                            if (n.get(topologia.get(before).get(now)) == null)
                                n.put(topologia.get(before).get(now), new LinkedList<>());
                            n.get(topologia.get(before).get(now)).add(topologia.get(now).get(after));
                            break;
                        case 1:
                            // Caso o server seja vizinho do proprio RP
                            before = sp.get(0).getName();
                            now = node.getName();
                            // Procura o endereço da ligação RP ao Server
                            if (n.get(topologia.get(now).get(before)) == null)
                                n.put(topologia.get(now).get(before), new LinkedList<>());
                            n.get(topologia.get(now).get(before)).add(topologia.get(before).get(now));
                        default:
                            for (int i = 2; i < sp.size(); i++) {
                            before = sp.get(i-2).getName();
                            now = sp.get(i-1).getName();
                            after = sp.get(i).getName();
                            if (n.get(topologia.get(before).get(now)) == null)
                                n.put(topologia.get(before).get(now), new LinkedList<>());
                            n.get(topologia.get(before).get(now)).add(topologia.get(now).get(after));
                            }
                            break;
                    }
                }
            }
    }
    return n;
}
}
