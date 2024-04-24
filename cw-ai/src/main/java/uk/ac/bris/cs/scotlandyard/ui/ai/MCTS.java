package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;
import java.util.concurrent.*;

public class MCTS {
    protected final AiType ai;
    protected final Tree<Move, AiState, Double> tree;
    private final Map<Move, MoveWrapper> moveWrapperMap;
    protected Tree<Move, AiState, Double>.Node selectNode;
    protected Tree<Move, AiState, Double>.Node expandNode;
    protected double playOutScore;


    public MCTS(AiState state, AiType ai) {
        this.ai = ai;
        this.tree = new Tree<>(null, state, 0.0);
        this.selectNode = this.tree.getRoot();
        this.expandNode = this.tree.getRoot();
        this.playOutScore = 0;
        this.moveWrapperMap = new HashMap<>();
    }

    private double calcUctOfNode(Tree<Move, AiState, Double>.Node node) {
        double avgNodeValue = node.getValue() / node.getVisits();
        int visitsToParent = node.getParent().getVisits();
        MoveWrapper moveW = moveWrapperMap.get(node.getEdgeValue());
        double avgMoveScore = moveW == null ? 0 : moveW.getAvgScore();

        // TODO Check VISITS TO NODE and not VISITS TO NODE 1.
        if (node.getVisits() == 0) return Double.POSITIVE_INFINITY;
        double uct = avgNodeValue + (ai == AiType.MRX ? ScotLandYardAi.MRX_EXPLORATION_CONSTANT : ScotLandYardAi.DETECTIVE_EXPLORATION_CONSTANT) * Math.sqrt(Math.log(visitsToParent) / node.getVisits());
        double progressiveHistory = ScotLandYardAi.HISTORY_INFLUENCE_CONSTANT * (avgMoveScore / (node.getVisits() * (1 - avgNodeValue) + 1));
        return uct + progressiveHistory;
    }

    protected Tree<Move, AiState, Double>.Node selection() {
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

    protected Tree<Move, AiState, Double>.Node expansion() {
        if (selectNode.getVisits() < 1) return selectNode;

        AiState state = selectNode.getState();
        if (state.getMoves().isEmpty())
            throw new NoSuchElementException("No moves can be made!");
        for (Move move : state.getMoves())
            selectNode.addChild(move, state.advance(move), 0.0);
        return selectNode.getChildren().get(0);
    }

    // If the AI is MrX and the winner is MrX the game score is 1, and 0 if MrX isn't the winner vice-versa
    private double calcGameScore(AiState aiState, Optional<Piece> turn) {
        double gameScore = 0;
        ImmutableSet<Piece> winner = aiState.getGameState().getWinner();
        if (winner.contains(Piece.MrX.MRX)) {
            if (ai == AiType.MRX) gameScore = 1;
        } else {
            // Apply coalition reduction
            if (ai == AiType.DETECTIVES) {
                if (!turn.equals(aiState.getTurn())) gameScore = 1 - ScotLandYardAi.COALITION_REDUCTION_CONSTANT;
                else gameScore = 1;
            }
        }
        return gameScore;
    }

    protected Double playOut() {
        if (expandNode.getEdgeValue() != null)
            System.out.println("Nodes move is commenced by: " + expandNode.getEdgeValue().commencedBy());
        Random random = new Random();
        Optional<Piece> turn = expandNode.getState().getTurn();
        // TODO Make list a set
        List<Move> movesMade = new ArrayList<>();
        AiState aiState = expandNode.getState();
        // Get a double between 0.0 & 1.0.
        while(aiState.getGameState().getWinner().isEmpty()) {
            // Implement epsilon for epsilon-greedy play-outs.
            double randNum = random.nextDouble();
            boolean useHeuristic = randNum > (ai == AiType.MRX ? ScotLandYardAi.MRX_EPSILON : ScotLandYardAi.DETECTIVE_EPSILON);
            if (useHeuristic) {
                Heuristic heuristic = switch (ai) {
                    // When the Ai is MrX the best heuristics: MCD & MTD.
                    case MRX -> aiState.getTurn().isPresent() && aiState.getTurn().get().isMrX() ? Heuristic.MCD : Heuristic.MTD;
                    // When the Ai is Detectives the best heuristics: MCD & CAL.
                    case DETECTIVES -> aiState.getTurn().isPresent() && aiState.getTurn().get().isMrX() ? Heuristic.MCD : Heuristic.CAL;
                };
                Move move = aiState.applyHeuristic(heuristic);
                movesMade.add(move);
                aiState = aiState.advance(move);
            } else {
                // Heuristic is null for '< epsilon' due to application of epsilon greedy play-outs.
                Move move = aiState.applyHeuristic(Heuristic.NONE);
                movesMade.add(move);
                aiState = aiState.advance(move);
            }
        }
        System.out.println(aiState.getGameState().getWinner());


        double score = calcGameScore(aiState, turn);
        System.out.println("Moves made: " + movesMade.stream().filter(m -> m instanceof Move.SingleMove).map(m -> m.commencedBy()).toList());
        System.out.println("Moves made: " + movesMade.stream().filter(m -> m instanceof Move.SingleMove).map(m -> ((Move.SingleMove) m).destination).toList());
        for (Move move : movesMade) {
            MoveWrapper moveW = moveWrapperMap.computeIfAbsent(move, m -> new MoveWrapper(m));
            moveW.add(score);
        }
        return score;
    }

    protected void backPropagation() {
        Tree<Move, AiState, Double>.Node currentNode = expandNode;
        currentNode.incrementVisits();
        currentNode.setValue(currentNode.getValue() + playOutScore);
        while (!currentNode.isRoot()) {
            currentNode = currentNode.getParent();
            currentNode.incrementVisits();
            currentNode.setValue(currentNode.getValue() + playOutScore);
        }
    }

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

    public Optional<Tree<Move, AiState, Double>.Node> getOptimalNode() {
        return tree.getRoot().getChildren().stream().max(Comparator.comparingInt(n -> n.getVisits()));
    }

    // TODO REMOVE
    public void printInfo() {
        System.out.println(EfficiencyCalculator.getRatio());
        if (tree.getRoot().getChildren().isEmpty()) throw new NoSuchElementException("There are no available moves!");
        for (Tree<Move, AiState, Double>.Node child : tree.getRoot().getChildren()) {
            if (child.getEdgeValue() instanceof Move.SingleMove s)
                System.out.println("Move: " + s.destination + " has score: " + child.getValue());
            if (child.getEdgeValue() instanceof Move.DoubleMove d)
                System.out.println("Move: " + d.destination2 + " has score: " + child.getValue());
        }
    }
}
