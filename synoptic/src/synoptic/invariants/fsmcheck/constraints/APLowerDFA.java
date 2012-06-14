package synoptic.invariants.fsmcheck.constraints;

import synoptic.invariants.constraints.IThresholdConstraint;
import synoptic.invariants.constraints.LowerBoundConstraint;
import synoptic.invariants.constraints.TempConstrainedInvariant;
import synoptic.model.event.EventType;
import synoptic.model.interfaces.INode;
import synoptic.util.time.DTotalTime;
import synoptic.util.time.ITime;

/**
 * DFA for constrained lower bound threshold AP invariant.
 * 
 * @author Kevin
 *
 * @param <Node>
 */
public class APLowerDFA<Node extends INode<Node>> {
	private ITime currTime;
	private APState state;
	
	private EventType a;
	private EventType b;
	private IThresholdConstraint constraint;
	
	@SuppressWarnings("rawtypes")
	public APLowerDFA(TempConstrainedInvariant inv) {
		this.currTime = null;
		this.state = APState.NIL;
		this.a = inv.getFirst();
		this.b = inv.getSecond();
		
		IThresholdConstraint constr = inv.getConstraint();
		// check that inv has lower bound constraint
		if (!constr.getClass().equals(LowerBoundConstraint.class)) {
			throw new IllegalArgumentException("TempConstrainedInvariant must be for lower bound");
		}
		this.constraint = constr;
	}
	
	public APState getState() {
		return state;
	}
	
	public void transition(Node target, ITime delta) {
		EventType name = target.getEType();
		switch(this.state) {
			case NIL:
				nilTransition(name);
				break;
			case FIRST_A:
				firstATransition(name, delta);
				break;
			case FAIL_B: // permanent failure
				break;
			case NEITHER:
				neitherTransition(name, delta);
				break;
			case SUCCESS_B:
				successBTransition(name, delta);
				break;
			default: break;
		}
	}
	
	private void nilTransition(EventType name) {
		if (name.equals(a)) {
			currTime = new DTotalTime(0);
			state = APState.FIRST_A;
		} else if (name.equals(b)) { 
			state = APState.FAIL_B;
		} 
	}
	
	private void firstATransition(EventType name, ITime delta) {
		if (name.equals(b)) {
			if (constraint.evaluate(currTime)) {
				currTime = currTime.incrBy(delta);
				state = APState.SUCCESS_B;
			} else { // permanent failure
				state = APState.FAIL_B;
			}
		} else if (!name.equals(a)) { // not a
			currTime = currTime.incrBy(delta);
			state = APState.NEITHER;
		}
	}
	
	private void neitherTransition(EventType name, ITime delta) {
		if (name.equals(a)) {
			currTime = new DTotalTime(0);
			state = APState.FIRST_A;
		} else if (name.equals(b)) {
			currTime = currTime.incrBy(delta);
			if (constraint.evaluate(currTime)) {
				state = APState.SUCCESS_B;
			} else { // permanent failure
				state = APState.FAIL_B;
			}
		} else {
			currTime = currTime.incrBy(delta);
		}
	}
	
	private void successBTransition(EventType name, ITime delta) {
		if (name.equals(a)) {
			currTime = new DTotalTime(0);
			state = APState.FIRST_A;
		}
	}
}