package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;

public class Utils {
    public static ImmutableSet<Piece.Detective> initDetectives(Board board) {
        ImmutableSet.Builder<Piece.Detective> detectivesB = ImmutableSet.builder();
        for (Piece piece : board.getPlayers()) {
            if (piece instanceof Piece.Detective detective) {
                detectivesB.add(detective);
            }
        }
        return detectivesB.build();
    }
}
