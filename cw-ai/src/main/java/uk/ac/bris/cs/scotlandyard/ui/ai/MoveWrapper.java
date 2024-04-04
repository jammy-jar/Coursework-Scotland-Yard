package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

public class MoveWrapper {
    private final Move move;
    private double score;
    private int playouts;

    public MoveWrapper(Move move) {
        this.move = move;
        this.score = 0;
        this.playouts = 0;
    }

    public void add(double score) {
        this.score += score;
        this.playouts++;
    }

    public double getAvgScore() {
        return this.score / this.playouts;
    }
}
