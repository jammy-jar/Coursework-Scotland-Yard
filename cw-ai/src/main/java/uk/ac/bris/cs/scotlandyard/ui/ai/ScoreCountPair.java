package uk.ac.bris.cs.scotlandyard.ui.ai;

public class ScoreCountPair {
    private double score;
    private int count;

    public ScoreCountPair() {
        this.score = 0;
        this.count = 0;
    }

    public void add(double score) {
        this.score += score;
        this.count++;
    }

    public double getAvgScore() {
        return this.score / this.count;
    }
}
