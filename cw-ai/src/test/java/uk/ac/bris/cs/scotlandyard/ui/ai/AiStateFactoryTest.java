package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;
import static uk.ac.bris.cs.scotlandyard.ui.ai.Utils.movesFinalDestination;

public class AiStateFactoryTest extends TestBase {
    @Test
    public void testPossibleMrXLocations() {
        var black = new Player(MRX, defaultMrXTickets(), 45);
        var red = new Player(RED, defaultDetectiveTickets(), 111);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        Board.GameState state = new MyGameStateFactory().build(standard24MoveSetup(), black, red, blue);
        MCTSInterface mcts = new MCTSInterface(new MrXAiStateFactory().build(state), AiType.MRX);

        AiState aiState = mcts.getSelectNode().getState();

        aiState = aiState.advance(new Move.SingleMove(MRX, 45, ScotlandYard.Ticket.TAXI, 59));
        aiState = aiState.advance(new Move.SingleMove(RED, 111, ScotlandYard.Ticket.TAXI, 110));
        aiState = aiState.advance(new Move.SingleMove(BLUE, 94, ScotlandYard.Ticket.TAXI, 95));
        aiState = aiState.advance(new Move.SingleMove(MRX, 59, ScotlandYard.Ticket.TAXI, 45));
        aiState = aiState.advance(new Move.SingleMove(RED, 110, ScotlandYard.Ticket.TAXI, 111));
        aiState = aiState.advance(new Move.SingleMove(BLUE, 95, ScotlandYard.Ticket.TAXI, 94));
        aiState = aiState.advance(new Move.SingleMove(MRX, 45, ScotlandYard.Ticket.TAXI, 32));

        assertThat(aiState.getPossibleMrXLocations().size()).isEqualTo(1);
        assertThat(aiState.getPossibleMrXLocations().stream().findFirst().get()).isEqualTo(32);

        aiState = aiState.advance(new Move.SingleMove(RED, 111, ScotlandYard.Ticket.TAXI, 112));
        aiState = aiState.advance(new Move.SingleMove(BLUE, 94, ScotlandYard.Ticket.TAXI, 93));
        aiState = aiState.advance(new Move.SingleMove(MRX, 32, ScotlandYard.Ticket.TAXI, 44));

        assertThat(aiState.getPossibleMrXLocations()).containsExactly(33, 19, 44, 45);
    }

    @Test
    public void testHeuristic() {
        var black = new Player(MRX, defaultMrXTickets(), 45);
        var red = new Player(RED, defaultDetectiveTickets(), 111);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        Board.GameState state = new MyGameStateFactory().build(standard24MoveSetup(), black, red, blue);

        MCTSInterface mcts = new MCTSInterface(new MrXAiStateFactory().build(state), AiType.MRX);

        mcts.next();
        int i = 0;
        while (i < 100) {
            mcts.next();
            AiState aiState = mcts.getSelectNode().getState();
            if (mcts.getSelectNode().getState().getTurn().isPresent() && mcts.getSelectNode().getState().getTurn().get().isMrX()) {
                Move move = aiState.applyHeuristic(Heuristic.MCD);

                int max = -1;
                for (int dest : state.getSetup().graph.adjacentNodes(move.source())) {
                    max = Math.max(max, ScotLandYardAi.LOOKUP.getMinDistance(dest, aiState.getDetectiveLocations()));
                }

                assertThat(ScotLandYardAi.LOOKUP.getMinDistance(movesFinalDestination(move), aiState.getDetectiveLocations()))
                        .isEqualTo(max);
            } else {
                Move move;
                if (mcts.getAi() == AiType.MRX) {
                    move = aiState.applyHeuristic(Heuristic.MTD);
                    int min = Integer.MAX_VALUE;
                    for (int dest : state.getSetup().graph.adjacentNodes(move.source())) {
                        int total = 0;
                        for (int mrXLoc : aiState.getPossibleMrXLocations())
                            total += ScotLandYardAi.LOOKUP.getDistance(dest, mrXLoc);
                        min = Math.min(min, total);
                    }

                    int sum = 0;
                    for (int mrXLoc : aiState.getPossibleMrXLocations())
                        sum += ScotLandYardAi.LOOKUP.getDistance(movesFinalDestination(move), mrXLoc);
                    assertThat(sum).isEqualTo(min);
                }
            }
            i++;
        }
    }
}