package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;

public class ScotLandYardAi {
    // Constants for MCTS.
    public final static double MRX_EXPLORATION_CONSTANT = 0.5;
    public final static double DETECTIVE_EXPLORATION_CONSTANT = 0.5;
    public final static double HISTORY_INFLUENCE_CONSTANT = 5;
    public final static double MRX_EPSILON = 0.1;
    public final static double DETECTIVE_EPSILON = 0.2;
    public final static double COALITION_REDUCTION_CONSTANT = 0.5;

    public final static double ITERATIONS = 1000;

    public static LocationDistanceLookup LOOKUP = null;
    public static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph;


    public static final ImmutableSet<Integer> MRX_START_LOCATIONS =
            ImmutableSet.of(35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172);
    // All locations that can be reached directly from the start locations.
    public static final ImmutableSet<Integer> MRX_SECOND_LOCATIONS =
            ImmutableSet.of(29, 91, 94, 128, 65, 1, 67, 3, 133, 199, 135, 72, 74, 140, 13, 77, 142, 78, 82, 22, 86, 23, 87, 89, 154, 156, 157, 161, 34, 102, 105, 41, 42, 107, 108, 46, 52, 116, 180, 55, 184, 185, 58, 187, 124, 63, 127);
    public static final ImmutableList<Double> MIN_DIST_CAT_WEIGHTS =
            ImmutableList.of(2454.0/12523, 9735.0/14502, 4047.0/7491, 1109.0/2890, 344.0/1756);

    public static void setUp() {
        try {
            defaultGraph = readGraph(Resources.toString(Resources.getResource(
                            "graph.txt"),
                    StandardCharsets.UTF_8));
        } catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
    }

    public static void initLookup() {
        // As soon as the first move is made populate the location-distance lookup table.
        // This is to initialise the distance between all pairs of locations for increased efficiency.
        LOOKUP = new LocationDistanceLookup(defaultGraph);
    }

    public interface Factory<T> {
        @Nonnull
        T build(Board.GameState state);
        @Nonnull
        T build(Board.GameState state, Set<Integer> possibleMrXLocations);
    }
}
