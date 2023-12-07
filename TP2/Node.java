

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Node {
    private String name;
    private List<Node> shortestPath = new LinkedList<>();
    private Long distance = Long.MAX_VALUE;


    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Nó: "+name;
    }
    Map<Node, Long> distanceMap = new HashMap<>();

    public void addDestination(Node destination, Long distance) {
        distanceMap.put(destination, distance);
    }

    public String getName() {
        return name;
    }
    public Node(String name) {
        this.name = name;
    }

    public void setDistance(Long distance) {
        this.distance = distance;
    }

    public Long getDistance() {
        return distance;
    }

    public void setShortestPath(List<Node> shortestPath) {
        this.shortestPath = shortestPath;
    }


    public Node clone2() throws CloneNotSupportedException {
        Node n = new Node(this.name);
        n.setDistance(this.distance);
        n.setShortestPath(new LinkedList<>());
        return n;
    }

    public List<Node> getShortestPath() {
        return shortestPath;
    }
    public Map<Node, Long> getDistanceMap() {
        return distanceMap;
    }
    
}

