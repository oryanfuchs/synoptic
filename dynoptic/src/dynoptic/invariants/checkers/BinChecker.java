package dynoptic.invariants.checkers;

import dynoptic.invariants.AlwaysFollowedBy;
import dynoptic.invariants.AlwaysPrecedes;
import dynoptic.invariants.BinaryInvariant;
import dynoptic.invariants.EventuallyHappens;
import dynoptic.invariants.NeverFollowedBy;

import synoptic.model.event.DistEventType;

/**
 * Factory of binary invariant checkers, as well as the base class for all of
 * these checkers.
 */
public abstract class BinChecker {

    public enum Validity {
        // Temporarily failing -- failed if at end of trace, but might
        // recover/succeed if trace is incomplete.
        TEMP_FAIL,

        // Temporarily successful -- successful if at end of trace, but might
        // fail if trace is incomplete.
        TEMP_SUCCESS,

        // (optimization) Permanently failing -- no reason to continue checking
        // the invariant.
        PERM_FAIL,

        // (optimization) Permanently successful -- no reason to continue
        // checking the invariant.
        PERM_SUCCESS,
    }

    public static BinChecker newChecker(BinaryInvariant inv) {
        assert inv != null;

        // NOTE: EventuallyHappens instance must be checked for first, as it
        // sub-classes AFby.
        if (inv instanceof EventuallyHappens) {
            return new EventuallyChecker(inv);
        }
        if (inv instanceof AlwaysFollowedBy) {
            return new AFbyChecker(inv);
        }
        if (inv instanceof NeverFollowedBy) {
            return new NFbyChecker(inv);
        }
        if (inv instanceof AlwaysPrecedes) {
            return new APChecker(inv);
        }

        throw new IllegalArgumentException("Invariant " + inv.toString()
                + " has an unsupported type.");
    }

    // ////////////////////////////////////////////////////////////////

    /** The invariant that this checker corresponds to. */
    protected final BinaryInvariant inv;

    public BinChecker(BinaryInvariant inv) {
        this.inv = inv;
    }

    /** @return whether or not the new state is an accepting state. */
    abstract public Validity transition(DistEventType e);

    /** @return whether or not the current state is a rejecting state. */
    abstract public boolean isFail();

}
