package dynoptic.model.fifosys.cfsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dynoptic.invariants.AlwaysFollowedBy;
import dynoptic.invariants.AlwaysPrecedes;
import dynoptic.invariants.BinaryInvariant;
import dynoptic.invariants.NeverFollowedBy;
import dynoptic.main.DynopticMain;
import dynoptic.model.AbsFSM;
import dynoptic.model.alphabet.EventType;
import dynoptic.model.alphabet.FSMAlphabet;
import dynoptic.model.fifosys.FifoSys;
import dynoptic.model.fifosys.cfsm.fsm.FSM;
import dynoptic.model.fifosys.cfsm.fsm.FSMState;
import dynoptic.model.fifosys.channel.ChannelId;
import dynoptic.util.Util;

/**
 * <p>
 * Represents a CFSM that consists of some number of FSM processes, which
 * communicate through some number of channels with each other. Channels are
 * uni-directional, involve just two end-points, and do not have to exist
 * between every two processes.
 * </p>
 * <p>
 * The number of processes, channels, and the channels configurations are fixed
 * when a CFSM is constructed. Once constructed, to specify/add an FSM instance
 * for a process id (pid), use the addFSM() method. Once all the FSMs have been
 * specified the CFSM is considered initialized. Before the FSM is initialized,
 * none of the other public methods will work.
 * </p>
 * <p>
 * Note that a CFSM is merely a representation of a machine, and does not
 * maintain execution instance state. FifoSysExecution does this.
 * </p>
 */
public class CFSM extends FifoSys<CFSMState> {

    // Fn: (FSM f) -> initial states of f.
    static private IStateToStateSetFn<FSMState> fnGetInitialStates = new IStateToStateSetFn<FSMState>() {
        @Override
        public Set<FSMState> eval(AbsFSM<FSMState> f) {
            return f.getInitStates();
        }
    };

    // Fn: (FSM f) -> accept states of f.
    static private IStateToStateSetFn<FSMState> fnGetAcceptStates = new IStateToStateSetFn<FSMState>() {
        @Override
        public Set<FSMState> eval(AbsFSM<FSMState> f) {
            return f.getAcceptStates();
        }
    };

    // FSMs participating in this CFSM, ordered according to process ID.
    private final List<FSM> fsms;

    // A count of the number of processes/FSMs that still remain to be
    // added/specified.
    private int unSpecifiedPids;

    // Maintains the index in the channeldIds list of the first synthetic
    // channelId. All channelIds at or above this index are synthetic.
    // Synthetic channels are used for invariant checking, and are
    // not part of the true model.
    private int firstSyntheticChIndex;

    // The set of invariants that are encoded in this CFSM. Each invariant
    // at index i in invs corresponds to the synthetic channel at index
    // (firstSyntheticChIndex + i) in channelIds list.
    private final List<BinaryInvariant> invs;

    // //////////////////////////////////////////////////////////////////

    public CFSM(int numProcesses, List<ChannelId> channelIds) {
        super(numProcesses, channelIds);
        fsms = new ArrayList<FSM>(Collections.nCopies(numProcesses, (FSM) null));
        unSpecifiedPids = numProcesses;
        firstSyntheticChIndex = Integer.MAX_VALUE;
        invs = new ArrayList<BinaryInvariant>();
    }

    // //////////////////////////////////////////////////////////////////

    @Override
    public FSMAlphabet getAlphabet() {
        assert unSpecifiedPids == 0;

        // Return the union of the alphabets of all of the FSMs.
        return super.getAlphabet();
    }

    @Override
    public Set<CFSMState> getInitStates() {
        assert unSpecifiedPids == 0;

        return deriveAllPermsOfStates(fnGetInitialStates);
    }

    @Override
    public Set<CFSMState> getAcceptStates() {
        assert unSpecifiedPids == 0;

        return deriveAllPermsOfStates(fnGetAcceptStates);
    }

    // //////////////////////////////////////////////////////////////////

    /** Returns the bad states for all invariants that augment this CFSM. */
    public List<BadState> getBadStates() {
        assert invs.size() != 0;

        // TODO: Sub-optimality -- we are needlessly creating many lists by
        // calling getBadState(inv) repeatedly.
        List<BadState> ret = new ArrayList<BadState>();
        for (BinaryInvariant inv : invs) {
            ret.addAll(getBadStates(inv));
        }
        return ret;
    }

