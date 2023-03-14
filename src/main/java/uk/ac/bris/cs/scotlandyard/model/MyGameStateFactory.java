package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;
import java.util.stream.Collectors;
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
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			//mrX checks
			if (mrX == null) throw new NullPointerException("MrX is null!");
			//detective checks
			if (detectives == null) throw new NullPointerException("detectives is null!");


			// clearly needs cleaning up
			for (int i = 0; i < detectives.size(); i++) {
				for (int j = 0; j < detectives.size(); j++){
					if ((i != j) && (detectives.get(i).location() == detectives.get(j).location()) ) {
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
			//this.moves = makeSingleMoves(setup, detectives, p, source);
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
			ImmutableList<Piece> p = ImmutableList.copyOf(detectives.stream().map(d -> d.piece()).collect(Collectors.toList()));
			ImmutableSet<Piece> players = new ImmutableSet.Builder<Piece>()
					.add(mrX.piece())
					.addAll(p)
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
					.add(mrX)
					.addAll(detectives)
					.build();

			TicketBoard t;
			for (Player player : players) {
				if (player.piece().equals(piece)) {
					t = ticket -> player.tickets().get(ticket);
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
			//to Implement
			return ImmutableSet.of();
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
//			for (Player p : detectives) {
//
//			}
			//ImmutableSet<Move> moves = new ImmutableSet.Builder<Move>()
			//		.addAll(makeSingleMoves(setup, detectives, , ))

			return null;
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<Move.SingleMove> singleMoves = new HashSet<>();
			for(int destination : setup.graph.adjacentNodes(source)) {
				if (!(detectives.stream().anyMatch(d -> d.location() == source))) {
					for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						if (player.has(t.requiredTicket())) {
							if (player.has(ScotlandYard.Ticket.SECRET)){
								singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), 0));

							} else {
								singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
							}
						}
					}
				}

			}
			return singleMoves;
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
