package test.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.ui.ai.*;

public class MCTSInterface extends MCTS {
    public MCTSInterface(AiState state, AiType ai) {
        super(state, ai);
    }

    public AiType getAi() {
        return super.ai;
    }

    public Tree<Move, AiState, Double>.Node getSelectNode() {
        return super.selectNode;
    }

    public Tree<Move, AiState, Double>.Node getExpandNode() {
        return super.expandNode;
    }

    public Tree<Move, AiState, Double> getTree() {
        return super.tree;
    }


    public double getPlayOutScore() {
        return super.playOutScore;
    }
}
