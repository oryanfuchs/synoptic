package dynoptic.model.fifosys.cfsm;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import dynoptic.DynopticTest;
import dynoptic.model.alphabet.EventType;
import dynoptic.model.fifosys.cfsm.fsm.FSM;
import dynoptic.model.fifosys.cfsm.fsm.FSMState;
import dynoptic.model.fifosys.channel.ChannelId;

public class CFSMTests extends DynopticTest {

    // Non-accepting state at pid 0/1.
    FSMState init_0, init_1;
    // Accepting state at pid 0/1.
    FSMState accepting_0, accepting_1;

    // cid: 0->1
    ChannelId cid;
    // cid!m, cid?m
    EventType e_pid0, e_pid1;
    // e_0, e_1
    EventType e2_pid0, e2_pid1;

    // FSM for pids 0,1
    FSM f0, f1;

    Set<FSMState> states;

    @Override
    public void setUp() {
        init_0 = new FSMState(false, true, 0, 0);
        accepting_0 = new FSMState(true, false, 0, 1);

        states = new LinkedHashSet<FSMState>();
        states.add(init_0);
        states.add(accepting_0);

        cid = new ChannelId(0, 1, 0);
        e_pid0 = EventType.SendEvent("m", cid);
        e2_pid0 = EventType.LocalEvent("e", 0);

        init_0.addTransition(e_pid0, accepting_0);
        accepting_0.addTransition(e2_pid0, init_0);

        f0 = new FSM(0, init_0, accepting_0, states, 0);

        // ///////////

        init_1 = new FSMState(false, true, 1, 0);
        accepting_1 = new FSMState(true, false, 1, 1);

        states.clear();
        states.add(init_1);
        states.add(accepting_1);

        e_pid1 = EventType.RecvEvent("m", cid);
        e2_pid1 = EventType.LocalEvent("e'", 1);

        init_1.addTransition(e_pid1, accepting_1);
        accepting_1.addTransition(e2_pid1, init_1);

        f1 = new FSM(1, init_1, accepting_1, states, 0);
    }

    @SuppressWarnings("unused")
    @Test
    public void createEmptyCFSM() {
        CFSM c = new CFSM(2, this.getAllToAllChannelIds(2));
    }

    @Test
    public void scmString() {
        CFSM c = new CFSM(2, this.getAllToAllChannelIds(2));
        c.addFSM(f0);
        c.addFSM(f1);
        logger.info(c.toScmString());
    }

    @Test
    public void initsAccepts() {
        CFSM c = new CFSM(2, this.getAllToAllChannelIds(2));
        c.addFSM(f0);
        c.addFSM(f1);

        assertEquals(c.getInitStates().size(), 1);
        assertEquals(c.getInitStates().iterator().next().getFSMState(0), init_0);
        assertEquals(c.getInitStates().iterator().next().getFSMState(1), init_1);

        assertEquals(c.getAcceptStates().size(), 1);
        assertEquals(c.getAcceptStates().iterator().next().getFSMState(0),
                accepting_0);
        assertEquals(c.getAcceptStates().iterator().next().getFSMState(1),
                accepting_1);

    }
}