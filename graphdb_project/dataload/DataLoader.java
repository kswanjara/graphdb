package dataload;

import experiment.GenerateGCode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("ALL")
public class DataLoader {

    private static AtomicInteger counter = new AtomicInteger(100000);
    private static int incremetor = 100000;

    //    private final static String targetPath = "C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\Assignment6\\DummyInputUsed\\TARGET.TXT";
    private final static String targetPath = "C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\target";

    public static void main(String[] args) throws IOException {
        Long startTime = System.currentTimeMillis();
        // load data
        System.out.println("started loading @" + new Date());
        BatchInserter inserter = null;

        try {
            inserter = BatchInserters.inserter(new File("proteins"));
            File folder = new File(targetPath);

            for (File file : folder.listFiles()) {
//            BufferedReader br = new BufferedReader(new FileReader(new File("Trial.txt")));
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                int totalNodes = Integer.parseInt(br.readLine());
                int numberOfEdges = 0;
                boolean newNode = false;
                String newNodeId = "";

                int prefixVal;
                prefixVal = counter.getAndAdd(incremetor);

                for (int i = 1; i <= totalNodes; i++) {
                    // createNodes
                    String[] str = br.readLine().split(" ");
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put("id", Integer.parseInt(str[0]));
                    map.put("attr", str[1]);
                    inserter.createNode(prefixVal + Long.parseLong(str[0]), map,
                            Label.label(str[1]), Label.label(file.getName().substring(0, file.getName().indexOf('.'))));
//                inserter.createNode(prefixVal + Long.parseLong(str[0]), map,
//                        Label.label(str[1]), Label.label("trial.txt".substring(0, "trial.txt".indexOf('.'))));
                }
                List<String> neigh = new ArrayList<String>();
                while ((line = br.readLine()) != null) {
                    if (line.split(" ").length == 1) {
                        // number of edges
                        numberOfEdges = Integer.parseInt(line);
                        newNode = true;
                        if (!newNodeId.equals("") && neigh.size() > 0) {
                            Collections.sort(neigh);
                            String[] neighs = neigh.toArray(new String[0]);
                            inserter.setNodeProperty(prefixVal + Long.parseLong(newNodeId), "neigh", neighs);
                            neigh = new ArrayList<String>();
                        }
                    } else {
                        // edge
                        String[] nodes = line.split(" ");
                        if (newNode) {
                            newNode = false;
                            newNodeId = nodes[0];
                        }
                        inserter.createRelationship(prefixVal + Long.parseLong(nodes[0]), prefixVal + Long.parseLong(nodes[1]),
                                RelationshipType.withName("HasEdge"), new HashMap<>());

                        neigh.add((String) inserter.getNodeProperties(prefixVal + Long.parseLong(nodes[1])).get("attr"));
                    }
                }
                if (neigh.size() > 0) {
                    Collections.sort(neigh);
                    String[] neighs = neigh.toArray(new String[0]);
                    inserter.setNodeProperty(prefixVal + Long.parseLong(newNodeId), "neigh", neighs);
                }
//                System.out.println("File " + file.getName() + " loaded!");
//            System.out.println("File trial.txt loaded!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        inserter.shutdown();
        Long endTime = System.currentTimeMillis();
        System.out.println("ended loading @" + new Date());
        System.out.println("Total time taken to load data : " + ((endTime - startTime) / 1000));

        System.out.println();

        startTime = System.currentTimeMillis();
        System.out.println("Indexing the database started @" + new Date());
        startIndexing();
        endTime = System.currentTimeMillis();
        System.out.println("Indexing the database started @" + new Date());
        System.out.println("Time taken for indexing " + (endTime - startTime) / 1000);
    }

    private static void startIndexing() {
        File folder = new File(targetPath);
        GenerateGCode g = new GenerateGCode();
        g.connectToGraphDB();
        try (Transaction trax = g.db.beginTx()) {
            for (File fileEntry1 : folder.listFiles()) {
                String targetFile = fileEntry1.getName().substring(0, fileEntry1.getName().indexOf("."));
                g.generateGCode(targetFile);
            }
            trax.success();
        }
        g.db.shutdown();
    }


}
