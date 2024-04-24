package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.STANDARD24MOVES;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;

abstract class TestBase {

    @BeforeClass
    public static void init() {
        ScotLandYardAi.setUp();
        ScotLandYardAi.initLookup();
    }

    static GameSetup standard24MoveSetup() {
        return new GameSetup(ScotLandYardAi.defaultGraph, STANDARD24MOVES);
    }
}
