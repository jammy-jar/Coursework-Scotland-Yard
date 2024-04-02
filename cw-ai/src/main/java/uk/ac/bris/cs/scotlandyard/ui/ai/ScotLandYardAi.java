package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;

import javax.annotation.Nonnull;

public class ScotLandYardAi {
    public static final ImmutableSet<Integer> MRX_START_LOCATIONS =
            ImmutableSet.of(35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172);
    public static final ImmutableList<Double> MIN_DIST_CAT_WEIGHTS =
            ImmutableList.of(2454.0/12523, 9735.0/14502, 4047.0/7491, 1100.0/2890, 344.0/1756);

    public interface Factory<T> {
        @Nonnull
        T build(Board.GameState state);
    }
}
