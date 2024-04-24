package test.ai;

import uk.ac.bris.cs.scotlandyard.model.GameSetup;

import javax.annotation.Nonnull;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.STANDARD24MOVES;
import static uk.ac.bris.cs.scotlandyard.ui.ai.ScotLandYardAi.*;

public class TestWorld {

    @Nonnull
    static GameSetup standard24MoveSetup() {
        return new GameSetup(defaultGraph, STANDARD24MOVES);
    }

    public static void main(String[] args) {
        setUp();
        initLookup();

        MCTSTest.testMCTS(standard24MoveSetup());
        DijkstraTest.testDijkstra();
        AiStateTest.testPossibleMrXLocations(standard24MoveSetup());
        AiStateTest.testHeuristic(standard24MoveSetup());
    }
}
