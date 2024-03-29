package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

public class DetectiveAi {
    private List<Integer> possibleMrXLocations;
    private final ImmutableSet<Piece.Detective> detectives;

    private ImmutableSet<Piece.Detective> initDetectives(Board board) {
        ImmutableSet.Builder<Piece.Detective> detectivesB = ImmutableSet.builder();
        for (Piece piece : board.getPlayers()) {
            if (piece instanceof Piece.Detective detective) {
                detectivesB.add(detective);
            }
        }
        return detectivesB.build();
    }
    public DetectiveAi(Board board) {
        this.possibleMrXLocations = new ArrayList<>(ScotLandYardAi.MRX_START_LOCATIONS);
        this.detectives = initDetectives(board);
    }

    private List<Integer> calcReachableLocations(Board board, Integer location, ScotlandYard.Ticket ticket) {
        List<Integer> locations = new ArrayList<>();
        for (Integer destination : board.getSetup().graph.adjacentNodes(location)) {
            if (detectives.stream().noneMatch(d -> board.getDetectiveLocation(d).equals(Optional.of(destination)))) {
                ImmutableSet<ScotlandYard.Transport> transports = board.getSetup().graph.edgeValueOrDefault(location, destination, ImmutableSet.of());
                if (transports.stream().anyMatch(t -> t.requiredTicket() == ticket))
                    locations.add(destination);
            }
        }
        return locations;
    }

    public void calcPossibleMrXLocations(Board board) {
        ImmutableList<Integer> oldLocations = ImmutableList.copyOf(possibleMrXLocations);
        int moveNum = board.getMrXTravelLog().size();
        LogEntry latestLog = board.getMrXTravelLog().get(moveNum - 1);

        if (board.getSetup().moves.get(moveNum)) {
            if (latestLog.location().isPresent())
                this.possibleMrXLocations = new ArrayList<>(List.of(latestLog.location().get()));
        } else {
            for (Integer location : oldLocations)
                this.possibleMrXLocations.addAll(calcReachableLocations(board, location, latestLog.ticket()));
        }
    }
}
