package fr.meijin.run4win.model;

import java.io.Serializable;


public class Game implements Comparable<Game>,Serializable {
	
	private static final long serialVersionUID = -2454932855638944686L;

	public int roundNumber;

	public int tableNumber;
	
	public Player player1;
	
	public Player player2;
	
	public Result p1Result;
	
	public Result p2Result;
	
	public Game () {
		p1Result = new Result();
		p2Result = new Result();
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public void setRoundNumber(int roundNumber) {
		this.roundNumber = roundNumber;
	}

	public void setTableNumber(int tableNumber) {
		this.tableNumber = tableNumber;
	}

	public void setPlayer1(Player player1) {
		this.player1 = player1;
	}

	public void setPlayer2(Player player2) {
		this.player2 = player2;
	}

	public void setP1Result(Result p1Result) {
		this.p1Result = p1Result;
	}

	public void setP2Result(Result p2Result) {
		this.p2Result = p2Result;
	}

	public int getTableNumber() {
		return tableNumber;
	}

	public Player getPlayer1() {
		return player1;
	}

	public Player getPlayer2() {
		return player2;
	}

	public Result getP1Result() {
		return p1Result;
	}

	public Result getP2Result() {
		return p2Result;
	}

	@Override
	public int compareTo(Game g) {
		Integer thisTable = this.tableNumber;
		Integer gTable = g.tableNumber;
		return thisTable.compareTo(gTable);
	}

}
