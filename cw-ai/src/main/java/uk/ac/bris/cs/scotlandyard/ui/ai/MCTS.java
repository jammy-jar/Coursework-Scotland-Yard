package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;

public class MCTS {
    private final static double MRX_EXPLORATION_CONSTANT = 0.5;
    private final static double DETECTIVE_EXPLORATION_CONSTANT = 0.5;
    private final static double HISTORY_INFLUENCE_CONSTANT = 5;
    private final static double MRX_EPSILON = 0.1;
    private final static double DETECTIVE_EPSILON = 0.2;
    private final static double COALITION_REDUCTION_CONSTANT = 0.1;

    private final AiType ai;
    private final Tree<Double> tree;
    private final Map<Move, MoveWrapper> moveWrapperMap;


    public MCTS(AiState state, AiType ai) {
        this.ai = ai;
        this.tree = new Tree<>(null, state, 0.0);
        this.moveWrapperMap = new HashMap<>();
    }

    private double calcUctOfNode(Tree<Double>.Node node) {
        double avgNodeValue = node.getValue() / node.getVisits();
        int visitsToParent = node.getParent().getVisits();
        MoveWrapper moveW = moveWrapperMap.get(node.getMove());
        double avgMoveScore = moveW == null ? 0 : moveW.getAvgScore();

        // TODO Check VISITS TO NODE and not VISITS TO NODE 1.
        if (node.getVisits() == 0) return Double.POSITIVE_INFINITY;
        double uct = avgNodeValue + (ai == AiType.MRX ? MRX_EXPLORATION_CONSTANT : DETECTIVE_EXPLORATION_CONSTANT) * Math.sqrt(Math.log(visitsToParent) / node.getVisits());
        double progressiveHistory = HISTORY_INFLUENCE_CONSTANT * (avgMoveScore / (node.getVisits() * (1 - avgNodeValue) + 1));
        return uct + progressiveHistory;
    }

    public Tree<Double>.Node selection() {
        Tree<Double>.Node node = tree.getRoot();
        while (!node.isLeaf()) {
            Tree<Double>.Node maxChild = node.getChildren().get(0);
            for (Tree<Double>.Node child : node.getChildren()) {
                if (calcUctOfNode(child) > calcUctOfNode(maxChild))
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

    // If the AIs MrX and the winner is MrX the game score is 1, and 0 if MrX isn't the winner vice-versa
    private double calcGameScore(AiState aiState, Optional<Piece> turn) {
        double gameScore = 0;
        ImmutableSet<Piece> winner = aiState.getGameState().getWinner();
        if (winner.contains(Piece.MrX.MRX)) {
            if (ai == AiType.MRX) gameScore = 1;
        } else {
            // Apply coalition reduction
            if (ai == AiType.DETECTIVES) {
                if (turn != aiState.getTurn()) gameScore = 1 - COALITION_REDUCTION_CONSTANT;
                else gameScore = 1;
            }
        }
        return gameScore;
    }

    public double playOut(Tree<Double>.Node node) {
        Random random = new Random();
        Optional<Piece> turn = node.getState().getTurn();
        Set<Move> movesMade = new HashSet<>();
        AiState aiState = node.getState();
        // Get a double between 0.0 & 1.0.
        while(aiState.getGameState().getWinner().isEmpty()) {
            // Implement epsilon for epsilon-greedy play-outs.
            double randNum = random.nextDouble();
            boolean useHeuristic = randNum > (ai == AiType.MRX ? MRX_EPSILON : DETECTIVE_EPSILON);
            if (useHeuristic) {
                // MCD for the MrX, and MTD for the detective is the best heuristic when the Ai is MrX
                if (ai == AiType.MRX) {
                    Move move = aiState.applyHeuristic(Heuristic.MCD, Heuristic.MTD);
                    movesMade.add(move);
                    aiState = aiState.advance(move);
                } else if (ai == AiType.DETECTIVES) {
                    // MCD for the MrX, and CAL for the detective is the best heuristic when the Ai is Detectives.
                    Move move = aiState.applyHeuristic(Heuristic.MCD, Heuristic.CAL);
                    movesMade.add(move);
                    aiState = aiState.advance(move);
                }
            } else {
                Move move = aiState.applyHeuristic(Heuristic.NONE, Heuristic.NONE);
                movesMade.add(move);
                aiState = aiState.advance(move);
            }
        }

        double score = calcGameScore(aiState, turn);
        for (Move move : movesMade) {
            MoveWrapper moveW = moveWrapperMap.computeIfAbsent(move, m -> new MoveWrapper(m));
            moveW.add(score);
        }
        return score;
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
        long initTime = System.currentTimeMillis();
        for (int i = 0; i < 2500; i++) {
            // System.out.println("LOOP " + i);
            // System.out.println(EfficiencyCalculator.getRatio());
            Tree<Double>.Node selectNode = selection();
            Tree<Double>.Node expandNode = expansion(selectNode);
            double gameScore = playOut(expandNode);
            backPropagation(expandNode, gameScore);
        }
        System.out.println("Took " + (System.currentTimeMillis() - initTime) / 1000 + " seconds to run.");
        if (tree.getRoot().getChildren().isEmpty()) throw new IllegalArgumentException("There are no available moves!");
        return tree.getRoot().getChildren().stream().max(Comparator.comparingInt(n -> n.getVisits()));
    }
}
