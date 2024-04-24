package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AiState {
    @Nonnull
    Board.GameState getGameState();
    @Nonnull
    Set<Integer> getPossibleMrXLocations();
    @Nonnull
    Set<Integer> getDetectiveLocations();
    @Nonnull
    Optional<Piece> getTurn();
    @Nonnull
    List<Move> getMoves();


    @Nonnull
    Move applyHeuristic(Heuristic heuristic);
    @Nonnull
    AiState advance(Move move);
}
