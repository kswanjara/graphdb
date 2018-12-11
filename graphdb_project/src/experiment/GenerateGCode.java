package experiment;

import common.TreeNode;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;


/**
 * @author Jinal Shah
 * @author Kunal Wanjara
 * <p>
 * This class takes the nodes from each disjointed graph from neo4j database and generates the Gcode for each graph.
 * Stores the indexes for each disjointed graph back in neo4j with the label as the target file name.
 * <p>
 * The process of the generating indexes requires the generation of hash code for labels, it's neighbors, and the
 * eigen values obtained from the n-level path tree from each vertex.
 * <p>
 * The {@link HashMap} is used to store the hash of each label and its neighbors. {@link ArrayList} of {@link Double} is
 * used to maintain the ordered sequence of top two eigen values of each vertex.
 */
@SuppressWarnings("ALL")
public class GenerateGCode {

    public static GraphDatabaseFactory dbFactory;
    public static GraphDatabaseService db;
    static final File targetfolder = new File("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\target");
    static final File queryfolder = new File("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\query");
    //    static final File targetfolder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/target/");
    //    static final File queryfolder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/query/");


    // variables used to store the indexes for each graph
    static ArrayList<Double> eigenSeq1 = new ArrayList<>();
    static ArrayList<Double> eigenSeq2 = new ArrayList<>();
    static Map<String, Integer> occurences = new HashMap<>();
    static Map<String, Integer> neighOccurences = new HashMap<>();

    private static Logger logger = Logger.getLogger(GenerateGCode.class.getName());


    private static HashSet<Node> getAdjacentDataNodes(Integer v, String target) {
        HashSet<Node> nodes = new HashSet<>();

        Node n = db.findNode(Label.label(target), "id", v);

        for (Relationship r : n.getRelationships()) {
            Node neigh = r.getEndNode();
            Integer nodeId = (Integer) neigh.getProperty("id");
            if (!nodeId.equals(v))
                nodes.add(neigh);
            else
                nodes.add(r.getStartNode());
        }
        return nodes;

    }

    private static void search(Node n, int level, ArrayList<Integer> visited, TreeNode node, String target, HashMap<Integer, Integer> count) {
        if (level > 0) {

            ArrayList<TreeNode> adjNodes = new ArrayList<>();
            HashSet<Node> nodes = getAdjacentDataNodes((Integer) n.getProperty("id"), target);
            for (Node neigh : nodes) {
                Integer id = (Integer) neigh.getProperty("id");
                if (!visited.contains(id)) {
                    TreeNode r = new TreeNode((String) neigh.getProperty("attr"), id);
                    adjNodes.add(r);
                    visited.add(id);
                    count.put(id, count.getOrDefault(id, count.size()));
                    search(neigh, level - 1, visited, r, target, count);
                    visited.remove(id);
                }
            }
            node.setAdjList(adjNodes);
        }
        return;
    }

    public static double[][] createAdjacency(TreeNode node, double[][] adjList, HashMap<Integer, Integer> numNodes) {
        if (node.getAdjList().size() > 0) {
            for (TreeNode n : node.getAdjList()) {
                adjList[numNodes.get(node.getId())][numNodes.get(n.getId())] = 1;
                adjList[numNodes.get(n.getId())][numNodes.get(node.getId())] = 1;
                adjList = createAdjacency(n, adjList, numNodes);
            }
        }
        return adjList;
    }

    private static double[][] createAdjMat(Node n, int level, String target) {

        HashMap<Integer, Integer> numNodes = new HashMap<>();
        TreeNode root = createLNPT(n, level, target, numNodes);

        double[][] adjList = new double[numNodes.size()][numNodes.size()];
        for (TreeNode tn : root.getAdjList()) {
            adjList[numNodes.get(root.getId())][numNodes.get(tn.getId())] = 1;
            adjList[numNodes.get(tn.getId())][numNodes.get(root.getId())] = 1;
            adjList = createAdjacency(tn, adjList, numNodes);
        }
        return adjList;
    }

