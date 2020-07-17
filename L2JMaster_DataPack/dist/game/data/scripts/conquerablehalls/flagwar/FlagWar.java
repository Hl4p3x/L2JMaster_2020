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
package conquerablehalls.flagwar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.L2SpecialSiegeGuardAI;
import com.l2jserver.gameserver.data.sql.impl.ClanTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.L2SiegeClan;
import com.l2jserver.gameserver.model.L2SiegeClan.SiegeClanType;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.TeleportWhereType;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.clanhall.ClanHallSiegeEngine;
import com.l2jserver.gameserver.model.entity.clanhall.SiegeStatus;
import com.l2jserver.gameserver.model.zone.type.L2ResidenceHallTeleportZone;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.util.Broadcast;

/**
 * @author Sacrifice
 */
public abstract class FlagWar extends ClanHallSiegeEngine
{
	private static final String LOAD_ATTACKERS = "SELECT * FROM `siegable_hall_flagwar_attackers` WHERE `hall_id` = ?";
	private static final String SAVE_ATTACKER = "INSERT INTO `siegable_hall_flagwar_attackers_members` VALUES (?, ?, ?)";
	private static final String LOAD_MEMBERS = "SELECT `object_id` FROM `siegable_hall_flagwar_attackers_members` WHERE `clan_id` = ?";
	private static final String SAVE_CLAN = "INSERT INTO `siegable_hall_flagwar_attackers` VALUES(?, ?, ?, ?)";
	private static final String SAVE_NPC = "UPDATE `siegable_hall_flagwar_attackers` SET `npc` = ? WHERE `clan_id` = ?";
	private static final String CLEAR_CLAN = "DELETE FROM `siegable_hall_flagwar_attackers` WHERE `hall_id` = ?";
	private static final String CLEAR_CLAN_ATTACKERS = "DELETE FROM `siegable_hall_flagwar_attackers_members` WHERE `hall_id` = ?";
	
	protected static L2ResidenceHallTeleportZone[] _teleZones = new L2ResidenceHallTeleportZone[6];
	
	protected int _royalFlag;
	
	protected int _flagRed;
	protected int _flagYellow;
	protected int _flagGreen;
	protected int _flagBlue;
	protected int _flagPurple;
	
	protected int[] _outerDoorsToOpen = new int[2];
	protected int[] _innerDoorsToOpen = new int[2];
	
	protected Location[] _flagCoords = new Location[7];
	
	protected int _questReward;
	
	protected Location _center;
	
	protected Map<Integer, ClanData> _data = new HashMap<>(6);
	
	protected L2Clan _winner;
	
	protected boolean _firstPhase;
	
	public FlagWar(String name, int hallId)
	{
		super(name, "conquerablehalls/flagwar", hallId);
		
		// If siege ends w/ more than 1 flag alive, winner is old owner
		_winner = ClanTable.getInstance().getClan(_hall.getOwnerId());
	}
	
	public boolean canPayRegistration()
	{
		return true;
	}
	
	@Override
	public boolean canPlantFlag()
	{
		return false;
	}
	
	@Override
	public boolean doorIsAutoAttackable()
	{
		return false;
	}
	
	@Override
	public void endSiege()
	{
		if (_hall.getOwnerId() > 0)
		{
			final L2Clan clan = ClanTable.getInstance().getClan(_hall.getOwnerId());
			clan.setHideoutId(0);
			_hall.free();
		}
		super.endSiege();
	}
	
	public abstract String getAllyHtml(int ally);
	
	public abstract String getFlagHtml(int flag);
	
	@Override
	public final Location getInnerSpawnLoc(final L2PcInstance player)
	{
		Location loc = null;
		if (player.getClanId() == _hall.getOwnerId())
		{
			loc = _hall.getZone().getSpawns().get(0);
		}
		else
		{
			final ClanData clanData = _data.get(player.getClanId());
			if (clanData != null)
			{
				final int index = clanData.flag - _flagRed;
				if ((index >= 0) && (index <= 4))
				{
					loc = _hall.getZone().getChallengerSpawns().get(index);
				}
				else
				{
					throw new ArrayIndexOutOfBoundsException();
				}
			}
		}
		return loc;
	}
	
	@Override
	public L2Clan getWinner()
	{
		return _winner;
	}
	
