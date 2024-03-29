package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;

public interface ScoreBoard {
    int getScore(Move move);
}
