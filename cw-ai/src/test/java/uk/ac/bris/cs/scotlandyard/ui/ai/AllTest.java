package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        MCTSTest.class,
        DijkstraTest.class,
        AiStateFactoryTest.class,
})
public class AllTest {}