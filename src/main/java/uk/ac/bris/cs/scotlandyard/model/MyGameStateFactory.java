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
            moveBuilder.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
            moves = moveBuilder.build();
        }

        private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            Set<Move.SingleMove> singleMoves = new HashSet<>();
            for (int destination : setup.graph.adjacentNodes(source)) {
                if (detectives.stream().noneMatch(d -> d.location() == destination)) {
                    for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                        if (player.has(t.requiredTicket())) {
                            singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
                        }
                    }
                    if (player.has(ScotlandYard.Ticket.SECRET)) {
                        // TODO secret tickets
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
                    continue;
                }
                for (ScotlandYard.Transport t1 : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                    if (!player.has(t1.requiredTicket())) {
                        continue;
                    }
                    for (int destination2 : setup.graph.adjacentNodes(destination)) {
                        if (detectives.stream().anyMatch(d -> d.location() == destination)) {
                            continue;
                        }
                        for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination, destination2, ImmutableSet.of())) {
                            if (!player.has(t2.requiredTicket())) {
                                continue;
                            }
                            doubleMoves.add(new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination, t2.requiredTicket(), destination2));
                        }
                    }
                }
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
                        ImmutableSet<Piece> newRemaining = calculateNewRemaining(log.isEmpty(), remaining, move.commencedBy());
                        return new MyGameState(setup, newRemaining, newLog, newMrX, detectives);
                    }

                    @Override
                    public GameState visit(Move.DoubleMove move) {
                        Move move1 = new Move.SingleMove(move.commencedBy(), move.source(), move.ticket1, move.destination1);
                        Move move2 = new Move.SingleMove(move.commencedBy(), move.destination1, move.ticket2, move.destination2);

                        GameState withoutDoubleCard = new MyGameState(setup, remaining, log, mrX.use(ScotlandYard.Ticket.DOUBLE), detectives);

                        GameState state1 = withoutDoubleCard.advance(move1);
                        return state1.advance(move2);

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
                        ImmutableSet<Piece> newRemaining = calculateNewRemaining(false, remaining, move.commencedBy());
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

        /**
         * Calculates the new set for {@link MyGameState#remaining}, removing an element or refreshing the set based on the following criteria:
         *
         * @param isRound1 If the just played action was in round 1. This is a special case as Mr X gets one free turn at the start of the game before the detectives
         * @param current  The current set of {@link MyGameState#remaining}
         * @param without  The piece that just played who should be removed from the set
         * @return The new set
         */
        private ImmutableSet<Piece> calculateNewRemaining(boolean isRound1, ImmutableSet<Piece> current, Piece without) {
            if (isRound1) {
                return ImmutableSet.<Piece>builder()
                        .addAll(detectives.stream().map(Player::piece).toList())
                        .build(); //refresh the remaining to all the players except MrX
            }

            if (current.isEmpty()) {
                return ImmutableSet.<Piece>builder()
                        .addAll(getPlayers())
                        .build(); //refresh the remaining to all the players, next round
            }

            if (current.equals(ImmutableSet.of(without))) {
                return ImmutableSet.of(mrX.piece());
            }
            return Sets.difference(remaining, ImmutableSet.of(without)).immutableCopy();
        }

    }

}
