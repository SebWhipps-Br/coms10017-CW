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


		new Model(){
			ImmutableSet<Observer> observerSet;

			@Nonnull
			@Override
			public Board getCurrentBoard() {
				//return new Board.GameState(){}
				return null;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				observerSet = new ImmutableSet.Builder<Observer>()
						.addAll(observerSet)
						.add(observer)
						.build();
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				for (Observer o : observerSet){
					if (observer.equals(o)){
						observerSet = new ImmutableSet.Builder<Observer>()
								.addAll(observerSet.stream().filter(x -> x != o).collect(Collectors.toList()))
								.build();
					}
				}
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return observerSet;
			}

			@Override
			public void chooseMove(@Nonnull Move move) {

			}
		};
		// TODO
		throw new RuntimeException("Implement me!");
	}
}
