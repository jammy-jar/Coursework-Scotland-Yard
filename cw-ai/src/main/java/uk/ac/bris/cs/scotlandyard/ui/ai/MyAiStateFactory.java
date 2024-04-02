package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

public class MyAiStateFactory implements ScotLandYardAi.Factory<AiState> {

    @Nonnull
    @Override
    public AiState build(Board.GameState state) {
        return new MyAiState(state, Utils.initDetectiveLocations(state), new HashSet<>(ScotLandYardAi.MRX_START_LOCATIONS), Map.of(), 0);
    }

    @Nonnull
    public AiState build(Board.GameState state, Set<Integer> possibleMrXLocations, Map<Integer, Category> categoryMap, int assumedMrXLocation) {
        return new MyAiState(state, Utils.initDetectiveLocations(state), possibleMrXLocations, categoryMap, assumedMrXLocation);
    }

    private class MyAiState implements AiState {
        private final Board.GameState state;
        private final Optional<Piece> turn;
        private final Set<Integer> possibleMrXLocations;
        private final Set<Integer> detectiveLocations;
        private final Map<Integer, Category> categoryMap;
        private final Integer assumedMrXLocation;

        public MyAiState(Board.GameState state, Set<Integer> detectiveLocations, Set<Integer> mrXLocations, Map<Integer, Category> categoryMap, Integer assumedMrXLocation) {
            this.state = state;
            // Lambda handles 'Optional' location.
            this.detectiveLocations = detectiveLocations;

            Optional<Piece> currentPlayer;
            if (state.getAvailableMoves().stream().findFirst().isPresent())
                currentPlayer = Optional.of(state.getAvailableMoves().stream().findFirst().get().commencedBy());
            else currentPlayer = Optional.empty();
            this.turn = currentPlayer;

            this.possibleMrXLocations = mrXLocations;
            this.categoryMap = categoryMap;
            this.assumedMrXLocation = assumedMrXLocation;
        }

        // Finding the 'Maximum closest distance'.
        private Move calcMCDHeuristic() {
            int maximum = 0;
            Move maxMove = null;
            if (this.state.getAvailableMoves().isEmpty())
                throw new IllegalArgumentException("There are no available moves for this player!");
            for (Move move : this.state.getAvailableMoves()) {
                int distance;
                if (move instanceof Move.SingleMove)
                    distance = MyAi.LOOKUP.getMinDistance(((Move.SingleMove) move).destination, detectiveLocations);
                else {
                    distance = MyAi.LOOKUP.getMinDistance(((Move.DoubleMove) move).destination2, detectiveLocations);
                }
                if (distance >= maximum) {
                    maximum = distance;
                    maxMove = move;
                }
            }
            if (maxMove == null) throw new NullPointerException();
            return maxMove;
        }

        // Finding the 'Minimum total distance'.
        private Move calcMTDHeuristic() {
            int minimum = Integer.MAX_VALUE;
            Move minMove = null;
            if (state.getAvailableMoves().isEmpty())
                throw new IllegalArgumentException("There are no available moves for this player!");
            for (Move move : state.getAvailableMoves()) {
                int total = 0;
                for (Integer mrXLoc : possibleMrXLocations) {
                    // Assume detectives turn so only single moves.
                    total += MyAi.LOOKUP.getDistance(mrXLoc, ((Move.SingleMove) move).destination);
                }
                if (total <= minimum) {
                    minimum = total;
                    minMove = move;
                }
            }
            if (minMove == null) throw new NullPointerException();
            return minMove;
        }

        // Finding the 'Chase assumed location' (shortest distance to assumed MrX location).
        private Move calcCALHeuristic() {
            int minimum = Integer.MAX_VALUE;
            Move minMove = null;
            if (state.getAvailableMoves().isEmpty())
                throw new IllegalArgumentException("There are no available moves for this player!");
            for (Move move : state.getAvailableMoves()) {
                // Assume detectives turn so only single moves.
                int distance = MyAi.LOOKUP.getDistance(assumedMrXLocation, ((Move.SingleMove) move).destination);
                if (distance <= minimum) {
                    minimum = distance;
                    minMove = move;
                }
            }
            if (minMove == null) throw new NullPointerException();
            return minMove;
        }

        private Move calcRandomMove() {
            List<Move> moves = state.getAvailableMoves().stream().toList();
            int rand = new Random().nextInt(moves.size());

            return moves.get(rand);
        }

        private Set<Integer> calcMrXReachableLocations(Board.GameState newState, Set<Integer> newDetectiveLocations, Integer source, ScotlandYard.Ticket ticket) {
            Set<Integer> locations = new HashSet<>();
            for (Integer destination : newState.getSetup().graph.adjacentNodes(source)) {
                if (!newDetectiveLocations.contains(destination)) {
                    ImmutableSet<ScotlandYard.Transport> transports = newState.getSetup().graph.edgeValueOrDefault(source, destination, ImmutableSet.of());
                    if (ticket == ScotlandYard.Ticket.SECRET || transports.stream().anyMatch(t -> t.requiredTicket() == ticket))
                        locations.add(destination);
                }
            }
            return locations;
        }

