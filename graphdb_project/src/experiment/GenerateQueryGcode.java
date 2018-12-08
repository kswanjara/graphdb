package experiment;

import common.TreeNode;
import org.apache.commons.lang.StringUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.neo4j.cypher.internal.frontend.v3_3.phases.Do;

import java.io.*;
import java.util.*;


public class GenerateQueryGcode {

    static final File folder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/query/");
    Graph<Integer, DefaultEdge> queryGraph = null;
    HashMap<Integer, String> attributeLabels = null;
    HashMap<Integer, List<String>> profiles = new HashMap<>();
    long labelHash = 0;
    long neighHash = 0;
    double minEigen1 = Double.MIN_VALUE;
    double minEigen2 = Double.MIN_VALUE;
    ArrayList<Double> eigens1 = new ArrayList<>();
    ArrayList<Double> eigens2 = new ArrayList<>();

    public void createGraph(String filePath) {

        queryGraph = new SimpleGraph<>(DefaultEdge.class);
        attributeLabels = new HashMap<Integer, String>();
        String l = null;
        try {

            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while ((l = br.readLine()) != null) {
                String[] str = l.split(" ");
                if (str.length > 1) {
                    if (StringUtils.isNumeric(str[1])) {
                        queryGraph.addEdge(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
                    } else {
                        queryGraph.addVertex(Integer.parseInt(str[0]));
                        attributeLabels.put(Integer.parseInt(str[0]), str[1]);
                    }

                }

            }

            br.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void updateProfilesForQuery() {
        for (Integer u : queryGraph.vertexSet()) {

            List<String> profile = new ArrayList<>();
            for (Integer neigh : Graphs.neighborListOf(queryGraph, u)) {
                profile.add(attributeLabels.get(neigh));
            }
            Collections.sort(profile);
            profiles.put(u, profile);
        }
    }

    private void search(Integer n, int level, ArrayList<Integer> visited, TreeNode node, HashMap<Integer, Integer> count) {
        if (level > 0) {

            ArrayList<TreeNode> adjNodes = new ArrayList<>();
            for (Integer neigh : Graphs.neighborListOf(queryGraph, n)) {
                if (!visited.contains(neigh)) {
                    TreeNode r = new TreeNode(attributeLabels.get(neigh), neigh);
                    adjNodes.add(r);
                    visited.add(r.getId());
                    count.put(r.getId(), count.getOrDefault(r.getId(), count.size()));
                    search(neigh, level - 1, visited, r, count);
                    visited.remove(neigh);
                }
            }
            node.setAdjList(adjNodes);
        }
        return;
    }

    private TreeNode createLNPT(Integer n, int level, HashMap<Integer, Integer> count) {
        ArrayList<Integer> visited = new ArrayList<>();
        TreeNode root = new TreeNode(attributeLabels.get(n), n);
        visited.add(n);
        count.put(n, count.getOrDefault(n, count.size()));

        ArrayList<TreeNode> adjNodes = new ArrayList<>();
        for (Integer neigh : Graphs.neighborListOf(queryGraph, n)) {
            TreeNode r = new TreeNode(attributeLabels.get(neigh), neigh);
            adjNodes.add(r);
            visited.add(r.getId());
            count.put(r.getId(), count.getOrDefault(r.getId(), count.size()));
            search(neigh, level - 1, visited, r, count);
        }
        root.setAdjList(adjNodes);
        return root;
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

    private double[][] createAdjMat(Integer node, int level) {

        HashMap<Integer, Integer> numNodes = new HashMap<>();
        TreeNode root = createLNPT(node, level, numNodes);

        double[][] adjList = new double[numNodes.size()][numNodes.size()];
        for (TreeNode tn : root.getAdjList()) {
            adjList[numNodes.get(root.getId())][numNodes.get(tn.getId())] = 1;
            adjList[numNodes.get(tn.getId())][numNodes.get(root.getId())] = 1;
            adjList = createAdjacency(tn, adjList, numNodes);
        }
        return adjList;
    }

    private Double[] getEigenValues(double[][] adjMatrix) {

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

    public void generateGCode() {
        long start = System.currentTimeMillis();
        minEigen1 = Double.MIN_VALUE;
        minEigen2 = Double.MIN_VALUE;
        labelHash = 0;
        neighHash = 0;
        eigens1 = new ArrayList<>();
        eigens2 = new ArrayList<>();

        for (Integer u : queryGraph.vertexSet()) {
            double[][] adjMatrix = createAdjMat(u, 2);
            Double[] eigen = getEigenValues(adjMatrix);
            minEigen1 = Double.max(eigen[0], minEigen1);
            minEigen2 = Double.max(eigen[1], minEigen2);
            eigens1.add(eigen[0]);
            eigens2.add(eigen[1]);
//                System.out.println(eigen);

            generateHashForNode(u);
        }

        Collections.sort(eigens1, Collections.reverseOrder());
        Collections.sort(eigens2, Collections.reverseOrder());
//        System.out.println(eigens1);
//        System.out.println(eigens2);

//        Node n = db.createNode(Label.label(target));
//        n.setProperty("L", labelHash);
//        n.setProperty("N", neighHash);
//        n.setProperty("minEigen", minEigen);


        long end = System.currentTimeMillis();
        System.out.println("GraphIndex for : L = " + labelHash + " N = " + neighHash + " minEigen1 = " + minEigen1 + " minEigen2 = " + minEigen2);
        System.out.println("time taken : " + ((end - start) / 1000));
    }

    private void generateHashForNode(Integer n) {
        String label = attributeLabels.get(n);
        labelHash += getHashCode(label);
        List<String> neigh = profiles.get(n);
        long neighborHash = 0;
        for (String s : neigh) {
            if (s != null || !s.equalsIgnoreCase("")) {
                neighborHash += getHashCode(s);
            }
        }
        neighHash += neighborHash;
    }

    private long getHashCode(String label) {
        char character = label.toUpperCase().charAt(0);
        int ascii = (int) character - 64;

        return ascii % 27;
    }

    public static void main(String[] args) throws FileNotFoundException {
        GenerateQueryGcode lnpt = new GenerateQueryGcode();
//        for (File fileEntry : folder.listFiles()) {
//            String queryFileFile = null;
//            try {
//                queryFileFile = fileEntry.getCanonicalPath();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        lnpt.createGraph("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\query\\mus_musculus_1U34.8.sub.grf");
        lnpt.createGraph("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\query\\backbones_1EMA.8.sub.grf");
        lnpt.updateProfilesForQuery();
        lnpt.generateGCode();
//        }
    }
}
