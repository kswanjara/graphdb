package common;

import java.util.ArrayList;

public class TreeNode {

    String name;
    Integer id;
    int index;
    ArrayList<TreeNode> adjList = new ArrayList<>();

    public TreeNode(String node, Integer id){
        this.name = node;
        this.id=id;
    }

    public Integer getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setAdjList(ArrayList<TreeNode> adjList) {
        this.adjList.addAll(adjList);
    }

    public void setAdjList(TreeNode node) {
        this.adjList.add(node);
    }

    public ArrayList<TreeNode> getAdjList() {
        return adjList;
    }

    public String getName() {
        return name;
    }
}
