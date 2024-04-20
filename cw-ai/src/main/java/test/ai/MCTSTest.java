package test.ai;

import org.assertj.core.api.Assertions;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.ui.ai.AiType;
import uk.ac.bris.cs.scotlandyard.ui.ai.MCTS;
import uk.ac.bris.cs.scotlandyard.ui.ai.MrXAiStateFactory;

import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

public class MCTSTest {
    public static void testMCTS() {
        var black = new Player(MRX, defaultMrXTickets(), 45);
        var red = new Player(RED, defaultDetectiveTickets(), 111);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        Board.GameState state = new MyGameStateFactory().build(TestWorld.standard24MoveSetup(), black, red, blue);

        MCTSInterface mcts = new MCTSInterface(new MrXAiStateFactory().build(state), AiType.MRX);

        Assertions.assertThat(mcts.getSelectNode()).isEqualTo(mcts.getExpandNode());
        Assertions.assertThat(mcts.getOptimalNode()).isEmpty();
        mcts.next();
        Assertions.assertThat(mcts.getOptimalNode()).isNotEmpty();
    }
}
