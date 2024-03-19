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
		private ImmutableSet<Observer> observers;
		private Board.GameState state;

		private MyModel(GameSetup setup, Player MrX, ImmutableList<Player> detectives) {
			this.setup = setup;
			this.mrX = MrX;
			this.detectives = detectives;
			this.observers = ImmutableSet.of();
			this.state = new MyGameStateFactory().build(setup, mrX, detectives);
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
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
		// TODO
		throw new RuntimeException("Implement me!");
	}
}