        private Set<Integer> updatePossibleMrXLocations(Board.GameState oldState, Board.GameState newState, Set<Integer> newDetectiveLocations, Set<Integer> oldLocations) {
            int moveNum = newState.getMrXTravelLog().size();
            LogEntry latestLog = newState.getMrXTravelLog().get(moveNum - 1);

            int lastMoveNum = oldState.getMrXTravelLog().size();

            boolean madeDoubleMove = moveNum - lastMoveNum == 2;

            if (!madeDoubleMove) {
                if (newState.getSetup().moves.get(moveNum - 1)) {
                    if (latestLog.location().isPresent())
                        return new HashSet<>(Set.of(latestLog.location().get()));
                    else
                        throw new IllegalArgumentException("The Log Entry of the last move is not present!");
                } else {
                    Set<Integer> locations = new HashSet<>();
                    for (Integer location : oldLocations)
                        locations.addAll(calcMrXReachableLocations(newState, newDetectiveLocations, location, latestLog.ticket()));

                    return locations;
                }
            } else {
                LogEntry secondMostLatestLog = newState.getMrXTravelLog().get(moveNum - 2);
                Set<Integer> firstLocations = new HashSet<>();
                if (newState.getSetup().moves.get(moveNum - 2)) {
                    if (secondMostLatestLog.location().isPresent())
                        firstLocations.add(secondMostLatestLog.location().get());
                    else
                        throw new IllegalArgumentException("The Log Entry of the last move is not present!");
                } else {
                    for (Integer location : oldLocations)
                        firstLocations.addAll(calcMrXReachableLocations(newState, newDetectiveLocations, location, secondMostLatestLog.ticket()));
                }

                if (newState.getSetup().moves.get(moveNum - 1)) {
                    if (latestLog.location().isPresent())
                        return new HashSet<>(Set.of(latestLog.location().get()));
                    else
                        throw new IllegalArgumentException("The Log Entry of the last move is not present!");
                } else {
                    Set<Integer> locations = new HashSet<>();
                    for (int location : firstLocations)
                        locations.addAll(calcMrXReachableLocations(newState, newDetectiveLocations, location, latestLog.ticket()));
                    return locations;
                }
            }
        }

        private HashMap<Integer, Category> updateLocationCategoryMap(Set<Integer> mrXLocations, Set<Integer> newDetectiveLocations) {
            HashMap<Integer, Category> map = new HashMap<>();
            // Assign each location a category based on its distance from the nearest detective.
            for (Integer location : mrXLocations)
                map.put(location, new Category(Math.min(MyAi.LOOKUP.getMinDistance(location, newDetectiveLocations), 5)));
            return map;
        }

        private HashMap<Integer, Double> calcLocationProbabilities(Map<Integer, Category> newCategoryMap) {
            HashMap<Integer, Double> probabilities = new HashMap<>();
            Optional<Double> weightSum = newCategoryMap.values().stream().map(c -> c.getWeight()).reduce((s, w) -> s + w);
            if (weightSum.isEmpty())
                throw new IllegalArgumentException("The category map is empty!");
            int prevLocation = -1;
            for (int location : newCategoryMap.keySet()) {
                double prevProb = probabilities.isEmpty() ? 0 : probabilities.get(prevLocation);
                probabilities.put(location, prevProb + newCategoryMap.get(location).getWeight() / weightSum.get());
                prevLocation = location;
            }
            return probabilities;
        }

        private Integer selectAssumedMrXLocation(Map<Integer, Category> newCategoryMap) {
            HashMap<Integer, Double> locationProbabilities = calcLocationProbabilities(newCategoryMap);
            double rand = new Random().nextDouble();

            int catchC = -1;
            for (int location : newCategoryMap.keySet()) {
                if (locationProbabilities.get(location) >= rand)
                    return location;
                catchC = location;
            }
            if (catchC < 0)
                throw new IllegalArgumentException("The category map is empty!");
            return catchC;
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
        public Map<Integer, Category> getCategoryMap() {
            return categoryMap;
        }

        @Override
        public int getAssumedMrXLocation() {
            return assumedMrXLocation;
        }

        @Nonnull
        @Override
        public Move applyHeuristic(Heuristic mrXHeuristic, Heuristic detectivesHeuristic) {
            Move move;
            if (mrXHeuristic == Heuristic.NONE && detectivesHeuristic == Heuristic.NONE) move = calcRandomMove();
            else if (turn.isPresent() && turn.get().isMrX()) {
                long initialTime = System.currentTimeMillis();
                if (mrXHeuristic == Heuristic.MCD) move = calcMCDHeuristic();
                else throw new IllegalArgumentException("This heuristic is not valid for MrX");
                EfficiencyCalculator.calcMCDHeuristicTime += System.currentTimeMillis() - initialTime;
            } else {
                long initialTime = System.currentTimeMillis();
                if (detectivesHeuristic == Heuristic.MTD) move = calcMTDHeuristic();
                else if (detectivesHeuristic == Heuristic.CAL) move = calcCALHeuristic();
                else throw new IllegalArgumentException("This heuristic is not valid for Detectives");
                EfficiencyCalculator.calcMTDORCALHeuristicTime += System.currentTimeMillis() - initialTime;
            }
            return move;
        }

        @Nonnull
        @Override
        public AiState advance(Move move) {
            Board.GameState newGameState = state.advance(move);
            Set<Integer> newDetectiveLocations = Utils.initDetectiveLocations(newGameState);
            Set<Integer> newMrXLocations;
            Map<Integer, Category> newCategoryMap;
            Integer newAssumedMrXLocation;
            if (turn.isPresent() && turn.get().isMrX()) {
                newMrXLocations = updatePossibleMrXLocations(this.state, newGameState, newDetectiveLocations, possibleMrXLocations);
                newCategoryMap = updateLocationCategoryMap(newMrXLocations, newDetectiveLocations);
                newAssumedMrXLocation = selectAssumedMrXLocation(newCategoryMap);
            } else {
                newMrXLocations = this.possibleMrXLocations;
                newCategoryMap = this.categoryMap;
                newAssumedMrXLocation = this.assumedMrXLocation;
            }
            return new MyAiStateFactory().build(newGameState, newMrXLocations, newCategoryMap, newAssumedMrXLocation);
        }
    }
}