    /**
     * Returns a set of bad states that correspond to a specific invariant that
     * is augmenting this CFSM. Each invariant corresponds to (possibly
     * multiple) bad states. A bad states is a combination of FSM states and a
     * sequence of regular expressions that describe the contents of each of the
     * queues in the CFSM. For an invariant I, a bad state B has the property
     * that if B is reachable in the CFSM then I is falsified. That is, the path
     * to reach B is the counter-example for I.
     */
    public List<BadState> getBadStates(BinaryInvariant inv) {
        assert invs.contains(inv);

        // Without invariants there are no bad states.
        if (invs.size() == 0) {
            return Collections.emptyList();
        }
        List<BadState> badStates = new ArrayList<BadState>();

        Set<CFSMState> accepts = this.getAcceptStates();
        List<String> qReList = new ArrayList<String>(channelIds.size());

        // Set non-synthetic queues reg-exps to accept the empty string.
        for (int i = 0; i < firstSyntheticChIndex; i++) {
            qReList.add("_");
        }

        int invIndex = invs.indexOf(inv);

        // Set the synthetic queue reg-exps.
        for (int i = firstSyntheticChIndex; i < channelIds.size(); i++) {
            if (i == invIndex) {
                qReList.add(inv.scmBadStateQRe(this.alphabet));
            } else {
                // Initialize non-inv invariant synthetic queues to accept
                // everything.
                qReList.add(".*");
            }
        }

        // For each accept, generate a bad state <accept, qReList>.
        for (CFSMState accept : accepts) {
            badStates.add(new BadState(accept, qReList));
        }

        return badStates;
    }

    /**
     * Adds a new FSM instance to the CFSM. Once all the FSMs have been added,
     * the CFSM is considered initialized.
     * 
     * @param fsm
     */
    public void addFSM(FSM fsm) {
        assert unSpecifiedPids > 0;
        assert fsm != null;

        int pid = fsm.getPid();

        if (DynopticMain.assertsOn) {
            // Must be a valid pid (in the right range).
            assert (pid >= 0 && pid < numProcesses);
            // Only allow to set the FSM for a pid once.
            assert (fsms.get(pid) == null);

            // Check that the FSM alphabet conforms to the expected number of
            // processes.
            for (EventType e : fsm.getAlphabet()) {
                if (e.isCommEvent()) {
                    pid = e.getChannelId().getDstPid();
                    assert pid >= 0 && pid < numProcesses;

                    pid = e.getChannelId().getSrcPid();
                    assert pid >= 0 && pid < numProcesses;
                } else {
                    pid = e.getEventPid();
                    assert pid >= 0 && pid < numProcesses;
                }
            }
        }

        fsms.set(pid, fsm);
        alphabet.addAll(fsm.getAlphabet());

        unSpecifiedPids -= 1;
    }

    public void augmentWithInvTracing(AlwaysFollowedBy inv) {
        augmentWithInvTracingGeneric(inv);

        // 5. Keep track of these synthetic events, so that we can later
        // identify and filter them out from the generated counter-example event
        // sequence.
        // 6. Return a set of "bad states" that should be used by the model
        // checker to check if invariant is invalid.
        // 6.1 bad states are a set of CFSM states (that can be emitted in scm
        // format)
        // 6.2 each bad state encodes a terminal/accepting CFSM FSM state and
        // queue contents that are empty except for a reg-exp specifying the
        // queue contents of the cid_inv channel that is an invalid
        // configuration for the invariant.
    }

    public void augmentWithInvTracing(NeverFollowedBy inv) {
        augmentWithInvTracingGeneric(inv);
    }

    public void augmentWithInvTracing(AlwaysPrecedes inv) {
        augmentWithInvTracingGeneric(inv);
    }

    /** Generate SCM representation of this CFSM (without bad_states). */
    public String toScmString() {
        assert unSpecifiedPids == 0;

        String ret;

        // Parameters to the SCM representation of this CFSM.
        String cfsmName = "blah";
        boolean lossy = false;

        ret = "scm " + cfsmName + ":\n\n";

        // Channels:
        ret += "nb_channels = " + channelIds.size() + " ;\n";
        ret += "/*\n";
        for (int i = 0; i < channelIds.size(); i++) {
            ret += "channel " + Integer.toString(i) + " : "
                    + channelIds.get(i).toString() + "\n";
        }
        ret += "*/\n\n";

        // Parameters/Alphabet:
        ret += "parameters :\n";
        ret += alphabet.toScmParametersString();
        ret += "\n";

        // Whether or not the channels are lossy:
        if (lossy) {
            ret += "lossy: 1\n\n";
        } else {
            ret += "lossy: 0\n\n";
        }

        // FSMS:
        for (int pid = 0; pid < numProcesses; pid++) {
            FSM f = fsms.get(pid);
            ret += "automaton p" + Integer.toString(pid) + " :\n";
            ret += f.toScmString();
            ret += "\n";
        }

        return ret;
    }

