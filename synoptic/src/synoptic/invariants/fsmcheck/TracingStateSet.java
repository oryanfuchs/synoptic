package synoptic.invariants.fsmcheck;

import java.util.ArrayList;
import java.util.Collections;

import synoptic.invariants.ITemporalInvariant;
import synoptic.invariants.RelationPath;
import synoptic.model.Partition;
import synoptic.model.interfaces.ITransition;

/**
 * Abstract NFA state set which keeps the shortest path justifying a given state
 * being inhabited. This allows for synoptic.model checking which yields short
 * counterexample paths for failing synoptic.invariants.
 * 
 * @author Michael Sloan (mgsloan@gmail.com)
 * @param <T>
 *            The node type, used as an input, and stored in path-history.
 */
public abstract class TracingStateSet<T> implements
        IStateSet<T, TracingStateSet<T>> {
    public static boolean checkPath = false;

    /**
     * HistoryNode class used to construct a linked-list path through the
     * synoptic.model graph. This linked list structure is used, rather than
     * explicit lists, in order to benefit from the sharing of prefixes.
     */
    public class HistoryNode implements Comparable<HistoryNode> {
        T node;
        HistoryNode previous;
        int count;

        HistoryNode(T node, HistoryNode previous, int count) {
            this.node = node;
            this.previous = previous;
            this.count = count;
        }

        public int compareTo(HistoryNode other) {
            if (this == other) {
                return 0;
            }
            return this.count - other.count;
        }

        // Converts this chain into a RelationPath list.
        public RelationPath<T> toCounterexample(ITemporalInvariant inv) {
            ArrayList<T> path = new ArrayList<T>();
            HistoryNode cur = this;
            assert ((Partition) cur.node).isFinal();
            while (cur != null) {
                path.add(cur.node);
                if (checkPath && cur.previous != null) {
                    Partition prev = (Partition) cur.previous.node;
                    boolean found = false;
                    for (ITransition<Partition> trans : prev.getTransitions()) {
                        if (trans.getTarget().equals(cur.node)) {
                            found = true;
                            break;
                        }
                    }
                    assert found;
                }
                cur = cur.previous;
            }
            Collections.reverse(path);
            return new RelationPath<T>(inv, path);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            HistoryNode cur = this;
            while (cur != null) {
                Partition p = (Partition) cur.node;
                sb.append(p.getLabel());
                sb.append(" <- ");
                cur = cur.previous;
            }
            return sb.toString();
        }
    }

    /*
     * Helper to extend this history path with another node. If the passed in
     * path is null, then null is yielded.
     */
    public HistoryNode extend(T node, HistoryNode prior) {
        if (prior == null) {
            return null;
        }
        return new HistoryNode(node, prior, prior == null ? 1 : prior.count + 1);
    }

    /*
     * Helper to yield the shortest non-null path of the two passed in.
     */
    public HistoryNode preferShorter(HistoryNode a, HistoryNode b) {
        if (b == null) {
            return a;
        }
        if (a == null) {
            return b;
        }
        if (a.count < b.count) {
            return a;
        }
        return b;
    }

    /**
     * Queries the state for the shortest path which leads to a failing state.
     * 
     * @return The HistoryNode at the head of the linked list of nodes within
     *         the synoptic.model.
     */
    public abstract HistoryNode failpath();

    @Override
    public boolean isFail() {
        return failpath() != null;
    }

    // Utility function in common with all TracingSet toString definitions.
    protected static void appendWNull(StringBuilder s, Object o) {
        if (o == null) {
            s.append("0");
        } else {
            s.append(o.toString());
        }
    }

}
