package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface AiState {
    @Nonnull
    Board.GameState getGameState();
    @Nonnull
    AiState advance(Heuristic mrXHeuristic, Heuristic detectivesHeuristic);
}
