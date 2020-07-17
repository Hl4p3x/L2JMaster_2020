/*
 * Copyright (C) 2004-2020 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.instancemanager.AntiFeedManager;
import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * @author DS
 */
public class OlympiadManager
{
	private final List<Integer> _nonClassBasedRegisters;
	private final Map<Integer, List<Integer>> _classBasedRegisters;
	private final List<List<Integer>> _teamsBasedRegisters;
	
	protected OlympiadManager()
	{
		_nonClassBasedRegisters = new CopyOnWriteArrayList<>();
		_classBasedRegisters = new ConcurrentHashMap<>();
		_teamsBasedRegisters = new CopyOnWriteArrayList<>();
	}
	
	public static final OlympiadManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public final List<Integer> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}
	
	public final Map<Integer, List<Integer>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}
	
	public final List<List<Integer>> getRegisteredTeamsBased()
	{
		return _teamsBasedRegisters;
	}
	
	protected final List<List<Integer>> hasEnoughRegisteredClassed()
	{
		List<List<Integer>> result = null;
		for (Map.Entry<Integer, List<Integer>> classList : _classBasedRegisters.entrySet())
		{
			if ((classList.getValue() != null) && (classList.getValue().size() >= Config.ALT_OLY_CLASSED))
			{
				if (result == null)
				{
					result = new CopyOnWriteArrayList<>();
				}
				
				result.add(classList.getValue());
			}
		}
		return result;
	}
	
	protected final boolean hasEnoughRegisteredNonClassed()
	{
		return _nonClassBasedRegisters.size() >= Config.ALT_OLY_NONCLASSED;
	}
	
	protected final boolean hasEnoughRegisteredTeams()
	{
		return _teamsBasedRegisters.size() >= Config.ALT_OLY_TEAMS;
	}
	
	protected final void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
		_teamsBasedRegisters.clear();
		AntiFeedManager.getInstance().clear(AntiFeedManager.OLYMPIAD_ID);
	}
	
	public final boolean isRegistered(L2PcInstance noble)
	{
		return isRegistered(noble, noble, false);
	}
	
	private final boolean isRegistered(L2PcInstance noble, L2PcInstance player, boolean showMessage)
	{
		final Integer objId = Integer.valueOf(noble.getObjectId());
		// party may be already dispersed
		for (List<Integer> team : _teamsBasedRegisters)
		{
			if ((team != null) && team.contains(objId))
			{
				if (showMessage)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_NON_CLASS_LIMITED_EVENT_TEAMS);
					sm.addPcName(noble);
					player.sendPacket(sm);
				}
				return true;
			}
		}
		
		if (_nonClassBasedRegisters.contains(objId))
		{
			if (showMessage)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			return true;
		}
		
		final List<Integer> classed = _classBasedRegisters.get(noble.getBaseClass());
		if ((classed != null) && classed.contains(objId))
		{
			if (showMessage)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			return true;
		}
		
		return false;
	}
	
	public final boolean isRegisteredInComp(L2PcInstance noble)
	{
		return isRegistered(noble, noble, false) || isInCompetition(noble, noble, false);
	}
	
	private final boolean isInCompetition(L2PcInstance noble, L2PcInstance player, boolean showMessage)
	{
		if (!Olympiad._inCompPeriod)
		{
			return false;
		}
		
		AbstractOlympiadGame game;
		for (int i = OlympiadGameManager.getInstance().getNumberOfStadiums(); --i >= 0;)
		{
			game = OlympiadGameManager.getInstance().getOlympiadTask(i).getGame();
			if (game == null)
			{
				continue;
			}
			
			if (game.containsParticipant(noble.getObjectId()))
			{
				if (!showMessage)
				{
					return true;
				}
				
				switch (game.getType())
				{
					case CLASSED:
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
					case NON_CLASSED:
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
					case TEAMS:
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_NON_CLASS_LIMITED_EVENT_TEAMS);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public final boolean registerNoble(L2PcInstance player, CompetitionType type)
	{
		if (!Olympiad._inCompPeriod)
		{
			player.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		
		if (Olympiad.getInstance().getMillisToCompEnd() < 600000)
		{
			player.sendPacket(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			return false;
		}
		
		final int charId = player.getObjectId();
		if (Olympiad.getInstance().getRemainingWeeklyMatches(charId) < 1)
		{
			player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED);
			return false;
		}
		
		switch (type)
		{
			case CLASSED:
			{
				if (!checkNoble(player, player))
				{
					return false;
				}
				
				if (Olympiad.getInstance().getRemainingWeeklyMatchesClassed(charId) < 1)
				{
					player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED_60_NON_CLASSED_30_CLASSED_10_TEAM);
					return false;
				}
				
				List<Integer> classed = _classBasedRegisters.get(player.getBaseClass());
				if (classed != null)
				{
					classed.add(charId);
				}
				else
				{
					classed = new CopyOnWriteArrayList<>();
					classed.add(charId);
					_classBasedRegisters.put(player.getBaseClass(), classed);
				}
				
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
				break;
			}
			case NON_CLASSED:
			{
				if (!checkNoble(player, player))
				{
					return false;
				}
				
				if (Olympiad.getInstance().getRemainingWeeklyMatchesNonClassed(charId) < 1)
				{
					player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED_60_NON_CLASSED_30_CLASSED_10_TEAM);
					return false;
				}
				
				_nonClassBasedRegisters.add(charId);
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
				break;
			}
			case TEAMS:
			{
				final L2Party party = player.getParty();
				if ((party == null) || (party.getMemberCount() != 3))
				{
					player.sendPacket(SystemMessageId.PARTY_REQUIREMENTS_NOT_MET);
					return false;
				}
				if (!party.isLeader(player))
				{
					player.sendPacket(SystemMessageId.ONLY_PARTY_LEADER_CAN_REQUEST_TEAM_MATCH);
					return false;
				}
				
				int teamPoints = 0;
				List<Integer> team = new ArrayList<>(party.getMemberCount());
				for (L2PcInstance noble : party.getMembers())
				{
					if (!checkNoble(noble, player))
					{
						// remove previously registered party members
						if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
						{
							for (L2PcInstance unreg : party.getMembers())
							{
								if (unreg == noble)
								{
									break;
								}
								
								AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, unreg);
							}
						}
						return false;
					}
					
					if (Olympiad.getInstance().getRemainingWeeklyMatchesTeam(noble.getObjectId()) < 1)
					{
						player.sendPacket(SystemMessageId.MAX_OLY_WEEKLY_MATCHES_REACHED_60_NON_CLASSED_30_CLASSED_10_TEAM);
						return false;
					}
					team.add(noble.getObjectId());
					teamPoints += Olympiad.getInstance().getNoblePoints(noble.getObjectId());
				}
				if (teamPoints < 10)
				{
					// TODO: replace with retail message
					player.sendMessage("Your team must have at least 10 points in total.");
					// remove previously registered party members
					if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
					{
						for (L2PcInstance unreg : party.getMembers())
						{
							AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, unreg);
						}
					}
					return false;
				}
				
				party.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_REGISTERED_IN_A_WAITING_LIST_OF_TEAM_GAMES));
				_teamsBasedRegisters.add(team);
				break;
			}
		}
		return true;
	}
	
	public final boolean unRegisterNoble(L2PcInstance noble)
	{
		if (!Olympiad._inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		
		if (!noble.isNoble())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		
		if (!isRegistered(noble, noble, false))
		{
			noble.sendPacket(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			return false;
		}
		
		if (isInCompetition(noble, noble, false))
		{
			return false;
		}
		
		Integer objId = Integer.valueOf(noble.getObjectId());
		if (_nonClassBasedRegisters.remove(objId))
		{
			if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, noble);
			}
			
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}
		
		final List<Integer> classed = _classBasedRegisters.get(noble.getBaseClass());
		if ((classed != null) && classed.remove(objId))
		{
			_classBasedRegisters.put(noble.getBaseClass(), classed);
			
			if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, noble);
			}
			
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			return true;
		}
		
		for (List<Integer> team : _teamsBasedRegisters)
		{
			if ((team != null) && team.contains(objId))
			{
				_teamsBasedRegisters.remove(team);
				ThreadPoolManager.getInstance().executeGeneral(new AnnounceUnregToTeam(team));
				return true;
			}
		}
		return false;
	}
	
	public final void removeDisconnectedCompetitor(L2PcInstance player)
	{
		final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
		if ((task != null) && task.isGameStarted())
		{
			task.getGame().handleDisconnect(player);
		}
		
		final Integer objId = Integer.valueOf(player.getObjectId());
		if (_nonClassBasedRegisters.remove(objId))
		{
			return;
		}
		
		final List<Integer> classed = _classBasedRegisters.get(player.getBaseClass());
		if ((classed != null) && classed.remove(objId))
		{
			return;
		}
		
		for (List<Integer> team : _teamsBasedRegisters)
		{
			if ((team != null) && team.contains(objId))
			{
				_teamsBasedRegisters.remove(team);
				ThreadPoolManager.getInstance().executeGeneral(new AnnounceUnregToTeam(team));
				return;
			}
		}
	}
	
	/**
	 * @param noble - checked noble
	 * @param player - messages will be sent to this L2PcInstance
	 * @return true if all requirements are met
	 */
	// TODO: move to the bypass handler after reworking points system
	private final boolean checkNoble(L2PcInstance noble, L2PcInstance player)
	{
		SystemMessage sm;
		if (!noble.isNoble())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}
		
		if (noble.isSubClassActive())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_CLASS_CHARACTER);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}
		
		if (noble.isCursedWeaponEquipped())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_JOIN_OLYMPIAD_POSSESSING_S2);
			sm.addPcName(noble);
			sm.addItemName(noble.getCursedWeaponEquippedId());
			player.sendPacket(sm);
			return false;
		}
		
		if (!noble.isInventoryUnder90(true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_INVENTORY_SLOT_EXCEEDS_80_PERCENT);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}
		
		final int charId = noble.getObjectId();
		if (noble.isOnEvent())
		{
			player.sendMessage("You can't join olympiad while participating on TvT Event.");
			return false;
		}
		
		if (isRegistered(noble, player, true))
		{
			return false;
		}
		
		if (isInCompetition(noble, player, true))
		{
			return false;
		}
		
		StatsSet statDat = Olympiad.getNobleStats(charId);
		if (statDat == null)
		{
			statDat = new StatsSet();
			statDat.set(Olympiad.CLASS_ID, noble.getBaseClass());
			statDat.set(Olympiad.CHAR_NAME, noble.getName());
			statDat.set(Olympiad.POINTS, Olympiad.DEFAULT_POINTS);
			statDat.set(Olympiad.COMP_DONE, 0);
			statDat.set(Olympiad.COMP_WON, 0);
			statDat.set(Olympiad.COMP_LOST, 0);
			statDat.set(Olympiad.COMP_DRAWN, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK_CLASSED, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK_NON_CLASSED, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK_TEAM, 0);
			statDat.set("to_save", true);
			Olympiad.addNobleStats(charId, statDat);
		}
		
		final int points = Olympiad.getInstance().getNoblePoints(charId);
		if (points <= 0)
		{
			final NpcHtmlMessage message = new NpcHtmlMessage(player.getLastHtmlActionOriginId());
			message.setFile(player.getHtmlPrefix(), "data/html/olympiad/noble_nopoints1.htm");
			message.replace("%objectId%", String.valueOf(noble.getLastHtmlActionOriginId()));
			player.sendPacket(message);
			return false;
		}
		
		if ((Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OLYMPIAD_ID, noble, Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP))
		{
			final NpcHtmlMessage message = new NpcHtmlMessage(player.getLastHtmlActionOriginId());
			message.setFile(player.getHtmlPrefix(), "data/html/mods/OlympiadIPRestriction.htm");
			message.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP)));
			player.sendPacket(message);
			return false;
		}
		
		return true;
	}
	
	private static final class AnnounceUnregToTeam implements Runnable
	{
		private final List<Integer> _team;
		
		public AnnounceUnregToTeam(List<Integer> t)
		{
			_team = t;
		}
		
		@Override
		public final void run()
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			for (int objectId : _team)
			{
				L2PcInstance teamMember = L2World.getInstance().getPlayer(objectId);
				if (teamMember != null)
				{
					teamMember.sendPacket(sm);
					if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
					{
						AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, teamMember);
					}
				}
			}
		}
	}
	
	public int getCountOpponents()
	{
		return _nonClassBasedRegisters.size() + _classBasedRegisters.size() + _teamsBasedRegisters.size();
	}
	
	private static class SingletonHolder
	{
		protected static final OlympiadManager _instance = new OlympiadManager();
	}
}