package experiment;

import common.Node;
import common.TreeNode;
import org.apache.commons.lang.StringUtils;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;


public class LNPTAlgorithm {

    //    public static final String filePath = "C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\target\\backbones_1AF7.grf";
//    public static final String filePath = "C:\\Users\\Kunal Wanjara\\Desktop\\trial.txt";
    public static final String filePath = "trial.txt";
    private static Map<String, Node> nodeVals;
    Graph<Integer, DefaultEdge> queryGraph = null;
    HashMap<Integer, String> attributeLabels = null;

    public static void main(String[] args) throws FileNotFoundException {
        LNPTAlgorithm lnpt = new LNPTAlgorithm();
        lnpt.createGraph();
        if (lnpt.queryGraph.vertexSet().size() > 0) {
            Set vertex = lnpt.queryGraph.vertexSet();
            Iterator<Integer> it = vertex.iterator();
            while(it.hasNext()) {

//                Integer v = it.next();
//                int[][] adjMatrix = lnpt.getLnptTree(v,2);
//                for (int i = 0; i < adjMatrix.length; i++) {
//                    for (int j = 0; j < adjMatrix.length; j++) {
//                        System.out.print(adjMatrix[i][j]);
//                    }
//                    System.out.println();
//                }
            }
        }

    }

//    public int[][] getLnptTree(Integer v, int level) {
//        TreeNode root = new TreeNode(v);
//        HashSet<Integer> visited = new HashSet<>();
//        visited.add(v);
//        ArrayList<TreeNode> adjNodes = new ArrayList<>();
//        for (Integer i : Graphs.neighborListOf(queryGraph, v)){
//            TreeNode r = new TreeNode(i);
//            adjNodes.add(r);
//            visited.add(i);
//            search(i,level-1, visited,r);
//        }
//        root.setAdjList(adjNodes);
//        int[][] adjList = new int[visited.size()][visited.size()];
//        for(TreeNode n : root.getAdjList()){
//            adjList[root.getName()][n.getName()] = 1;
//            adjList[n.getName()][root.getName()] = 1;
//            adjList = createAdjacency(n,adjList);
//        }
//        return adjList;
//    }
//
//    public int[][] createAdjacency(TreeNode node, int[][] adjList){
//       if(node.getAdjList().size() > 0) {
//           for (TreeNode n : node.getAdjList()) {
//               adjList[node.getName()][n.getName()] = 1;
//               adjList[n.getName()][node.getName()] = 1;
//               adjList = createAdjacency(n, adjList);
//           }
//       }
//       return adjList;
//    }

//    public void search(Integer v, int level, HashSet<Integer> visited, TreeNode node){
//        if(level == 0){
//            return;
//        }
//        ArrayList<TreeNode> adjNodes = new ArrayList<>();
//        for (Integer i : Graphs.neighborListOf(queryGraph, v)){
//            if(!visited.contains(i)) {
//                TreeNode r = new TreeNode(i);
//                adjNodes.add(r);
//                visited.add(i);
//                search(i, level - 1, visited, r);
//            }
//        }
//        node.setAdjList(adjNodes);
//    }

    public void createGraph(){

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
}
