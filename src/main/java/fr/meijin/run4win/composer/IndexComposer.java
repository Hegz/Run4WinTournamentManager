package fr.meijin.run4win.composer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Include;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Window;

import fr.meijin.run4win.model.Game;
import fr.meijin.run4win.model.Player;
import fr.meijin.run4win.model.PlayerRanking;
import fr.meijin.run4win.model.Ranking;
import fr.meijin.run4win.model.Round;
import fr.meijin.run4win.model.Tournament;
import fr.meijin.run4win.util.MatchingUtils;
import fr.meijin.run4win.util.RankingUtils;
import fr.meijin.run4win.util.TournamentUtils;
import fr.meijin.run4win.util.file.ExportUtils;
import fr.meijin.run4win.util.identity.CorporationIdentityEnum;
import fr.meijin.run4win.util.identity.RunnerIdentityEnum;
import fr.meijin.run4win.util.lang.LangEnum;
import fr.meijin.run4win.util.lang.LangUtils;

public class IndexComposer extends GenericForwardComposer<Div> {

	private static final long serialVersionUID = 5120754564562369429L;
	
	private AnnotateDataBinder binder;
	private Tabbox singleTabbox;
	private Tabbox resultTabbox;
	private Listbox playersList;
	private Grid playersResultsGrid;

	private Combobox tieBreakCombobox;

	public void doAfterCompose(Div div) throws Exception {
		super.doAfterCompose(div);
		binder = new AnnotateDataBinder(div);
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		if(tournament == null){
			tournament = new Tournament();
			session.setAttribute("tournament",tournament);
		}else {
			reloadTabs(tournament);
			reloadPlayerRankingTab(tournament);
		}
		
		page.setAttribute("V2Visibility", (tournament.tieBreak == 1));

		binder.loadAll();
	}
	
	private void deleteTabs (){
		List<Component> componentsToRemove = new ArrayList<Component>();
		for(Component c : page.getFellows()){
			if(c.getId().matches("(round)?(ranking)?[0-9]+(Panel)?(Tab)?(Include)?")){
				componentsToRemove.add(c);
			}
		}
		
		for(Component c : componentsToRemove){
			c.setParent(null);
			c.detach();
			c.setId(null);
			c = null;
		}
	}
	
	/*-------------------------------------------------------------------------------
	 * 								MENU BAR
	 --------------------------------------------------------------------------------*/
	
	public void onClick$addRound (Event e) throws InterruptedException{
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		RankingUtils.updatePlayersRanking(tournament.roundsList, tournament.players);
		boolean addBye = false;
		
		Collections.sort(tournament.players);
		if(tournament.rounds >= 1){
			Ranking ranking = new Ranking();
			ranking.roundNumber = tournament.rounds;
			for(Player p : tournament.players){
				PlayerRanking pr = new PlayerRanking();
				pr.nickname = p.nickname;
				pr.prestige = p.getPrestige();
				pr.weakestSideWins = p.getWeakestSideWins();
				pr.opponentsStrength = p.getOpponentsStrength();
				pr.opponentsPoints = p.getOpponentsPoints();
				pr.points = p.getPoints();
				ranking.playerRankings.add(pr);
			}
			
			addResultTab(ranking);
			
			tournament.rankings.add(ranking);
		}
		
		Collections.sort(tournament.players);
		
		List<Player> toMatch = new ArrayList<Player>(tournament.players);
		
		List<Player> forfeitPlayers = new ArrayList<Player>();
		for(Player p : toMatch){
			System.out.println("Player to match "+p.nickname);
			if (p.forfeit && p.id !=0)
				forfeitPlayers.add(p);
		}
		
		toMatch.removeAll(forfeitPlayers);

		if(toMatch.size()%2 == 1){
			int rep = Messagebox.show(LangUtils.getMessage(LangEnum.CREATE_BYE), LangUtils.getMessage(LangEnum.WARNING), Messagebox.YES | Messagebox.NO, Messagebox.EXCLAMATION);
			if(rep == Messagebox.NO){
				return;
			} else {
				addBye = true;
			}
		} else if (tournament.players.isEmpty()){
			Messagebox.show(LangUtils.getMessage(LangEnum.NO_PLAYERS), LangUtils.getMessage(LangEnum.ERROR), Messagebox.OK, Messagebox.ERROR);
			return;
		}
		
		if(addBye){
			Player p = new Player();
			p.id=0;
			p.nickname = "BYE";
			p.forfeit = true;
			p.idCorporation = CorporationIdentityEnum.BLANK;
			p.idRunner = RunnerIdentityEnum.BLANK;
			toMatch.add(p);
		}
		
		tournament.rounds++;
		
		
		Collections.sort(toMatch);
			
		if(tournament.rounds == 1)
			Collections.shuffle(toMatch);
		
		Round r = MatchingUtils.doSingleMatch(tournament.rounds,tournament.roundsList, toMatch, new ArrayList<Game>());
		tournament.roundsList.add(r);
		
		Tab tab = addRoundTab(r);
		
		session.setAttribute("tournament", tournament);
		tab.setSelected(true);
		binder.loadAll();
	}
	
