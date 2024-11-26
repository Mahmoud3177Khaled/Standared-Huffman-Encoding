package stdhuff;
// ودة كلاس node 
import java.util.ArrayList;

public class Node {
    char character;
    float probability;
    boolean isLeaf;
    Node [] children = new Node[2];
}