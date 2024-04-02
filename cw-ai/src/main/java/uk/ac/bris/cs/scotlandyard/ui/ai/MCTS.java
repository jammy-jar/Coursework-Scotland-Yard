package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

public class MCTS {
    private final static double MRX_EXPLORATION_CONSTANT = 0.2;
    private final static double DETECTIVE_EXPLORATION_CONSTANT = 2.0;
    private final static double MRX_EPSILON = 0.1;
    private final static double DETECTIVE_EPSILON = 0.2;
    private final static double COALITION_REDUCTION_CONSTANT = 0.1;

    private final AiType ai;
    private final Tree<Double> tree;

    public MCTS(AiState state, AiType ai) {
        this.ai = ai;
        this.tree = new Tree<>(null, state, 0.0);
    }

    private double calcScoreOfNode(Tree<Double>.Node node) {
        double avgNodeValue = node.getValue() / node.getVisits();
        int visitsToParent = node.getParent().getVisits();

        // TODO Check VISITS TO NODE and not VISITS TO NODE 1.
        if (node.getVisits() == 0) return Double.MAX_VALUE;
        double uct = avgNodeValue + (ai == AiType.MRX ? MRX_EXPLORATION_CONSTANT : DETECTIVE_EXPLORATION_CONSTANT) * Math.sqrt(Math.log(visitsToParent) / node.getVisits());
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

        AiState state = node.getState();
        // If no available moves don't make a move.
        if (state.getGameState().getAvailableMoves().isEmpty())
            node.addChild(null, state, 0.0);
        for (Move move : state.getGameState().getAvailableMoves())
            node.addChild(move, state.advance(move), 0.0);
        return node.getChildren().get(0);
    }

    public double playOut(Tree<Double>.Node node) {
        Random random = new Random();
        Optional<Piece> turn = node.getState().getTurn();
        AiState aiState = node.getState();
        // Get a double between 0.0 & 1.0.
        while(aiState.getGameState().getWinner().isEmpty()) {
            // Implement epsilon for epsilon-greedy playouts.
            double randNum = random.nextDouble();
            boolean useHeuristic = randNum > (ai == AiType.MRX ? MRX_EPSILON : DETECTIVE_EPSILON);
            if (useHeuristic) {
                // MCD for the MrX, and MTD for the detective is the best heuristic when the Ai is MrX
                if (ai == AiType.MRX) aiState = aiState.advance(aiState.applyHeuristic(Heuristic.MCD, Heuristic.MTD));
                    // MCD for the MrX, and CAL for the detective is the best heuristic when the Ai is Detectives.
                else if (ai == AiType.DETECTIVES) aiState = aiState.advance(aiState.applyHeuristic(Heuristic.MCD, Heuristic.CAL));
            } else {
                aiState = aiState.advance(aiState.applyHeuristic(Heuristic.NONE, Heuristic.NONE));
            }
        }

        // If the AIs MrX and the winner is MrX the game score is 1, and 0 if MrX isn't the winner vice-versa
        double gameScore;
        ImmutableSet<Piece> winner = aiState.getGameState().getWinner();
        if (winner.contains(Piece.MrX.MRX)) {
            if (ai == AiType.MRX) gameScore = 1;
            else gameScore = -1;
        } else {
            // Apply coalition reduction
            if (ai == AiType.DETECTIVES) {
                if (turn != aiState.getTurn()) gameScore = 1 - COALITION_REDUCTION_CONSTANT;
                else gameScore = 1;
            } else {
                gameScore = -1;
            }
        }
        return gameScore;
    }

    public void backPropagation(Tree<Double>.Node node, double gameScore) {
        Tree<Double>.Node currentNode = node;
        currentNode.incrementVisits();
        currentNode.setValue(currentNode.getValue() + gameScore);
        while (!currentNode.isRoot()) {
            currentNode = currentNode.getParent();
            currentNode.incrementVisits();
            currentNode.setValue(currentNode.getValue() + gameScore);
        }
    }

    public Optional<Tree<Double>.Node> run() {
        for (int i = 0; i < 2500; i++) {
            System.out.println("LOOP " + i);
            System.out.println(EfficiencyCalculator.getRatio());
            Tree<Double>.Node selectNode = selection();
            Tree<Double>.Node expandNode = expansion(selectNode);
            double gameScore = playOut(expandNode);
            backPropagation(expandNode, gameScore);
        }
        if (tree.getRoot().getChildren().isEmpty()) throw new IllegalArgumentException("There are available moves!");
        return tree.getRoot().getChildren().stream().max(Comparator.comparingInt(n -> n.getVisits()));
    }
}