	public void onClick$htmlDownload (Event e) throws Exception{
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		RankingUtils.updatePlayersRanking(tournament.roundsList, tournament.players);
		File exportFile = ExportUtils.exportAsHTML(tournament);
		Filedownload.save(exportFile, "text/plain");
	}
	
	public void onClick$jsonDownload (Event e) throws Exception{
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		RankingUtils.updatePlayersRanking(tournament.roundsList, tournament.players);
		File exportFile = ExportUtils.exportAsJSON(tournament);
		Filedownload.save(exportFile, "text/plain");
	}
	
	public void onClick$csvDownloadRanking (Event e) throws Exception{
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		RankingUtils.updatePlayersRanking(tournament.roundsList, tournament.players);
		File exportFile = ExportUtils.exportRankingAsCSV(tournament);
		Filedownload.save(exportFile, "text/csv");
	}
	
	public void onClick$csvDownloadRounds (Event e) throws Exception{
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		File exportFile = ExportUtils.exportRoundsAsCSV(tournament);
		Filedownload.save(exportFile, "text/csv");
	}

	public void onClick$newTournament (Event e){
		int ret = Messagebox.show(LangUtils.getMessage(LangEnum.RESET), "Reset", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION);
		
		if(ret == Messagebox.YES){
			Tournament t = new Tournament();
			session.setAttribute("tournament", t);
			deleteTabs();
		}
		((Tab) page.getFellowIfAny("playerTab")).setSelected(true);
		
		binder.loadAll();
	}
	
	public void onClick$fileSave (Event e) throws Exception {
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		File exportFile = ExportUtils.exportAsR4W(tournament);
		Filedownload.save(exportFile, "text/plain");
	}
	
	public void onUpload$fileOpen (UploadEvent e) throws Exception {
		Media m = e.getMedia();
		if(m != null){
			ObjectInputStream ois = new ObjectInputStream(m.getStreamData());
			Tournament tournament = (Tournament) ois.readObject();
			deleteTabs();
			tournament = reloadTabs(tournament);
			tournament = reloadPlayerRankingTab(tournament);
			session.setAttribute("tournament",tournament);
			ois.close();
		}
		
		binder.loadAll();
	}
	
	/*-------------------------------------------------------------------------------
	 * 								Result Tab
	 --------------------------------------------------------------------------------*/
	
	public void onClick$resultTab (Event e){
		
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		RankingUtils.updatePlayersRanking(tournament.roundsList, tournament.players);
		Collections.sort(tournament.players);
		session.setAttribute("tournament",tournament);
		playersResultsGrid.invalidate();
		playersResultsGrid.renderAll();
		binder.loadComponent(playersResultsGrid);
	}
	
	public void onSelect$tieBreakCombobox(Event e){
		int tieBreak = Integer.parseInt((String) tieBreakCombobox.getSelectedItem().getValue());
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		
		tournament.tieBreak = tieBreak;
		
		for(Player p : tournament.players){
			p.tieBreak = tieBreak;
		}
		
		page.setAttribute("V2Visibility", (tieBreak == 1));
		TournamentUtils.reloadRanking(tournament);
		
		deleteTabs();
		tournament = reloadTabs(tournament);
		tournament = reloadPlayerRankingTab(tournament);
		session.setAttribute("tournament", tournament);
		binder.loadAll();
	}
	
	private Tournament reloadPlayerRankingTab(Tournament tournament) {
		if(tournament.rankings.size() != 0){
			for(Ranking r : tournament.rankings){
				System.out.println("Reloading player ranking on round "+r.roundNumber);
				addResultTab(r);
			}
		}
		return tournament;
	}

