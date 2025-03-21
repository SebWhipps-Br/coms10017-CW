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
import java.util.stream.Stream;

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
        return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

    }

    private final class MyGameState implements GameState {

        private final Player mrX;
        private final ImmutableList<Player> detectives;
        private final GameSetup setup;
        private final ImmutableList<LogEntry> log;
        private final ImmutableSet<Move> moves;
        private final ImmutableSet<Piece> winner;
        private ImmutableSet<Piece> remaining;

        private MyGameState(
                @Nonnull final GameSetup setup,
                @Nonnull final ImmutableSet<Piece> remaining,
                @Nonnull final ImmutableList<LogEntry> log,
                final Player mrX,
                final ImmutableList<Player> detectives) {

            //Checks: state creation
            if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
            if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
            //Checks: mrX
            if (mrX == null) throw new NullPointerException("MrX is null!");
            //Checks: detectives
            detectiveChecks(detectives);

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

        private void detectiveChecks(ImmutableList<Player> detectives) {
            if (detectives == null) throw new NullPointerException("detectives is null!");
            if (detectives.contains(null)) throw new NullPointerException("Some detectives are null!");
            for (Player detective : detectives) {
                // Ensures that there is exactly one detective in each location occupied by a detective
                if (detectives.stream().filter(x -> x.location() == (detective.location())).toList().size() != 1) throw new IllegalArgumentException("Duplicate detectives!");
                if (detective.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException("Detective has a double ticket!");
                if (detective.has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException("Detective has a secret ticket!");
            }
        }

        private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            return setup.graph.adjacentNodes(source)
                    .stream()
                    .filter(destination -> detectives.stream().noneMatch(d -> d.location() == destination)) // can't move to a detective's location
                    .flatMap(destination -> {
                        //noinspection DataFlowIssue will never be null as we passed a default value
                        return setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())
                                .stream()
                                .flatMap(transport -> {
                                    var normalMove = new Move.SingleMove(player.piece(), source, transport.requiredTicket(), destination);
                                    var secretMove = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
                                    return Stream.of(normalMove, secretMove)
                                            .filter(m -> player.has(m.ticket));
                                });
                    })
                    .collect(Collectors.toSet());
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

            if ((ticket1 == ticket2 && player.hasAtLeast(ticket1, 2))  // special case if both tickets are the same
                    || (ticket1 != ticket2 && player.has(ticket1) && player.has(ticket2))) {
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

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            List<Piece> p = detectives.stream()
                    .map(Player::piece)
                    .collect(Collectors.toList());

            return new ImmutableSet.Builder<Piece>()
                    .add(mrX.piece())
                    .addAll(p)
                    .build();
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            for (Player d : detectives) {
                if (d.piece().equals(detective)) return Optional.of(d.location());
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
            if (!getWinner().isEmpty()) { // game is over, so no moves are available
                return ImmutableSet.of();
            }
            return moves
                    .stream()
                    .filter(move -> remaining.contains(move.commencedBy()))
                    .collect(ImmutableSet.toImmutableSet());
        }

        @Nonnull
        @Override
        public MyGameState advance(Move move) {
            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
            if (move.commencedBy().isMrX()) {
                return visitMrXMove(move);
            } else {
                return visitDetectiveMove(move);
            }
        }

        private MyGameState visitMrXMove(Move move) {
            return move.accept(new Move.Visitor<>() {
                @Override
                public MyGameState visit(Move.SingleMove move) {
                    boolean revealMove = setup.moves.get(log.size()); // whether Mr X should reveal his move
                    LogEntry moveLog = revealMove ? LogEntry.reveal(move.ticket, move.destination) : LogEntry.hidden(move.ticket); // create log entry
                    ImmutableList<LogEntry> newLog = ImmutableList.<LogEntry>builder().addAll(log).add(moveLog).build(); // add log entry to log
                    Player newMrX = mrX.use(move.ticket).at(move.destination); // update Mr X's location and tickets
                    ImmutableSet<Piece> newRemaining = calculateNewRemaining(remaining, move.commencedBy()); // update remaining players
                    return new MyGameState(setup, newRemaining, newLog, newMrX, detectives);
                }

                @Override
                public MyGameState visit(Move.DoubleMove move) {
                        /*
                         Double Moves are basically just 2 single moves, so we can use the SingleMove visitor
                         This requires a slight change to the types, returning MyGameState instead of GameState
                         but the interface hasn't changed so this is fine
                        */
                    var move1 = new Move.SingleMove(move.commencedBy(), move.source(), move.ticket1, move.destination1);
                    var move2 = new Move.SingleMove(move.commencedBy(), move.destination1, move.ticket2, move.destination2);
                    var newState = advance(move1).advance(move2);

                    var newMrX = newState.mrX.use(ScotlandYard.Ticket.DOUBLE); //also use the double ticket
                    return new MyGameState(setup, newState.remaining, newState.log, newMrX, newState.detectives);
                }
            });
        }

        private MyGameState visitDetectiveMove(Move move) {
            return move.accept(new Move.Visitor<>() {
                @Override
                public MyGameState visit(Move.SingleMove move) {
                    Player matchingPlayer = getPlayerForPiece(move.commencedBy())
                            .orElseThrow(() -> new IllegalStateException("No player for move " + move));
                    // figure out what player actually made the move

                    matchingPlayer = matchingPlayer.at(move.destination).use(move.ticket); // update player's location and tickets

                    Player newMrX = mrX.give(move.ticket); // Mr X gets the used ticket back
                    ImmutableSet<Piece> newRemaining = calculateNewRemaining(remaining, move.commencedBy()); // update remaining players

                    // this is a bit messy, but since the Player class is immutable and the detectives list is immutable,
                    // we have to do this to update the detectives list with the updated player object.
                    // The easiest way of doing this is just a loop which compares the pieces of the players
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
                public MyGameState visit(Move.DoubleMove move) {
                    throw new IllegalStateException("Only Mr X can do double moves!");
                }
            });
        }

        /**
         * Finds the Player who "owns" a given piece
         *
         * @param piece the piece to find the owner of
         * @return the owner of the piece if it exists, otherwise an empty optional
         */
        private Optional<Player> getPlayerForPiece(Piece piece) {
            if (piece.isMrX()) {
                return Optional.of(mrX);
            }
            return detectives.stream().filter(detective -> detective.piece().equals(piece)).findFirst();
        }

        /**
         * Updates {@link MyGameState#remaining} to remove any detectives who can't move.
         * This should be done at the start of every move.
         *
         * @implNote Because of how the turns work, we can't use {@link MyGameState#calculateNewRemaining(ImmutableSet, Piece)} here. This creates a bit of mess with very similar empty set checks, but I don't think it's too bad
         */
        private void skipDetectivesWhoCantMove() {
            for (Player detective : detectives) {
                if (detective.tickets().isEmpty() || moves.stream().filter(move -> move.commencedBy().equals(detective.piece())).findAny().isEmpty()) {
                    remaining = Sets.difference(remaining, ImmutableSet.of(detective.piece())).immutableCopy();
                }
            }
            if (remaining.isEmpty()) remaining = ImmutableSet.of(mrX.piece());
        }

        /**
         * Calculates the new set for {@link MyGameState#remaining}, removing an element or refreshing the set.
         *
         * @param current The current set of {@link MyGameState#remaining}
         * @param without The piece that just played who should be removed from the set
         * @return The new set
         */
        private ImmutableSet<Piece> calculateNewRemaining(ImmutableSet<Piece> current, Piece without) {
            // At the start of the round, Mr X is the only one who can move. after he's gone, then any of the detectives can move
            if (current.equals(Set.of(mrX.piece()))) {
                return detectives.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet()); //refresh the remaining to all the detectives
            }

            // If the current set is empty, then we've just removed the last detective, and we should move to the next round, with Mr X taking the first move
            if (current.isEmpty()) {
                return ImmutableSet.of(mrX.piece()); //refresh the remaining to just Mr X
            }

            // Otherwise, we're just removing the piece that just moved
            return Sets.difference(remaining, ImmutableSet.of(without)).immutableCopy();
        }

    }

}
