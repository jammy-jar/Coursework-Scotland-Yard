package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

public class MCTS {
    private final static double MRX_EXPLORATION_CONSTANT = 0.2;
    private final static double DETECTIVE_EXPLORATION_CONSTANT = 2.0;
    private final static double MRX_EPSILON = 0.1;
    private final static double DETECTIVE_EPSILON = 0.2;

    private final Ai ai;
    private final Tree<Double> tree;

    public MCTS(Board.GameState state) {
        this.ai = Ai.MRX;
        this.tree = new Tree<>(null, state, 0.0);
    }

    private double calcScoreOfNode(Tree<Double>.Node node) {
        double avgNodeValue = node.getValue() / node.getVisits();
        int visitsToParent = node.getParent().getVisits();

        // TODO Check VISITS TO NODE and not VISITS TO NODE 1.
        if (node.getVisits() == 0) return Double.MAX_VALUE;
        double uct = avgNodeValue + (ai == Ai.MRX ? MRX_EXPLORATION_CONSTANT : DETECTIVE_EXPLORATION_CONSTANT) * Math.sqrt(Math.log(visitsToParent) / node.getVisits());
        // TODO Implement progressive history later.
        // double progressiveHistory = HISTORY_INFLUENCE_CONSTANT * (avgScoreOfMove / (node.getVisits() * (1 - avgNodeValue) + 1));
        return uct;
    }

    public Tree<Double>.Node selection() {
        Tree<Double>.Node node = tree.getRoot();
        while (!node.isLeaf()) {
            Tree<Double>.Node maxChild = node.getChildren().get(0);
            for (Tree<Double>.Node child : node.getChildren()) {
                if (calcScoreOfNode(child) > calcScoreOfNode(maxChild))
                    maxChild = child;
            }
            node = maxChild;
        }
        return node;
    }

    public Tree<Double>.Node expansion(Tree<Double>.Node node) {
        if (node.getVisits() == 0) return node;

        Board.GameState state = node.getGameState();
        // If no available moves don't make a move.
        if (state.getAvailableMoves().isEmpty())
            node.addChild(null, state, 0.0);
        for (Move move : state.getAvailableMoves())
            node.addChild(move, state.advance(move), 0.0);
        System.out.println("Expansion ran");
        return node.getChildren().get(0);
    }

    public double playOut(Tree<Double>.Node node) {
        Random random = new Random();
        AiState aiState = new MyAiStateFactory().build(node.getGameState());
        // Get a double between 0.0 & 1.0.
        while(aiState.getGameState().getWinner().isEmpty()) {
            double randNum = random.nextDouble();
            boolean useHeuristic = randNum > (ai == Ai.MRX ? MRX_EPSILON : DETECTIVE_EPSILON);
            if (useHeuristic) {
                // MCD for the MrX, and MTD for the detective is the best heuristic when the Ai is MrX
                if (ai == Ai.MRX) aiState = aiState.advance(Heuristic.MCD, Heuristic.MTD);
                    // MCD for the MrX, and CAL for the detective is the best heuristic when the Ai is Detectives.
                else if (ai == Ai.DETECTIVES) aiState = aiState.advance(Heuristic.MCD, Heuristic.CAL);
            } else {
                aiState = aiState.advance(Heuristic.NONE, Heuristic.NONE);
            }
        }

        // If the AIs MrX and the winner is MrX the game score is 1, and 0 if MrX isn't the winner vice-versa
        double gameScore;
        ImmutableSet<Piece> winner = aiState.getGameState().getWinner();
        if (winner.contains(Piece.MrX.MRX)) {
            if (ai == Ai.MRX) gameScore = 1;
            else gameScore = -1;
        } else {
            if (ai == Ai.DETECTIVES) gameScore = 1;
            else gameScore = -1;
        }
        return gameScore;
    }

    public void backPropagation(Tree<Double>.Node node, double gameScore) {
        node.incrementVisits();
        node.setValue(node.getValue() + gameScore);
    }

    public Optional<Move> run() {
        for (int i = 0; i < 2500; i++) {
            System.out.println("LOOP " + i);
            System.out.println(EfficiencyCalculator.getRatio());
            Tree<Double>.Node selectNode = selection();
            Tree<Double>.Node expandNode = expansion(selectNode);
            double gameScore = playOut(expandNode);
            backPropagation(expandNode, gameScore);
        }
        if (tree.getRoot().getChildren().isEmpty()) throw new IllegalArgumentException("There are available moves!");
        Move move = tree.getRoot().getChildren().stream().max(Comparator.comparingInt(n -> n.getVisits())).get().getMove();
        if (move == null) return Optional.empty();
        else return Optional.of(move);
    }
}
