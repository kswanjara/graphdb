package experiment;

import common.Node;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class LNPTAlgorithm {

    //    public static final String filePath = "C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\target\\backbones_1AF7.grf";
    public static final String filePath = "C:\\Users\\Kunal Wanjara\\Desktop\\trial.txt";
    private static Map<String, Node> nodeVals;

    public static void main(String[] args) throws FileNotFoundException {
        Set<Node> graph = generateQueryGraph();
        if (graph.size() > 0) {
            System.out.println(graph.size());
            Node v = graph.iterator().next();
            Node lnptTree = getAdjacencyList(v, 2);

            System.out.println("Done = " + counter);
        }
    }

    private static Node getAdjacencyList(Node v, int n) {
        Map<Node, ArrayList<Node>> lnptTree = new HashMap<>();
        Node root = new Node("", v.getId(), v.getAttr());
        Set<Integer> visited = new HashSet<>();
        visited.add(v.getId());
        counter++;
        for (Node r : v.getNeighbors()) {
            Node r1 = new Node("", r.getId(), r.getAttr());
            root.getNeighbors().add(r1);
            visited.add(r.getId());
            counter++;
            digTree(r, r1, n, visited);
            visited.remove(r.getId());
        }

        return root;
    }

    public static int counter = 0;

    private static void digTree(Node r, Node r1, int n, Set<Integer> visited) {
        n = n - 1;
        if (n == 0) {
            return;
        }
        for (Node neigh : r.getNeighbors()) {
            if (!visited.add(neigh.getId())) {
                continue;
            }
            Node neigh1 = new Node("", neigh.getId(), neigh.getAttr());
            r1.getNeighbors().add(neigh1);
            counter++;
            digTree(neigh, neigh1, n, visited);
            visited.remove(neigh.getId());
        }
    }

    public static Set<Node> generateQueryGraph() throws FileNotFoundException {
        Map<Node, ArrayList<Node>> map = new LinkedHashMap<>();
        nodeVals = new HashMap<>();
        File file = new File(filePath);
        Scanner sc = new Scanner(file);
        while (sc.hasNextLine()) {
            String[] input = sc.nextLine().trim().split(" ");
            if (input.length > 1) {
                if (!StringUtils.isNumeric(input[1])) {
                    String nodeName = "u" + input[0];
                    Node node = new Node(nodeName, Integer.parseInt(input[0]), input[1]);
                    map.put(node, new ArrayList<>());
                    nodeVals.put(nodeName, node);

                } else {
                    String node1 = "u" + input[0];
                    String node2 = "u" + input[1];

                    map.get(nodeVals.get(node1).getNeighbors().add((nodeVals.get(node2))));
                }
            }
        }
        return map.keySet();
    }
}
