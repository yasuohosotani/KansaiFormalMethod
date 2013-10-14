package org.xtext.example.modelchecking2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.xtext.example.modelchecking2.transitionSystem.Composition;
import org.xtext.example.modelchecking2.transitionSystem.CompositionOperand;
import org.xtext.example.modelchecking2.transitionSystem.Event;
import org.xtext.example.modelchecking2.transitionSystem.SetOfStates;
import org.xtext.example.modelchecking2.transitionSystem.SetOfTransitions;
import org.xtext.example.modelchecking2.transitionSystem.State;
import org.xtext.example.modelchecking2.transitionSystem.TransitionSystemFactory;
import org.xtext.example.modelchecking2.transitionSystem.Transition;
import org.xtext.example.modelchecking2.transitionSystem.TransitionSystem;
import org.xtext.example.modelchecking2.transitionSystem.impl.TransitionSystemFactoryImpl;

public class TransitionSystemComposer {

	public static final String nameSeparator = "#";

	public static String concatNames( String name1, String name2 ){
		return name1 + nameSeparator + name2;
	}

	public static String[] splitName( String name ){
		return name.split( nameSeparator );
	}
	
	public static TransitionSystem compose(
		TransitionSystem ts1,
		TransitionSystem ts2,
		boolean handshake)
	{
		TransitionSystemFactory factory = TransitionSystemFactoryImpl.init();
		String name = concatNames( ts1.getName(), ts2.getName() );
		TransitionSystem newts = factory.createTransitionSystem();
		newts.setName(name);
		newts.setSetOfStates(factory.createSetOfStates());
		newts.setSetOfEvents(factory.createSetOfEvents());
		newts.setSetOfTransitions(factory.createSetOfTransitions());
		newts.setSetOfInitialStates(factory.createSetOfInitialStates());
		
		composeSetOfEvents( ts1, ts2, newts );

		List<State[]> stateTrioList = new ArrayList<State[]>(); 
		composeSetOfInitialStates( ts1, ts2, newts, stateTrioList );
		
		composeSetOfStatesAndTransitions( ts1, ts2, newts, stateTrioList, handshake );
		
		return newts;
	}

	public static void composeSetOfEvents(
		TransitionSystem ts1, 
		TransitionSystem ts2, 
		TransitionSystem newts
	)
	{
		TransitionSystemFactory factory = TransitionSystemFactoryImpl.init();
		for( Event event1: ts1.getSetOfEvents().getEvents() ){
			Event event = factory.createEvent();
			event.setName(event1.getName());
			newts.getSetOfEvents().getEvents().add(event);
		}
		for( Event event2: ts2.getSetOfEvents().getEvents() ){
			if( findEvent( newts, event2.getName() ) == null ){
				Event event = factory.createEvent();
				event.setName(event2.getName());
				newts.getSetOfEvents().getEvents().add(event);
			}
		}
		ECollections.sort(newts.getSetOfEvents().getEvents(), 
				new Comparator<Event>() {
					public int compare( Event e1, Event e2 ){
						return e1.getName().compareTo(e2.getName());
					}
				}
			);
	}

	public static void composeSetOfInitialStates(
		TransitionSystem ts1, 
		TransitionSystem ts2, 
		TransitionSystem newts,
		List<State[]> stateTrioList
	)
	{
		TransitionSystemFactory factory = TransitionSystemFactoryImpl.init();
		for( State initialState1: ts1.getSetOfInitialStates().getInitialStates() ){
			for( State initialState2: ts2.getSetOfInitialStates().getInitialStates() ){
				String stateName = concatNames( initialState1.getName(),initialState2.getName() );
				State newState = factory.createState();
				newState.setName(stateName);
				State[] stateTrio = new State[3];
				stateTrio[0] = newState;
				stateTrio[1] = initialState1;
				stateTrio[2] = initialState2;
				stateTrioList.add( stateTrio );
				newts.getSetOfStates().getStates().add(newState);
				newts.getSetOfInitialStates().getInitialStates().add(newState);
			}
		}
		ECollections.sort(newts.getSetOfInitialStates().getInitialStates(), 
				new Comparator<State>() {
					public int compare( State s1, State s2 ){
						return s1.getName().compareTo(s2.getName());
					}
				}
			);
		
	}
	
