package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.Optional;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(
				@Nonnull final GameSetup setup,
				@Nonnull final ImmutableSet<Piece> remaining,
				@Nonnull final ImmutableList<LogEntry> log,
				final Player mrX,
				final ImmutableList<Player> detectives)
		{
			//checking state creation
			//probably sensible to put these into private methods to clear this up

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if (mrX == null) throw new NullPointerException("MrX is null!");
			//detective checks
			if (detectives == null) throw new NullPointerException("detectives is null!");


			// clearly needs cleaning up
			for (int i = 0; i < detectives.size(); i++) {
				for (int j = 0; j < detectives.size(); j++){
					if ((i != j) && (detectives.get(i).location() == detectives.get(j).location()) ) {
						System.out.println("asdf");
						throw new IllegalArgumentException("Duplicate detectives!");
					}
				}
				if (detectives.get(i).has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException("Detective has secret ticket!");
			}

			if (detectives.contains(null)) throw new NullPointerException("Some detectives are null!");


			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			ImmutableSet players = new ImmutableSet.Builder<Player>()
					.add(mrX)
					.addAll(detectives)
					.build();
			return players;
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {

			//if (detectives.stream().anyMatch(detective));
			// perhaps there is a better way than using a for loop?
			boolean found = false;
			for (int i = 0; i < detectives.size(); i++){
				found = found || detectives.get(i).equals(detective);
			}
			if (found) return Optional.of(detective.ordinal());
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return null;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return null;
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			return null;
		}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		// TODO
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}

}
