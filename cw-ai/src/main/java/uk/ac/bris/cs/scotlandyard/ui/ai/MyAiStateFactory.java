package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class MyAiStateFactory implements ScotLandYardAi.StateFactory<AiState> {

    private class MyAiState implements AiState {
        private final Board.GameState state;
        private final Optional<Piece> turn;
        private final Set<Integer> possibleMrXLocations;
        private final ImmutableSet<Piece.Detective> detectives;
        private final Set<Integer> detectiveLocations;
        private final HashMap<Integer, Category> categoryMap;
        private final Integer assumedMrXLocation;

        // TODO Fix ticket check.
        private Set<Integer> calcMrXReachableLocations(Integer location, ScotlandYard.Ticket ticket) {
            Set<Integer> locations = new HashSet<>();
            for (Integer destination : state.getSetup().graph.adjacentNodes(location)) {
                if (!detectiveLocations.contains(destination)) {
                    ImmutableSet<ScotlandYard.Transport> transports = state.getSetup().graph.edgeValueOrDefault(location, destination, ImmutableSet.of());
                    // if (transports.stream().anyMatch(t -> t.requiredTicket() == ticket))
                        locations.add(destination);
                }
            }
            return locations;
        }

        private Set<Integer> initPossibleMrXLocations(Set<Integer> oldLocations) {
            int moveNum = state.getMrXTravelLog().size();
            LogEntry latestLog = state.getMrXTravelLog().get(moveNum - 1);

            if (state.getSetup().moves.get(moveNum - 1)) {
                if (latestLog.location().isPresent())
                    return new HashSet<>(Set.of(latestLog.location().get()));
                else
                    throw new IllegalArgumentException("The Log Entry of the last move is not present!");
            } else {
                Set<Integer> locations = new HashSet<>();
                for (Integer location : oldLocations)
                    locations.addAll(calcMrXReachableLocations(location, latestLog.ticket()));

                return locations;
            }
        }

//        private int calcShortestDistance(int startLocation, Set<Integer> endLocations) {
//            Set<Integer> unchecked = new HashSet<>();
//            for (int i = 1; i < 200; i++)
//                unchecked.add(i);
//
//            HashMap<Integer, Integer> nodeDistanceMap = new HashMap<>();
//            int distance = 0;
//            nodeDistanceMap.put(startLocation, distance);
//            if (endLocations.contains(startLocation)) return distance;
//
//            boolean match = false;
//            while (!match) {
//                distance++;
//                // Copy the set to avoid mutation during iteration.
//                for (Integer location : Set.copyOf(nodeDistanceMap.keySet())) {
//                    for (Integer destination : state.getSetup().graph.adjacentNodes(location)) {
//                        if (unchecked.contains(destination)) {
//                            if (!nodeDistanceMap.containsKey(destination)) {
//                                if (nodeDistanceMap.get(location) == distance - 1) {
//                                    nodeDistanceMap.put(destination, distance);
//                                    if (endLocations.contains(destination)) match = true;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            return distance;
//        }

        private HashMap<Integer, Category> initLocationCategoryMap() {
            HashMap<Integer, Category> map = new HashMap<>();
            // Assign each location a category based on its distance from the nearest detective.
            for (Integer location : possibleMrXLocations) {
                OptionalInt distance = detectiveLocations.stream().mapToInt(l -> MyAi.LOOKUP.getDistance(location, l)).min();
                if (distance.isEmpty()) throw new IllegalArgumentException("Detective locations is empty!");
                map.put(location, new Category(Math.min(distance.getAsInt(), 5)));
            }
            return map;
        }

        private HashMap<Integer, Double> calcLocationProbabilities() {
            HashMap<Integer, Double> probabilities = new HashMap<>();
            Optional<Double> weightSum = categoryMap.values().stream().map(c -> c.getWeight()).reduce((s, w) -> s + w);
            if (weightSum.isEmpty())
                throw new IllegalArgumentException("The category map is empty!");
            int prevLocation = -1;
            for (int location : categoryMap.keySet()) {
                double prevProb = probabilities.isEmpty() ? 0 : probabilities.get(prevLocation);
                probabilities.put(location, prevProb + categoryMap.get(location).getWeight() / weightSum.get());
                prevLocation = location;
            }
            return probabilities;
        }

        private Integer selectAssumedMrXLocation() {
            HashMap<Integer, Double> locationProbabilities = calcLocationProbabilities();
            double rand = new Random().nextDouble();

            int catchC = -1;
            for (int location : this.categoryMap.keySet()) {
                if (locationProbabilities.get(location) >= rand)
                    return location;
                catchC = location;
            }
            if (catchC < 0)
                throw new IllegalArgumentException("The category map is empty!");
            return catchC;
        }

        // Finding the 'Maximum closest distance'.
        private Move calcMCDHeuristic() {
            int maximum = 0;
            Move maxMove = null;
            if (this.state.getAvailableMoves().isEmpty())
                throw new IllegalArgumentException("There are no available moves for this player!");
            for (Move move : this.state.getAvailableMoves()) {
                OptionalInt distance;
                if (move instanceof Move.SingleMove)
                    distance = detectiveLocations.stream().mapToInt(l -> MyAi.LOOKUP.getDistance(((Move.SingleMove) move).destination, l)).min();
                else
                    distance = detectiveLocations.stream().mapToInt(l -> MyAi.LOOKUP.getDistance(((Move.DoubleMove) move).destination2, l)).min();
                if (distance.isEmpty()) throw new IllegalArgumentException("Detective locations is empty!");
                if (distance.getAsInt() >= maximum) {
                    maximum = distance.getAsInt();
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
            double rand = new Random().nextDouble();

            List<Move> moves = state.getAvailableMoves().stream().toList();
            int randScaled = (int) (rand * moves.size());
            return moves.get(randScaled);
        }

        public MyAiState(Board.GameState state, ImmutableSet<Piece.Detective> detectives, Set<Integer> oldMrXLocations) {
            this.state = state;
            this.detectives = detectives;
            // Lambda handles 'Optional' location.
            this.detectiveLocations = detectives.stream()
                    .map(d -> state.getDetectiveLocation(d))
                    .filter(l -> l.isPresent())
                    .map(l -> l.get()).collect(Collectors.toSet());

            Optional<Piece> currentPlayer;
            if (state.getAvailableMoves().stream().findFirst().isPresent())
                currentPlayer = Optional.of(state.getAvailableMoves().stream().findFirst().get().commencedBy());
            else currentPlayer = Optional.empty();
            this.turn = currentPlayer;

            // Assumes turn will never be 'empty' if MrX's turn, since Game Over otherwise.
            if (!state.getWinner().isEmpty()) {
                this.possibleMrXLocations = null;
                this.categoryMap = null;
                this.assumedMrXLocation = null;
            } else if (currentPlayer.isPresent() && currentPlayer.get().isMrX()) {
                this.possibleMrXLocations = Set.copyOf(oldMrXLocations);
                this.categoryMap = null;
                this.assumedMrXLocation = null;
            } else {
                long initialTime = System.currentTimeMillis();
                this.possibleMrXLocations = initPossibleMrXLocations(oldMrXLocations);
                EfficiencyCalculator.initPossibleMrXLocationsTime += System.currentTimeMillis() - initialTime;
                this.categoryMap = initLocationCategoryMap();
                EfficiencyCalculator.initLocationCategoryMapTime += System.currentTimeMillis() - initialTime;
                this.assumedMrXLocation = selectAssumedMrXLocation();
                EfficiencyCalculator.selectAssumedMrXLocationTime += System.currentTimeMillis() - initialTime;
            }
        }

        @Nonnull
        @Override
        public Board.GameState getGameState() {
            return state;
        }

        @Nonnull
        @Override
        public AiState advance(Heuristic mrXHeuristic, Heuristic detectivesHeuristic) {
            if (!state.getWinner().isEmpty()) System.out.println("THERE ARE WINNERS");
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
            return new MyAiStateFactory().build(state.advance(move), this.possibleMrXLocations);
        }
    }

    @Nonnull
    @Override
    public AiState build(Board.GameState state) {
        return new MyAiState(state, Utils.initDetectives(state), new HashSet<>(ScotLandYardAi.MRX_START_LOCATIONS));
    }

    @Nonnull
    public AiState build(Board.GameState state, Set<Integer> oldMrXLocations) {
        return new MyAiState(state, Utils.initDetectives(state), oldMrXLocations);
    }
}
