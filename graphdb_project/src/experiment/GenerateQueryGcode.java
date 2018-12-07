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

import java.io.*;
import java.util.*;


public class GenerateQueryGcode {

    static final File folder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/query/");
    Graph<Integer, DefaultEdge> queryGraph = null;
    HashMap<Integer, String> attributeLabels = null;

    public void createGraph(String filePath){

        queryGraph = new SimpleGraph<>(DefaultEdge.class);
        attributeLabels = new HashMap<Integer, String>();
        String l = null;
        try {

            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while((l = br.readLine()) != null) {
                String[] str = l.split(" ");
                if(str.length > 1){
                    if(StringUtils.isNumeric(str[1])){
                        queryGraph.addEdge(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
                    }else{
                        queryGraph.addVertex(Integer.parseInt(str[0]));
                        attributeLabels.put(Integer.parseInt(str[0]),str[1]);
                    }

                }

            }

            br.close();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    private void search(Integer n, int level, ArrayList<Integer> visited, TreeNode node, HashMap<Integer,Integer> count){
        if(level > 0) {

            ArrayList<TreeNode> adjNodes = new ArrayList<>();
            for(Integer neigh :  Graphs.neighborListOf(queryGraph, n)){
                if (!visited.contains(neigh)) {
                    TreeNode r = new TreeNode(attributeLabels.get(neigh), neigh);
                    adjNodes.add(r);
                    visited.add(r.getId());
                    count.put(r.getId(),count.getOrDefault(r.getId(),count.size()));
                    search(neigh,level-1, visited,r,count);
                    visited.remove(neigh);
                }
            }
            node.setAdjList(adjNodes);
        }
        return;
    }

    private TreeNode createLNPT(Integer n, int level, HashMap<Integer,Integer> count ){
        ArrayList<Integer> visited = new ArrayList<>();
        TreeNode root = new TreeNode(attributeLabels.get(n),n);
        visited.add(n);
        count.put(n,count.getOrDefault(n,count.size()));

        ArrayList<TreeNode> adjNodes = new ArrayList<>();
        for(Integer neigh :  Graphs.neighborListOf(queryGraph, n)){
            TreeNode r = new TreeNode(attributeLabels.get(neigh),neigh);
            adjNodes.add(r);
            visited.add(r.getId());
            count.put(r.getId(),count.getOrDefault(r.getId(),count.size()));
            search(neigh,level-1, visited,r,count);
        }
        root.setAdjList(adjNodes);
        return root;
    }

    public double[][] createAdjacency(TreeNode node, double[][] adjList){
        if(node.getAdjList().size() > 0) {
            for (TreeNode n : node.getAdjList()) {
                adjList[node.getIndex()][n.getIndex()] = 1;
                adjList[n.getIndex()][node.getIndex()] = 1;
                adjList = createAdjacency(n, adjList);
            }
        }
        return adjList;
    }

    private double[][] createAdjMat(Integer node, int level){

        HashMap<Integer,Integer> numNodes = new HashMap<>();
        TreeNode root = createLNPT(node,level,numNodes);

        double[][] adjList = new double[numNodes.size()][numNodes.size()];
        for(TreeNode tn : root.getAdjList()){
            adjList[numNodes.get(root.getId())][numNodes.get(tn.getId())] = 1;
            adjList[numNodes.get(tn.getId())][numNodes.get(root.getId())] = 1;
            adjList = createAdjacency(tn,adjList);
        }
        return adjList;
    }

    private static Double getEigenValues(double[][] adjMatrix){

        ComplexDoubleMatrix eigenVectors = Eigen.eigenvalues(new DoubleMatrix(adjMatrix));

        ArrayList<Double> eigen = new ArrayList<>();

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < eigenVectors.rows; ++i) {
            for (int j = 0; j < eigenVectors.columns; ++j) {
                eigen.add(eigenVectors.get(i, j).real());
            }
        }
        Collections.sort(eigen,Collections.reverseOrder());
        return eigen.get(0);

    }

    private void generateGCode(){

        for(Integer u : queryGraph.vertexSet()) {
            double[][] adjMatrix = createAdjMat(u, 2);
            Double eigen = getEigenValues(adjMatrix);
            System.out.println(eigen);

        }

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
            lnpt.createGraph("/Users/jinalshah/Downloads/Proteins/Proteins/query/human_2KM2.8.sub.grf");
            lnpt.generateGCode();
//        }
    }
}
