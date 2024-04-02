package test.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.*;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public class DetectiveAiTest {

    // Test calc reachable locations


//    public void testCalcDistanceToNearestDetective() {
//        var black = new Player(MRX, defaultDetectiveTickets(), 45);
//        var red = new Player(RED, defaultDetectiveTickets(), 111);
//        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
//        Board.GameState state = new MyGameStateFactory().build(TestWorld.standard24MoveSetup(), black, red, blue);
//
//        DetectiveAi ai = new DetectiveAi(state);
//
//        System.out.println(ai.calcDistanceToNearestDetective(state, 53));
//        System.out.println(ai.calcDistanceToNearestDetective(state, 85));
//        System.out.println(ai.calcDistanceToNearestDetective(state, 87));
//        // assertThat(ai.calcDistanceToNearestDetective(state, 53)).isEqualTo();
//        // assertThat(ai.calcDistanceToNearestDetective(state, 85)).isEqualTo();
//        // assertThat(ai.calcDistanceToNearestDetective(state, 94)).isEqualTo();
//        // assertThat(ai.calcDistanceToNearestDetective(state, 87)).isEqualTo();
//
//    }
//
//    public void testSelectAssumedMrXLocation() {
//        var black = new Player(MRX, defaultDetectiveTickets(), 45);
//        var red = new Player(RED, defaultDetectiveTickets(), 111);
//        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
//        Board.GameState state = new MyGameStateFactory().build(TestWorld.standard24MoveSetup(), black, red, blue);
//
//        DetectiveAi ai = new DetectiveAi(state);
//
//        System.out.println(ai.selectAssumedMrXLocation(state));
//    }
}
