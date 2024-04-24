package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DijkstraTest extends TestBase {
    @Test
    public void testGetDistance() {
        assertThat(ScotLandYardAi.LOOKUP.getDistance(185, 182)).isEqualTo(4);
        assertThat(ScotLandYardAi.LOOKUP.getDistance(109, 98)).isEqualTo(2);
        assertThat(ScotLandYardAi.LOOKUP.getDistance(58, 8)).isEqualTo(2);
        assertThat(ScotLandYardAi.LOOKUP.getDistance(8, 21)).isEqualTo(4);
        assertThat(ScotLandYardAi.LOOKUP.getDistance(106, 56)).isEqualTo(3);
    }

    @Test
    public void testMinDistance() {
        assertThat(ScotLandYardAi.LOOKUP.getMinDistance(185, Sets.newHashSet(159, 133, 33, 21))).isEqualTo(2);
        assertThat(ScotLandYardAi.LOOKUP.getMinDistance(98, Sets.newHashSet(100, 110, 23))).isEqualTo(1);
        assertThat(ScotLandYardAi.LOOKUP.getMinDistance(30, Sets.newHashSet(120, 145, 179, 189, 177))).isEqualTo(8);
    }
}
