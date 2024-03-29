package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class MCTS {
    private final static double EXPLORATION_CONSTANT = 0.5;
    private final static double HISTORY_INFLUENCE_CONSTANT = 5;

    private final TreeMap<Integer, Double> tree;
    private Integer parent;

    public MCTS() {
        this.tree = new TreeMap<>();
        // this.parent = ;
    }

    private double calcScoreOfNode(double avgScoreOfNode, int visitsToNode, int visitsToParent, double avgScoreOfMove) {
        // TODO Check VISITS TO NODE and not VISITS TO NODE 1.
        if (visitsToNode == 0) return Double.MAX_VALUE;
        double pt1 = EXPLORATION_CONSTANT * Math.sqrt(Math.log(visitsToParent) / visitsToNode);
        double pt2 = HISTORY_INFLUENCE_CONSTANT * (avgScoreOfMove / (visitsToNode * (1 - avgScoreOfNode) + 1));
        return avgScoreOfNode + pt1 + pt2;
    }

    //
    public Integer selection() {
        Pair<Integer, Integer> topNode = new Pair<>(-1, -1);
        for (Integer node : nodes)
            if (calcScoreOfNode(node) > topNode.value()) topNode = new Pair<>(node, calcScoreOfNode(node));
        return topNode.key();
    }

    public void expansion() {
        Integer node = selection();
        tree.put(node, newNode);
        parent = node;
    }

    public void playout() {

    }

    public void backPropagation() {

    }
}
