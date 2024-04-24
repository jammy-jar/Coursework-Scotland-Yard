package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;

public abstract class AbstractAiState implements AiState {
    protected Map<Integer, Category> createLocationCategoryMap(Set<Integer> possibleMrXLocations, Set<Integer> detectiveLocations) {
        HashMap<Integer, Category> map = new HashMap<>();
        int categoryCount = 5;
        // Assign each location a category based on its distance from the nearest detective.
        for (Integer location : possibleMrXLocations)
            map.put(location, new Category(Math.min(ScotLandYardAi.LOOKUP.getMinDistance(location, detectiveLocations), categoryCount)));
        return map;
    }

    protected Integer selectAssumedMrXLocation(Map<Integer, Category> categoryMap) {
            Optional<Double> weightSum = categoryMap.values().stream().map(c -> c.getWeight()).reduce((s, w) -> s + w);
            if (weightSum.isEmpty())
                throw new NoSuchElementException("The category map is empty!");

            double rand = new Random().nextDouble(weightSum.get());

            int catchC = -1;
            for (int location : categoryMap.keySet()) {
                rand -= categoryMap.get(location).getWeight();
                if (rand < 0)
                    return location;
                catchC = location;
            }
            if (catchC < 0)
                throw new NoSuchElementException("The category map is empty!");
            return catchC;
        }
    }