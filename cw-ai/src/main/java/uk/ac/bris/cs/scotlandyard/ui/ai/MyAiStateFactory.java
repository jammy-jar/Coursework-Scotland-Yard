package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

public class MyAiStateFactory implements ScotLandYardAi.Factory<AiState> {

    @Nonnull
    @Override
    public AiState build(Board.GameState state, AiType ai) {
        Set<Integer> possibleMrXLocations;
        if (ai == AiType.MRX)
            possibleMrXLocations = new HashSet<>(ScotLandYardAi.MRX_START_LOCATIONS);
        else
            possibleMrXLocations = new HashSet<>(ScotLandYardAi.MRX_SECOND_LOCATIONS);

        return new MyAiState(state, possibleMrXLocations);
    }

    @Nonnull
    public AiState build(Board.GameState state, Set<Integer> possibleMrXLocations) {
        return new MyAiState(state, possibleMrXLocations);
    }

    @Nonnull
    public AiState build(Board.GameState state, Set<Integer> possibleMrXLocations, Map<Integer, Category> categoryMap, int assumedMrXLocation) {
        return new MyAiState(state, possibleMrXLocations, categoryMap, assumedMrXLocation);
    }

    private class MyAiState implements AiState {
        private final Board.GameState state;
        private final Optional<Piece> turn;
        private final Set<Integer> possibleMrXLocations;
        private final Set<Integer> detectiveLocations;
        private final Map<Integer, Category> categoryMap;
        private final Integer assumedMrXLocation;

        private Map<Integer, Category> createLocationCategoryMap(Set<Integer> possibleMrXLocations, Set<Integer> detectiveLocations) {
            HashMap<Integer, Category> map = new HashMap<>();
            // Assign each location a category based on its distance from the nearest detective.
            for (Integer location : possibleMrXLocations)
                map.put(location, new Category(Math.min(MyAi.LOOKUP.getMinDistance(location, detectiveLocations), 5)));
            return map;
        }

        private Integer selectAssumedMrXLocation(Map<Integer, Category> categoryMap) {
            Optional<Double> weightSum = categoryMap.values().stream().map(c -> c.getWeight()).reduce((s, w) -> s + w);
            if (weightSum.isEmpty())
                throw new IllegalArgumentException("The category map is empty!");

            double rand = new Random().nextDouble(weightSum.get());

            int catchC = -1;
            for (int location : categoryMap.keySet()) {
                rand -= categoryMap.get(location).getWeight();
                if (rand < 0)
                    return location;
                catchC = location;
            }
            if (catchC < 0)
                throw new IllegalArgumentException("The category map is empty!");
            return catchC;
        }

        public MyAiState(Board.GameState state, Set<Integer> mrXLocations, Map<Integer, Category> locationCategorisationMap, int assumedMrXLocation) {
            this.state = state;
            // Lambda handles 'Optional' location.
            this.detectiveLocations = Utils.initDetectiveLocations(state);

            Optional<Piece> currentPlayer;
            if (state.getAvailableMoves().stream().findFirst().isPresent())
                currentPlayer = Optional.of(state.getAvailableMoves().stream().findFirst().get().commencedBy());
            else currentPlayer = Optional.empty();
            this.turn = currentPlayer;

            this.possibleMrXLocations = mrXLocations;
            this.categoryMap = locationCategorisationMap;
            this.assumedMrXLocation = assumedMrXLocation;
        }

        public MyAiState(Board.GameState state, Set<Integer> mrXLocations) {
            this.state = state;
            // Lambda handles 'Optional' location.
            this.detectiveLocations = Utils.initDetectiveLocations(state);

            Optional<Piece> currentPlayer;
            if (state.getAvailableMoves().stream().findFirst().isPresent())
                currentPlayer = Optional.of(state.getAvailableMoves().stream().findFirst().get().commencedBy());
            else currentPlayer = Optional.empty();
            this.turn = currentPlayer;

            this.possibleMrXLocations = mrXLocations;
            if (state.getWinner().isEmpty()) {
                this.categoryMap = createLocationCategoryMap(this.possibleMrXLocations, this.detectiveLocations);
                this.assumedMrXLocation = selectAssumedMrXLocation(this.categoryMap);
            } else {
                this.categoryMap = null;
                this.assumedMrXLocation = null;
            }
        }

