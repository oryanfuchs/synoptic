package dynoptic.model.fifosys.gfsm.trace;

/**
 * Represents the state observed at a _single_ FSM, without any context -- i.e.,
 * no transition events or following states.
 */
public class ObservedFSMState {

    private static int prevAnonId = -1;

    private static String getNextAnonName() {
        prevAnonId++;
        return "a" + Integer.toString(prevAnonId);
    }

    // //////////////////////////////////////////////////////////////////

    // The process id of the FSM that generated this state.
    final private int pid;

    // Whether or not this state is an anonymous state -- it was synthesized
    // because no concrete state name was given in the trace between two events.
    // final boolean isAnon;

    // Whether or not this state is an initial state.
    final private boolean isInitial;

    // Whether or not this state is a terminal state in a trace.
    final private boolean isTerminal;

    // The string representation of this state.
    final private String name;

    // TODO: For non-anon states include things like line number and filename,
    // and so forth.

    public static ObservedFSMState ObservedTerminalFSMState(int pid) {
        return new ObservedFSMState(pid, false, true, getNextAnonName());
    }

    public static ObservedFSMState ObservedTerminalFSMState(int pid, String name) {
        return new ObservedFSMState(pid, false, true, name);
    }

    // ///////////

    public static ObservedFSMState ObservedInitialFSMState(int pid) {
        return new ObservedFSMState(pid, true, false, getNextAnonName());
    }

    public static ObservedFSMState ObservedInitialFSMState(int pid, String name) {
        return new ObservedFSMState(pid, true, false, name);
    }

    // ///////////

    public static ObservedFSMState ObservedInitialTerminalFSMState(int pid) {
        return new ObservedFSMState(pid, true, true, getNextAnonName());
    }

    public static ObservedFSMState ObservedInitialTerminalFSMState(int pid,
            String name) {
        return new ObservedFSMState(pid, true, true, name);
    }

    // ///////////

    public static ObservedFSMState ObservedIntermediateFSMState(int pid) {
        return new ObservedFSMState(pid, false, false, getNextAnonName());
    }

    public static ObservedFSMState ObservedIntermediateFSMState(int pid,
            String name) {
        return new ObservedFSMState(pid, false, false, name);
    }

    // //////////////////////////////////////////////////////////////////

    private ObservedFSMState(int pid, boolean isInit, boolean isTerminal,
            String name) {
        this.pid = pid;
        this.isInitial = isInit;
        this.isTerminal = isTerminal;
        this.name = name;
    }

    // //////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return ((isInitial) ? "i_" : "") + name + ((isTerminal) ? "_t" : "");
    }

    // //////////////////////////////////////////////////////////////////

    public int getPid() {
        return pid;
    }

    public boolean isInitial() {
        return isInitial;
    }

    public boolean isTerminal() {
        return isTerminal;
    }
}