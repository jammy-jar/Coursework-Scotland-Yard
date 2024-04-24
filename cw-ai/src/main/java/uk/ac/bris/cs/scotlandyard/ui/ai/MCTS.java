package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;

public class MCTS {
    protected final AiType ai;
    protected final Tree<Move, AiState, Double> tree;
    private final Map<Move, ScoreCountPair> moveScoreCountMap;
    protected Tree<Move, AiState, Double>.Node selectNode;
    protected Tree<Move, AiState, Double>.Node expandNode;
    private double playOutScore;


    public MCTS(AiState state, AiType ai) {
        this.ai = ai;
        this.tree = new Tree<>(null, state, 0.0);
        this.selectNode = this.tree.getRoot();
        this.expandNode = this.tree.getRoot();
        this.playOutScore = 0;
        this.moveScoreCountMap = new HashMap<>();
    }

    private double calcUctOfNode(Tree<Move, AiState, Double>.Node node) {
        double avgNodeValue = node.getValue() / node.getVisits();
        int visitsToParent = node.getParent().getVisits();
        ScoreCountPair moveW = moveScoreCountMap.get(node.getEdgeValue());
        double avgMoveScore = moveW == null ? 0 : moveW.getAvgScore();

        if (node.getVisits() == 0) return Double.POSITIVE_INFINITY;
        double uct = avgNodeValue + (ai == AiType.MRX ? ScotLandYardAi.MRX_EXPLORATION_CONSTANT : ScotLandYardAi.DETECTIVE_EXPLORATION_CONSTANT) * Math.sqrt(Math.log(visitsToParent) / node.getVisits());
        double progressiveHistory = ScotLandYardAi.HISTORY_INFLUENCE_CONSTANT * (avgMoveScore / (node.getVisits() * (1 - avgNodeValue) + 1));
        return uct + progressiveHistory;
    }

    // Traverse the tree, selecting the node with the max UCT until leaf node.
    private Tree<Move, AiState, Double>.Node selection() {
        Tree<Move, AiState, Double>.Node node = tree.getRoot();
        while (!node.isLeaf()) {
            Tree<Move, AiState, Double>.Node maxChild = node.getChildren().get(0);
            for (Tree<Move, AiState, Double>.Node child : node.getChildren()) {
                if (calcUctOfNode(child) > calcUctOfNode(maxChild))
                    maxChild = child;
            }
            node = maxChild;
        }
        return node;
    }

    // Expand and return the first child node if the select node has been visited before, otherwise simple return the select node.
    private Tree<Move, AiState, Double>.Node expansion() {
        if (selectNode.getVisits() < 1) return selectNode;

        AiState state = selectNode.getState();
        if (state.getMoves().isEmpty())
            throw new NoSuchElementException("No moves can be made!");
        for (Move move : state.getMoves())
            selectNode.addChild(move, state.advance(move), 0.0);
        return selectNode.getChildren().get(0);
    }

    // If the AI is MrX and the winner is MrX the game score is 1, and 0 if MrX isn't the winner vice-versa (apply coalition reduction for detectives).
    private double calcGameScore(AiState aiState, Optional<Piece> turn) {
        double gameScore = 0;
        ImmutableSet<Piece> winner = aiState.getGameState().getWinner();
        if (winner.contains(Piece.MrX.MRX)) {
            if (ai == AiType.MRX) gameScore = 1;
        } else {
            if (ai == AiType.DETECTIVES) {
                // Apply coalition reduction
                if (!turn.equals(aiState.getTurn())) gameScore = 1 - ScotLandYardAi.COALITION_REDUCTION_CONSTANT;
                else gameScore = 1;
            }
        }
        return gameScore;
    }

    // Play-out a simulation and get a score based on which player wins.
    private Double playOut() {
        Random random = new Random();

        Optional<Piece> turn = expandNode.getState().getTurn();
        Set<Move> movesMade = new HashSet<>();
        AiState aiState = expandNode.getState();

        while(aiState.getGameState().getWinner().isEmpty()) {
            // Implement 'epsilon-greedy play-outs. Heuristic is only used if randNum > epsilon.
            double randNum = random.nextDouble();
            boolean useHeuristic = randNum > (ai == AiType.MRX ? ScotLandYardAi.MRX_EPSILON : ScotLandYardAi.DETECTIVE_EPSILON);

            Heuristic heuristic;
            if (useHeuristic) {
                heuristic = switch (ai) {
                    // When the Ai is MrX the best heuristics: MCD & MTD.
                    case MRX ->
                            aiState.getTurn().isPresent() && aiState.getTurn().get().isMrX() ? Heuristic.MCD : Heuristic.MTD;
                    // When the Ai is Detectives the best heuristics: MCD & CAL.
                    case DETECTIVES ->
                            aiState.getTurn().isPresent() && aiState.getTurn().get().isMrX() ? Heuristic.MCD : Heuristic.CAL;
                };
            } else {
                // Heuristic is NONE for '< epsilon' due to application of epsilon greedy play-outs.
                heuristic = Heuristic.NONE;
            }
            Move move = aiState.applyHeuristic(heuristic);
            movesMade.add(move);
            aiState = aiState.advance(move);
        }

        double score = calcGameScore(aiState, turn);

        // Add moves from simulation to moveWrapperMap, for use in progressive history part of UCT.
        for (Move move : movesMade) {
            ScoreCountPair scPair = new ScoreCountPair();
            scPair.add(score);
            moveScoreCountMap.put(move, scPair);
        }
        return score;
    }

    // Back-propagate the score up the tree to the root.
    private void backPropagation() {
        Tree<Move, AiState, Double>.Node currentNode = expandNode;
        currentNode.incrementVisits();
        currentNode.setValue(currentNode.getValue() + playOutScore);
        while (!currentNode.isRoot()) {
            currentNode = currentNode.getParent();
            currentNode.incrementVisits();
            currentNode.setValue(currentNode.getValue() + playOutScore);
        }
    }

    // Iterate through MCTS.
    public void next() {
        this.selectNode = selection();
        if (!selectNode.getState().getGameState().getWinner().isEmpty()) {
            this.expandNode = this.selectNode;
            this.playOutScore = calcGameScore(expandNode.getState(), expandNode.getState().getTurn());
        } else {
            this.expandNode = expansion();
            this.playOutScore = playOut();
        }
        backPropagation();
    }

    // Get the child of the root node that has the most visits.
    public Optional<Tree<Move, AiState, Double>.Node> getOptimalNode() {
        return tree.getRoot().getChildren().stream().max(Comparator.comparingInt(n -> n.getVisits()));
    }
}