	@Override
	public void loadAttackers()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_ATTACKERS))
		{
			ps.setInt(1, _hall.getId());
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int clanId = rset.getInt("clan_id");
					
					if (ClanTable.getInstance().getClan(clanId) == null)
					{
						_log.warn("{}: Loaded an unexistent clan as attacker! Clan ID {}!", getName(), clanId);
						continue;
					}
					
					final ClanData clanData = new ClanData();
					clanData.flag = rset.getInt("flag");
					clanData.npc = rset.getInt("npc");
					
					_data.put(clanId, clanData);
					loadAttackerMembers(clanId);
				}
				rset.close();
			}
			ps.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not load attackers for {}!", getName(), e);
		}
	}
	
	@Override
	public void onSiegeEnds()
	{
		if (_data.size() > 0)
		{
			for (int clanId : _data.keySet())
			{
				if (_hall.getOwnerId() == clanId)
				{
					removeParticipant(clanId, false);
				}
				else
				{
					removeParticipant(clanId, true);
				}
			}
		}
		clearTables();
	}
	
	@Override
	public void onSiegeStarts()
	{
		for (Entry<Integer, ClanData> clan : _data.entrySet())
		{
			// Spawns challengers flags and npcs
			try
			{
				final ClanData clanData = clan.getValue();
				doSpawns(clan.getKey(), clanData);
				fillPlayerList(clanData);
			}
			catch (Exception e)
			{
				endSiege();
				_log.warn("{}: Problems in siege initialization!", getName(), e);
			}
		}
	}
	
	@Override
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			registerClan(ClanTable.getInstance().getClan(_hall.getOwnerId()));
		}
		
		_hall.banishForeigners();
		final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.REGISTRATION_TERM_FOR_S1_ENDED);
		msg.addString(getName());
		Broadcast.toAllOnlinePlayers(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStarts(), 3600000);
	}
	
	@Override
	public void startSiege()
	{
		if (getAttackers().size() < 2)
		{
			onSiegeEnds();
			getAttackers().clear();
			_hall.updateNextSiege();
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(sm);
			return;
		}
		
		// Open doors for challengers
		for (int door : _outerDoorsToOpen)
		{
			_hall.openCloseDoor(door, true);
		}
		
		// Teleport owner inside
		if (_hall.getOwnerId() > 0)
		{
			final L2Clan owner = ClanTable.getInstance().getClan(_hall.getOwnerId());
			final Location loc = _hall.getZone().getSpawns().get(0); // Owner restart point
			for (L2ClanMember clanMember : owner.getMembers())
			{
				if (clanMember != null)
				{
					final L2PcInstance player = clanMember.getPlayerInstance();
					if ((player != null) && player.isOnline())
					{
						player.teleToLocation(loc, false);
					}
				}
			}
		}
		
		// Schedule open doors closement, banish non siege participants and<br>
		// siege start in 2 minutes
		ThreadPoolManager.getInstance().scheduleGeneral(() ->
		{
			for (int door : _outerDoorsToOpen)
			{
				_hall.openCloseDoor(door, false);
			}
			
			_hall.getZone().banishNonSiegeParticipants();
			
			startSiege();
		}, 300000);
	}
	
	protected void doSpawns(int clanId, ClanData data)
	{
		try
		{
			int index = 0;
			if (_firstPhase)
			{
				index = data.flag - _flagRed;
			}
			else
			{
				index = clanId == _hall.getOwnerId() ? 5 : 6;
			}
			Location loc = _flagCoords[index];
			
			data.flagInstance = new L2Spawn(data.flag);
			data.flagInstance.setLocation(loc);
			data.flagInstance.setRespawnDelay(10000);
			data.flagInstance.setAmount(1);
			data.flagInstance.init();
			
			data.warrior = new L2Spawn(data.npc);
			data.warrior.setLocation(loc);
			data.warrior.setRespawnDelay(10000);
			data.warrior.setAmount(1);
			data.warrior.init();
			((L2SpecialSiegeGuardAI) data.warrior.getLastSpawn().getAI()).getAlly().addAll(data.players);
		}
		catch (Exception e)
		{
			_log.warn("{}: Could not make clan spawns!", getName(), e);
		}
	}
	
	protected final void doUnSpawns(ClanData data)
	{
		if (data.flagInstance != null)
		{
			data.flagInstance.stopRespawn();
			data.flagInstance.getLastSpawn().deleteMe();
		}
		if (data.warrior != null)
		{
			data.warrior.stopRespawn();
			data.warrior.getLastSpawn().deleteMe();
		}
	}
	
	protected void registerClan(L2Clan clan)
	{
		final int clanId = clan.getId();
		
		final L2SiegeClan siegeClan = new L2SiegeClan(clanId, SiegeClanType.ATTACKER);
		getAttackers().put(clanId, siegeClan);
		
		final ClanData clanData = new ClanData();
		clanData.flag = _royalFlag + _data.size();
		clanData.players.add(clan.getLeaderId());
		_data.put(clanId, clanData);
		
		saveClan(clanId, clanData.flag);
		saveMember(clanId, clan.getLeaderId());
	}
	
	protected final void removeParticipant(int clanId, boolean teleport)
	{
		final ClanData clanData = _data.remove(clanId);
		
		if (clanData != null)
		{
			// Destroy clan flag
			if (clanData.flagInstance != null)
			{
				clanData.flagInstance.stopRespawn();
				if (clanData.flagInstance.getLastSpawn() != null)
				{
					clanData.flagInstance.getLastSpawn().deleteMe();
				}
			}
			
			if (clanData.warrior != null)
			{
				// Destroy clan warrior
				clanData.warrior.stopRespawn();
				if (clanData.warrior.getLastSpawn() != null)
				{
					clanData.warrior.getLastSpawn().deleteMe();
				}
			}
			
			clanData.players.clear();
			
			if (teleport)
			{
				// Teleport players outside
				for (L2PcInstance player : clanData.playersInstance)
				{
					if (player != null)
					{
						player.teleToLocation(TeleportWhereType.TOWN);
					}
				}
			}
			clanData.playersInstance.clear();
		}
	}
	
	protected final void saveMember(int clanId, int objectId)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_ATTACKER))
		{
			ps.setInt(1, _hall.getId());
			ps.setInt(2, clanId);
			ps.setInt(3, objectId);
			ps.execute();
			ps.close();
		}
		catch (Exception e)
		{
			_log.warn("{}: saveMember", getName(), e);
		}
	}
	
	protected final void saveNpc(int npc, int clanId)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_NPC))
		{
			ps.setInt(1, npc);
			ps.setInt(2, clanId);
			ps.execute();
			ps.close();
		}
		catch (Exception e)
		{
			_log.warn("{}: saveNpc()", getName(), e);
		}
	}
	
	protected void sendRegistrationPageDate(L2PcInstance player)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(getHtm(player.getHtmlPrefix(), "siege_date.html"));
		msg.replace("%nextSiege%", _hall.getSiegeDate().getTime().toString());
		player.sendPacket(msg);
	}
	
	private void clearTables()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps1 = con.prepareStatement(CLEAR_CLAN);
			PreparedStatement ps2 = con.prepareStatement(CLEAR_CLAN_ATTACKERS))
		{
			ps1.setInt(1, _hall.getId());
			ps1.execute();
			ps1.close();
			ps2.setInt(1, _hall.getId());
			ps2.execute();
			ps2.close();
		}
		catch (Exception e)
		{
			_log.warn("Unable to clear data tables for {}!", getName(), e);
		}
	}
	
	private void fillPlayerList(ClanData data)
	{
		for (int objId : data.players)
		{
			final L2PcInstance player = L2World.getInstance().getPlayer(objId);
			if (player != null)
			{
				data.playersInstance.add(player);
			}
		}
	}
	
	private final void loadAttackerMembers(int clanId)
	{
		final List<Integer> listInstance = _data.get(clanId).players;
		if (listInstance == null)
		{
			_log.warn(getName() + ": Tried to load unregistered clan with ID " + clanId);
			return;
		}
		
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_MEMBERS))
		{
			ps.setInt(1, clanId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					listInstance.add(rset.getInt("object_id"));
				}
				rset.close();
			}
			ps.close();
		}
		catch (Exception e)
		{
			_log.warn("{}: loadAttackerMembers", getName(), e);
		}
	}
	
	private final void saveClan(int clanId, int flag)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_CLAN))
		{
			ps.setInt(1, _hall.getId());
			ps.setInt(2, flag);
			ps.setInt(3, 0);
			ps.setInt(4, clanId);
			ps.execute();
			ps.close();
		}
		catch (Exception e)
		{
			_log.warn("{}: saveClan", getName(), e);
		}
	}
	
	protected class ClanData
	{
		public int flag = 0;
		public int npc = 0;
		public List<Integer> players = new ArrayList<>(18);
		List<L2PcInstance> playersInstance = new ArrayList<>(18);
		L2Spawn warrior = null;
		L2Spawn flagInstance = null;
	}
}