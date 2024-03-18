package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class MoveVisitor implements Move.Visitor<Board.GameState> {

    private GameSetup setup;
    private ImmutableSet<Piece> remaining;
    private ImmutableList<LogEntry> log;
    private Player mrX;
    private List<Player> detectives;

    public MoveVisitor(GameSetup setup, ImmutableSet<Piece> remaining, ImmutableList<LogEntry> log, Player mrX, List<Player> detectives) {
        this.setup = setup;
        this.remaining = remaining;
        this.log = log;
        this.mrX = mrX;
        this.detectives = detectives;
    }

    @Override
    public Board.GameState visit(Move.SingleMove move) {
        // Find the detective to match the piece (if available).
        Optional<Player> playerO = detectives.stream().filter(d -> d.piece() == move.commencedBy()).findFirst();
        if (move.commencedBy().isMrX()) playerO = Optional.of(mrX);

        if (playerO.isEmpty())
            throw new IllegalArgumentException("The piece that commenced the move does not have a corresponding player!");

        Player player = playerO.get();

        int location = move.destination;

        Player newMrX;
        if (move.commencedBy().isMrX())
            newMrX = player.use(move.ticket).at(location);
        else
            newMrX = mrX.give(move.ticket);

        Player newPlayer;
        if (move.commencedBy().isDetective())
            newPlayer = player.use(move.ticket).at(location);
        else newPlayer = newMrX;

        // Remove current player from remaining players.
        ImmutableSet.Builder<Piece> newRemainingB = ImmutableSet.builder();
        // Add all players in remaining exl the one which just played.
        newRemainingB.addAll(remaining.stream().filter(p -> p != move.commencedBy()).toList());
        // If there's no more players swap turns.
        if (remaining.size() - 1 == 0) {
            if (move.commencedBy().isMrX())
                newRemainingB.addAll(detectives.stream().map(d -> d.piece()).toList());
            else
                newRemainingB.add(mrX.piece());
        }

        ImmutableList.Builder<LogEntry> newLogB = ImmutableList.builder();
        newLogB.addAll(log);
        List<Player> newDetectives = new ArrayList<>(detectives);
        if (move.commencedBy().isMrX()) {
            // Move number starts at zero
            int currentMove = log.size();

            LogEntry entry;
            // Check if move is a reveal move.
            if (setup.moves.get(currentMove)) entry = LogEntry.reveal(move.ticket, move.destination);
            else entry = LogEntry.hidden(move.ticket);

            // Add the new log entry.
            newLogB.add(entry);
        } else {
            // Replace detective with updated.
            newDetectives.removeIf(d -> d.piece() == move.commencedBy());
            newDetectives.add(newPlayer);
        }

        return new MyGameStateFactory().build(
                setup,
                newRemainingB.build(),
                newLogB.build(),
                newMrX,
                ImmutableList.copyOf(newDetectives)
        );
    }

    @Override
    public Board.GameState visit(Move.DoubleMove move) {
        if (!move.commencedBy().isMrX())
            throw new IllegalArgumentException("Only MrX can make double moves!");

        Player newMrX = mrX.use(ScotlandYard.Ticket.DOUBLE).use(move.ticket1).use(move.ticket2).at(move.destination2);

        ImmutableSet<Piece> newRemaining = ImmutableSet.copyOf(detectives.stream().map(d -> d.piece()).toList());

        ImmutableList.Builder<LogEntry> newLogB = ImmutableList.builder();
        newLogB.addAll(log);

        // Move number starts at zero
        int currentMove = log.size();

        // Check if move is a reveal move.
        if (setup.moves.get(currentMove)) newLogB.add(LogEntry.reveal(move.ticket1, move.destination1));
        else newLogB.add(LogEntry.hidden(move.ticket1));

        // Check if move is a reveal move.
        if (setup.moves.get(currentMove + 1)) newLogB.add(LogEntry.reveal(move.ticket2, move.destination2));
        else newLogB.add(LogEntry.hidden(move.ticket2));

        return new MyGameStateFactory().build(
                setup,
                newRemaining,
                newLogB.build(),
                newMrX,
                ImmutableList.copyOf(detectives)
        );
    }
}
