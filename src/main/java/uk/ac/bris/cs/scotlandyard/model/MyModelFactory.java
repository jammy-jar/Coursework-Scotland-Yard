package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model {
		private GameSetup setup;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private Set<Observer> observers;
		private Board.GameState state;

		private MyModel(GameSetup setup, Player MrX, ImmutableList<Player> detectives) {
			this.setup = setup;
			this.mrX = MrX;
			this.detectives = detectives;
			this.observers = new HashSet<>();
			this.state = new MyGameStateFactory().build(setup, mrX, detectives);
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException("The observer is null!");
			if (observers.contains(observer))
				throw new IllegalArgumentException("This observer already exists.");
			observers.add(observer);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException("The observer is null");
			if (!observers.contains(observer))
				throw new IllegalArgumentException("This observer was not registered.");
			observers.remove(observer);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		@Override
		public void chooseMove(@Nonnull Move move){
			state = state.advance(move);

			for (Observer observer : observers) {
				if (!state.getWinner().isEmpty()) observer.onModelChanged(state, Observer.Event.GAME_OVER);
				else observer.onModelChanged(state, Observer.Event.MOVE_MADE);
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);

	}
}
