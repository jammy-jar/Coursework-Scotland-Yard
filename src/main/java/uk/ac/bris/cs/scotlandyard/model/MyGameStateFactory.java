package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

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

                    if (!player.isDetective() && player.has(ScotlandYard.Ticket.SECRET)) {
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

            validateConstructor();

            // Initialise all available moves.
            Set<Move> pMoves = new HashSet<Move>();
            for (Player detective : detectives)
                    pMoves.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
            pMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
            pMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
            moves = ImmutableSet.copyOf(pMoves);
        }

        private Optional<Player> getPlayerByPiece(Piece piece) {
            // Find the detective to match the piece (if available).
            Optional<Player> player = detectives.stream().filter(d -> d.piece() == piece).findFirst();
            if (piece.isMrX()) player = Optional.of(mrX);

            return player;
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
            Optional<Player> player = getPlayerByPiece(piece);
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
            return null;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            return winner;
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            return moves;
        }

        @Nonnull
        @Override
        public GameState advance(Move move) {
            if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

            Move.Visitor<GameState> visitor = new Move.Visitor<>() {
                @Override
                public GameState visit(Move.SingleMove move) {
                    if (move.commencedBy().isMrX()) {
                        // TODO
                    }
//                    Optional<Player> player = getPlayerByPiece(move.commencedBy());
//                    if (player.isEmpty()) throw new IllegalArgumentException("The piece that commenced the move does not have a corresponding player!");
//                    Player concreteP = player.get();
//
//                    Map<ScotlandYard.Ticket, Integer> tickets = concreteP.tickets();
//                    tickets.put(move.ticket, tickets.get(move.ticket) - 1);
//
//                    Player newPlayer = new Player(concreteP.piece(), ImmutableMap.copyOf(tickets), move.destination);
//                    return new MyGameStateFactory().build(
//                            setup,
//                            newPlayer,
//                            ImmutableList.copyOf(detectives)
//                    );
                    return null;
                }

                @Override
                public GameState visit(Move.DoubleMove move) {
//                    Optional<Player> player = getPlayerByPiece(move.commencedBy());
//                    if (player.isEmpty()) throw new NullPointerException("The piece that commenced the move does not have a corresponding player!");
//                    Player concreteP = player.get();
//
//                    Map<ScotlandYard.Ticket, Integer> tickets = concreteP.tickets();
//                    tickets.put(move.ticket1, tickets.get(move.ticket1) - 1);
//                    tickets.put(move.ticket2, tickets.get(move.ticket2) - 1);
//                    Player newMrX = new Player(mrX.piece(), ImmutableMap.copyOf(tickets), move.destination2);
                    return null;
                }
            };

            return move.accept(visitor);
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
}
