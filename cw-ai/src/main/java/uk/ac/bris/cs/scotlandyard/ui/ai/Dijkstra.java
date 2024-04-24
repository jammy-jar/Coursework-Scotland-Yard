package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class Dijkstra {
    // Finds the shortest distance from the start node to every other node in the graph.
    public static Map<Integer, Integer> findShortestPath(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, int startNode) {
        Map<Integer, Integer> nodeDistanceMap = new HashMap<>();
        nodeDistanceMap.put(startNode, 0);

        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(i -> nodeDistanceMap.get(i)));
        pq.add(startNode);

        while (!pq.isEmpty()) {
            int current = pq.poll();

            for (int neighbor : graph.adjacentNodes(current)) {
                int distance = nodeDistanceMap.get(current) + 1;

                if (!nodeDistanceMap.containsKey(neighbor) || distance < nodeDistanceMap.get(neighbor)) {
                    nodeDistanceMap.put(neighbor, distance);
                    pq.add(neighbor);
                }
            }
        }

        return nodeDistanceMap;
    }
}