	public static void composeSetOfStatesAndTransitions(
		TransitionSystem ts1, 
		TransitionSystem ts2, 
		TransitionSystem newts,
		List<State[]> stateTrioList,
		boolean handshake
	)
	{
		for( int i = 0; i < stateTrioList.size(); i++ ){
			State stateTrio[] = stateTrioList.get(i);
		//for( State[] stateTrio: stateTrioList ){
			State state = stateTrio[0];
			State subState1 = stateTrio[1];
			State subState2 = stateTrio[2];
			for( Event event: newts.getSetOfEvents().getEvents() ){
				List<State> nextStateList1 = getNextStates( ts1, subState1, event.getName() );
				List<State> nextStateList2 = getNextStates( ts2, subState2, event.getName() );
				if( handshake && ! event.getName().equals("_") ){
					if( ! nextStateList1.isEmpty() && ! nextStateList2.isEmpty() ){
						for( State nextState1: nextStateList1 ){
							for( State nextState2: nextStateList2 ){
								addComposedTransition( newts, state, event, nextState1, nextState2, stateTrioList );
							}
						}
					}
					else if( ! nextStateList1.isEmpty() && findEvent( ts2, event.getName() ) == null ){
						for( State nextState1: nextStateList1){
							addComposedTransition( newts, state, event, nextState1, subState2, stateTrioList );
						}
					}
					else if( ! nextStateList2.isEmpty() && findEvent( ts1, event.getName() ) == null ){
						for( State nextState2: nextStateList2 ){
							addComposedTransition( newts, state, event, subState1, nextState2, stateTrioList );
						}
					}
				}
				else{
					for( State nextState1: nextStateList1){
						addComposedTransition( newts, state, event, nextState1, subState2, stateTrioList );
					}
					for( State nextState2: nextStateList2 ){
						addComposedTransition( newts, state, event, subState1, nextState2, stateTrioList );
					}
				}
			}
		}
		ECollections.sort(newts.getSetOfStates().getStates(), 
			new Comparator<State>() {
				public int compare( State s1, State s2 ){
					return s1.getName().compareTo(s2.getName());
				}
			}
		);
	}
	
	public static List<State> getNextStates( TransitionSystem ts, State state, String eventName )
	{
		List<State> nextStateList = new ArrayList<State>();
		for( Transition transition: ts.getSetOfTransitions().getTransitions() ){
			if( transition.getFrom() == state && 
				transition.getEvent().getName().equals(eventName) ){
				nextStateList.add(transition.getTo());
			}
		}
		return nextStateList;
	}
	
	public static State findState( TransitionSystem ts, String stateName ){
		State foundState = null;
		for( State state: ts.getSetOfStates().getStates() ){
			if( state.getName().equals(stateName) ){
				foundState = state;
				break;
			}
		}
		return foundState;
	}

	public static Event findEvent( TransitionSystem ts, String eventName ){
		Event foundEvent = null;
		for( Event event: ts.getSetOfEvents().getEvents() ){
			if( event.getName().equals(eventName) ){
				foundEvent = event;
				break;
			}
		}
		return foundEvent;
	}

