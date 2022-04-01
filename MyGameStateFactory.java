package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		// TODO
		if (mrX.equals(null)) throw new NullPointerException("MrX cannot be Null");
		if (detectives.contains(null)) throw new NullPointerException("Detectives cannot be Null");
		if (!mrX.isMrX()) throw new IllegalArgumentException("There is no MrX");
		if (detectives.stream().anyMatch(x -> x.isMrX())) throw new IllegalArgumentException("There are multiple MrX");
		for (Player x : detectives){
			int counter = 0;
			if (x.hasAtLeast(ScotlandYard.Ticket.SECRET, 1)) throw new IllegalArgumentException("Detectives cannot have secret tickets");
			if (x.hasAtLeast(ScotlandYard.Ticket.DOUBLE, 1)) throw new IllegalArgumentException("Detectives cannot have double tickets");
			for (int i = 0 ; i < detectives.size() ; i++){
				if (detectives.get(i).equals(x)) counter += 1;
				if (detectives.get(i).location() == x.location() && x!=detectives.get(i)) {
					throw new IllegalArgumentException("Detectives in same location");
				}
			}
			if (counter > 1) throw new IllegalArgumentException("There are duplicate detectives");
		}
		if (setup.moves.isEmpty()) throw new IllegalArgumentException("The given moves is empty");
		if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("The graph is empty");
		class MyTicketBoard implements Board.TicketBoard {
			public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
				return ticket.hashCode();
			}
		}
		final class MyGameState implements GameState {
			final private GameSetup setup;
			final private ImmutableSet<Piece> remaining;
			private ImmutableList<LogEntry> log;
			private Player mrX;
			private List<Player> detectives;
			private ImmutableSet<Move> moves;
			private ImmutableSet<Piece> winner;
			private TicketBoard ticketBoard;
			private MyGameState(
					final GameSetup setup,
					final ImmutableSet<Piece> remaining,
					final ImmutableList<LogEntry> log,
					final Player mrX,
					final List<Player> detectives){
				this.setup = setup;
				this.mrX = mrX;
				this.remaining = remaining;
				this.log = log;
				this.detectives = detectives;
			}
			@Override public GameSetup getSetup() {  return setup; }
			@Override  public ImmutableSet<Piece> getPlayers() {
				Set PLAYERS = new HashSet();
				PLAYERS.add(MrX.MRX);
				detectives.stream().forEach(x -> PLAYERS.add(x.piece()));
				return ImmutableSet.copyOf(PLAYERS);
			}
			@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
				if (detectives.isEmpty()) return Optional.of(null);
				var player = detectives.stream()
						.filter(x -> x.piece() == detective)
						.findFirst();
				if (player.isPresent()) return Optional.of(player.get().location());
				else return Optional.empty();
			}
			@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
				Optional<Player> player;
				if (piece.isDetective()){
					player = detectives.stream()
							.filter(x -> x.piece() == piece)
							.findFirst();
				}
				else{
					player = Optional.of(mrX);
				}
				if (player.isPresent()) {
					TicketBoard Tb = new MyTicketBoard() {
						public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
							return player.get().tickets().get(ticket);
						}
					};
					return Optional.of(Tb);
				}
				else {
					return Optional.empty();
				}
			}

			@Override public ImmutableList<LogEntry> getMrXTravelLog(){return log;}

			@Override public ImmutableSet<Piece> getWinner(){
				Set<Piece> Winners = new HashSet<>();
				return ImmutableSet.copyOf(Winners);
			}

			@Override public ImmutableSet<Move> getAvailableMoves(){
				Set<Move> singleMoves = Set.copyOf(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				Set<Move> doubleMoves = Set.copyOf(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
				Set<Move> allMoves = new HashSet<>();
				allMoves.addAll(singleMoves);
				allMoves.addAll(doubleMoves);
				return ImmutableSet.copyOf(allMoves);
			}

			private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
				// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
				Set<SingleMove> allSingleMove= new HashSet<>();
				boolean unoccupied = Boolean.TRUE;
				for(int destination : setup.graph.adjacentNodes(source)) {
					// TODO find out if destination is occupied by a detective
					//  if the location is occupied, don't add to the collection of moves to return
						for (Player x : detectives) {
							if (destination == x.location()) unoccupied = Boolean.FALSE;
						}
						if (unoccupied) {
							for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
								// TODO find out if the player has the required tickets
								//  if it does, construct a SingleMove and add it the collection of moves to return
								if (player.hasAtLeast(t.requiredTicket(), 1)) {
									allSingleMove.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
									if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1)){
										allSingleMove.add(new SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
									}
								}
							}
						}

					// TODO consider the rules of secret moves here
					//  add moves to the destination via a secret ticket if there are any left with the player
				}
				// TODO return the collection of moves
				return allSingleMove;
			}

			private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
				Set<Integer> detectiveLocations = detectives.stream()
						.map(x -> x.location())
						.collect(Collectors.toSet());
				Set<DoubleMove> doubleMoves = new HashSet<>();
				if (player.hasAtLeast(ScotlandYard.Ticket.DOUBLE,1)) {
					for (int destination : setup.graph.adjacentNodes(source)) {
						if (!(detectiveLocations.contains(destination))) {
							for (ScotlandYard.Transport t1 : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
//								if (player.hasAtLeast(t1.requiredTicket(), 1)) {
									for (int destination2 : setup.graph.adjacentNodes(destination)) {
										for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
											if (!(detectiveLocations.contains(destination2))) {
												if (((player.hasAtLeast(t2.requiredTicket(), 1)) && t1.requiredTicket() != t2.requiredTicket()) || (player.hasAtLeast(t2.requiredTicket(), 2)) && t1.requiredTicket() == t2.requiredTicket()) {
													doubleMoves.add(new DoubleMove(player.piece(), source, t1.requiredTicket(), destination, t2.requiredTicket(), destination2));
												}
												if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1) && player.hasAtLeast(t1.requiredTicket(), 1)) {
														doubleMoves.add(new DoubleMove(player.piece(), source, t1.requiredTicket(), destination, ScotlandYard.Ticket.SECRET, destination2));
												}
												if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1) && player.hasAtLeast(t2.requiredTicket(), 1)) {
														doubleMoves.add(new DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination, t2.requiredTicket(), destination2));
												}
												if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2)) {
														doubleMoves.add(new DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination, ScotlandYard.Ticket.SECRET, destination2));
												}
										}
									}
								}
							}
						}
					}
				}
				return doubleMoves;
			}

			@Override public GameState advance(Move move) {

//				throw new IllegalArgumentException("Move is not in available moves");
				return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
			}
		}
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
