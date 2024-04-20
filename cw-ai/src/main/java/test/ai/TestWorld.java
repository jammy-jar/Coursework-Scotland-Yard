package test.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.STANDARD24MOVES;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;

public class TestWorld {
    private static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph;

    public static void setUp() {
        try {
            defaultGraph = readGraph(Resources.toString(Resources.getResource(
                            "graph.txt"),
                    StandardCharsets.UTF_8));
        } catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
    }

    @Nonnull
    static GameSetup standard24MoveSetup() {
        return new GameSetup(defaultGraph, STANDARD24MOVES);
    }

    public static void main(String[] args) {
        setUp();

        MCTSTest.testMCTS();
    }
}
