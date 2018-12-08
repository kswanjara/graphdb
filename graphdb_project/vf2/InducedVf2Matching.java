package vf2;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import common.Node;

@SuppressWarnings({"unused", "StringBufferReplaceableByString", "SuspiciousMethodCalls", "StringConcatenationInsideStringBufferAppend"})
public class InducedVf2Matching {
    private static GraphDatabaseService db;

    //    private final static String targetPath = "C:\Users\Kunal Wanjara\Desktop\GarphDB\GraphDB_Assignment5\Proteins\Proteins\Proteins\target\\";
    private final static String queryPath = "C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\query\\";
    private final static String groundTruthPath = "C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\ground_truth\\";
//    private final static String groundTruthPath = "C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\trial\\";


    private static Logger logger = Logger.getLogger(InducedVf2Matching.class.getName());

    private static List<Integer> dataNodes;
    private static List<Node> queryNodes;
    private static List<Integer> unusedDataNodes;
    private static List<Node> unusedQueryNodes;
    private static Map<String, Node> nodeVals;

    private static long start = 0;

    private static void connectToProteins() {
        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
        db = dbFactory.newEmbeddedDatabase(new File("Proteins"));
    }

    public static void main(String[] args) throws IOException {
        Handler fileHandler = new FileHandler("./InducedVf2Matching.log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        processQueries();
    }

    private static void processQueries() {
        try {
            File groundTruth = new File(groundTruthPath + "Proteins.8.gtr");
//            File groundTruth = new File(groundTruthPath + "file1.txt");

            String targetFile = null;
            String queryFile = null;
            int totalSolutions = -1;
            Set<String> solutions = new HashSet<>();

            Scanner sc = new Scanner(groundTruth);

            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.startsWith("T")) {
                    targetFile = line.substring(line.indexOf(":") + 1);
                } else if (line.startsWith("P")) {
                    queryFile = line.substring(line.indexOf(":") + 1);
                } else if (line.startsWith("N")) {
                    totalSolutions = Integer.parseInt(line.substring(line.indexOf(":") + 1));
                } else if (line.startsWith("S")) {
                    solutions.add(line);
                } else if (line.trim().length() == 0) {
                    if (targetFile != null && queryFile != null && totalSolutions != -1) {
                        start = System.currentTimeMillis();
                        connectToProteins();
                        processQuery(targetFile, queryFile, totalSolutions, solutions);
                        targetFile = null;
                        queryFile = null;
                        totalSolutions = -1;
                        solutions = new HashSet<>();
                        dataNodes = null;
                        unusedDataNodes = null;
                        unusedQueryNodes = null;
                        queryNodes = null;
                        db.shutdown();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processQuery(String targetFile, String queryFile, int totalSolutions, Set<String> solutions) throws FileNotFoundException {
        Map<Node, ArrayList<Node>> sg = generateQueryGraph(queryFile);
        updateProfilesForQuery(sg);

        Set<ArrayList<StringBuffer>> matchSolutions = new HashSet<>();
        for (Node node : sg.keySet()) {
            ArrayList<Integer> candidates = generateCandidateSet(targetFile, node);
            if (candidates.size() == 0) {
                System.out.println("No Subgraph!");
            }
            node.setCandidates(candidates);
        }


        ArrayList<Node> order = generateOrderBasedOnCost(sg);

        checkSGI(targetFile, queryFile, totalSolutions, sg, solutions, order);
    }

    private static void checkSGI(String targetFile, String queryFile, int totalSolutions,
                                 Map<Node, ArrayList<Node>> sg, Set<String> solutions, ArrayList<Node> order) {
        Set<ArrayList<StringBuffer>> matchSolutions = new HashSet<>();

        ArrayList<StringBuffer> mapping = new ArrayList<>();
        mapping = findMapping(order, new HashMap<Integer, Integer>(), targetFile, mapping, sg);
        compareAndPrintResults(mapping, targetFile, queryFile, totalSolutions, solutions);

    }

    private static ArrayList<StringBuffer> findMapping(ArrayList<Node> order, HashMap<Integer, Integer> embed, String target, ArrayList<StringBuffer> result, Map<Node, ArrayList<Node>> sg) {

//        System.out.println(embed);

        if (embed.size() == order.size()) {
            StringBuffer s = new StringBuffer("S:").append(embed.size()).append(":");
            for (Integer i : embed.keySet()) {
                s.append(i + "," + embed.get(i) + ";");
            }
            s = s.delete(s.length() - 1, s.length());
            result.add(s);
//            System.out.println(s.toString());

        } else {
            int index = embed.size();
            Node u = order.get(index);
            for (Integer v : getNewCandidateSet(u.getCandidates())) {
                if (embed.size() == 0) {
                    embed.put(u.getId(), v);
                    updateState(embed, target, sg, u, v);
                    findMapping(order, embed, target, result, sg);
                    embed.remove(u.getId());
                } else {
                    if (!embed.containsValue(v)) {
                        if (checkEdge(embed, u, v, order, target)) {
                            if (checkCardinality(u, v, embed, target)) {
                                if (checkNewMapping(u, v, embed, target)) {
                                    embed.put(u.getId(), v);
                                    updateState(embed, target, sg, u, v);
                                    findMapping(order, embed, target, result, sg);
                                    embed.remove(u.getId());
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void updateState(HashMap<Integer, Integer> embed, String target, Map<Node, ArrayList<Node>> sg, Node u, Integer v) {
        // calculate
        // T1 - neighbors of last embedding added (v)
        dataNodes = getDataNodes(embed, v, target);

        // T2 - neighbors of last embedding added (u)
        queryNodes = getQueryNodes(embed, u);

        // N1 - {total nodes in proteins} - T1 - embed(v)
        unusedDataNodes = getUnusedDataNodes(embed, v, dataNodes, target);

        // N2 - {total nodes in query} - T2 - embed(u)
        unusedQueryNodes = getUnusedQueryNodes(embed, v, queryNodes, sg, u);
    }

    /**
     * @param embed
     * @param u
     * @param target
     * @return T1 - neighbors of last embedding added (v)
     */
    private static List<Integer> getDataNodes(HashMap<Integer, Integer> embed, Integer u, String target) {
        List<Integer> result = new ArrayList<>();
        for (Integer v : embed.values()) {
            Set<Integer> neighbors = getNeighbors(target, v);
            for (Integer i : neighbors) {
                if (!embed.containsValue(i) & !result.contains(i))
                    result.add(i);
            }
        }
        return result;
    }

    /**
     * @param embed
     * @param u2
     * @return T2 - neighbors of last embedding added (u)
     */
    private static List<Node> getQueryNodes(HashMap<Integer, Integer> embed, Node u2) {
        List<Node> result = new ArrayList<>();

        for (Integer u : embed.keySet()) {
            ArrayList<Node> neighbors = nodeVals.get("u" + u).getNeighbors();
            for (Node n : neighbors) {
                if (!embed.containsKey(n.getId()) && !result.contains(n))
                    result.add(n);
            }
        }
        return result;
    }

    /**
     * @param embed
     * @param v
     * @param dataNodes
     * @param targetFile
     * @return N1 - {total nodes in proteins} - T1 - embed(v)
     */
    private static List<Integer> getUnusedDataNodes(HashMap<Integer, Integer> embed, Integer v, List<Integer> dataNodes, String targetFile) {
        List<Integer> result = new ArrayList<>();

        HashSet<Integer> adjOfV = getNeighbors(targetFile, v);
        adjOfV.removeAll(dataNodes);
        adjOfV.removeAll(embed.values());

        return new ArrayList<>(adjOfV);

    }

    /**
     * @param embed
     * @param v
     * @param queryNodes
     * @param sg
     * @param u
     * @return N2 - {total nodes in query} - T2 - embed(u)
     */
    private static List<Node> getUnusedQueryNodes(HashMap<Integer, Integer> embed, Integer v, List<Node> queryNodes, Map<Node, ArrayList<Node>> sg, Node u) {
        List<Node> result = new ArrayList<>();

        ArrayList<Node> neigh = new ArrayList<>(u.getNeighbors());
        neigh.removeAll(queryNodes);
        for (Integer i : embed.keySet()) {
            neigh.remove(nodeVals.get("u" + i));
        }

        return neigh;
    }

    private static boolean checkCardinality(Node u, Integer v, HashMap<Integer, Integer> embed, String targetFile) {
        // | T1 intersects Adj(v) | >= | T2 intersects Adj(u)|
        List<Integer> neighbors = new ArrayList<>(getNeighbors(targetFile, v));
        neighbors.retainAll(dataNodes);
        ArrayList<Node> nodes = new ArrayList<>(queryNodes);
        nodes.retainAll(u.getNeighbors());

        return neighbors.size() >= nodes.size();
    }

    private static boolean checkEdge(HashMap<Integer, Integer> embed, Node u, Integer v, ArrayList<Node> order, String target) {
        ArrayList<Integer> dataGraphNeighbors = new ArrayList<>(getNeighbors(target, v));
        ArrayList<Integer> queryCommon = new ArrayList<>();
        ArrayList<Integer> dataCommon = new ArrayList<>();
        ArrayList<Integer> adjacentQueryNodeIds = new ArrayList<>();

        for (Node neighbors : u.getNeighbors()) {
            adjacentQueryNodeIds.add(neighbors.getId());
        }
        for (Integer i : embed.keySet()) {
            if (dataGraphNeighbors.contains(embed.get(i))) {
                dataCommon.add(embed.get(i));
            }
            if (adjacentQueryNodeIds.contains(i)) {
                queryCommon.add(i);
            }
        }
        for (Integer i : queryCommon) {
            if (!dataGraphNeighbors.contains(embed.get(i))) {
                return false;
            }
        }

        ArrayList<Integer> keys = new ArrayList<>(embed.keySet());
        ArrayList<Integer> values = new ArrayList<>(embed.values());
        for (Integer i : dataCommon) {

            int index = values.indexOf(i);
            if (!adjacentQueryNodeIds.contains(keys.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static ArrayList<Integer> getNewCandidateSet(ArrayList<Integer> candidates) {
        if (dataNodes == null)
            return candidates;

        ArrayList<Integer> newSet = new ArrayList<>(candidates);
        newSet.retainAll(dataNodes);
        return newSet;
    }

    private static boolean checkNewMapping(Node u, Integer v, HashMap<Integer, Integer> embed, String targetFile) {
        // | Adj(v) intersects N1 | >= | Adj(u) intersects N2|
        return unusedDataNodes.size() >= unusedQueryNodes.size();
    }

    private static HashSet<Integer> getNeighbors(String targetFile, Integer v) {
        HashSet<Integer> neighbors = new HashSet<>();
        try (Transaction t = db.beginTx()) {
            org.neo4j.graphdb.Node node = db.findNode(Label.label(targetFile.substring(0, targetFile.indexOf("."))), "id", v);
            for (Relationship r : node.getRelationships()) {
                neighbors.add((Integer) r.getEndNode().getProperty("id"));
            }
            t.success();
        }
        return neighbors;
    }

    private static void compareAndPrintResults(ArrayList<StringBuffer> mapping, String targetFile, String queryFile, int totalSolutions, Set<String> solutions) {
        logger.info("T:" + targetFile + "\tP:" + queryFile + "\tN:" + totalSolutions);
        // calculate TP, FP, FN
        int tp = 0;
        int fp = 0;
        int fn;
        for (StringBuffer generated : mapping) {
            if (solutions.remove(generated.toString())) {
                tp++;
            } else {
                fp++;
            }
        }
        fn = solutions.size();

        long end = System.currentTimeMillis();
        logger.info("TP: " + tp + "\tFP: " + fp + "\tFN: " + fn + "\ttime: " + (end - start) + " ms\n");
    }

    private static void updateProfilesForQuery(Map<Node, ArrayList<Node>> sg) {
        for (Map.Entry entry : sg.entrySet()) {
            Node key = (Node) entry.getKey();
            ArrayList<Node> neighs = key.getNeighbors();
            List<String> profile = new ArrayList<>();
            for (Node n : neighs) {
                profile.add(n.getAttr());
            }
            Collections.sort(profile);
            key.setProfile(profile.toArray(new String[0]));
        }
    }

    private static ArrayList<Integer> generateCandidateSet(String targetFile, Node node) {
        ArrayList<Integer> candidates = new ArrayList<>();

        ArrayList<Integer> allDataNodes = new ArrayList<>();
        try (Transaction tx = db.beginTx();) {
            ResourceIterator<org.neo4j.graphdb.Node> allNodes = db.findNodes(Label.label(targetFile.substring(0, targetFile.indexOf("."))), "attr", node.getAttr());
            while (allNodes.hasNext()) {
                org.neo4j.graphdb.Node n = allNodes.next();
                if (node.getNeighbors().size() <= n.getDegree() && checkProfileValidity((String[]) n.getProperty("neigh"), node.getProfile())) {
                    allDataNodes.add((Integer) n.getProperty("id"));
                }
            }
            tx.success();
        }
        return allDataNodes;

    }

    private static boolean checkProfileValidity(String[] neigh, String[] profile) {
        int counter = 0;
        int neighs = neigh.length;
        int i = 0;
        int profilesLength = profile.length;

        while (i < profilesLength && counter < neighs) {
            if (profile[i].charAt(0) == neigh[counter].charAt(0)) {
                i++;
                counter++;
            } else {
                counter++;
            }
        }
        //            System.out.println("Comparison true for : " + Arrays.toString(neigh) + " and " + Arrays.toString(profile));
        return i == profilesLength;

    }

    private static Map<Node, ArrayList<Node>> generateQueryGraph(String queryFile) throws FileNotFoundException {
        Map<Node, ArrayList<Node>> map = new LinkedHashMap<>();
        nodeVals = new HashMap<>();
        File file = new File(queryPath + queryFile);
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
        return map;
    }

    private static ArrayList<Node> generateOrderBasedOnCost(Map<Node, ArrayList<Node>> sg) {

        Node startNode = null;
        int minCandidates = Integer.MAX_VALUE;
        for (Node n : sg.keySet()) {
            if (n.getCandidates().size() < minCandidates) {
                minCandidates = n.getCandidates().size();
                startNode = n;
            }
        }

        ArrayList<Node> order = new ArrayList<>();

        order.add(startNode);
        double minCostValue = Double.MAX_VALUE;
        Node nextNode = null;
        // initially calculate for single edge ( without considering r )
        assert startNode != null;
        for (Node neighbors : startNode.getNeighbors()) {
            if (neighbors.getCandidates().size() * startNode.getCandidates().size() < minCostValue) {
                minCostValue = neighbors.getCandidates().size() * startNode.getCandidates().size();
                nextNode = neighbors;
            }
        }

        order.add(nextNode);
        double r = 0.5;

        while (order.size() != sg.size()) {
            double temp = Double.MAX_VALUE;
            Node tempNode = null;
            for (Node visited : order) {
                for (Node neigh : visited.getNeighbors()) {
                    ArrayList<Node> neighbors = new ArrayList<>(neigh.getNeighbors());
                    if (!order.contains(neigh)) {
                        neighbors.retainAll(order);
                        int power = neighbors.size();
                        double tempCost = visited.getCandidates().size() * neigh.getCandidates().size() * Math.pow(r, power);
                        if (tempCost < temp) {
                            temp = tempCost;
                            tempNode = neigh;
                        }
                    }
                }
            }
            if (tempNode != null) {
                order.add(tempNode);
            }
        }
        return order;
    }
}
