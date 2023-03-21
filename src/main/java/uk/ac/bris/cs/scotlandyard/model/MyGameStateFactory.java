package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
    @Nonnull
    @Override
    public GameState build(
            GameSetup setup,
            Player mrX,
            ImmutableList<Player> detectives) {
        // TODO
        return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

    }

    private final class MyGameState implements GameState {

        private final Player mrX;
        private final ImmutableList<Player> detectives;
        private GameSetup setup;
        private ImmutableSet<Piece> remaining;
        private ImmutableList<LogEntry> log;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner;

        private MyGameState(
                @Nonnull final GameSetup setup,
                @Nonnull final ImmutableSet<Piece> remaining,
                @Nonnull final ImmutableList<LogEntry> log,
                final Player mrX,
                final ImmutableList<Player> detectives) {
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
                for (int j = 0; j < detectives.size(); j++) {
                    if ((i != j) && (detectives.get(i).location() == detectives.get(j).location())) {
                        throw new IllegalArgumentException("Duplicate detectives!");
                    }
                }
                if (detectives.get(i).has(ScotlandYard.Ticket.DOUBLE))
                    throw new IllegalArgumentException("Detective has a double ticket!");
                if (detectives.get(i).has(ScotlandYard.Ticket.SECRET))
                    throw new IllegalArgumentException("Detective has a secret ticket!");
            }
            if (detectives.contains(null)) throw new NullPointerException("Some detectives are null!");


            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;

            ImmutableSet.Builder<Move> moveBuilder = new ImmutableSet.Builder<>();
            for (Player detective : detectives) {
                moveBuilder.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
            }
            moveBuilder.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
            if (log.size() < setup.moves.size() - 1) { // double moves are only available if there are at least 2 moves left
                moveBuilder.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
            }
            moves = moveBuilder.build();

            skipDetectivesWhoCantMove();

            this.winner = calculateWinner();
        }

        private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            Set<Move.SingleMove> singleMoves = new HashSet<>();
            for (int destination : setup.graph.adjacentNodes(source)) {
                if (detectives.stream().noneMatch(d -> d.location() == destination)) {
                    for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                        if (player.has(t.requiredTicket())) {
                            singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
                        }
                        if (player.has(ScotlandYard.Ticket.SECRET)) {
                            singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
                        }
                    }

                }

            }
            return singleMoves;
        }

        private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            if (!player.has(ScotlandYard.Ticket.DOUBLE)) {
                return Set.of();
            }
            Set<Move.DoubleMove> doubleMoves = new HashSet<>();
            for (int destination : setup.graph.adjacentNodes(source)) {
                if (detectives.stream().anyMatch(detective -> detective.location() == destination)) {
                    continue; // can't move to a location occupied by a detective
                }
                for (ScotlandYard.Transport t1 : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                    if (!player.has(t1.requiredTicket()) && !player.has(ScotlandYard.Ticket.SECRET)) {
                        continue;  // short circuit if neither ticket is available, slight optimisation
                    }
                    for (int destination2 : setup.graph.adjacentNodes(destination)) {
                        if (detectives.stream().anyMatch(d -> d.location() == destination2)) {
                            continue; // can't move to a location occupied by a detective
                        }
                        for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination, destination2, ImmutableSet.of())) {
                            doubleMoves.addAll(makeDoubleMoveCombinations(player, source, t1.requiredTicket(), destination, t2.requiredTicket(), destination2));
                        }
                    }
                }
            }
            return doubleMoves;
        }

        private static Set<Move.DoubleMove> makeDoubleMoveCombinations(Player player, int source, ScotlandYard.Ticket ticket1, int destination, ScotlandYard.Ticket ticket2, int destination2) {
            Set<Move.DoubleMove> doubleMoves = new HashSet<>();

            if (ticket1 == ticket2) { // special case if both tickets are the same
                if (player.hasAtLeast(ticket1, 2)) {
                    doubleMoves.add(new Move.DoubleMove(player.piece(), source, ticket1, destination, ticket2, destination2));
                }
            } else if (player.has(ticket1) && player.has(ticket2)) {
                doubleMoves.add(new Move.DoubleMove(player.piece(), source, ticket1, destination, ticket2, destination2));
            }

            if (player.has(ticket1) && player.has(ScotlandYard.Ticket.SECRET)) {
                doubleMoves.add(new Move.DoubleMove(player.piece(), source, ticket1, destination, ScotlandYard.Ticket.SECRET, destination2));
            }
            if (player.has(ScotlandYard.Ticket.SECRET) && player.has(ticket2)) {
                doubleMoves.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination, ticket2, destination2));
            }
            if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2)) {
                doubleMoves.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination, ScotlandYard.Ticket.SECRET, destination2));
            }
            return doubleMoves;
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
            return winner;
        }

        private ImmutableSet<Piece> calculateWinner() {
            // detective win case 1, detectives finish a move on the same station as Mr X
            if (detectives.stream().anyMatch(detective -> detective.location() == mrX.location())) {
                return detectives.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet());
            }
            // detective win case 2, no free locations for Mr X to move to
            if (moves.stream().filter(move -> move.commencedBy().equals(mrX.piece())).findAny().isEmpty()) {
                return detectives.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet());
            }
            // mr x win case 1, log filled and detectives can't catch with final moves
            if (log.size() == setup.moves.size()) {
                return ImmutableSet.of(mrX.piece());
            }

            // detectives can no longer move
            if (detectives.stream().flatMap(detective -> moves.stream().filter(move -> move.commencedBy().equals(detective.piece()))).findAny().isEmpty()) {
                return ImmutableSet.of(mrX.piece());
            }
            return ImmutableSet.of();
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            if (!getWinner().isEmpty()) {
                return ImmutableSet.of();
            }
            return moves
                    .stream()
                    .filter(move -> remaining.contains(move.commencedBy()))
                    .collect(ImmutableSet.toImmutableSet());
        }

        @Nonnull
        @Override
        public GameState advance(Move move) {
            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
            if (move.commencedBy().isMrX()) {
                return move.accept(new Move.Visitor<>() {
                    @Override
                    public GameState visit(Move.SingleMove move) {
                        boolean revealMove = setup.moves.get(log.size());
                        LogEntry moveLog = revealMove ? LogEntry.reveal(move.ticket, move.destination) : LogEntry.hidden(move.ticket);
                        ImmutableList<LogEntry> newLog = ImmutableList.<LogEntry>builder().addAll(log).add(moveLog).build();
                        Player newMrX = mrX.use(move.ticket).at(move.destination);
                        ImmutableSet<Piece> newRemaining = calculateNewRemaining(remaining, move.commencedBy());
                        return new MyGameState(setup, newRemaining, newLog, newMrX, detectives);
                    }

                    @Override
                    public GameState visit(Move.DoubleMove move) {
                        boolean revealMove1 = setup.moves.get(log.size());
                        LogEntry moveLog = revealMove1 ? LogEntry.reveal(move.ticket1, move.destination1) : LogEntry.hidden(move.ticket1);
                        ImmutableList<LogEntry> newLog = ImmutableList.<LogEntry>builder().addAll(log).add(moveLog).build();
                        Player newMrX = mrX.use(move.ticket1).at(move.destination1);

                        var state1 = new MyGameState(setup, remaining, newLog, newMrX, detectives);
                        boolean revealMove2 = setup.moves.get(state1.log.size());
                        LogEntry moveLog2 = revealMove2 ? LogEntry.reveal(move.ticket2, move.destination2) : LogEntry.hidden(move.ticket2);
                        ImmutableList<LogEntry> newLog2 = ImmutableList.<LogEntry>builder().addAll(state1.log).add(moveLog2).build();
                        ImmutableSet<Piece> newRemaining2 = calculateNewRemaining(state1.remaining, move.commencedBy());
                        Player newMrX2 = state1.mrX.use(move.ticket2).use(ScotlandYard.Ticket.DOUBLE).at(move.destination2);
                        return new MyGameState(setup, newRemaining2, newLog2, newMrX2, detectives);
                    }
                });
            } else {
                return move.accept(new Move.Visitor<>() {
                    @Override
                    public GameState visit(Move.SingleMove move) {
                        Player matchingPlayer = detectives
                                .stream()
                                .filter(p -> p.piece().equals(move.commencedBy()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("No player for move " + move));

                        matchingPlayer = matchingPlayer.at(move.destination).use(move.ticket);
                        Player newMrX = mrX.give(move.ticket);
                        ImmutableSet<Piece> newRemaining = calculateNewRemaining(remaining, move.commencedBy());
                        ImmutableList.Builder<Player> newDetectivesBuilder = new ImmutableList.Builder<>();
                        for (Player detective : detectives) {
                            if (detective.piece().equals(move.commencedBy())) {
                                newDetectivesBuilder.add(matchingPlayer);
                            } else {
                                newDetectivesBuilder.add(detective);
                            }
                        }
                        return new MyGameState(setup, newRemaining, log, newMrX, newDetectivesBuilder.build());
                    }

                    @Override
                    public GameState visit(Move.DoubleMove move) {
                        throw new IllegalStateException("Only Mr X can do double moves!");
                    }
                });
            }
        }

        private void skipDetectivesWhoCantMove() {
            for (Player detective : detectives) {
                if (detective.tickets().isEmpty() || moves.stream().filter(move -> move.commencedBy().equals(detective.piece())).findAny().isEmpty()) {
                    remaining = Sets.difference(remaining, ImmutableSet.of(detective.piece())).immutableCopy();
                }
            }
            if (remaining.isEmpty()) remaining = ImmutableSet.of(mrX.piece());
        }

        /**
         * Calculates the new set for {@link MyGameState#remaining}, removing an element or refreshing the set based on the following criteria:
         *
         * @param current The current set of {@link MyGameState#remaining}
         * @param without The piece that just played who should be removed from the set
         * @return The new set
         */
        private ImmutableSet<Piece> calculateNewRemaining(ImmutableSet<Piece> current, Piece without) {
            if (current.equals(Set.of(mrX.piece()))) {
                return detectives.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet()); //refresh the remaining to all the detectives
            }

            if (current.isEmpty()) {
                return ImmutableSet.of(mrX.piece()); //refresh the remaining to just Mr X
            }

            return Sets.difference(remaining, ImmutableSet.of(without)).immutableCopy();
        }

    }

}
