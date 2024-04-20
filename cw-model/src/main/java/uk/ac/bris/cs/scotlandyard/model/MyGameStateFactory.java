package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.*;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
        private List<Player> detectives;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner;

        private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            HashSet<Move.SingleMove> singleMoves = new HashSet<Move.SingleMove>();

            for (int destination : setup.graph.adjacentNodes(source)) {
                if (detectives.stream().noneMatch(d -> d.location() == destination)) {
                    for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                        if (player.has(t.requiredTicket())) {
                            Move.SingleMove move = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
                            singleMoves.add(move);
                        }
                    }

                    if (player.isMrX() && player.has(ScotlandYard.Ticket.SECRET)) {
                        Move.SingleMove move = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
                        singleMoves.add(move);
                    }
                }
            }

            return singleMoves;
        }

        private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            Set<Move.DoubleMove> doubleMoves = new HashSet<Move.DoubleMove>();

            // Validate if double moves can be made.
            if (!player.has(ScotlandYard.Ticket.DOUBLE)) return doubleMoves;
            if (setup.moves.size() < 2) return doubleMoves;

            Set<Move.SingleMove> singleMoves1 = makeSingleMoves(setup, detectives, player, source);
            for (Move.SingleMove sMove1 : singleMoves1) {
                int newSource = sMove1.destination;
                Set<Move.SingleMove> singleMoves2 = makeSingleMoves(setup, detectives, player, newSource);
                for (Move.SingleMove sMove2 : singleMoves2) {
                    // Check if tickets are repeated, if not then continue, if they are then also check there's at least 2.
                    if (sMove1.ticket != sMove2.ticket
                            || sMove1.ticket == sMove2.ticket && player.hasAtLeast(sMove1.ticket, 2)) {
                        Move.DoubleMove dMove = new Move.DoubleMove(
                                player.piece(), source, sMove1.ticket, sMove1.destination, sMove2.ticket, sMove2.destination
                        );
                        doubleMoves.add(dMove);
                    }
                }
            }

            return doubleMoves;
        }

        private void validateConstructor() {
            if (setup == null) throw new NullPointerException("Setup is null!");
            if (mrX == null) throw new NullPointerException("MrX is null!");
            if (detectives == null) throw new NullPointerException("Detectives is null!");

            Set<Piece> seenPieces = new HashSet<Piece>();
            Set<Integer> seenLocations = new HashSet<Integer>();
            for (Player d : detectives) {
                if (d == null) throw new NullPointerException("At least one detective is null!");
                if (d.isMrX()) throw new IllegalArgumentException("MrX cannot be a detective!");

                if (seenPieces.contains(d.piece()))
                    throw new IllegalArgumentException("More than one detective repeated!");
                seenPieces.add(d.piece());
                if (seenLocations.contains(d.location()))
                    throw new IllegalArgumentException("More than one detective is in the same position!");
                seenLocations.add(d.location());

                if (d.has(ScotlandYard.Ticket.SECRET) || d.has(ScotlandYard.Ticket.DOUBLE))
                    throw new IllegalArgumentException("A detective should not have a secret or double ticket!");
            }

            if (!mrX.isMrX()) throw new IllegalArgumentException("There was no MrX!");
            if (mrX.isDetective()) throw new IllegalArgumentException("MrX is a detective!");

            if (setup.moves.isEmpty()) throw new IllegalArgumentException("The moves are empty!");
            if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("The Graph is empty!");
        }

        private void initMoves() {
            // Initialise all available moves.
            ImmutableSet.Builder<Move> pMoves = ImmutableSet.builder();
            if (remaining.contains(Piece.MrX.MRX)) {
                pMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
                pMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
            } else {
                for (Player detective : detectives)
                    pMoves.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
            }

            this.moves = pMoves.build();
        }

        private boolean checkDetectivesCanMove() {
            return detectives.stream().anyMatch(d -> !makeSingleMoves(setup, detectives, d, d.location()).isEmpty());
        }

        private boolean checkMrXCornered() {
            Set<Integer> detectiveLocations = detectives.stream().map(d -> d.location()).collect(Collectors.toSet());

            return makeSingleMoves(setup, detectives, mrX, mrX.location()).stream().allMatch(m -> detectiveLocations.contains(m.destination));
        }

        private ImmutableSet<Piece> setupWinner() {
            ImmutableSet<Piece> detectivePieces = ImmutableSet.copyOf(detectives.stream().map(d -> d.piece()).toList());
            ImmutableSet<Piece> mrXPiece = ImmutableSet.of(mrX.piece());

            if (detectives.stream().anyMatch(d -> d.location() == mrX.location())
            || (getAvailableMoves().isEmpty() && remaining.contains(mrX.piece()))
            || (checkMrXCornered() && remaining.contains(mrX.piece()))) {
                moves = ImmutableSet.of();
                return detectivePieces;
            } else if (!checkDetectivesCanMove() || setup.moves.size() == log.size() && remaining.contains(mrX.piece())) {
                moves = ImmutableSet.of();
                return mrXPiece;
            } else {
                return ImmutableSet.of();
            }
        }

        private MyGameState(
                final GameSetup setup,
                final ImmutableSet<Piece> remaining,
                final ImmutableList<LogEntry> log,
                final Player mrX,
                final List<Player> detectives
        ) {
            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;
            this.winner = setupWinner();

            validateConstructor();
            initMoves();
        }

        @Nonnull
        @Override
        public GameSetup getSetup() {
            return setup;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            Set<Piece> piecesSet = new HashSet<>();
            piecesSet.add(mrX.piece());

            // Map detective 'Players' to detective 'Pieces'.
            List<Piece> detectivePieces = detectives.stream().map(d -> d.piece()).toList();
            piecesSet.addAll(detectivePieces);

            return ImmutableSet.copyOf(piecesSet);
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            for (Player d : detectives) {
                if (d.piece() == detective) return Optional.of(d.location());
            }
            return Optional.empty();
        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            // Find the detective to match the piece (if available).
            Optional<Player> player = detectives.stream().filter(d -> d.piece() == piece).findFirst();
            if (piece.isMrX()) player = Optional.of(mrX);

            if (player.isEmpty()) return Optional.empty();
            Player concreteP = player.get();

            ImmutableMap<ScotlandYard.Ticket, Integer> tickets = concreteP.tickets();
            // Lambda to define the one function that a ticket board has.
            TicketBoard tBoard = ticket -> tickets.get(ticket);

            return Optional.of(tBoard);
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

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            // Extract only moves where player hasn't moved;
            ImmutableSet.Builder<Move> availableMovesB = ImmutableSet.builder();
            availableMovesB.addAll(moves.stream().filter(m -> remaining.contains(m.commencedBy())).toList());

            return availableMovesB.build();
        }

        @Nonnull
        @Override
        public GameState advance(Move move) {
            if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
            return move.accept(new MoveVisitor(setup, remaining, log, mrX, detectives));
        }
    }

    @Nonnull
    @Override
    public GameState build(
            GameSetup setup,
            Player mrX,
            ImmutableList<Player> detectives) {
        return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
    }

    @Nonnull
    public GameState build(
            GameSetup setup,
            ImmutableSet<Piece> remaining,
            ImmutableList<LogEntry> log,
            Player mrX,
            ImmutableList<Player> detectives
            ) {
        return new MyGameState(setup, remaining, log, mrX, detectives);
    }
}
