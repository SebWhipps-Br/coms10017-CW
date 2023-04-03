package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.stream.Collectors;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		return new Model(){
			private ImmutableSet<Observer> observerSet;
			private Board.GameState gameState;

			@Nonnull
			@Override
			public Board getCurrentBoard() {
				// ensures gameState is initialised
				if (gameState == null) {
					MyGameStateFactory gsf = new MyGameStateFactory();
					this.gameState = gsf.build(setup,mrX,detectives);
				}
				return this.gameState;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if (observerSet != null) {
					//prevents duplicate observers
					if (observerSet.contains(observer)) {
						throw new IllegalArgumentException("Cannot register the same observer twice!");
					} else {
						// replaces old observerSet with new one containing the new observer and the old set
						observerSet = new ImmutableSet.Builder<Observer>()
								.addAll(observerSet)
								.add(observer)
								.build();
					}
				} else {
					// Empty case: creates new observerSet
					observerSet = new ImmutableSet.Builder<Observer>()
							.add(observer)
							.build();
				}

			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (observer == null) throw new NullPointerException("Observer is null!");
				if (observerSet == null) throw new IllegalArgumentException("ObserverSet empty!");
				boolean found = false;
				for (Observer o : observerSet){
					if (observer.equals(o)){
						found = true;
						// Replaces observerSet with itself, without the specified observer
						observerSet = new ImmutableSet.Builder<Observer>()
								.addAll(observerSet.stream().filter(x -> x != o).collect(Collectors.toList()))
								.build();
					}
				}
				if (!found) throw new IllegalArgumentException("Observer was never registered!");
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return observerSet;
			}

			@Override
			public void chooseMove(@Nonnull Move move) {
				// In case the gameState hasn't been initialised
				//# Have a look at the gameState variable in this, to be cleaned up
				getCurrentBoard();
				gameState = gameState.advance(move);
				Observer.Event event;
				// Game is finished when there is a winner
				if (gameState.getWinner().isEmpty()){
					event = Observer.Event.MOVE_MADE;
				} else {
					event = Observer.Event.GAME_OVER;
				}
				for (Observer observer : observerSet){
					observer.onModelChanged(getCurrentBoard(), event);
				}
			}
		};
	}
}
