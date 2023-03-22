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
			Board.GameState gameState;

			@Nonnull
			@Override
			public Board getCurrentBoard() {
				//I think this is how it should work?!
				if (gameState == null) {
					MyGameStateFactory gsf = new MyGameStateFactory();
					this.gameState = gsf.build(setup,mrX,detectives);
				}
				return this.gameState;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if (observerSet != null) {
					if (observerSet.contains(observer)) {
						throw new IllegalArgumentException("Cannot register the same observer twice!");
					}
					observerSet = new ImmutableSet.Builder<Observer>()
							.addAll(observerSet)
							.add(observer)
							.build();
				} else {
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
				getCurrentBoard();
				gameState.advance(move);
				Observer.Event event;
				if (gameState.getWinner().isEmpty()){
					event = Observer.Event.GAME_OVER;
				} else {
					event = Observer.Event.MOVE_MADE;
				}
				for (Observer observer : observerSet){
					observer.onModelChanged(getCurrentBoard(), event);
				}
			}
		};
	}
}
