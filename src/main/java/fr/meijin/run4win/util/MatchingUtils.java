package fr.meijin.run4win.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.meijin.run4win.model.Game;
import fr.meijin.run4win.model.Player;
import fr.meijin.run4win.model.Round;

public class MatchingUtils {

public static boolean matches (int roundNumber, List<Player> toMatch, List<Player> matchedPlayers,List<Round> lastRounds, List<Game> matched) {
		
		boolean foundMatch = false;
		
		while(toMatch.size()>0){
			
			foundMatch = false;
			
			for(int i = 0; i < toMatch.size(); i++){
				for(int j = 0; j < toMatch.size(); j++ ){
					if(i < toMatch.size() && j < toMatch.size()){
						if(i != j && !havePlayedEachOther(lastRounds, toMatch.get(j), toMatch.get(i))){
							Player p1 = toMatch.get(i);
							Player p2 = toMatch.get(j);
							Game g  = createMatchGame(roundNumber, lastRounds, p1, p2, matched);
							matchedPlayers.add(p1);
							matchedPlayers.add(p2);
							
							toMatch.remove(p1);
							toMatch.remove(p2);
							
							matched.add(g);
							foundMatch = true;
							System.out.println("Matched : "+g.player1.nickname+" ["+g.player1.getPrestige()+"] vs "+g.player2.nickname+" ["+g.player2.getPrestige()+"] on "+g.tableNumber);
							break;
						}
					}
				}
				if(foundMatch)
					break;
			}
			
			if(!foundMatch) return foundMatch;
		}
		
		return foundMatch;
	}
	
	public static Round doSingleMatch(int roundNumber, List<Round> lastRounds, List<Player> toMatch, List<Game> matched){
		Round round = new Round();
		round.roundNumber = roundNumber;
		
		List<Player> matchedPlayers = new ArrayList<Player>();
		
		boolean foundMatch = false;
		int pass = 0;
		while (!foundMatch){
			foundMatch = matches(roundNumber, toMatch, matchedPlayers, lastRounds, matched);
			
			if(pass < matched.size()) pass++;
			
			if(!foundMatch){
				System.out.println("Error in matching, splitting and re-matching from sublist !");
				int split = 0;
				if(matched.size() % 2 == 0){
					split = matched.size() / 2;
				} else {
					split = (matched.size() / 2)-pass;
				}
				
				System.out.println("Splitting from "+split);
				List<Game> matchedGames = matched.subList(0, split);
				
				
				matchedPlayers.addAll(toMatch);
				toMatch.clear();
				
				List<Player> sublistMatchedPlayers = new ArrayList<Player>();
				for(Game g : matchedGames){
					matchedPlayers.remove(g.player1);
					matchedPlayers.remove(g.player2);
					sublistMatchedPlayers.add(g.player1);
					sublistMatchedPlayers.add(g.player2);
				}
				
				matched = matchedGames;
				toMatch = matchedPlayers;
				matchedPlayers = sublistMatchedPlayers;
				Collections.reverse(toMatch);
				System.out.println("Size of matched game "+matched.size());
				System.out.println("#players to match "+toMatch.size());
				System.out.println("#matched players "+matchedPlayers.size());
			}
		}
		
		round.games = matched;
		return round;
	}
	
	private static Game createMatchGame(int roundNumber, List<Round> lastRounds, Player p1, Player p2, List<Game> matched) {
		Game sg = new Game();
		sg.player1 = p1;
		sg.player2 = p2;
		sg.roundNumber = roundNumber;
		
		int tableNumber = 1;
		for(Game g : matched){
			System.out.println("Table Number = "+g.tableNumber);
			if(g.tableNumber == tableNumber)
				tableNumber++;
			else
				break;
		}
		
		sg.tableNumber = tableNumber;
		
		if(sg.player1.forfeit){
			sg.p1Result.resultCorporation = 0;
			sg.p1Result.resultRunner = 0;
			sg.p2Result.resultCorporation = 10;
			sg.p2Result.resultRunner = 10;
		}else if (sg.player2.forfeit){
			sg.p2Result.resultCorporation = 0;
			sg.p2Result.resultRunner = 0;
			sg.p1Result.resultCorporation = 10;
			sg.p1Result.resultRunner = 10;
		}

		return sg;
	}

	private static boolean havePlayedEachOther(List<Round> lastRounds, Player p1, Player p2){
		for(Round r : lastRounds){
			for(Game g : r.games){
				if(g.player1 == p1 && g.player2 == p2 || g.player1 == p2 && g.player2 == p1)
					return true;
			}
		}
		return false;
	}
}