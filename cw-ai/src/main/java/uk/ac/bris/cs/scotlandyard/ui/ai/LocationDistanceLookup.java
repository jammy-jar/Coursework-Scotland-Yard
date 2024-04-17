package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.graph.Traverser;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.*;
import java.util.stream.Collectors;

public class LocationDistanceLookup {
    private final Board board;
    private static Map<Integer, Map<Integer, Integer>> locationDistanceMap;

    private Map<Integer, Map<Integer, Integer>> initValues() {
        Map<Integer, Map<Integer, Integer>> map = new HashMap<>();

        for (int source : board.getSetup().graph.nodes())
            map.put(source, Dijkstra.findShortestPath(board, source));

        return map;
    }
    public LocationDistanceLookup(Board board) {
        this.board = board;
        locationDistanceMap = initValues();
    }

    public int getDistance(int source, int destination) {
        return locationDistanceMap.get(source).get(destination);
    }

    public int getMinDistance(int source, Set<Integer> destinations) {
        OptionalInt distance = destinations.stream().mapToInt(l -> MyAi.LOOKUP.getDistance(source, l)).min();
        if (distance.isEmpty()) throw new NoSuchElementException("Destination locations is empty!");
        return distance.getAsInt();
    }
}
