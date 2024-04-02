package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.graph.ValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.*;

public class Dijkstra {
    public static Map<Integer, Integer> findShortestPath(Board board, int startNode) {
        Map<Integer, Integer> nodeDistanceMap = new HashMap<>();

        // Set starting node distance to 0
        nodeDistanceMap.put(startNode, 0);

        // Priority queue (use a min-heap) based on distance
        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(i -> nodeDistanceMap.get(i)));
        pq.add(startNode);

        while (!pq.isEmpty()) {
            int current = pq.poll();

            // Explore neighbors
            for (int neighbor : board.getSetup().graph.adjacentNodes(current)) {
                int distance = nodeDistanceMap.get(current) + 1;

                if (!nodeDistanceMap.containsKey(neighbor) || distance < nodeDistanceMap.get(neighbor)) {
                    nodeDistanceMap.put(neighbor, distance);
                    pq.add(neighbor);
                }
            }
        }

        System.out.println(nodeDistanceMap);
        System.out.println(nodeDistanceMap.size());


        return nodeDistanceMap;
    }
}
