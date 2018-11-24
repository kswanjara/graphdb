package common;

import java.util.ArrayList;
import java.util.Objects;

public class Node {
    private String nodeName;
    private int id;
    private String attr;
    private ArrayList<Node> neighbors = new ArrayList<>();
    private ArrayList<Integer> candidates = new ArrayList<>();
    private String[] profile;

    public Node(String nodeName) {
        this.nodeName = nodeName;
    }

    public Node(String nodeName, int id, String attr) {
        this.nodeName = nodeName;
        this.id = id;
        this.attr = attr;
    }

    public ArrayList<Node> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<Node> neighbors) {
        this.neighbors = neighbors;
    }

    public ArrayList<Integer> getCandidates() {
        return candidates;
    }

    public void setCandidates(ArrayList<Integer> candidates) {
        this.candidates = candidates;
    }

    public String getNodeName() {
        return nodeName;
    }

    public int getId() {
        return id;
    }

    public String getAttr() {
        return attr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(nodeName, node.nodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeName);
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeName='" + nodeName + '\'' +
                ", id=" + id +
                ", attr='" + attr + '\'' +
                '}';
    }

    public String[] getProfile() {
        return profile;
    }

    public void setProfile(String[] profile) {
        this.profile = profile;
    }
}