        // We apply move filtering so black tickets & double tickets aren't wasted.
        private List<Move> filterMoves(ImmutableSet<Move> moves) {
            /* Our Rules:
            SECRET tickets shouldn't be used when:
            1. On round 1 or 2.
            2. MrX reveals his location.
            3. All edges only have TAXI transport.
            */

            // Just check for single moves, as most double moves will have at least 1 edge that's not TAXI.
            Set<ScotlandYard.Ticket> connectingEdges = new HashSet<>();
            for (int source : moves.stream().filter(m -> m instanceof Move.SingleMove).map(m -> ((Move.SingleMove) m).destination).toList()) {
                for (int destination : state.getSetup().graph.adjacentNodes(source))
                    connectingEdges.addAll(state.getSetup().graph.edgeValueOrDefault(source, destination, ImmutableSet.of()).stream().map(t -> t.requiredTicket()).toList());
            }

            // Apply filter.
            int round = state.getMrXTravelLog().size() + 1;
            if (round < 2
            || round < state.getSetup().moves.size() && state.getSetup().moves.get(round)
            || connectingEdges.stream().allMatch(t -> t == ScotlandYard.Ticket.TAXI)) {
                // Filter out SingleMoves or DoubleMoves where one or more of the tickets is a secret ticket.
                List<Move> filteredMoves = new ArrayList<>();
                for (Move move : moves) {
                    if (move instanceof Move.SingleMove s && s.ticket != ScotlandYard.Ticket.SECRET)
                        filteredMoves.add(move);
                    else if (move instanceof Move.DoubleMove d
                            && (d.ticket1 != ScotlandYard.Ticket.SECRET && d.ticket2 != ScotlandYard.Ticket.SECRET))
                        filteredMoves.add(move);
                }
                if (filteredMoves.isEmpty()) return moves.stream().toList();
                return filteredMoves;
            } else return moves.stream().toList();
        }

        // Finding the 'Maximum closest distance'.
        private Move calcMCDHeuristic() {
            int maximum = 0;
            Move maxMove = null;
            if (this.state.getAvailableMoves().isEmpty())
                throw new IllegalArgumentException("There are no available moves for this player!");

            List<Move> filteredMoves = filterMoves(this.state.getAvailableMoves());
            for (Move move : filteredMoves) {
                int distance;
                if (move instanceof Move.SingleMove s)
                    distance = MyAi.LOOKUP.getMinDistance(s.destination, detectiveLocations);
                else if (move instanceof Move.DoubleMove d)
                    distance = MyAi.LOOKUP.getMinDistance(d.destination2, detectiveLocations);
                else throw new RuntimeException("This move type is not valid!");
                if (distance >= maximum) {
                    maximum = distance;
                    maxMove = move;
                }
            }
            if (maxMove == null) throw new NullPointerException("The optimal move is null!");
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
                    if (!(move instanceof Move.SingleMove singleMove))
                        throw new RuntimeException("Detectives can only make single moves!");
                    total += MyAi.LOOKUP.getDistance(mrXLoc, singleMove.destination);
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
            if (state.getAvailableMoves().isEmpty())
                throw new IllegalArgumentException("There are no available moves for this player!");
            for (Move move : state.getAvailableMoves()) {
                if (!(move instanceof Move.SingleMove singleMove))
                    throw new RuntimeException("Detectives can only make single moves!");
                int distance = MyAi.LOOKUP.getDistance(assumedMrXLocation, singleMove.destination);
                if (distance <= minimum) {
                    minimum = distance;
                    minMove = move;
                }
            }
            if (minMove == null) throw new NullPointerException("The optimal move is null!");
            return minMove;
        }

        private Move calcRandomMove() {
            List<Move> moves = state.getAvailableMoves().stream().toList();
            if (turn.isPresent() && turn.get().isMrX())
                moves = filterMoves(state.getAvailableMoves());

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

        @Override
        public Optional<Piece> getTurn() {
            return turn;
        }

        @Nonnull
        @Override
        public Move applyHeuristic(Heuristic mrXHeuristic, Heuristic detectivesHeuristic) {
            Move move;
            if (mrXHeuristic == Heuristic.NONE && detectivesHeuristic == Heuristic.NONE) return calcRandomMove();
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

        public static Set<Integer> calcMrXReachableLocations(Board.GameState newState, Set<Integer> newDetectiveLocations, Integer source, ScotlandYard.Ticket ticket) {
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

        public static Set<Integer> updatePossibleMrXLocations(Board.GameState oldState, Board.GameState newState, Set<Integer> newDetectiveLocations, Set<Integer> oldLocations) {
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

        @Nonnull
        @Override
        public AiState advance(Move move) {
            Board.GameState newGameState = state.advance(move);
            if (turn.isPresent() && turn.get().isMrX()) {
                long initialTime = System.currentTimeMillis();
                Set<Integer> newMrXLocations = updatePossibleMrXLocations(this.state, newGameState, Utils.initDetectiveLocations(newGameState), possibleMrXLocations);
                EfficiencyCalculator.initPossibleMrXLocationsTime += System.currentTimeMillis() - initialTime;
                initialTime = System.currentTimeMillis();
                Map<Integer, Category> newCategoryMap = createLocationCategoryMap(newMrXLocations, Utils.initDetectiveLocations(newGameState));
                EfficiencyCalculator.initLocationCategoryMapTime += System.currentTimeMillis() - initialTime;
                initialTime = System.currentTimeMillis();
                int newAssumedLocation = selectAssumedMrXLocation(newCategoryMap);
                EfficiencyCalculator.selectAssumedMrXLocationTime += System.currentTimeMillis() - initialTime;

                return new MyAiStateFactory().build(newGameState, newMrXLocations, newCategoryMap, newAssumedLocation);
            } else {
                return new MyAiStateFactory().build(newGameState, this.possibleMrXLocations, this.categoryMap, this.assumedMrXLocation);
            }
        }
    }
}
