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
package com.l2jserver.gameserver.model.zone.type;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.l2jserver.gameserver.GameServer;
import com.l2jserver.gameserver.instancemanager.GrandBossManager;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.TeleportWhereType;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.zone.AbstractZoneSettings;
import com.l2jserver.gameserver.model.zone.L2ZoneType;

/**
 * @author DaRkRaGe
 */
public class L2BossZone extends L2ZoneType
{
	private int _timeInvade;
	
	private int[] _oustLoc =
	{
		0,
		0,
		0
	};
	
	public final class Settings extends AbstractZoneSettings
	{
		// track the times that players got disconnected. Players are allowed
		// to log back into the zone as long as their log-out was within _timeInvade time...
		// <player objectId, expiration time in milliseconds>
		private final Map<Integer, Long> _playerAllowedReEntryTimes = new ConcurrentHashMap<>();
		
		// track the players admitted to the zone who should be allowed back in
		// after reboot/server downtime (outside of their control), within 30 of server restart
		private final List<Integer> _playersAllowed = new CopyOnWriteArrayList<>();
		
		private final List<L2Character> _raidList = new CopyOnWriteArrayList<>();
		
		protected Settings()
		{
		}
		
		public Map<Integer, Long> getPlayerAllowedReEntryTimes()
		{
			return _playerAllowedReEntryTimes;
		}
		
		public List<Integer> getPlayersAllowed()
		{
			return _playersAllowed;
		}
		
		public List<L2Character> getRaidList()
		{
			return _raidList;
		}
		
		@Override
		public void clear()
		{
			_playerAllowedReEntryTimes.clear();
			_playersAllowed.clear();
			_raidList.clear();
		}
	}
	
