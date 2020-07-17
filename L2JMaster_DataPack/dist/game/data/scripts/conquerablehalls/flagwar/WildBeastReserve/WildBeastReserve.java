/*
 * Copyright (C) 2004-2020 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package conquerablehalls.flagwar.WildBeastReserve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.data.sql.impl.ClanTable;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.zone.type.L2ResidenceHallTeleportZone;

import conquerablehalls.flagwar.FlagWar;

/**
 * @author Sacrifice
 */
public final class WildBeastReserve extends FlagWar
{
	private static final int ROYAL_FLAG = 35606;
	private static final int FLAG_RED = 35607; // White flag
	private static final int FLAG_YELLOW = 35608; // Red flag
	private static final int FLAG_GREEN = 35609; // Blue flag
	private static final int FLAG_BLUE = 35610; // Green flag
	private static final int FLAG_PURPLE = 35611; // Black flag
	
	private static final int ALLY_1 = 35618;
	private static final int ALLY_2 = 35619;
	private static final int ALLY_3 = 35620;
	private static final int ALLY_4 = 35621;
	private static final int ALLY_5 = 35622;
	
	private static final int TELEPORT = 35612;
	
	public static final int MESSENGER = 35627;
	
	private static final Location[] FLAG_COORDS =
	{
		new Location(56963, -92211, -1303, 60611),
		new Location(58090, -91641, -1303, 47274),
		new Location(58908, -92556, -1303, 34450),
		new Location(58336, -93600, -1303, 21100),
		new Location(57152, -93360, -1303, 8400),
		new Location(59116, -93251, -1302, 31000),
		new Location(56432, -92864, -1303, 64000)
	};
	
	//@formatter:off
	private static final int[] OUTTER_DOORS_TO_OPEN = {21150003, 21150004};
	private static final int[] INNER_DOORS_TO_OPEN = {21150001, 21150002};
	//@formatter:on
	
	static
	{
		final Collection<L2ResidenceHallTeleportZone> zoneList = ZoneManager.getInstance().getAllZones(L2ResidenceHallTeleportZone.class);
		for (L2ResidenceHallTeleportZone teleZone : zoneList)
		{
			if (teleZone.getResidenceId() != BEAST_FARM)
			{
				continue;
			}
			
			final int id = teleZone.getResidenceZoneId();
			if ((id < 0) || (id >= 6))
			{
				continue;
			}
			_teleZones[id] = teleZone;
		}
	}
	
	private static final int QUEST_REWARD = 8293; // Trainer License
	private static final Location CENTER = new Location(57762, -92696, -1359, 0);
	
	private WildBeastReserve()
	{
		super(WildBeastReserve.class.getSimpleName(), BEAST_FARM);
		
		_royalFlag = ROYAL_FLAG;
		_flagRed = FLAG_RED;
		_flagYellow = FLAG_YELLOW;
		_flagGreen = FLAG_GREEN;
		_flagBlue = FLAG_BLUE;
		_flagPurple = FLAG_PURPLE;
		_outerDoorsToOpen = OUTTER_DOORS_TO_OPEN;
		_innerDoorsToOpen = INNER_DOORS_TO_OPEN;
		_flagCoords = FLAG_COORDS;
		_questReward = QUEST_REWARD;
		_center = CENTER;
		
		addStartNpc(MESSENGER);
		addFirstTalkId(MESSENGER);
		addTalkId(MESSENGER);
		
		for (int i = 0; i < 6; i++)
		{
			addFirstTalkId(TELEPORT + i);
			addFirstTalkId(TELEPORT + i);
		}
		
		addKillId(ALLY_1, ALLY_2, ALLY_3, ALLY_4, ALLY_5);
		
		addSpawnId(ALLY_1, ALLY_2, ALLY_3, ALLY_4, ALLY_5);
	}
	
	public static void main(String[] args)
	{
		new WildBeastReserve();
	}
	
	@Override
	public boolean canPayRegistration()
	{
		return false;
	}
	
	@Override
	public String getAllyHtml(int ally)
	{
		String result = null;
		
		switch (ally)
		{
			case ALLY_1:
			{
				result = "messenger_ally1result.html";
				break;
			}
			case ALLY_2:
			{
				result = "messenger_ally2result.html";
				break;
			}
			case ALLY_3:
			{
				result = "messenger_ally3result.html";
				break;
			}
			case ALLY_4:
			{
				result = "messenger_ally4result.html";
				break;
			}
			case ALLY_5:
			{
				result = "messenger_ally5result.html";
				break;
			}
		}
		return result;
	}
	
