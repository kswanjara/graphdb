package experiment;

import common.TreeNode;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

public class GenerateGCode {

    static GraphDatabaseFactory dbFactory;
    static GraphDatabaseService db;
    static final File folder = new File("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\target");
    //    static final File folder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/target/");
    private static double minEigen1 = Double.MAX_VALUE;
    private static double minEigen2 = Double.MAX_VALUE;
    private static long labelHash = 0;
    private static long neighHash = 0;
    static ArrayList<Double> eigenSeq1 = new ArrayList<>();
    static ArrayList<Double> eigenSeq2 = new ArrayList<>();

    private static long id = 0L;

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

    private static void generateGCode(String target) {
        long start = System.currentTimeMillis();
        minEigen1 = Double.MAX_VALUE;
        minEigen2 = Double.MAX_VALUE;
        labelHash = 0;
        neighHash = 0;
        eigenSeq1 = new ArrayList<>();
        eigenSeq2 = new ArrayList<>();

        try (ResourceIterator<Node> allNodes = db.findNodes(Label.label(target))) {
            while (allNodes.hasNext()) {
                Node node = allNodes.next();
                double[][] adjMatrix = createAdjMat(node, 2, target);
                Double[] eigen = getEigenValues(adjMatrix);
                minEigen1 = Double.min(eigen[0], minEigen1);
                minEigen2 = Double.min(eigen[1], minEigen2);
                eigenSeq1.add(eigen[0]);
                eigenSeq2.add(eigen[1]);
//                System.out.println(eigen);

                generateHashForNode(node);
            }
        }

//        Node n = db.createNode(Label.label(target));
//        n.setProperty("L", labelHash);
//        n.setProperty("N", neighHash);
//        n.setProperty("minEigen", minEigen);

        Collections.sort(eigenSeq1, Collections.reverseOrder());
        Collections.sort(eigenSeq2, Collections.reverseOrder());
//        System.out.println(eigens1);
//        System.out.println(eigens2);

        long end = System.currentTimeMillis();
        System.out.println("GraphIndex for " + target + ": L = " + labelHash + " N = " + neighHash + " minEigen1 = " + minEigen1 + " minEigen2 = " + minEigen2);
        System.out.println("time taken : " + ((end - start) / 1000));
    }

    private static void generateHashForNode(Node node) {
        String label = (String) node.getProperty("attr");
        labelHash += getHashCode(label);
        String[] neigh = (String[]) node.getProperty("neigh");
        long neighborHash = 0;
        for (String s : neigh) {
            if (s != null || !s.equalsIgnoreCase("")) {
                neighborHash += getHashCode(s);
            }
        }
        neighHash += neighborHash;
    }

    private static long getHashCode(String label) {
        char character = label.toUpperCase().charAt(0);
        int ascii = (int) character - 64;

        return ascii % 27;
    }

    private static void connectToGraphDB() {
        dbFactory = new GraphDatabaseFactory();
        db = dbFactory.newEmbeddedDatabase(new File("proteins"));
    }

    public static void main(String[] args) {

        connectToGraphDB();
        try (Transaction trax = db.beginTx()) {
//            for (File fileEntry : folder.listFiles()) {
//                String targetFile = fileEntry.getName().substring(0, fileEntry.getName().indexOf("."));
//            generateGCode("backbones_3GLD");
            generateGCode("backbones_1KFN");
            //      generateGCode("trial");
//                generateGCode(targetFile);
//            }
            trax.success();
        }
        db.shutdown();

        GenerateQueryGcode lnpt = new GenerateQueryGcode();
//        for (File fileEntry : folder.listFiles()) {
//            String queryFileFile = null;
//            try {
//                queryFileFile = fileEntry.getCanonicalPath();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        lnpt.createGraph("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\query\\mus_musculus_1U34.8.sub.grf");
        lnpt.updateProfilesForQuery();
        lnpt.generateGCode();

        compareGCode(lnpt);

    }

    private static void compareGCode(GenerateQueryGcode lnpt) {
        boolean allFine = true;
        if (lnpt.labelHash < labelHash) {
            System.out.println("Label condition true!");
        } else {
            allFine = false;
            System.out.println("Label condition failed!");
            return;
        }
        if (lnpt.neighHash < neighHash) {
            System.out.println("Neighbour condition true!");
        } else {
            allFine = false;
            System.out.println("Neighbour condition failed!");
            return;
        }
        if (lnpt.eigens1.size() > eigenSeq1.size() || lnpt.eigens2.size() > eigenSeq2.size()) {
            allFine = false;
            System.out.println("Eigen sequence size did not match!");
            return;
        }
        for (int i = 0; i < lnpt.eigens1.size(); i++) {
            if (lnpt.eigens1.get(i) <= eigenSeq1.get(i)) {
                continue;
            } else {
                allFine = false;
                System.out.println("Seq1 conparison failed!");
                return;
            }
        }

        for (int i = 0; i < lnpt.eigens2.size(); i++) {
            if (lnpt.eigens2.get(i) <= eigenSeq2.get(i)) {
                continue;
            } else {
                allFine = false;
                System.out.println("Seq2 conparison failed!");
                return;
            }
        }

        System.out.println("Eigen values compared successfully!");

    }

}