	private void addResultTab(Ranking ranking){
		System.out.println("Adding result for round "+ranking.roundNumber);
		Tab tab = new Tab(LangUtils.getMessage(LangEnum.RESULT)+" "+ranking.roundNumber);
		tab.setId("ranking"+ranking.roundNumber+"Tab");
		
		tab.setParent(resultTabbox.getTabs());
		Tabpanel panel = new Tabpanel();
		panel.setId("ranking"+ranking.roundNumber+"Panel");
		panel.setParent(resultTabbox.getTabpanels());
		
		Include inc = new Include("ranking.zul");
		inc.setDynamicProperty("ranking", ranking);
		inc.setId("ranking"+ranking.roundNumber+"Include");
		inc.setParent(panel);
	}
	
	/*-------------------------------------------------------------------------------
	 * 								Player Tab
	 --------------------------------------------------------------------------------*/
	
	public void onClick$addPlayerButton(Event e) throws Exception {
		Window window = (Window) Executions.createComponents("add_player.zul", null, null);
		window.doModal();
		onPlayerChange();
	}
	
	public void onEditPlayer(Event e){
		Player oldPlayer = (Player) e.getData();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("oldPlayer", oldPlayer);
		Window window = (Window) Executions.createComponents("add_player.zul", null, map);
		window.doModal();
		onPlayerChange();
		page.removeAttribute("oldPlayer");
	}
	
	private void onPlayerChange() {
		Player newPlayer = (Player) page.getAttribute("newPlayer");
		
		if(newPlayer != null){
			Tournament tournament = (Tournament) session.getAttribute("tournament");
			if(tournament.players.isEmpty()){
				tournament.players.add(newPlayer);
			} else {
				
				int indexToremove = -1;
				for(Player p : tournament.players){
					if(p.id == newPlayer.id){
						indexToremove = tournament.players.indexOf(p);
					}
				}
				
				if(indexToremove >= 0){
					tournament.players.remove(indexToremove);
					tournament.players.add(indexToremove, newPlayer);
				} else {
					tournament.players.add(newPlayer);
				}
				
				page.removeAttribute("newPlayer");
				session.setAttribute("tournament", tournament);
				playersList.renderAll();
			}
		}
		binder.loadAll();
	}
	
	public void onDeletePlayer(Event e){
		Player p = (Player) e.getData();
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		tournament.players.remove(p);
		tournament.playersCount--;
		session.setAttribute("tournament", tournament);
		playersList.renderAll();
		binder.loadAll();
	}
	
	/*-------------------------------------------------------------------------------
	 * 								Round Tabs
	 --------------------------------------------------------------------------------*/
	
	public void onDeleteRound(Event e){
		Tournament tournament = (Tournament) session.getAttribute("tournament");
		
		Integer roundToRemove = (Integer) e.getData();
		if(roundToRemove > 0){
			
			int index = -1;
			for(Round r : tournament.roundsList){
				if (r.roundNumber == roundToRemove)
					index = tournament.roundsList.indexOf(r);
			}
			
			if (index != -1)
				tournament.roundsList.remove(index);
			tournament.rounds--;
			
			int i = 1;
			for(Round r : tournament.roundsList){
				r.roundNumber = i++;
				for(Game g : r.games){
					g.roundNumber = r.roundNumber;
				}
			}
			
			RankingUtils.updatePlayersRanking(tournament.roundsList, tournament.players);

			
		}

		deleteTabs();
		reloadTabs(tournament);
		TournamentUtils.reloadRanking(tournament);
		reloadPlayerRankingTab(tournament);
		
		session.setAttribute("tournament", tournament);
		((Tab) page.getFellowIfAny("playerTab")).setSelected(true);
		
		binder.loadAll();
	}
	
	private Tournament reloadTabs (Tournament tournament){
		if(tournament.roundsList.size()  != 0){
			for(Round r : tournament.roundsList){
				addRoundTab(r);
			}
		}
		return tournament;
	}
	
	private Tab addRoundTab(Round r) {
		Tab tab = new Tab(LangUtils.getMessage(LangEnum.ROUND)+" "+r.roundNumber);
		tab.setId("round"+r.roundNumber+"Tab");
		tab.setParent(singleTabbox.getTabs());
		
		Tabpanel panel = new Tabpanel();
		panel.setId("round"+r.roundNumber+"Panel");
		panel.setParent(singleTabbox.getTabpanels());
		
		Include inc = new Include("single_round.zul");
		inc.setDynamicProperty("round", r);
		inc.setId("round"+r.roundNumber+"Include");
		inc.setParent(panel);
		
		return tab;
	}
}
