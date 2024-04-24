package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.HashSet;
import java.util.Set;

public class Utils {
    public static Set<Integer> initDetectiveLocations(Board board) {
        Set<Integer> detectiveLocations = new HashSet<>();
        for (Piece piece : board.getPlayers()) {
            if (piece instanceof Piece.Detective detective) {
                if (board.getDetectiveLocation(detective).isPresent())
                    detectiveLocations.add(board.getDetectiveLocation(detective).get());
            }
        }
        return detectiveLocations;
    }

    public static int movesFinalDestination(Move move) {
        if (move instanceof Move.SingleMove s)
            return s.destination;
        else if (move instanceof Move.DoubleMove d)
            return d.destination2;
        else throw new RuntimeException("This move type is not valid!");
    }
}
