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
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.enums.TrapAction;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.knownlist.TrapKnownList;
import com.l2jserver.gameserver.model.actor.tasks.npc.trap.TrapTask;
import com.l2jserver.gameserver.model.actor.tasks.npc.trap.TrapTriggerTask;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.events.EventDispatcher;
import com.l2jserver.gameserver.model.events.impl.character.trap.OnTrapAction;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.items.L2Weapon;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameManager;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AbstractNpcInfo.TrapInfo;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.taskmanager.DecayTaskManager;

/**
 * Trap instance.
 * @author Zoey76
 */
public final class L2TrapInstance extends L2Npc
{
	private static final int TICK = 1000; // 1s
	private boolean _hasLifeTime;
	private boolean _isInArena = false;
	private boolean _isTriggered;
	private final int _lifeTime;
	private L2PcInstance _owner;
	private final List<Integer> _playersWhoDetectedMe = new ArrayList<>();
	private final SkillHolder _skill;
	private int _remainingTime;
	// Tasks
	private ScheduledFuture<?> _trapTask = null;
	
	/**
	 * Creates a trap.
	 * @param template the trap NPC template
	 * @param instanceId the instance ID
	 * @param lifeTime the life time
	 */
	public L2TrapInstance(L2NpcTemplate template, int instanceId, int lifeTime)
	{
		super(template);
		setInstanceType(InstanceType.L2TrapInstance);
		setInstanceId(instanceId);
		setName(template.getName());
		setIsInvul(false);
		
		_owner = null;
		_isTriggered = false;
		_skill = getTemplate().getParameters().getObject("trap_skill", SkillHolder.class);
		_hasLifeTime = lifeTime >= 0;
		_lifeTime = lifeTime != 0 ? lifeTime : 30000;
		_remainingTime = _lifeTime;
		if (_skill != null)
		{
			_trapTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TrapTask(this), TICK, TICK);
		}
	}
	
	/**
	 * Creates a trap.
	 * @param template the trap NPC template
	 * @param owner the owner
	 * @param lifeTime the life time
	 */
	public L2TrapInstance(L2NpcTemplate template, L2PcInstance owner, int lifeTime)
	{
		this(template, owner.getInstanceId(), lifeTime);
		_owner = owner;
	}
	
	@Override
	public void broadcastPacket(L2GameServerPacket mov)
	{
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if ((player != null) && (_isTriggered || canBeSeen(player)))
			{
				player.sendPacket(mov);
			}
		}
	}
	
	@Override
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if ((player != null) && isInsideRadius(player, radiusInKnownlist, false, false) && (_isTriggered || canBeSeen(player)))
			{
				player.sendPacket(mov);
			}
		}
	}
	
	/**
	 * Verify if the character can see the trap.
	 * @param cha the character to verify
	 * @return {@code true} if the character can see the trap, {@code false} otherwise
	 */
	public boolean canBeSeen(L2Character cha)
	{
		if ((cha != null) && _playersWhoDetectedMe.contains(cha.getObjectId()))
		{
			return true;
		}
		
		if ((_owner == null) || (cha == null))
		{
			return false;
		}
		if (cha == _owner)
		{
			return true;
		}
		
		if (cha instanceof L2PcInstance)
		{
			// observers can't see trap
			if (((L2PcInstance) cha).inObserverMode())
			{
				return false;
			}
			
			// olympiad competitors can't see trap
			if (_owner.isInOlympiadMode() && ((L2PcInstance) cha).isInOlympiadMode() && (((L2PcInstance) cha).getOlympiadSide() != _owner.getOlympiadSide()))
			{
				return false;
			}
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		if (_owner.isInParty() && cha.isInParty() && (_owner.getParty().getLeaderObjectId() == cha.getParty().getLeaderObjectId()))
		{
			return true;
		}
		return false;
	}
	
	public boolean checkTarget(L2Character target)
	{
		// Range seems to be reduced from Freya(300) to H5(150)
		if (!target.isInsideRadius(this, 150, false, false))
		{
			return false;
		}
		
		if (!Skill.checkForAreaOffensiveSkills(this, target, _skill.getSkill(), _isInArena))
		{
			return false;
		}
		
		// observers
		if (target.isPlayer() && target.getActingPlayer().inObserverMode())
		{
			return false;
		}
		
		// olympiad own team and their summons not attacked
		if ((_owner != null) && _owner.isInOlympiadMode())
		{
			final L2PcInstance player = target.getActingPlayer();
			if ((player != null) && player.isInOlympiadMode() && (player.getOlympiadSide() == _owner.getOlympiadSide()))
			{
				return false;
			}
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		// trap owned by players not attack non-flagged players
		if (_owner != null)
		{
			if (target instanceof L2Attackable)
			{
				return true;
			}
			
			final L2PcInstance player = target.getActingPlayer();
			if ((player == null) || ((player.getPvpFlag() == 0) && (player.getKarma() == 0)))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean deleteMe()
	{
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		return super.deleteMe();
	}
	
	@Override
	public L2PcInstance getActingPlayer()
	{
		return _owner;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	public int getKarma()
	{
		return _owner != null ? _owner.getKarma() : 0;
	}
	
	@Override
	public TrapKnownList getKnownList()
	{
		return (TrapKnownList) super.getKnownList();
	}
	
	/**
	 * Get the owner of this trap.
	 * @return the owner
	 */
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	public byte getPvpFlag()
	{
		return _owner != null ? _owner.getPvpFlag() : 0;
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
	
	public Skill getSkill()
	{
		return _skill.getSkill();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new TrapKnownList(this));
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return !canBeSeen(attacker);
	}
	
	@Override
	public boolean isTrap()
	{
		return true;
	}
	
	/**
	 * Checks is triggered
	 * @return True if trap is triggered.
	 */
	public boolean isTriggered()
	{
		return _isTriggered;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInArena = isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE);
		_playersWhoDetectedMe.clear();
	}
	
	@Override
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss || (_owner == null))
		{
			return;
		}
		
		if (_owner.isInOlympiadMode() && (target instanceof L2PcInstance) && ((L2PcInstance) target).isInOlympiadMode() && (((L2PcInstance) target).getOlympiadGameId() == _owner.getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
		}
		
		if (target.isInvul() && !(target instanceof L2NpcInstance))
		{
			_owner.sendPacket(SystemMessageId.ATTACK_WAS_BLOCKED);
		}
		else
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DONE_S3_DAMAGE_TO_C2);
			sm.addCharName(this);
			sm.addCharName(target);
			sm.addInt(damage);
			_owner.sendPacket(sm);
		}
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (_isTriggered || canBeSeen(activeChar))
		{
			activeChar.sendPacket(new TrapInfo(this, activeChar));
		}
	}
	
	public void setDetected(L2Character detector)
	{
		if (_isInArena)
		{
			if (detector.isPlayable())
			{
				sendInfo(detector.getActingPlayer());
			}
			return;
		}
		
		if ((_owner != null) && (_owner.getPvpFlag() == 0) && (_owner.getKarma() == 0))
		{
			return;
		}
		
		_playersWhoDetectedMe.add(detector.getObjectId());
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, detector, TrapAction.TRAP_DETECTED), this);
		
		if (detector.isPlayable())
		{
			sendInfo(detector.getActingPlayer());
		}
	}
	
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancel(this);
	}
	
	/**
	 * Trigger the trap.
	 * @param target the target
	 */
	public void triggerTrap(L2Character target)
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		_isTriggered = true;
		broadcastPacket(new TrapInfo(this, null));
		setTarget(target);
		
		EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, target, TrapAction.TRAP_TRIGGERED), this);
		
		ThreadPoolManager.getInstance().scheduleGeneral(new TrapTriggerTask(this), 500);
	}
	
	public void unSummon()
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		
		if (isVisible() && !isDead())
		{
			if (getWorldRegion() != null)
			{
				getWorldRegion().removeFromZones(this);
			}
			
			deleteMe();
		}
	}
	
	@Override
	public void updateAbnormalEffect()
	{
		
	}
	
	public boolean hasLifeTime()
	{
		return _hasLifeTime;
	}
	
	public void setHasLifeTime(boolean val)
	{
		_hasLifeTime = val;
	}
	
	public int getRemainingTime()
	{
		return _remainingTime;
	}
	
	public void setRemainingTime(int time)
	{
		_remainingTime = time;
	}
	
	public int getLifeTime()
	{
		return _lifeTime;
	}
}