	public L2BossZone(int id)
	{
		super(id);
		_oustLoc = new int[3];
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
		GrandBossManager.getInstance().addZone(this);
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("InvadeTime"))
		{
			_timeInvade = Integer.parseInt(value);
		}
		else if (name.equals("oustX"))
		{
			_oustLoc[0] = Integer.parseInt(value);
		}
		else if (name.equals("oustY"))
		{
			_oustLoc[1] = Integer.parseInt(value);
		}
		else if (name.equals("oustZ"))
		{
			_oustLoc[2] = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * Boss zones have special behaviors for player characters.<br>
	 * Players are automatically teleported out when the attempt to enter these zones, except if the time at which they enter the zone is prior to the entry expiration time set for that player.<br>
	 * Entry expiration times are set by any one of the following:<br>
	 * 1) A player logs out while in a zone (Expiration gets set to logoutTime + _timeInvade)<br>
	 * 2) An external source (such as a quest or AI of NPC) set up the player for entry.<br>
	 * There exists one more case in which the player will be allowed to enter.<br>
	 * That is if the server recently rebooted (boot-up time more recent than currentTime - _timeInvade) AND the player was in the zone prior to reboot.
	 */
	@Override
	protected void onEnter(L2Character character)
	{
		if (isEnabled())
		{
			if (character.isPlayer())
			{
				final L2PcInstance player = character.getActingPlayer();
				if (player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
				{
					return;
				}
				// if player has been (previously) cleared by npc/ai for entry and the zone is
				// set to receive players (aka not waiting for boss to respawn)
				if (getSettings().getPlayersAllowed().contains(player.getObjectId()))
				{
					// Get the information about this player's last logout-exit from
					// this zone.
					final Long expirationTime = getSettings().getPlayerAllowedReEntryTimes().get(player.getObjectId());
					
					// with legal entries, do nothing.
					if (expirationTime == null) // legal null expirationTime entries
					{
						long serverStartTime = GameServer.dateTimeServerStarted.getTimeInMillis();
						if ((serverStartTime > (System.currentTimeMillis() - _timeInvade)))
						{
							return;
						}
					}
					else
					{
						// legal non-null logoutTime entries
						getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
						if (expirationTime.longValue() > System.currentTimeMillis())
						{
							return;
						}
					}
					getSettings().getPlayersAllowed().remove(getSettings().getPlayersAllowed().indexOf(player.getObjectId()));
				}
				// teleport out all players who attempt "illegal" (re-)entry
				if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
				{
					player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2]);
				}
				else
				{
					player.teleToLocation(TeleportWhereType.TOWN);
				}
			}
			else if (character.isSummon())
			{
				final L2PcInstance player = character.getActingPlayer();
				if (player != null)
				{
					if (getSettings().getPlayersAllowed().contains(player.getObjectId()) || player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
					{
						return;
					}
					
					// remove summon and teleport out owner
					// who attempt "illegal" (re-)entry
					if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
					{
						player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2]);
					}
					else
					{
						player.teleToLocation(TeleportWhereType.TOWN);
					}
				}
				((L2Summon) character).unSummon(player);
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (isEnabled())
		{
			if (character.isPlayer())
			{
				final L2PcInstance player = character.getActingPlayer();
				if (player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
				{
					return;
				}
				
				// if the player just got disconnected/logged out, store the dc
				// time so that
				// decisions can be made later about allowing or not the player
				// to log into the zone
				if (!player.isOnline() && getSettings().getPlayersAllowed().contains(player.getObjectId()))
				{
					// mark the time that the player left the zone
					getSettings().getPlayerAllowedReEntryTimes().put(player.getObjectId(), System.currentTimeMillis() + _timeInvade);
				}
				else
				{
					if (getSettings().getPlayersAllowed().contains(player.getObjectId()))
					{
						getSettings().getPlayersAllowed().remove(getSettings().getPlayersAllowed().indexOf(player.getObjectId()));
					}
					getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
				}
			}
			if (character.isPlayable())
			{
				if ((getCharactersInside() != null) && !getCharactersInside().isEmpty())
				{
					getSettings().getRaidList().clear();
					int count = 0;
					for (L2Character obj : getCharactersInside())
					{
						if (obj == null)
						{
							continue;
						}
						if (obj.isPlayable())
						{
							count++;
						}
						else if (obj.isAttackable() && obj.isRaid())
						{
							getSettings().getRaidList().add(obj);
						}
					}
					// if inside zone isnt any player, force all boss instance return to its spawn points
					if ((count == 0) && !getSettings().getRaidList().isEmpty())
					{
						for (int i = 0; i < getSettings().getRaidList().size(); i++)
						{
							L2Attackable raid = (L2Attackable) getSettings().getRaidList().get(i);
							if ((raid == null) || (raid.getSpawn() == null) || raid.isDead())
							{
								continue;
							}
							if (!raid.isInsideRadius(raid.getSpawn(), 150, false, false))
							{
								raid.returnHome();
							}
						}
					}
				}
			}
		}
		if (character.isAttackable() && character.isRaid() && !character.isDead())
		{
			((L2Attackable) character).returnHome();
		}
	}
	
	@Override
	public void setEnabled(boolean flag)
	{
		if (isEnabled() != flag)
		{
			oustAllPlayers();
		}
		super.setEnabled(flag);
	}
	
	public int getTimeInvade()
	{
		return _timeInvade;
	}
	
	public void setAllowedPlayers(List<Integer> players)
	{
		if (players != null)
		{
			getSettings().getPlayersAllowed().clear();
			getSettings().getPlayersAllowed().addAll(players);
		}
	}
	
	public List<Integer> getAllowedPlayers()
	{
		return getSettings().getPlayersAllowed();
	}
	
	public boolean isPlayerAllowed(L2PcInstance player)
	{
		if (player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
		{
			return true;
		}
		else if (getSettings().getPlayersAllowed().contains(player.getObjectId()))
		{
			return true;
		}
		else
		{
			if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
			{
				player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2]);
			}
			else
			{
				player.teleToLocation(TeleportWhereType.TOWN);
			}
			return false;
		}
	}
	
	/**
	 * Some GrandBosses send all players in zone to a specific part of the zone, rather than just removing them all. If this is the case, this command should be used. If this is no the case, then use oustAllPlayers().
	 * @param loc
	 */
	public void movePlayersTo(Location loc)
	{
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (L2Character character : getCharactersInside())
		{
			if ((character != null) && character.isPlayer())
			{
				L2PcInstance player = character.getActingPlayer();
				if (player.isOnline())
				{
					player.teleToLocation(loc);
				}
			}
		}
	}
	
	/**
	 * Occasionally, all players need to be sent out of the zone (for example, if the players are just running around without fighting for too long, or if all players die, etc). This call sends all online players to town and marks offline players to be teleported (by clearing their relog expiration
	 * times) when they log back in (no real need for off-line teleport).
	 */
	public void oustAllPlayers()
	{
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (L2Character character : getCharactersInside())
		{
			if ((character != null) && character.isPlayer())
			{
				L2PcInstance player = character.getActingPlayer();
				if (player.isOnline())
				{
					if ((_oustLoc[0] != 0) && (_oustLoc[1] != 0) && (_oustLoc[2] != 0))
					{
						player.teleToLocation(_oustLoc[0], _oustLoc[1], _oustLoc[2]);
					}
					else
					{
						player.teleToLocation(TeleportWhereType.TOWN);
					}
				}
			}
		}
		getSettings().getPlayerAllowedReEntryTimes().clear();
		getSettings().getPlayersAllowed().clear();
	}
	
	/**
	 * This function is to be used by external sources, such as quests and AI in order to allow a player for entry into the zone for some time. Naturally if the player does not enter within the allowed time, he/she will be teleported out again...
	 * @param player reference to the player we wish to allow
	 * @param durationInSec amount of time in seconds during which entry is valid.
	 */
	public void allowPlayerEntry(L2PcInstance player, int durationInSec)
	{
		if (!player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
		{
			if (!getSettings().getPlayersAllowed().contains(player.getObjectId()))
			{
				getSettings().getPlayersAllowed().add(player.getObjectId());
			}
			getSettings().getPlayerAllowedReEntryTimes().put(player.getObjectId(), System.currentTimeMillis() + (durationInSec * 1000));
		}
	}
	
	public void removePlayer(L2PcInstance player)
	{
		if (!player.canOverrideCond(PcCondOverride.ZONE_CONDITIONS))
		{
			getSettings().getPlayersAllowed().remove(Integer.valueOf(player.getObjectId()));
			getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
		}
	}
	
	public void updateKnownList(L2Npc npc)
	{
		if ((_characterList == null) || _characterList.isEmpty())
		{
			return;
		}
		
		Map<Integer, L2PcInstance> npcKnownPlayers = npc.getKnownList().getKnownPlayers();
		for (L2Character character : getCharactersInside())
		{
			if ((character != null) && character.isPlayer())
			{
				L2PcInstance player = character.getActingPlayer();
				if (player.isOnline())
				{
					npcKnownPlayers.put(player.getObjectId(), player);
				}
			}
		}
	}
}
