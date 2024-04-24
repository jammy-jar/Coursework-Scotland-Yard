package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MyAi implements Ai {
	public static AiState previousAiState = null;

	@Nonnull @Override public String name() { return "Jammar"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		if (ScotLandYardAi.defaultGraph == null)
			ScotLandYardAi.setUp();
		if (ScotLandYardAi.LOOKUP == null)
			ScotLandYardAi.initLookup();

		Optional<Piece> currentPlayer;
		Optional<Move> someMove = board.getAvailableMoves().stream().findFirst();
		if (someMove.isPresent()) currentPlayer = Optional.of(someMove.get().commencedBy());
		else currentPlayer = Optional.empty();

		AiType ai;
		if (currentPlayer.isPresent() && currentPlayer.get().isMrX()) ai = AiType.MRX;
		else ai = AiType.DETECTIVES;

		MCTS mcts = initMCTS(board, ai);
		for (int i = 0; i < ScotLandYardAi.ITERATIONS; i++)
			mcts.next();

		Optional<Tree<Move, AiState, Double>.Node> node = mcts.getOptimalNode();

        if (node.isEmpty()) throw new NoSuchElementException("A best move could not be found!");
		previousAiState = node.get().getState();
		return node.get().getEdgeValue();
	}

	private MCTS initMCTS(Board board, AiType ai) {
		if (!(board instanceof Board.GameState state))
			throw new IllegalArgumentException("The AI does not support this type of board!");
		if (previousAiState == null) {
			if (ai == AiType.MRX)
				return new MCTS(new MrXAiStateFactory().build(state), ai);
			else
				return new MCTS(new DetectiveAiStateFactory().build(state), ai);
		} else {
			Set<Integer> mrXRemoveDetectiveLocations = previousAiState.getPossibleMrXLocations().stream().filter(l -> !Utils.initDetectiveLocations(state).contains(l)).collect(Collectors.toSet());
			if (ai == AiType.MRX)
				return new MCTS(new MrXAiStateFactory().build(state, mrXRemoveDetectiveLocations), ai);
			else
				return new MCTS(new DetectiveAiStateFactory().build(state, mrXRemoveDetectiveLocations), ai);
		}
	}
}