    private static TreeNode createLNPT(Node n, int level, String target, HashMap<Integer, Integer> count) {
        ArrayList<Integer> visited = new ArrayList<>();
        TreeNode root = new TreeNode((String) n.getProperty("attr"), (Integer) n.getProperty("id"));
        visited.add(root.getId());
        count.put(root.getId(), count.getOrDefault(root.getId(), count.size()));

        ArrayList<TreeNode> adjNodes = new ArrayList<>();
        for (Node neigh : getAdjacentDataNodes((Integer) n.getProperty("id"), target)) {
            TreeNode r = new TreeNode((String) neigh.getProperty("attr"), (Integer) neigh.getProperty("id"));

            adjNodes.add(r);
            visited.add(r.getId());
            count.put(r.getId(), count.getOrDefault(r.getId(), count.size()));
            search(neigh, level - 1, visited, r, target, count);
        }
        root.setAdjList(adjNodes);
        return root;
    }

    private static Double[] getEigenValues(double[][] adjMatrix) {
        ComplexDoubleMatrix eigenVectors = Eigen.eigenvalues(new DoubleMatrix(adjMatrix));

        ArrayList<Double> eigen = new ArrayList<>();

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < eigenVectors.rows; ++i) {
            for (int j = 0; j < eigenVectors.columns; ++j) {
                eigen.add(eigenVectors.get(i, j).real());
            }
        }

        Collections.sort(eigen, Collections.reverseOrder());
        Double[] eigenVals = {eigen.get(0), eigen.get(1)};
        return eigenVals;
    }

    public static void generateGCode(String target) {
        long start = System.currentTimeMillis();

        eigenSeq1 = new ArrayList<>();
        eigenSeq2 = new ArrayList<>();
        occurences = new HashMap<>();
        neighOccurences = new HashMap<>();

        try (ResourceIterator<Node> allNodes = db.findNodes(Label.label(target))) {
            while (allNodes.hasNext()) {
                Node node = allNodes.next();
                double[][] adjMatrix = createAdjMat(node, 2, target);
                Double[] eigen = getEigenValues(adjMatrix);
                eigenSeq1.add(eigen[0]);
                eigenSeq2.add(eigen[1]);

                generateHashForNode(node);
            }
        }

        Node n = db.createNode(Label.label("GCode"));

        n.setProperty("TARGET", target);

        for (String key : occurences.keySet()) {
            n.setProperty("L:" + key, occurences.get(key));
        }

        for (String key : neighOccurences.keySet()) {
            n.setProperty("N:" + key, neighOccurences.get(key));
        }

        Collections.sort(eigenSeq1, Collections.reverseOrder());
        Collections.sort(eigenSeq2, Collections.reverseOrder());

        Double[] seq1 = eigenSeq1.toArray(new Double[0]);
        Double[] seq2 = eigenSeq2.toArray(new Double[0]);

        n.setProperty("eigenSeq1", seq1);
        n.setProperty("eigenSeq2", seq2);


        long end = System.currentTimeMillis();
        System.out.println("Time taken for " + target + ": " + (end - start) + " ms.");
    }


    private static void generateHashForNode(Node node) {
        String label = (String) node.getProperty("attr");
        String[] neigh = (String[]) node.getProperty("neigh");

        if (!occurences.containsKey(label)) {
            occurences.put(label, 0);
        }
        occurences.put(label, occurences.get(label) + 1);

        for (String s : neigh) {
            if (s != null || !s.equalsIgnoreCase("")) {
                if (!neighOccurences.containsKey(s)) {
                    neighOccurences.put(s, 0);
                }
                neighOccurences.put(s, neighOccurences.get(s) + 1);
            }
        }
    }

    private static long getHashCode(String label) {
        char character = label.toUpperCase().charAt(0);
        int ascii = (int) character - 64;

        return ascii % 27;
    }

    public static void connectToGraphDB() {
        dbFactory = new GraphDatabaseFactory();
        db = dbFactory.newEmbeddedDatabase(new File("proteins"));
    }
}
