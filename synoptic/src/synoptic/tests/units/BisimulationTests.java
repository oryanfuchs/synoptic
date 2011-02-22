package synoptic.tests.units;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import synoptic.algorithms.bisim.Bisimulation;
import synoptic.algorithms.bisim.KTails;
import synoptic.main.Main;
import synoptic.main.ParseException;
import synoptic.main.TraceParser;
import synoptic.model.Graph;
import synoptic.model.LogEvent;
import synoptic.model.Partition;
import synoptic.model.PartitionGraph;
import synoptic.tests.SynopticTest;
import synoptic.util.InternalSynopticException;

public class BisimulationTests extends SynopticTest {
    // Simplifies graph generation from string expressions.
    TraceParser parser;

    @Override
    public void setUp() throws ParseException {
        super.setUp();
        parser = new TraceParser();
        parser.addRegex("^(?<VTIME>)(?<TYPE>)$");
        parser.addPartitionsSeparator("^--$");
        // Main.dumpIntermediateStages = true;
        Main.useFSMChecker = true;
    }

    /**
     * Test splitting on a graph whose nodes cannot be split in any way to
     * satisfy the (correctly) mined invariants.
     * 
     * @throws ParseException
     * @throws InternalSynopticException
     */
    @Test
    public void unsplittablePartitionsTest() throws InternalSynopticException,
            ParseException {
        // Simpler trace:
        // String[] traceStrArray = new String[] { "1,1,1 a", "2,2,2 b",
        // "1,2,3 c", "--", "1,0,4 a", "1,0,5 b", "2,0,4 c" };

        // More complex trace:
        String[] traceStrArray = new String[] { "1,1,1 a", "2,2,2 b",
                "1,2,3 c", "2,2,4 d", "2,2,5 d", "--", "1,0,4 a", "1,0,5 b",
                "2,0,4 c", "2,1,5 d", "2,1,6 d" };
        String traceStr = concatinateWithNewlines(traceStrArray);

        List<LogEvent> parsedEvents = parser.parseTraceString(traceStr,
                SynopticTest.testName.getMethodName(), -1);
        Graph<LogEvent> inputGraph = parser.generateDirectTemporalRelation(
                parsedEvents, true);

        PartitionGraph pGraph = Bisimulation.getSplitGraph(inputGraph);
        PartitionGraph expectedPGraph = new PartitionGraph(inputGraph, true);

        // Check that the resulting pGraph is identical to the initial
        // partitioning using kTails from INITIAL nodes with k > diameter of
        // graph.
        Partition initial1 = pGraph.getInitialNodes().iterator().next();
        Partition initial2 = expectedPGraph.getInitialNodes().iterator().next();
        assertTrue(KTails.kEquals(initial1, initial2, 4, false));
    }

    /**
     * Test splitting on a graph that requires the splitting of all partitions
     * to satisfy the mined invariants. The final graph will therefore look just
     * like the initial graph.
     * 
     * @throws Exception
     */
    @Test
    public void splittablePartitionsTest() throws Exception {
        String[] traceStrArray = new String[] { "a", "x", "y", "z", "b", "--",
                "c", "x", "y", "z", "d" };
        String traceStr = concatinateWithNewlines(traceStrArray);
        List<LogEvent> parsedEvents = defParser.parseTraceString(traceStr,
                SynopticTest.testName.getMethodName(), -1);
        Graph<LogEvent> inputGraph = defParser.generateDirectTemporalRelation(
                parsedEvents, true);

        exportTestGraph(inputGraph, 0);

        PartitionGraph pGraph = Bisimulation.getSplitGraph(inputGraph);
        exportTestGraph(pGraph, 1);

        boolean hasInitial = false;
        boolean hasTerminal = false;
        for (Partition p : pGraph.getNodes()) {
            // Check that each partition contains exactly one LogEvent, and that
            // the set of all LogEvents is exactly the set of the input
            // LogEvents.
            if (p.getLabel() == Main.initialNodeLabel) {
                hasInitial = true;
                continue;
            }
            if (p.isTerminal()) {
                hasTerminal = true;
                continue;
            }

            assertTrue(p.getMessages().size() == 1);
            LogEvent e = p.getMessages().iterator().next();
            logger.fine("Check partition: " + p.toString() + " and e: "
                    + e.toString());
            assertTrue(parsedEvents.contains(e));
            parsedEvents.remove(e);
        }
        assertTrue(hasInitial);
        assertTrue(hasTerminal);
        assertTrue(parsedEvents.size() == 0);
    }

    // TODO: test the single step splitPartitions version.

    @Test
    public void mergePartitionsTest() throws Exception {

        // A trace that cannot be reduced with any k.
        String[] traceStrArray = new String[] { "1,1,1 a", "2,2,2 b",
                "1,2,3 c", "2,2,4 d" };
        String traceStr = concatinateWithNewlines(traceStrArray);

        List<LogEvent> parsedEvents = parser.parseTraceString(traceStr,
                SynopticTest.testName.getMethodName(), -1);
        Graph<LogEvent> inputGraph = parser.generateDirectTemporalRelation(
                parsedEvents, true);

        PartitionGraph pGraph = new PartitionGraph(inputGraph);
        Bisimulation.kReduce(pGraph, 0);
        PartitionGraph expectedPGraph = new PartitionGraph(inputGraph);

        Partition initial1 = pGraph.getInitialNodes().iterator().next();
        Partition initial2 = expectedPGraph.getInitialNodes().iterator().next();
        assertTrue(KTails.kEquals(initial1, initial2, 4, false));
    }

    // TODO: test mergePartitions on graphs where merging is possible.

    // TODO: test mergePartitions with invariant preservation.

}
