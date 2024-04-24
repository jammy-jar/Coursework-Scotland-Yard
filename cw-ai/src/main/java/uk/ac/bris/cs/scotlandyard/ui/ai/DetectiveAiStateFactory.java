package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

public class DetectiveAiStateFactory implements ScotLandYardAi.Factory<AiState> {
    @Nonnull
    @Override
    public AiState build(Board.GameState state) {
        Set<Integer> possibleMrXLocations;
        possibleMrXLocations = new HashSet<>(ScotLandYardAi.MRX_SECOND_LOCATIONS);
        return new DetectiveAiState(state, possibleMrXLocations);
    }

    @Nonnull
    @Override
    public AiState build(Board.GameState state, Set<Integer> possibleMrXLocations) {
        return new DetectiveAiState(state, possibleMrXLocations);
    }

    @Nonnull
    public AiState build(Board.GameState state, Set<Integer> possibleMrXLocations, int assumedMrXLocation) {
        return new DetectiveAiState(state, possibleMrXLocations, assumedMrXLocation);
    }

    private class DetectiveAiState extends AbstractAiState {
        private final Board.GameState state;
        private final Piece turn;
        private final Set<Integer> possibleMrXLocations;
        private final Set<Integer> detectiveLocations;
        private final Integer assumedMrXLocation;
        private final List<Move> moves;

        // Apply filtering so only the current detectives moves are explored.
        private List<Move> filterMoves(ImmutableSet<Move> availableMoves) {
            if (availableMoves.stream().anyMatch(m -> m.commencedBy().isMrX()))
                throw new IllegalArgumentException("Detectives can't make this move!");
            return availableMoves.stream().filter(m -> m.commencedBy() == turn).toList();
        }

        public DetectiveAiState(Board.GameState state, Set<Integer> mrXLocations, int assumedMrXLocation) {
            this.state = state;
            this.detectiveLocations = Utils.initDetectiveLocations(state);
            this.possibleMrXLocations = mrXLocations;
            this.assumedMrXLocation = assumedMrXLocation;

            // Sorts, so that the order is determined. Sort by enum, eg. Piece.Detective.BLUE declared before Piece.Detective.RED, so blue will be picked first.
            Optional<Piece> turn = state.getAvailableMoves().stream().map(m -> m.commencedBy()).sorted().findFirst();
            this.turn = turn.orElseThrow(() -> new NoSuchElementException("There are no remaining players! (It is likely the detectives have no available moves)."));
            this.moves = filterMoves(state.getAvailableMoves());
        }

        public DetectiveAiState(Board.GameState state, Set<Integer> mrXLocations) {
            this(state, mrXLocations, selectAssumedMrXLocation(createLocationCategoryMap(mrXLocations, Utils.initDetectiveLocations(state))));
        }

        // Finding the 'Minimum total distance'.
        private Move calcMTDHeuristic() {
            int minimum = Integer.MAX_VALUE;
            Move minMove = null;
            if (moves.isEmpty())
                throw new NoSuchElementException("There are no available moves for this player!");
            for (Move move : moves) {
                int total = 0;
                for (Integer mrXLoc : possibleMrXLocations) {
                    if (!(move instanceof Move.SingleMove singleMove))
                        throw new RuntimeException("Detectives can only make single moves!");
                    total += ScotLandYardAi.LOOKUP.getDistance(mrXLoc, singleMove.destination);
                }
                if (total <= minimum) {
                    minimum = total;
                    minMove = move;
                }
            }
            if (minMove == null) throw new NullPointerException("The optimal move is null!");
            return minMove;
        }

        // Finding the 'Chase assumed location' (shortest distance to assumed MrX location).
        private Move calcCALHeuristic() {
            int minimum = Integer.MAX_VALUE;
            Move minMove = null;
            if (moves.isEmpty())
                throw new NoSuchElementException("There are no available moves for this player!");
            for (Move move : moves) {
                if (!(move instanceof Move.SingleMove singleMove))
                    throw new RuntimeException("Detectives can only make single moves!");
                int distance = ScotLandYardAi.LOOKUP.getDistance(assumedMrXLocation, singleMove.destination);
                if (distance <= minimum) {
                    minimum = distance;
                    minMove = move;
                }
            }
            return minMove;
        }

        private Move calcRandomMove() {
            int rand = new Random().nextInt(moves.size());
            return moves.get(rand);
        }

        @Nonnull
        @Override
        public Board.GameState getGameState() {
            return state;
        }

        @Nonnull
        @Override
        public Set<Integer> getPossibleMrXLocations() {
            return possibleMrXLocations;
        }

        @Nonnull
        @Override
        public Set<Integer> getDetectiveLocations() {
            return detectiveLocations;
        }

        @Nonnull
        @Override
        public Optional<Piece> getTurn() {
            if (turn != null) return Optional.of(turn);
            else return Optional.empty();
        }

        @Nonnull
        @Override
        public List<Move> getMoves() {
            return moves;
        }

        @Nonnull
        public Move applyHeuristic(Heuristic heuristic) {
            return switch (heuristic) {
                case NONE -> calcRandomMove();
                case MTD -> calcMTDHeuristic();
                case CAL -> calcCALHeuristic();
                case MCD -> throw new IllegalArgumentException("This heuristic is not valid for Detectives");
            };
        }

        @Nonnull
        @Override
        public AiState advance(Move move) {
            this.possibleMrXLocations.removeIf(p -> p.equals(Utils.movesFinalDestination(move)));
            Board.GameState newGameState = state.advance(move);
            Optional<Piece> nextTurn = newGameState.getAvailableMoves().stream().map(m -> m.commencedBy()).findFirst();
            if (nextTurn.isPresent() && nextTurn.get().isMrX())
                return new MrXAiStateFactory().build(newGameState, this.possibleMrXLocations);
            return new DetectiveAiStateFactory().build(newGameState, this.possibleMrXLocations, this.assumedMrXLocation);
        }
    }
}
