package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MyAi implements Ai {

	public static LocationDistanceLookup LOOKUP = null;
	public static AiState previousAiState = null;

	@Nonnull @Override public String name() { return "Jammar"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// As soon as the first move is made populate the location-distance lookup table.
		// This is to initialise the distance between all pairs of locations for increased efficiency.
		if (LOOKUP == null)
			LOOKUP = new LocationDistanceLookup(board);

		Optional<Piece> currentPlayer;
		Optional<Move> someMove = board.getAvailableMoves().stream().findFirst();
		if (someMove.isPresent()) currentPlayer = Optional.of(someMove.get().commencedBy());
		else currentPlayer = Optional.empty();

		AiType ai;
		if (currentPlayer.isPresent() && currentPlayer.get().isMrX())
			ai = AiType.MRX;
		else
			ai = AiType.DETECTIVES;

		if (!(board instanceof Board.GameState state))
			throw new IllegalArgumentException("The AI does not support this type of board!");
		Optional<Tree<Double>.Node> node;
		if (previousAiState == null) node = new MCTS(new MyAiStateFactory().build(state, ai), ai).run();
		else node = new MCTS(new MyAiStateFactory().build(state, previousAiState.getPossibleMrXLocations().stream().filter(l -> !Utils.initDetectiveLocations(state).contains(l)).collect(Collectors.toSet())), ai).run();
		if (node.isEmpty()) throw new IllegalArgumentException("A best move could not be found!");
		previousAiState = node.get().getState();
		return node.get().getMove();
	}
}
