package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class LocationDistanceLookup {
    private final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;
    private static Map<Integer, Map<Integer, Integer>> locationDistanceMap;

    private Map<Integer, Map<Integer, Integer>> initValues() {
        Map<Integer, Map<Integer, Integer>> map = new HashMap<>();
        for (int source : graph.nodes())
            map.put(source, Dijkstra.findShortestPath(graph, source));
        return map;
    }
    public LocationDistanceLookup(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        this.graph = graph;
        locationDistanceMap = initValues();
    }

    public int getDistance(int source, int destination) {
        return locationDistanceMap.get(source).get(destination);
    }

    public int getMinDistance(int source, Set<Integer> destinations) {
        OptionalInt distance = destinations.stream().mapToInt(l -> ScotLandYardAi.LOOKUP.getDistance(source, l)).min();
        if (distance.isEmpty()) throw new NoSuchElementException("Destination locations is empty!");
        return distance.getAsInt();
    }
}
