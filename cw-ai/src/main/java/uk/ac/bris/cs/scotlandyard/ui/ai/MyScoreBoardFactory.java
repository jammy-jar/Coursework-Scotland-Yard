package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class MyScoreBoardFactory implements ScotLandYardAi.Factory<ScoreBoard> {

    public final class MyScoreBoard implements ScoreBoard {
        private Board board;
        private ImmutableMap<Move, Integer> scorePair;
        private ImmutableSet<Move> mrXsMoves;

        private int calcScore(Move.SingleMove move) {
            Set<Piece.Detective> nearbyDetectives = new HashSet<>();

            Set<Piece.Detective> detectives = new HashSet<>();
            for (Piece piece : board.getPlayers()) {
                if (piece instanceof Piece.Detective detective) {
                    detectives.add(detective);
                }
            }

            Set<Integer> adjacentPositions = board.getSetup().graph.adjacentNodes(move.destination);
            for (int position : adjacentPositions) {
                for (Piece.Detective detective : detectives) {
                    if (board.getDetectiveLocation(detective).isPresent() && board.getDetectiveLocation(detective).get().equals(position)) {
                        for (ScotlandYard.Transport t : board.getSetup().graph.edgeValueOrDefault(move.source(), move.destination, ImmutableSet.of())) {
                            if (board.getPlayerTickets(detective).isPresent() && board.getPlayerTickets(detective).get().getCount(t.requiredTicket()) > 0) {
                                nearbyDetectives.add(detective);
                            }
                        }
                    }
                }
            }
            if (nearbyDetectives.isEmpty()) return 1;
            else return -1;
        }

        private int calcScore(Move.DoubleMove move) {
            return -1;
        }

        public MyScoreBoard(Board board) {
            this.board = board;
            this.mrXsMoves = board.getAvailableMoves();

            ImmutableMap.Builder<Move, Integer> scorePairB = ImmutableMap.builder();
            for (Move move : mrXsMoves) {
                if (move instanceof Move.SingleMove sMove)
                    scorePairB.put(move, calcScore(sMove));
                if (move instanceof Move.DoubleMove dMove)
                    scorePairB.put(move, calcScore(dMove));
            }
            this.scorePair = scorePairB.build();
        }

        @Override
        public int getScore(Move move) {
            return scorePair.get(move);
        }
    }

    @Override
    @Nonnull
    public MyScoreBoard build(Board board) {
        return new MyScoreBoard(board);
    }
}
