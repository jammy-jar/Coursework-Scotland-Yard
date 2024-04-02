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

//		// returns a random move, replace with your own implementation
//		ImmutableList<Move> moves = board.getAvailableMoves().asList();
//		int[] moveScores = moves.stream().mapToInt(m -> new MyScoreBoardFactory().build(board).getScore(m)).toArray();
//		int indexOfMax = 0;
//		for (int i = 0; i < moveScores.length; i++) {
//			if (moveScores[i] > moveScores[indexOfMax])
//				indexOfMax = i;
//		}

//		return moves.get(indexOfMax);

		Optional<Move> move = new MCTS((Board.GameState) board).run();
		if (move.isEmpty()) throw new IllegalArgumentException("There are no available moves!");
		return move.get();
	}
}
