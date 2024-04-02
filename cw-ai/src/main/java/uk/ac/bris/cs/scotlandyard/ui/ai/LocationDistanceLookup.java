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

//    public List<Integer> dijkstra(int source) {
//        int count = board.getSetup().graph.nodes().size();
//        List<Boolean> visitedVertex = new ArrayList<>();
//        List<Integer> distance = new ArrayList<>();
//        for (int i = 0; i < count; i++) {
//            visitedVertex.add(false);
//            distance.add(Integer.MAX_VALUE);
//        }
//
//        distance.set(source-1, 0);
//        for (int i = 0; i < count; i++) {
//            int u = findMinDistance(distance, visitedVertex);
//            visitedVertex.set(u, true);
//
//            for (int v = 0; v < count; v++) {
//                if (!visitedVertex.get(v) && board.getSetup().graph.hasEdgeConnecting(u, v) && (distance.get(u) + 1 < distance.get(v))) {
//                    distance.set(v, distance.get(u) + 1);
//                }
//            }
//        }
//
//        return distance;
//    }
//
//    // Finding the minimum distance
//    private static int findMinDistance(List<Integer> distance, List<Boolean> visitedVertex) {
//        int minDistance = Integer.MAX_VALUE;
//        int minDistanceVertex = -1;
//        for (int i = 0; i < distance.size(); i++) {
//            if (!visitedVertex.get(i) && distance.get(i) < minDistance) {
//                minDistance = distance.get(i);
//                minDistanceVertex = i;
//            }
//        }
//        return minDistanceVertex;
//    }

    public LocationDistanceLookup(Board board) {
        this.board = board;
        locationDistanceMap = initValues();
    }

    public int getDistance(int source, int destination) {
        return locationDistanceMap.get(source).get(destination);
    }
}
