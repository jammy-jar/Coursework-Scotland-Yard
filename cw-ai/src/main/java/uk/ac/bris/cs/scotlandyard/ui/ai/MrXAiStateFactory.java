package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class MrXAiStateFactory implements ScotLandYardAi.Factory<AiState> {

    @Nonnull
    @Override
    public AiState build(Board.GameState state) {
        Set<Integer> possibleMrXLocations;
        possibleMrXLocations = new HashSet<>(ScotLandYardAi.MRX_START_LOCATIONS);
        return new MrXAiState(state, possibleMrXLocations);
    }

    @Nonnull
    @Override
    public AiState build(Board.GameState state, Set<Integer> possibleMrXLocations) {
        return new MrXAiState(state, possibleMrXLocations);
    }

    private class MrXAiState extends AbstractAiState {
        private final Board.GameState state;
        private final Set<Integer> possibleMrXLocations;
        private final Set<Integer> detectiveLocations;
        private final List<Move> moves;

        public MrXAiState(Board.GameState state, Set<Integer> mrXLocations) {
            this.state = state;
            this.detectiveLocations = Utils.initDetectiveLocations(state);
            this.possibleMrXLocations = mrXLocations;

            this.moves = filterMoves(state.getAvailableMoves());
        }

        // Finding the 'Maximum closest distance'.
        private Move calcMCDHeuristic() {
            int maximum = 0;
            Move maxMove = null;
            if (moves.isEmpty())
                throw new NoSuchElementException("There are no available moves for this player!");

            for (Move move : moves) {
                int distance = ScotLandYardAi.LOOKUP.getMinDistance(Utils.movesFinalDestination(move), detectiveLocations);
                if (distance >= maximum) {
                    maximum = distance;
                    maxMove = move;
                }
            }
            if (maxMove == null) throw new NullPointerException("The optimal move is null!");
            return maxMove;
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
            return Optional.of(Piece.MrX.MRX);
        }

        @Nonnull
        @Override
        public List<Move> getMoves() {
            return moves;
        }

        @Nonnull
        @Override
        public Move applyHeuristic(Heuristic heuristic) {
            Move move;
            if (heuristic == Heuristic.NONE) return calcRandomMove();
            else {
                long initialTime = System.currentTimeMillis();
                if (heuristic == Heuristic.MCD) move = calcMCDHeuristic();
                else throw new IllegalArgumentException("This heuristic is not valid for MrX");
                EfficiencyCalculator.calcMCDHeuristicTime += System.currentTimeMillis() - initialTime;
            }
            return move;
        }

        public static Set<Integer> calcMrXReachableLocations(Board.GameState state, Set<Integer> detectiveLocations, Integer source, ScotlandYard.Ticket ticket) {
            Set<Integer> locations = new HashSet<>();
            for (Integer destination : state.getSetup().graph.adjacentNodes(source)) {
                if (!detectiveLocations.contains(destination)) {
                    ImmutableSet<ScotlandYard.Transport> transports = state.getSetup().graph.edgeValueOrDefault(source, destination, ImmutableSet.of());
                    if (ticket == ScotlandYard.Ticket.SECRET || transports.stream().anyMatch(t -> t.requiredTicket() == ticket))
                        locations.add(destination);
                }
            }
            return locations;
        }

        public static Set<Integer> updatePossibleMrXLocations(Board.GameState oldState, Board.GameState newState, Set<Integer> detectiveLocations, Set<Integer> prevMrXLocations) {
            int moveNum = newState.getMrXTravelLog().size();
            LogEntry latestLog = newState.getMrXTravelLog().get(moveNum - 1);

            int lastMoveNum = oldState.getMrXTravelLog().size();
            boolean madeDoubleMove = moveNum - lastMoveNum == 2;

            if (!madeDoubleMove) {
                if (newState.getSetup().moves.get(moveNum - 1)) {
                    if (latestLog.location().isPresent())
                        return new HashSet<>(Set.of(latestLog.location().get()));
                    else
                        throw new NoSuchElementException("The Log Entry of the last move is not present!");
                } else {
                    Set<Integer> locations = new HashSet<>();
                    for (Integer location : prevMrXLocations)
                        locations.addAll(calcMrXReachableLocations(newState, detectiveLocations, location, latestLog.ticket()));

                    return locations;
                }
            } else {
                LogEntry secondMostLatestLog = newState.getMrXTravelLog().get(moveNum - 2);
                Set<Integer> firstLocations = new HashSet<>();
                if (newState.getSetup().moves.get(moveNum - 2)) {
                    if (secondMostLatestLog.location().isPresent())
                        firstLocations.add(secondMostLatestLog.location().get());
                    else
                        throw new NoSuchElementException("The Log Entry of the last move is not present!");
                } else {
                    for (Integer location : prevMrXLocations)
                        firstLocations.addAll(calcMrXReachableLocations(newState, detectiveLocations, location, secondMostLatestLog.ticket()));
                }

                if (newState.getSetup().moves.get(moveNum - 1)) {
                    if (latestLog.location().isPresent())
                        return new HashSet<>(Set.of(latestLog.location().get()));
                    else
                        throw new NoSuchElementException("The Log Entry of the last move is not present!");
                } else {
                    Set<Integer> locations = new HashSet<>();
                    for (int location : firstLocations)
                        locations.addAll(calcMrXReachableLocations(newState, detectiveLocations, location, latestLog.ticket()));
                    return locations;
                }
            }
        }

        // Filter so that double moves are only made if no other moves can be made.
        private Set<Move> filterDoubleMoves(Set<Move> moves) {
            Set<Move> singleMoves = moves.stream().filter(m -> m instanceof Move.SingleMove).collect(Collectors.toSet());
            if (singleMoves.isEmpty())
                return moves;
            else return singleMoves;
        }

        // We apply move filtering so black tickets & double tickets aren't wasted.
        private List<Move> filterMoves(ImmutableSet<Move> availableMoves) {
            /* Our Rules:
            MrX Should never move next to a detective unless its his only choice.
            SECRET tickets shouldn't be used when:
            1. On round 1 or 2.
            2. MrX reveals his location.
            3. All edges only have TAXI transport.
            */

            // Apply filter.
            // Just check for single moves, as most double moves will have at least 1 edge that's not TAXI.
            Set<ScotlandYard.Ticket> connectingEdges = new HashSet<>();
            for (int source : availableMoves.stream().filter(m -> m instanceof Move.SingleMove).map(m -> ((Move.SingleMove) m).destination).toList()) {
                for (int destination : state.getSetup().graph.adjacentNodes(source))
                    connectingEdges.addAll(state.getSetup().graph.edgeValueOrDefault(source, destination, ImmutableSet.of()).stream().map(t -> t.requiredTicket()).toList());
            }

            Set<Move> filteredMovesPt1 = new HashSet<>();
            int round = state.getMrXTravelLog().size();
            if (round <= 2
                    || round < state.getSetup().moves.size() && state.getSetup().moves.get(round)
                    || connectingEdges.stream().allMatch(t -> t == ScotlandYard.Ticket.TAXI)) {
                // Filter out SingleMoves or DoubleMoves where one or more of the tickets is a secret ticket.
                for (Move move : availableMoves) {
                    if (move instanceof Move.SingleMove s && s.ticket != ScotlandYard.Ticket.SECRET)
                        filteredMovesPt1.add(move);
                    else if (move instanceof Move.DoubleMove d
                            && (d.ticket1 != ScotlandYard.Ticket.SECRET && d.ticket2 != ScotlandYard.Ticket.SECRET))
                        filteredMovesPt1.add(move);
                }
                if (filteredMovesPt1.isEmpty()) filteredMovesPt1.addAll(availableMoves);
            } else filteredMovesPt1.addAll(availableMoves);
            Set<Move> filteredMovesPt2 = new HashSet<>(filterDoubleMoves(filteredMovesPt1));

            Set<Move> filteredMovesPt3 = new HashSet<>();
            for (Move move : filteredMovesPt2) {
                if (ScotLandYardAi.LOOKUP.getMinDistance(Utils.movesFinalDestination(move), detectiveLocations) > 1)
                    filteredMovesPt3.add(move);
            }
            if (filteredMovesPt3.isEmpty())
                filteredMovesPt3.addAll(filteredMovesPt2);

            if (!filteredMovesPt3.isEmpty()) return filteredMovesPt3.stream().toList();
            else return availableMoves.stream().toList();
        }

        @Nonnull
        @Override
        public AiState advance(Move move) {
            Board.GameState newGameState = state.advance(move);
            long initialTime = System.currentTimeMillis();
            Set<Integer> newDetectiveLocations = Utils.initDetectiveLocations(newGameState);
            Set<Integer> newMrXLocations = updatePossibleMrXLocations(this.state, newGameState, newDetectiveLocations, this.possibleMrXLocations);
            if (!newGameState.getWinner().isEmpty())
                return new DetectiveAiStateFactory().build(newGameState, newMrXLocations);

            EfficiencyCalculator.initPossibleMrXLocationsTime += System.currentTimeMillis() - initialTime;
            initialTime = System.currentTimeMillis();
            Map<Integer, Category> newCategoryMap = createLocationCategoryMap(newMrXLocations, newDetectiveLocations);
            EfficiencyCalculator.initLocationCategoryMapTime += System.currentTimeMillis() - initialTime;
            initialTime = System.currentTimeMillis();
            int newAssumedLocation = selectAssumedMrXLocation(newCategoryMap);
            EfficiencyCalculator.selectAssumedMrXLocationTime += System.currentTimeMillis() - initialTime;

            return new DetectiveAiStateFactory().build(newGameState, newMrXLocations, newCategoryMap, newAssumedLocation);
        }
    }
}
