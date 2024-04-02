package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
import java.util.List;

public class Tree<T> {
    private final Node root;

    public Tree(Move move, AiState state, T rootValue) {
        this.root = new Node(null, move, state, rootValue);
    }

    public Node getRoot() {
        return root;
    }

    public class Node {
        private int visits;
        private final Move move;
        private final AiState state;
        private T value;
        private final Node parent;
        private final List<Node> children;

        public Node(Node parent, Move move, AiState state, T value) {
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

        public AiState getState() {
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
        public boolean isRoot() {
            return parent == null;
        }

        public void addChild(Move move, AiState state, T value) {
            this.children.add(new Node(this, move, state, value));
        }
    }

}
