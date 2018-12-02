package dataload;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class LoadData {

    public static void main(String[] args) {

        Long startTime = System.currentTimeMillis();
        BatchInserter inserter = null;
        final File folder = new File("/Users/jinalshah/Downloads/Proteins/Proteins/target/");
        try {
            inserter = BatchInserters.inserter(new File("Proteins"));
            int offset = 100;
            int graphid = 0;

            for (File fileEntry : folder.listFiles()) {
                BufferedReader br = new BufferedReader(new FileReader(fileEntry.getCanonicalPath()));
//            BufferedReader br = new BufferedReader(new FileReader("/Users/jinalshah/Downloads/Proteins/Proteins/target/backbones_1RH4.grf"));
                String s;
                br.readLine();
                int num = 0;
                String fileName = fileEntry.getName();
                ArrayList<String> profiles = new ArrayList<>();
                String prev = "";
//                System.out.println("Graph " + fileName + " started");
                while ((s = br.readLine()) != null) {
                    if (!s.startsWith("#") && s.length() > 0) {
                        String[] str = s.split(" ");
                        if (str.length == 1) {
                            num = Integer.parseInt(s);
                            if(prev.length() > 0) {
                                Map<String, Object> node1Properties = inserter.getNodeProperties(Long.parseLong((graphid * offset) + prev));
                                profiles.add((String) node1Properties.get("name"));
                                Collections.sort(profiles);
                                inserter.setNodeProperty(Long.parseLong((graphid * offset) + prev), "profile", profiles.toString());
                                profiles = new ArrayList<>();
                            }
                        } else {
                            if (StringUtils.isNumeric(str[1])) {

                                if (num > 0) {
                                    inserter.setNodeProperty(Long.parseLong((graphid * offset) + str[0]), "edge_count", num);
                                    num = 0;
                                }
                                inserter.createRelationship(Long.parseLong((graphid * offset) + str[0]), Long.parseLong((graphid * offset) + str[1]),
                                        RelationshipType.withName("HasEdge"), new HashMap<>());
                                Map<String,Object> node2Properties = inserter.getNodeProperties(Long.parseLong((graphid * offset) + str[1]));
                                profiles.add((String) node2Properties.get("name"));
                                prev = str[0];
                            } else {
                                HashMap<String, Object> map = new HashMap<>();
                                map.put("id", Integer.parseInt(str[0]));
                                map.put("name",str[1]);
                                inserter.createNode(Long.parseLong((graphid * offset) + str[0]), map,
                                        Label.label(str[1]), Label.label(fileName.substring(0, fileName.indexOf('.'))));
                            }
                        }
                    }
                }

                if(profiles.size() > 0) {
                    Map<String, Object> node1Properties = inserter.getNodeProperties(Long.parseLong((graphid * offset) + prev));
//                    if(!profiles.contains(node1Properties.get("name")))
                    profiles.add((String) node1Properties.get("name"));
                    Collections.sort(profiles);
                    inserter.setNodeProperty(Long.parseLong((graphid * offset) + prev), "profile", profiles.toString());
                }
//                System.out.println("Graph " + fileName + " inserted");
                graphid++;
            }



            Long endTime = System.currentTimeMillis();
            System.out.println("Time taken for creation of " + 3000 + " graphs database: " + (endTime-startTime)/1000 + " seconds");

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            inserter.shutdown();
        }

    }

}

//Time taken for creation of 3000 graphs database: 8 seconds