    // //////////////////////////////////////////////////////////////////

    private Set<CFSMState> deriveAllPermsOfStates(
            IStateToStateSetFn<FSMState> fn) {
        if (numProcesses == 1) {
            Set<CFSMState> ret = new LinkedHashSet<CFSMState>();
            for (FSMState s : fn.eval(fsms.get(0))) {
                ret.add(new CFSMState(s));
            }
            return ret;
        }

        assert numProcesses > 1;

        List<List<FSMState>> perms = Util.get2DPermutations(
                fn.eval(fsms.get(0)), fn.eval(fsms.get(1)));

        int i = 2;
        while (i != numProcesses) {
            // Modifies perms in place.
            Util.get2DPermutations(perms, fn.eval(fsms.get(i)));
            i += 1;
        }

        return CFSMState.CFSMStatesFromFSMListLists(perms);
    }

    /**
     * Augments the CFSM with a tracking channel to keep track of the two events
     * that are part of the binary invariant inv.
     */
    private void augmentWithInvTracingGeneric(BinaryInvariant inv) {
        EventType e1 = inv.getFirst();
        EventType e2 = inv.getSecond();

        assert alphabet.contains(e1);
        assert alphabet.contains(e2);
        assert fsms.size() < e1.getEventPid();
        assert fsms.size() < e2.getEventPid();
        assert !invs.contains(invs);

        invs.add(inv);

        int scmId = this.channelIds.size();

        if (firstSyntheticChIndex > scmId) {
            firstSyntheticChIndex = scmId;
        }

        // Create and add a new invariant-specific channel.
        //
        // NOTE: since the McScM model checker allows all processes to access
        // all channels, it does not matter which pids we use here.
        ChannelId invCid = new ChannelId(e1.getEventPid(), e1.getEventPid(),
                scmId, "ch-" + inv.toString());
        this.channelIds.add(invCid);

        // Update the FSM corresponding to e1.
        FSM f1 = this.fsms.get(e1.getEventPid());
        EventType e1Tracer = EventType.SendEvent(e1.getEventStr(), invCid);
        addSendToEventTx(f1, e1, e1Tracer);

        // Update the FSM corresponding to e2.
        FSM f2 = this.fsms.get(e2.getEventPid());
        EventType e2Tracer = EventType.SendEvent(e2.getEventStr(), invCid);
        addSendToEventTx(f2, e2, e2Tracer);
    }

    /**
     * Traverse the graph of FSM f and replace all transitions on e to some
     * state s to transition to a new state X, and add a transition from X to s
     * that enqueues event e on channel identified by invCid.
     * 
     * @param f1
     * @param e1
     * @param invCid
     */
    private void addSendToEventTx(FSM f, EventType eToTrace, EventType eTracer) {
        for (FSMState init : f.getInitStates()) {
            recurseAddSendToEventTx(f, init, eToTrace, eTracer);
        }
    }

    /**
     * Recursive call to perform DFA exploration of FSM f. Helper to
     * addSendToEventTx
     */
    private void recurseAddSendToEventTx(FSM f, FSMState s, EventType eToTrace,
            EventType eTracer) {

        for (EventType e : s.getTransitioningEvents()) {
            if (e.equals(eToTrace)) {
                for (FSMState next : s.getNextStates(e)) {
                    s.rmTransition(e, next);
                    FSMState newFSMState = new FSMState(s.isAccept(),
                            s.isInitial(), s.getPid(), f.getNextScmFSMStateId());
                    s.addTransition(e, newFSMState);
                    newFSMState.addTransition(eTracer, next);
                    recurseAddSendToEventTx(f, next, eToTrace, eToTrace);
                }
            } else {
                for (FSMState next : s.getNextStates(e)) {
                    recurseAddSendToEventTx(f, next, eToTrace, eToTrace);
                }
            }
        }
    }
}