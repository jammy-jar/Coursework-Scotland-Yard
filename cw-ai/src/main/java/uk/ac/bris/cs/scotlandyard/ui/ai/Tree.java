package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
import java.util.List;

public class Tree<T> {
    private Node root;

    public Tree(Move move, Board.GameState state, T rootValue) {
        this.root = new Node(null, move, state, rootValue);
    }

    public Node getRoot() {
        return root;
    }

    public class Node {

        private int visits;
        private Move move;
        private Board.GameState state;
        private T value;
        private Node parent;
        private List<Node> children;

        public Node(Node parent, Move move, Board.GameState state, T value) {
            this.move = move;
            this.state = state;
            this.visits = 0;
            this.value = value;
            this.parent = parent;
            this.children = new ArrayList<>();
        }

        public int getVisits() {
            return this.visits;
        }

        public Move getMove() {
            return this.move;
        }

        public Board.GameState getGameState() {
            return this.state;
        }

        public T getValue() {
            return this.value;
        }

        public void incrementVisits() {
            this.visits += 1;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public Node getParent() {
            return this.parent;
        }

        public List<Node> getChildren() {
            return this.children;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public void addChild(Move move, Board.GameState state, T value) {
            this.children.add(new Node(this, move, state, value));
        }
    }

}
