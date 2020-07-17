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
package com.l2jserver.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.L2CharacterAI;
import com.l2jserver.gameserver.ai.L2DoorAI;
import com.l2jserver.gameserver.data.xml.impl.DoorData;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.enums.Race;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.ClanHallManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.knownlist.DoorKnownList;
import com.l2jserver.gameserver.model.actor.stat.DoorStat;
import com.l2jserver.gameserver.model.actor.status.DoorStatus;
import com.l2jserver.gameserver.model.actor.templates.L2DoorTemplate;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.ClanHall;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.entity.clanhall.SiegableHall;
import com.l2jserver.gameserver.model.items.L2Weapon;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.DoorStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.OnEventTrigger;
import com.l2jserver.gameserver.network.serverpackets.StaticObject;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.util.Rnd;

public class L2DoorInstance extends L2Character
{
	private static final Logger LOG = LoggerFactory.getLogger(L2DoorInstance.class);
	
	public static final byte OPEN_BY_CLICK = 1;
	public static final byte OPEN_BY_TIME = 2;
	public static final byte OPEN_BY_ITEM = 4;
	public static final byte OPEN_BY_SKILL = 8;
	public static final byte OPEN_BY_CYCLE = 16;
	
	/** The castle index in the array of L2Castle this L2NpcInstance belongs to */
	private int _castleIndex = -2;
	/** The fort index in the array of L2Fort this L2NpcInstance belongs to */
	private int _fortIndex = -2;
	private ClanHall _clanHall;
	private boolean _open = false;
	private boolean _isAttackableDoor = false;
	private boolean _isTargetable;
	private int _meshindex = 1;
	// used for autoclose on open
	private Future<?> _autoCloseTask;
	
	/**
	 * Creates a door.
	 * @param template the door template
	 */
	public L2DoorInstance(L2DoorTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2DoorInstance);
		setIsInvul(false);
		setLethalable(false);
		_open = template.isOpenByDefault();
		_isAttackableDoor = template.isAttackable();
		_isTargetable = template.isTargetable();
		
		if (getGroupName() != null)
		{
			DoorData.addDoorGroup(getGroupName(), getId());
		}
		
		if (isOpenableByTime())
		{
			startTimerOpen();
		}
		
