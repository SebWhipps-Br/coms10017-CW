package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private final Player mrX;
		private final ImmutableList<Player> detectives;
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
				if (detectives.get(i).has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException("Detective has a double ticket!");
				if (detectives.get(i).has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException("Detective has a secret ticket!");

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


		//work in progress
		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {

			ImmutableSet<Piece> players = new ImmutableSet.Builder<Piece>()
					.add(mrX.piece())
					//.addAll(detectives
//							.stream()
//							.map( (d) -> d.piece()))

					.build();
			return players;
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			// can this be done without a loop?
			boolean found = false;
			for (int i = 0; i < detectives.size(); i++) {
				if (detectives.get(i).piece().equals(detective)) return Optional.of(detectives.get(i).location());
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			ImmutableList<Player> players = new ImmutableList.Builder<Player>()
					.addAll(detectives)
					.add(mrX)
					.build();

			TicketBoard t;
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).piece().equals(piece)) {
					int finalI = i;
					t = new TicketBoard() {
						@Override
						public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
							return players.get(finalI).tickets().get(ticket);
						}
					};
					return Optional.of(t);
				}
			}
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
