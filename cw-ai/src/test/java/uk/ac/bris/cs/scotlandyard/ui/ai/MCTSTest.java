package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

public class MCTSTest extends TestBase {
    @Test
    public void testMCTSInitialisation() {
        var black = new Player(MRX, defaultMrXTickets(), 45);
        var red = new Player(RED, defaultDetectiveTickets(), 111);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        Board.GameState state = new MyGameStateFactory().build(standard24MoveSetup(), black, red, blue);

        MCTSInterface mcts = new MCTSInterface(new MrXAiStateFactory().build(state), AiType.MRX);

        Assertions.assertThat(mcts.getSelectNode()).isEqualTo(mcts.getExpandNode());
        Assertions.assertThat(mcts.getOptimalNode()).isEmpty();
        mcts.next();
        mcts.next();
        Assertions.assertThat(mcts.getOptimalNode()).isNotEmpty();
        // Check optimal node is first child (this is the only child which has been visited).
        Assertions.assertThat(mcts.getOptimalNode().get()).isEqualTo(mcts.getTree().getRoot().getChildren().get(0));

    }
}