		int clanhallId = template.getClanHallId();
		if (clanhallId > 0)
		{
			ClanHall hall = ClanHallManager.getAllClanHalls().get(clanhallId);
			if (hall != null)
			{
				setClanHall(hall);
				hall.getDoors().add(this);
			}
		}
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2DoorAI(this);
	}
	
	private void startTimerOpen()
	{
		int delay = _open ? getTemplate().getOpenTime() : getTemplate().getCloseTime();
		if (getTemplate().getRandomTime() > 0)
		{
			delay += Rnd.get(getTemplate().getRandomTime());
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new TimerOpen(), delay * 1000);
	}
	
	@Override
	public final DoorKnownList getKnownList()
	{
		return (DoorKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new DoorKnownList(this));
	}
	
	@Override
	public L2DoorTemplate getTemplate()
	{
		return (L2DoorTemplate) super.getTemplate();
		
	}
	
	@Override
	public final DoorStatus getStatus()
	{
		return (DoorStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new DoorStatus(this));
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new DoorStat(this));
	}
	
	@Override
	public DoorStat getStat()
	{
		return (DoorStat) super.getStat();
	}
	
	/**
	 * @return {@code true} if door is open-able by skill.
	 */
	public final boolean isOpenableBySkill()
	{
		return (getTemplate().getOpenType() & OPEN_BY_SKILL) == OPEN_BY_SKILL;
	}
	
	/**
	 * @return {@code true} if door is open-able by item.
	 */
	public final boolean isOpenableByItem()
	{
		return (getTemplate().getOpenType() & OPEN_BY_ITEM) == OPEN_BY_ITEM;
	}
	
	/**
	 * @return {@code true} if door is open-able by double-click.
	 */
	public final boolean isOpenableByClick()
	{
		return (getTemplate().getOpenType() & OPEN_BY_CLICK) == OPEN_BY_CLICK;
	}
	
	/**
	 * @return {@code true} if door is open-able by time.
	 */
	public final boolean isOpenableByTime()
	{
		return (getTemplate().getOpenType() & OPEN_BY_TIME) == OPEN_BY_TIME;
	}
	
	/**
	 * @return {@code true} if door is open-able by Field Cycle system.
	 */
	public final boolean isOpenableByCycle()
	{
		return (getTemplate().getOpenType() & OPEN_BY_CYCLE) == OPEN_BY_CYCLE;
	}
	
	@Override
	public final int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * Gets the door ID.
	 * @return the door ID
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	/**
	 * @return Returns the open.
	 */
	public boolean getOpen()
	{
		return _open;
	}
	
	/**
	 * @param open The open to set.
	 */
	public void setOpen(boolean open)
	{
		_open = open;
		if (getChildId() > 0)
		{
			L2DoorInstance sibling = getSiblingDoor(getChildId());
			if (sibling != null)
			{
				sibling.notifyChildEvent(open);
			}
			else
			{
				LOG.warn("Cannot find child id: {}", getChildId());
			}
		}
	}
	
	public boolean getIsAttackableDoor()
	{
		return _isAttackableDoor;
	}
	
	public boolean getIsShowHp()
	{
		return getTemplate().isShowHp();
	}
	
	public void setIsAttackableDoor(boolean val)
	{
		_isAttackableDoor = val;
	}
	
	public int getDamage()
	{
		int dmg = 6 - (int) Math.ceil((getCurrentHp() / getMaxHp()) * 6);
		if (dmg > 6)
		{
			return 6;
		}
		if (dmg < 0)
		{
			return 0;
		}
		return dmg;
	}
	
	// TODO: Replace index with the castle id itself.
	public final Castle getCastle()
	{
		if (_castleIndex < 0)
		{
			_castleIndex = CastleManager.getInstance().getCastleIndex(this);
		}
		if (_castleIndex < 0)
		{
			return null;
		}
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}
	
	// TODO: Replace index with the fort id itself.
	public final Fort getFort()
	{
		if (_fortIndex < 0)
		{
			_fortIndex = FortManager.getInstance().getFortIndex(this);
		}
		if (_fortIndex < 0)
		{
			return null;
		}
		return FortManager.getInstance().getForts().get(_fortIndex);
	}
	
	public void setClanHall(ClanHall clanhall)
	{
		_clanHall = clanhall;
	}
	
	public ClanHall getClanHall()
	{
		return _clanHall;
	}
	
	public boolean isEnemy()
	{
		if ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getZone().isActive() && getIsShowHp())
		{
			return true;
		}
		if ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getZone().isActive() && getIsShowHp())
		{
			return true;
		}
		if ((getClanHall() != null) && getClanHall().isSiegableHall() && ((SiegableHall) getClanHall()).getSiegeZone().isActive() && getIsShowHp())
		{
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		// Doors can`t be attacked by NPCs
		if (!(attacker instanceof L2Playable))
		{
			return false;
		}
		
		if (getIsAttackableDoor())
		{
			return true;
		}
		if (!getIsShowHp())
		{
			return false;
		}
		
		L2PcInstance actingPlayer = attacker.getActingPlayer();
		
		if (getClanHall() != null)
		{
			SiegableHall hall = (SiegableHall) getClanHall();
			if (!hall.isSiegableHall())
			{
				return false;
			}
			return hall.isInSiege() && hall.getSiege().doorIsAutoAttackable() && hall.getSiege().checkIsAttacker(actingPlayer.getClan());
		}
		// Attackable only during siege by everyone (not owner)
		boolean isCastle = ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getZone().isActive());
		boolean isFort = ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getZone().isActive());
		int activeSiegeId = (getFort() != null ? getFort().getResidenceId() : (getCastle() != null ? getCastle().getResidenceId() : 0));
		
		if (TerritoryWarManager.getInstance().isTWInProgress())
		{
			return !TerritoryWarManager.getInstance().isAllyField(actingPlayer, activeSiegeId);
		}
		else if (isFort)
		{
			L2Clan clan = actingPlayer.getClan();
			if ((clan != null) && (clan == getFort().getOwnerClan()))
			{
				return false;
			}
		}
		else if (isCastle)
		{
			L2Clan clan = actingPlayer.getClan();
			if ((clan != null) && (clan.getId() == getCastle().getOwnerId()))
			{
				return false;
			}
		}
		return (isCastle || isFort);
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	/**
	 * Return null.
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public void broadcastStatusUpdate()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		if ((knownPlayers == null) || knownPlayers.isEmpty())
		{
			return;
		}
		
		StaticObject su = new StaticObject(this, false);
		StaticObject targetableSu = new StaticObject(this, true);
		DoorStatusUpdate dsu = new DoorStatusUpdate(this);
		OnEventTrigger oe = null;
		if (getEmitter() > 0)
		{
			oe = new OnEventTrigger(this, getOpen());
		}
		
		for (L2PcInstance player : knownPlayers)
		{
			if ((player == null) || !isVisibleFor(player))
			{
				continue;
			}
			
			if (player.isGM() || (((getCastle() != null) && (getCastle().getResidenceId() > 0)) || ((getFort() != null) && (getFort().getResidenceId() > 0))))
			{
				player.sendPacket(targetableSu);
			}
			else
			{
				player.sendPacket(su);
			}
			
			player.sendPacket(dsu);
			if (oe != null)
			{
				player.sendPacket(oe);
			}
		}
	}
	
	public final void openMe()
	{
		if (getGroupName() != null)
		{
			manageGroupOpen(true, getGroupName());
			return;
		}
		setOpen(true);
		broadcastStatusUpdate();
		startAutoCloseTask();
	}
	
	public final void closeMe()
	{
		// remove close task
		Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		if (getGroupName() != null)
		{
			manageGroupOpen(false, getGroupName());
			return;
		}
		setOpen(false);
		broadcastStatusUpdate();
	}
	
	private void manageGroupOpen(boolean open, String groupName)
	{
		Set<Integer> set = DoorData.getDoorsByGroup(groupName);
		L2DoorInstance first = null;
		for (Integer id : set)
		{
			L2DoorInstance door = getSiblingDoor(id);
			if (first == null)
			{
				first = door;
			}
			
			if (door.getOpen() != open)
			{
				door.setOpen(open);
				door.broadcastStatusUpdate();
			}
		}
		if ((first != null) && open)
		{
			first.startAutoCloseTask(); // only one from group
		}
	}
	
	/**
	 * Door notify child about open state change
	 * @param open true if opened
	 */
	private void notifyChildEvent(boolean open)
	{
		byte openThis = open ? getTemplate().getMasterDoorOpen() : getTemplate().getMasterDoorClose();
		
		if (openThis == 0)
		{
			return;
		}
		else if (openThis == 1)
		{
			openMe();
		}
		else if (openThis == -1)
		{
			closeMe();
		}
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getTemplate().getId() + "](" + getObjectId() + ")";
	}
	
	public String getDoorName()
	{
		return getTemplate().getName();
	}
	
	public int getX(int i)
	{
		return getTemplate().getNodeX()[i];
	}
	
	public int getY(int i)
	{
		return getTemplate().getNodeY()[i];
	}
	
	public int getZMin()
	{
		return getTemplate().getNodeZ();
	}
	
	public int getZMax()
	{
		return getTemplate().getNodeZ() + getTemplate().getHeight();
	}
	
	public List<L2DefenderInstance> getKnownDefenders()
	{
		final List<L2DefenderInstance> result = new ArrayList<>();
		for (L2Object obj : getKnownList().getKnownObjects().values())
		{
			if (obj instanceof L2DefenderInstance)
			{
				result.add((L2DefenderInstance) obj);
			}
		}
		return result;
	}
	
	public void setMeshIndex(int mesh)
	{
		_meshindex = mesh;
	}
	
	public int getMeshIndex()
	{
		return _meshindex;
	}
	
	public int getEmitter()
	{
		return getTemplate().getEmmiter();
	}
	
	public boolean isWall()
	{
		return getTemplate().isWall();
	}
	
	public String getGroupName()
	{
		return getTemplate().getGroupName();
	}
	
	public int getChildId()
	{
		return getTemplate().getChildDoorId();
	}
	
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (isWall() && (getInstanceId() == 0))
		{
			if (!attacker.isServitor())
			{
				return;
			}
			
			final L2ServitorInstance servitor = (L2ServitorInstance) attacker;
			if (servitor.getTemplate().getRace() != Race.SIEGE_WEAPON)
			{
				return;
			}
		}
		
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	@Override
	public void reduceCurrentHpByDOT(double i, L2Character attacker, Skill skill)
	{
		// doors can't be damaged by DOTs
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		boolean isFort = ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getSiege().isInProgress());
		boolean isCastle = ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getSiege().isInProgress());
		boolean isHall = ((getClanHall() != null) && getClanHall().isSiegableHall() && ((SiegableHall) getClanHall()).isInSiege());
		
		if (isFort || isCastle || isHall)
		{
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTLE_GATE_BROKEN_DOWN));
		}
		return true;
	}
	
	@Override
	public void moveToLocation(int x, int y, int z, int offset)
	{
	}
	
	@Override
	public void stopMove(Location loc)
	{
	}
	
	@Override
	public void doAttack(L2Character target)
	{
	}
	
	@Override
	public void doCast(Skill skill)
	{
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (isVisibleFor(activeChar))
		{
			if (getEmitter() > 0)
			{
				activeChar.sendPacket(new OnEventTrigger(this, getOpen()));
			}
			
			activeChar.sendPacket(new StaticObject(this, activeChar.isGM()));
		}
	}
	
	public void setTargetable(boolean b)
	{
		_isTargetable = b;
		broadcastStatusUpdate();
	}
	
	@Override
	public boolean isTargetable()
	{
		return _isTargetable;
	}
	
	public boolean checkCollision()
	{
		return getTemplate().isCheckCollision();
	}
	
	/**
	 * All doors are stored at DoorTable except instance doors
	 * @param doorId
	 * @return
	 */
	private L2DoorInstance getSiblingDoor(int doorId)
	{
		if (getInstanceId() == 0)
		{
			return DoorData.getInstance().getDoor(doorId);
		}
		
		Instance inst = InstanceManager.getInstance().getInstance(getInstanceId());
		if (inst != null)
		{
			return inst.getDoor(doorId);
		}
		
		return null; // 2 late
	}
	
	private void startAutoCloseTask()
	{
		if ((getTemplate().getCloseTime() < 0) || isOpenableByTime())
		{
			return;
		}
		Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		_autoCloseTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoClose(), getTemplate().getCloseTime() * 1000);
	}
	
	class AutoClose implements Runnable
	{
		@Override
		public void run()
		{
			if (getOpen())
			{
				closeMe();
			}
		}
	}
	
	class TimerOpen implements Runnable
	{
		@Override
		public void run()
		{
			boolean open = getOpen();
			if (open)
			{
				closeMe();
			}
			else
			{
				openMe();
			}
			
			int delay = open ? getTemplate().getCloseTime() : getTemplate().getOpenTime();
			if (getTemplate().getRandomTime() > 0)
			{
				delay += Rnd.get(getTemplate().getRandomTime());
			}
			ThreadPoolManager.getInstance().scheduleGeneral(this, delay * 1000);
		}
	}
	
	@Override
	public boolean isDoor()
	{
		return true;
	}
}
