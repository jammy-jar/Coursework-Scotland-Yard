package test.ai;

import org.assertj.core.api.Assertions;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.*;

import java.util.ArrayList;
import java.util.List;

import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

public class MCTSTest {
    private static void printTree(Tree<Move, AiState, Double> tree) {
        List<Tree<Move, AiState, Double>.Node> children = new ArrayList<>(List.of(tree.getRoot()));
        while (!children.isEmpty()) {
            System.out.println(children.stream().map(c -> c.getVisits() + " | " + c.getValue()).toList());
            List<Tree<Move, AiState, Double>.Node> oldChildren = children;
            children = new ArrayList<>();
            for (Tree<Move, AiState, Double>.Node child : oldChildren)
                children.addAll(child.getChildren());
        }
    }

    public static void testMCTS(GameSetup setup) {
        var black = new Player(MRX, defaultMrXTickets(), 45);
        var red = new Player(RED, defaultDetectiveTickets(), 111);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        Board.GameState state = new MyGameStateFactory().build(setup, black, red, blue);

        MCTSInterface mcts = new MCTSInterface(new MrXAiStateFactory().build(state), AiType.MRX);

        Assertions.assertThat(mcts.getSelectNode()).isEqualTo(mcts.getExpandNode());

        Assertions.assertThat(mcts.getOptimalNode()).isEmpty();
        mcts.next();
        printTree(mcts.getTree());
        mcts.next();
        printTree(mcts.getTree());

        Assertions.assertThat(mcts.getOptimalNode()).isNotEmpty();
        // Check optimal node is first child (this is the only child which has been visited).
        Assertions.assertThat(mcts.getOptimalNode().get()).isEqualTo(mcts.getTree().getRoot().getChildren().get(0));

    }
}
