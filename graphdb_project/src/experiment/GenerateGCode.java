package experiment;

import common.TreeNode;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GenerateGCode {

    static GraphDatabaseFactory dbFactory;
    static GraphDatabaseService db;
    static final File targetfolder = new File("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\target");
    static final File queryfolder = new File("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\query");
    //    static final File targetfolder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/target/");
    //    static final File queryfolder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/query/");
    private static double minEigen1 = Double.MAX_VALUE;
    private static double minEigen2 = Double.MAX_VALUE;
    private static long labelHash = 0;
    private static long neighHash = 0;
    static ArrayList<Double> eigenSeq1 = new ArrayList<>();
    static ArrayList<Double> eigenSeq2 = new ArrayList<>();
    private static Logger logger = Logger.getLogger(GenerateGCode.class.getName());
    static Map<String, Integer> occurences = new HashMap<>();
    static Map<String, Integer> neighOccurences = new HashMap<>();

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
        occurences = new HashMap<>();
        neighOccurences = new HashMap<>();

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
//        System.out.println("GraphIndex for " + target + ": L = " + labelHash + " N = " + neighHash + " minEigen1 = " + minEigen1 + " minEigen2 = " + minEigen2);
//        System.out.println("time taken : " + ((end - start) / 1000));
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
                if (!neighOccurences.containsKey(label)) {
                    neighOccurences.put(label, 0);
                }
                neighOccurences.put(label, neighOccurences.get(label) + 1);
            }
        }

//        String label = (String) node.getProperty("attr");
//        labelHash += getHashCode(label);
//        String[] neigh = (String[]) node.getProperty("neigh");
//        long neighborHash = 0;
//        for (String s : neigh) {
//            if (s != null || !s.equalsIgnoreCase("")) {
//                neighborHash += getHashCode(s);
//            }
//        }
//        neighHash += neighborHash;
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

        Handler fileHandler = null;
        try {
            fileHandler = new FileHandler("./GcodeTruth.log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        GenerateQueryGcode lnpt = new GenerateQueryGcode();
        for (File fileEntry : queryfolder.listFiles()) {
            String queryFile = null;
            try {
                queryFile = fileEntry.getCanonicalPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (queryFile.contains(".8.")) {
                lnpt.createGraph(queryFile);
                lnpt.updateProfilesForQuery();
                lnpt.generateGCode();

                connectToGraphDB();
                try (Transaction trax = db.beginTx()) {
                    for (File fileEntry1 : targetfolder.listFiles()) {
                        String targetFile = fileEntry1.getName().substring(0, fileEntry1.getName().indexOf("."));
                        //            generateGCode("backbones_3GLD");
                        //            generateGCode("backbones_1KFN");
                        //            generateGCode("trial");
                        generateGCode(targetFile);
                        boolean isEligible = compareGCode(lnpt);
                        if (isEligible) {
                            logger.info("Not pruned T: " + fileEntry1.getName() + "\tQ: " + fileEntry.getName() + "\n");
                        } else {
                            logger.info("Pruned T: " + fileEntry1.getName() + "\tQ: " + fileEntry.getName() + "\n");
                        }
                    }
                    trax.success();
                }
                db.shutdown();
            }
        }
    }

    private static boolean compareGCode(GenerateQueryGcode lnpt) {
        boolean allFine = true;
        if (compareLabels(lnpt)) {
//            System.out.println("Label condition true!");
        } else {
            logger.warning("Label condition failed!");
            return false;
        }
        if (compareNeighLabels(lnpt)) {
//            System.out.println("Neighbour condition true!");
        } else {
            logger.warning("Neighbour condition failed!");
            return false;
        }
        if (lnpt.eigens1.size() > eigenSeq1.size() || lnpt.eigens2.size() > eigenSeq2.size()) {
            logger.warning("Eigen sequence size did not match!");
            return false;
        } else {
            for (int i = 0; i < lnpt.eigens1.size(); i++) {
                if (lnpt.eigens1.get(i) <= eigenSeq1.get(i)) {
                    continue;
                } else {
                    logger.warning("Seq1 comparison failed!");
                    return false;
                }
            }


            for (int i = 0; i < lnpt.eigens2.size(); i++) {
                if (lnpt.eigens2.get(i) <= eigenSeq2.get(i)) {
                    continue;
                } else {
                    logger.warning("Seq2 comparison failed!");
                    return false;
                }
            }
        }

//        System.out.println("Eigen values compared successfully!");

        return allFine;
    }

    private static boolean compareNeighLabels(GenerateQueryGcode lnpt) {
        for (String key : lnpt.neighOccurences.keySet()) {
            if (!neighOccurences.containsKey(key) || lnpt.neighOccurences.get(key) > occurences.get(key)) {
                return false;
            }
        }
        return true;
    }

    private static boolean compareLabels(GenerateQueryGcode lnpt) {
        for (String key : lnpt.occurences.keySet()) {
            if (!occurences.containsKey(key) || lnpt.occurences.get(key) > occurences.get(key)) {
                return false;
            }
        }
        return true;
    }

}