	public static void addComposedTransition(
			TransitionSystem ts,
			State fromState,
			Event event,
			State nextSubState1,
			State nextSubState2,
			List<State[]> stateTrioList )
	{
		TransitionSystemFactory factory = TransitionSystemFactoryImpl.init();
		State nextState = null;
		for( State[] stateTrio: stateTrioList ){
			if( stateTrio[1] == nextSubState1 && stateTrio[2] == nextSubState2 ){
				nextState = stateTrio[0];
				break;
			}
		}
		if( nextState == null ){
			String nextStateName = concatNames( nextSubState1.getName(), nextSubState2.getName() );
			nextState = factory.createState();
			nextState.setName(nextStateName);
			State[] newStateTrio = new State[3];
			newStateTrio[0] = nextState;
			newStateTrio[1] = nextSubState1;
			newStateTrio[2] = nextSubState2;
			stateTrioList.add(newStateTrio);
			ts.getSetOfStates().getStates().add(nextState);
		}
		Transition transition = factory.createTransition();
		transition.setFrom(fromState);
		transition.setEvent(event);
		transition.setTo(nextState);
		ts.getSetOfTransitions().getTransitions().add(transition);
	}

	public static List<State> getFromStates(SetOfTransitions sot)
	{
		List<State> fromStates = new ArrayList<State>();
		for( Transition transition: sot.getTransitions() ){
			State from = transition.getFrom();
			if( ! fromStates.contains(from) ){
				fromStates.add( from );
			}
		}
		Collections.sort( fromStates,
			new Comparator<State>() {
				public int compare( State s1, State s2 ){
					return s1.getName().compareTo(s2.getName());
				}
			}
		);
		return fromStates;
	}
	
	public static List<State> getToStates(SetOfTransitions sot, State state)
	{
		List<State> nextStates = new ArrayList<State>();
		for( Transition transition: sot.getTransitions() ){
			if( transition.getFrom().getName().equals(state.getName()) ){
				if( ! nextStates.contains(transition.getTo())){
					nextStates.add( transition.getTo());
				}
			}
		}
		Collections.sort( nextStates,
				new Comparator<State>() {
					public int compare( State s1, State s2 ){
						return s1.getName().compareTo(s2.getName());
					}
				}
			);
		return nextStates;
	}

	public static List<Transition> getTransitionsFromState(SetOfTransitions sot, State state)
	{
		List<Transition> transitionList = new ArrayList<Transition>();
		for( Transition transition: sot.getTransitions() ){
			if( transition.getFrom().getName().equals(state.getName()) ){
				transitionList.add(transition);
			}
		}
		return transitionList;
	}

	public static List<String> getSubStateList( SetOfStates setOfStates )
	{
		List<String> subStateList = new ArrayList<String>();
		EList<State> stateList = setOfStates.getStates();
		for( State state: stateList ){
			String[] stateNames = splitName( state.getName() );
			for( String subName: stateNames ){
				if( ! subStateList.contains(subName)){
					subStateList.add(subName);
				}
			}
		}
		Collections.sort( subStateList );
		return subStateList;
	}
	
	public static List<String> getSubStateList( TransitionSystem ts, int index )
	{
		List<String> subStateList = new ArrayList<String>();
		EList<State> stateList = ts.getSetOfStates().getStates();
		for( State state: stateList ){
			String stateNames[] = splitName( state.getName() );
			if( ! subStateList.contains(stateNames[index])){
			subStateList.add(stateNames[index]);
			}
		}
		return subStateList;
	}
	
	public static TransitionSystem compose( Composition composition )
	{
		TransitionSystem left = compose( composition.getLeftOperand() );
		if( composition.getRightOperand() == null ){
			return left;
		}
		TransitionSystem right = compose( composition.getRightOperand() );
		if( composition.getOperator().equals("|||") ){
			return compose(left,right, false );
		}
		else if( composition.getOperator().equals("||") ){
			return compose(left,right, true );
		}
		else{
			return null;
		}
	}

	public static TransitionSystem compose( CompositionOperand operand )
	{
		if( operand.getTransitionSystem() != null ){
			return operand.getTransitionSystem();
		}
		else if( operand.getComposition() != null ){
			return compose( operand.getComposition() );
		}
		return null;
	}
}

