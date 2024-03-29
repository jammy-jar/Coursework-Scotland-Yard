package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Board;

import javax.annotation.Nonnull;

public class ScotLandYardAi {
    public static final ImmutableList<Integer> MRX_START_LOCATIONS =
            ImmutableList.of(35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172);

    public interface Factory<T> {
        @Nonnull
        T build(Board board);
    }
}