	@Override
	public String getFlagHtml(int flag)
	{
		String result = null;
		
		switch (flag)
		{
			case FLAG_RED:
			{
				result = "messenger_flag1.html";
				break;
			}
			case FLAG_YELLOW:
			{
				result = "messenger_flag2.html";
				break;
			}
			case FLAG_GREEN:
			{
				result = "messenger_flag3.html";
				break;
			}
			case FLAG_BLUE:
			{
				result = "messenger_flag4.html";
				break;
			}
			case FLAG_PURPLE:
			{
				result = "messenger_flag5.html";
				break;
			}
		}
		return result;
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String html = event;
		final L2Clan clan = player.getClan();
		
		if (event.startsWith("register_clan")) // Register the clan for the siege
		{
			if (!_hall.isRegistering())
			{
				if (_hall.isInSiege())
				{
					html = "messenger_registrationpassed.html";
				}
				else
				{
					sendRegistrationPageDate(player);
					return super.onAdvEvent(event, npc, player);
				}
			}
			else if ((clan == null) || !player.isClanLeader())
			{
				html = "messenger_notclannotleader.html";
			}
			else if (getAttackers().size() >= 5)
			{
				html = "messenger_attackersqueuefull.html";
			}
			else if (checkIsAttacker(clan))
			{
				html = "messenger_clanalreadyregistered.html";
			}
			else if (_hall.getOwnerId() == clan.getId())
			{
				html = "messenger_curownermessage.html";
			}
			else
			{
				final String[] arg = event.split(" ");
				if (arg.length >= 2)
				{
					// Register passing the quest
					if (arg[1].equals("wQuest"))
					{
						if (player.destroyItemByItemId(_hall.getName() + " Siege", _questReward, 1, npc, false)) // Quest passed
						{
							registerClan(clan);
							html = getFlagHtml(_data.get(clan.getId()).flag);
						}
						else
						{
							html = "messenger_noquest.html";
						}
					}
					// Register paying the fee
					else if (arg[1].equals("wFee") && canPayRegistration())
					{
						if (player.reduceAdena(getName() + " Siege", 200000, npc, false)) // Fee payed
						{
							registerClan(clan);
							html = getFlagHtml(_data.get(clan.getId()).flag);
						}
						else
						{
							html = "messenger_nomoney.html";
						}
					}
				}
			}
		}
		// Select the flag to defend
		else if (event.startsWith("select_clan_npc"))
		{
			if (!player.isClanLeader())
			{
				html = "messenger_onlyleaderselectally.html";
			}
			else if (!_data.containsKey(clan.getId()))
			{
				html = "messenger_clannotregistered.html";
			}
			else
			{
				final String[] var = event.split(" ");
				if (var.length >= 2)
				{
					int id = 0;
					
					try
					{
						id = Integer.parseInt(var[1]);
					}
					catch (Exception e)
					{
						_log.warn("{}: select_clan_npc->Wrong mahum warrior ID {}!", getName(), var[1], e);
					}
					
					if ((id > 0) && ((html = getAllyHtml(id)) != null))
					{
						_data.get(clan.getId()).npc = id;
						saveNpc(id, clan.getId());
					}
				}
				else
				{
					_log.warn("{}: Siege: Not enough parameters to save clan npc for clan {}! ", getName(), clan.getName());
				}
			}
		}
		// View (and change ? ) the current selected mahum warrior
		else if (event.startsWith("view_clan_npc"))
		{
			ClanData clanData = null;
			if (clan == null)
			{
				html = "messenger_clannotregistered.html";
			}
			else if ((clanData = _data.get(clan.getId())) == null)
			{
				html = "messenger_notclannotleader.html";
			}
			else if (clanData.npc == 0)
			{
				html = "messenger_leaderdidnotchooseyet.html";
			}
			else
			{
				html = getAllyHtml(clanData.npc);
			}
		}
		// Register a clan member for the fight
		else if (event.equals("register_member"))
		{
			if (clan == null)
			{
				html = "messenger_clannotregistered.html";
			}
			else if (!_hall.isRegistering())
			{
				html = "messenger_registrationpassed.html";
			}
			else if (!_data.containsKey(clan.getId()))
			{
				html = "messenger_notclannotleader.html";
			}
			else if (_data.get(clan.getId()).players.size() >= 18)
			{
				html = "messenger_clanqueuefull.html";
			}
			else
			{
				final ClanData clanData = _data.get(clan.getId());
				clanData.players.add(player.getObjectId());
				saveMember(clan.getId(), player.getObjectId());
				if (clanData.npc == 0)
				{
					html = "messenger_leaderdidnotchooseyet.html";
				}
				else
				{
					html = "messenger_clanregistered.html";
				}
			}
		}
		// Show cur attacker list
		else if (event.equals("view_attacker_list"))
		{
			if (_hall.isRegistering())
			{
				sendRegistrationPageDate(player);
			}
			else
			{
				html = getHtm(player.getHtmlPrefix(), "messenger_registeredclans.html");
				int i = 0;
				for (Entry<Integer, ClanData> clanData : _data.entrySet())
				{
					final L2Clan attackerClan = ClanTable.getInstance().getClan(clanData.getKey());
					if (attackerClan == null)
					{
						continue;
					}
					html = html.replaceAll("%clan" + i + "%", clan.getName());
					html = html.replaceAll("%clanMem" + i + "%", String.valueOf(clanData.getValue().players.size()));
					i++;
				}
				
				if (_data.size() < 5)
				{
					for (int c = _data.size(); c < 5; c++)
					{
						html = html.replaceAll("%clan" + c + "%", "Empty pos. ");
						html = html.replaceAll("%clanMem" + c + "%", "Empty pos. ");
					}
				}
			}
		}
		return html;
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String html = null;
		
		if (npc.getId() == MESSENGER)
		{
			if (!checkIsAttacker(player.getClan()))
			{
				final L2Clan clan = ClanTable.getInstance().getClan(_hall.getOwnerId());
				String content = getHtm(player.getHtmlPrefix(), "messenger_initial.html");
				content = content.replaceAll("%clanName%", (clan == null) ? "no owner" : clan.getName());
				content = content.replaceAll("%objectId%", String.valueOf(npc.getObjectId()));
				html = content;
			}
			else
			{
				html = "messenger_initial.html";
			}
		}
		else
		{
			final int index = (npc.getId() - TELEPORT);
			if ((index == 0) && _firstPhase)
			{
				html = "teleporter_notyet.html";
			}
			else
			{
				_teleZones[index].checkTeleporTask();
				html = "teleporter.html";
			}
		}
		return html;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		if (_hall.isInSiege())
		{
			final int npcId = npc.getId();
			
			for (int keys : _data.keySet())
			{
				if (_data.get(keys).npc == npcId)
				{
					removeParticipant(keys, true);
				}
			}
			
			synchronized (this)
			{
				// TODO: Zoey76: previous bad implementation.
				// Converting map.keySet() to List and map.values() to List doesn't ensure that
				// first element in the key's List correspond to the first element in the values' List
				// That's the reason that values aren't copied to a List, instead using _data.get(clanIds.get(0))
				final List<Integer> clanIds = new ArrayList<>(_data.keySet());
				
				if (_firstPhase)
				{
					// Siege ends if just 1 flag is alive
					// Hall was free before battle or owner didn't set the ally npc
					if (((clanIds.size() == 1) && (_hall.getOwnerId() <= 0)) || (_data.get(clanIds.get(0)).npc == 0))
					{
						_missionAccomplished = true;
						// _winner = ClanTable.getInstance().getClan(_data.keySet()[0]);
						// removeParticipant(_data.keySet()[0], false);
						cancelSiegeTask();
						endSiege();
					}
					else if ((_data.size() == 2) && (_hall.getOwnerId() > 0)) // Hall has defender (owner)
					{
						cancelSiegeTask(); // No time limit now
						_firstPhase = false;
						_hall.getSiegeZone().setIsActive(false);
						
						for (int doorId : _innerDoorsToOpen)
						{
							_hall.openCloseDoor(doorId, true);
						}
						
						for (ClanData data : _data.values())
						{
							doUnSpawns(data);
						}
						
						ThreadPoolManager.getInstance().scheduleGeneral(() ->
						{
							for (int doorId : _innerDoorsToOpen)
							{
								_hall.openCloseDoor(doorId, false);
							}
							
							for (Entry<Integer, ClanData> e : _data.entrySet())
							{
								doSpawns(e.getKey(), e.getValue());
							}
							
							_hall.getSiegeZone().setIsActive(true);
						}, 300000);
					}
				}
				else
				{
					_missionAccomplished = true;
					_winner = ClanTable.getInstance().getClan(clanIds.get(0));
					removeParticipant(clanIds.get(0), false);
					endSiege();
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, _center);
		return super.onSpawn(npc);
	}
}