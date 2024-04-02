package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

public class MyAi implements Ai {

	public static LocationDistanceLookup LOOKUP = null;

	@Nonnull @Override public String name() { return "Jammar"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		if (LOOKUP == null)
			LOOKUP = new LocationDistanceLookup(board);

		Optional<Move> move = new MCTS((Board.GameState) board).run();
		if (move.isEmpty()) throw new IllegalArgumentException("There are no available moves!");
		return move.get();
	}
}
