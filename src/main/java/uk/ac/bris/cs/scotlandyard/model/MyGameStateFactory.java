package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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


			if (!mrX.isMrX())
				throw new IllegalArgumentException("There was no MrX!");

			if (mrX.isDetective())
				throw new IllegalArgumentException("MrX is a detective!");

			if(setup.moves.isEmpty()) throw new IllegalArgumentException("The moves are empty!");
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("The Graph is empty!");
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
			// Find the detective to match the piece (optional as may be mrX).
			Optional<Player> player = detectives.stream().filter(d -> d.piece() == piece).findFirst();
			if (piece.isMrX()) player = Optional.of(mrX);

			if (player.isEmpty()) return Optional.empty();
			// We know player is Present.
			Player concreteP = player.get();

			ImmutableMap<ScotlandYard.Ticket, Integer> tickets = concreteP.tickets();

			TicketBoard tBoard = new TicketBoard() {
				@Override
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					return tickets.get(ticket);
				}
			};

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
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
