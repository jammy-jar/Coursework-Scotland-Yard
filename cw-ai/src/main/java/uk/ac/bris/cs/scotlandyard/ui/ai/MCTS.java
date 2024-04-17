package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;
import java.util.concurrent.*;

public class MCTS {
    private final AiType ai;
    private final Tree<Double> tree;
    private final Map<Move, MoveWrapper> moveWrapperMap;
    private final ExecutorService executorService;

    public MCTS(AiState state, AiType ai) {
        this.ai = ai;
        this.tree = new Tree<>(null, state, 0.0);
        this.moveWrapperMap = new HashMap<>();
        this.executorService = Executors.newFixedThreadPool(ScotLandYardAi.THREAD_COUNT);
    }

    private double calcUctOfNode(Tree<Double>.Node node) {
        double avgNodeValue = node.getValue() / node.getVisits();
        int visitsToParent = node.getParent().getVisits();
        MoveWrapper moveW = moveWrapperMap.get(node.getMove());
        double avgMoveScore = moveW == null ? 0 : moveW.getAvgScore();

        // TODO Check VISITS TO NODE and not VISITS TO NODE 1.
        if (node.getVisits() == 0) return Double.POSITIVE_INFINITY;
        double uct = avgNodeValue + (ai == AiType.MRX ? ScotLandYardAi.MRX_EXPLORATION_CONSTANT : ScotLandYardAi.DETECTIVE_EXPLORATION_CONSTANT) * Math.sqrt(Math.log(visitsToParent) / node.getVisits());
        double progressiveHistory = ScotLandYardAi.HISTORY_INFLUENCE_CONSTANT * (avgMoveScore / (node.getVisits() * (1 - avgNodeValue) + 1));
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
        if (node.getVisits() < 10) return node;

        AiState state = node.getState();
        // If no available moves don't make a move.
        if (state.getMoves().isEmpty()) {
//            // TODO Idk fix this
//            if (state.getTurn().isMrX())
//                throw new IllegalArgumentException("MrX can't move!");
//
//            node.addChild(null, state, 0.0);

            throw new IllegalArgumentException("No moves can be made!");
        }
        for (Move move : state.getMoves())
            node.addChild(move, state.advance(move), 0.0);
        return node;
    }

    // If the AIs MrX and the winner is MrX the game score is 1, and 0 if MrX isn't the winner vice-versa
    private double calcGameScore(AiState aiState, Piece turn) {
        double gameScore = 0;
        ImmutableSet<Piece> winner = aiState.getGameState().getWinner();
        if (winner.contains(Piece.MrX.MRX)) {
            if (ai == AiType.MRX) gameScore = 1;
        } else {
            // Apply coalition reduction
            if (ai == AiType.DETECTIVES) {
                if (turn != aiState.getTurn()) gameScore = 1 - ScotLandYardAi.COALITION_REDUCTION_CONSTANT;
                else gameScore = 1;
            }
        }
        return gameScore;
    }

    public Double playOut(Tree<Double>.Node node) {
        Random random = new Random();
        Piece turn = node.getState().getTurn();
        Set<Move> movesMade = new HashSet<>();
        AiState aiState = node.getState();
        // Get a double between 0.0 & 1.0.
        while(aiState.getGameState().getWinner().isEmpty()) {
            // Implement epsilon for epsilon-greedy play-outs.
            double randNum = random.nextDouble();
            boolean useHeuristic = randNum > (ai == AiType.MRX ? ScotLandYardAi.MRX_EPSILON : ScotLandYardAi.DETECTIVE_EPSILON);
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

    public Optional<Tree<Double>.Node> run() throws ExecutionException, InterruptedException {
        long initTime = System.currentTimeMillis();
        long executionTimeMillis = 5000;
        int i = 0;
        while (i < 10000) {
            Tree<Double>.Node selectNode = selection();
            if (!selectNode.getState().getGameState().getWinner().isEmpty()) {
                // TODO Check is this the right thing to do.
                backPropagation(selectNode, calcGameScore(selectNode.getState(), selectNode.getState().getTurn()));
            } else {
                Tree<Double>.Node expandNode = expansion(selectNode);
//            for (Tree<Double>.Node node : expandNodes) {
//                PlayOut playOut = new PlayOut(node, ai);
//                Future<Double> gameScoreFuture = executorService.submit(playOut);
//                nodeGameScoreMap.put(node, gameScoreFuture);
//            }
//            Future<Double> gameScoreFuture = nodeGameScoreMap.get(expandNodes.get(0));
//            double gameScore = gameScoreFuture.get();
//            if (gameScore < 0) throw new IllegalArgumentException("The gamescore should not be less than 0!");
                double gameScore = playOut(expandNode);
                backPropagation(expandNode, gameScore);
            }
            i++;
        }
        System.out.println(EfficiencyCalculator.getRatio());
        System.out.println("Completed: " + i + " loops.");
        executorService.shutdown();
        if (tree.getRoot().getChildren().isEmpty()) throw new IllegalArgumentException("There are no available moves!");
        for (Tree<Double>.Node child : tree.getRoot().getChildren()) {
            if (child.getMove() instanceof Move.SingleMove s)
                System.out.println("Move: " + s.destination + " has score: " + child.getValue());
            if (child.getMove() instanceof Move.DoubleMove d)
                System.out.println("Move: " + d.destination2 + " has score: " + child.getValue());
        }
        return tree.getRoot().getChildren().stream().max(Comparator.comparingInt(n -> n.getVisits()));
    }
}
