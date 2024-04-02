package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.graph.ValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.*;

public class Dijkstra {
    public static Map<Integer, Integer> findShortestPath(Board board, int startNode) {
        Map<Integer, Integer> nodeDistanceMap = new HashMap<>();
        nodeDistanceMap.put(startNode, 0);

        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(i -> nodeDistanceMap.get(i)));
        pq.add(startNode);

        while (!pq.isEmpty()) {
            int current = pq.poll();

            for (int neighbor : board.getSetup().graph.adjacentNodes(current)) {
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
