package experiment;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SuppressWarnings("ALL")
public class QueryIndexMatching {
    private static Logger logger = Logger.getLogger(QueryIndexMatching.class.getName());

    public static GraphDatabaseFactory dbFactory;
    public static GraphDatabaseService db;
    static final File queryfolder = new File("C:\\Users\\Kunal Wanjara\\Desktop\\GarphDB\\GraphDB_Assignment5\\Proteins\\Proteins\\Proteins\\query");
    //    static final File targetfolder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/target/");
//        static final File queryfolder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/query/");

    static int queryNodeNumber = 256;


    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Handler fileHandler = null;
        try {
            fileHandler = new FileHandler("GcodeTruth_" + queryNodeNumber + ".log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        compareIndexes();
        long end = System.currentTimeMillis();
//        System.out.println("Total time taken : " + (end - start) / 1000 + " sec. ");
        logger.info("Total time taken for " + queryNodeNumber + " nodes query: " + (end - start) / 1000 + " sec. ");
    }

    public static void compareIndexes() {
        GenerateQueryGcode lnpt = new GenerateQueryGcode();
        for (File fileEntry : queryfolder.listFiles()) {
            String queryFile = null;
            try {
                queryFile = fileEntry.getCanonicalPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (queryFile.contains("." + queryNodeNumber + ".")) {
                lnpt.createGraph(queryFile);
                lnpt.updateProfilesForQuery();
                lnpt.generateGCode();

                connectToGraphDB();
                try (Transaction trax = db.beginTx()) {
                    try (ResourceIterator<Node> allNodes = db.findNodes(Label.label("GCode"))) {
                        while (allNodes.hasNext()) {
                            Node node = allNodes.next();
                            boolean isEligible = compareGCode(lnpt, node);
                            if (isEligible) {
                                logger.info("Not pruned T: " + (String) node.getProperty("TARGET") + "\tQ: " + fileEntry.getName() + "\n");
                            } else {
                                logger.info("Pruned T: " + (String) node.getProperty("TARGET") + "\tQ: " + fileEntry.getName() + "\n");
                            }
                        }
                    }
                    trax.success();
                }
                db.shutdown();
            }
        }
    }

    public static void connectToGraphDB() {
        dbFactory = new GraphDatabaseFactory();
        db = dbFactory.newEmbeddedDatabase(new File("proteins"));
    }

    private static boolean compareGCode(GenerateQueryGcode lnpt, Node node) {

        Map<String, Object> props = node.getAllProperties();
        double[] seq1 = (double[]) props.get("eigenSeq1");
        double[] seq2 = (double[]) props.get("eigenSeq2");


        boolean allFine = true;
        if (!compareLabels(lnpt, props)) {
            logger.warning("Label condition failed!");
            allFine = false;
        }
        if (!compareNeighLabels(lnpt, props)) {
            logger.warning("Neighbour condition failed!");
            allFine = false;
        }
        if (lnpt.eigens1.size() > seq1.length || lnpt.eigens2.size() > seq2.length) {
            logger.warning("Eigen sequence size did not match!");
            allFine = false;
        } else {
            for (int i = 0; i < lnpt.eigens1.size(); i++) {
                if (lnpt.eigens1.get(i) <= seq1[i]) {
                    continue;
                } else {
                    logger.warning("Seq1 comparison failed!");
                    allFine = false;
                }
            }

            for (int i = 0; i < lnpt.eigens2.size(); i++) {
                if (lnpt.eigens2.get(i) <= seq2[i]) {
                    continue;
                } else {
                    logger.warning("Seq2 comparison failed!");
                    allFine = false;
                }
            }
        }

        return allFine;
    }

    private static boolean compareNeighLabels(GenerateQueryGcode lnpt, Map<String, Object> node) {
        for (String key : lnpt.neighOccurences.keySet()) {
            if (!node.containsKey("N:" + key) || lnpt.neighOccurences.get(key) > (int) node.get("N:" + key)) {
                return false;
            }
        }
        return true;
    }

    private static boolean compareLabels(GenerateQueryGcode lnpt, Map<String, Object> node) {
        for (String key : lnpt.occurences.keySet()) {
            if (!node.containsKey("L:" + key) || lnpt.occurences.get(key) > (int) node.get("L:" + key)) {
                return false;
            }
        }
        return true;
    }


}
