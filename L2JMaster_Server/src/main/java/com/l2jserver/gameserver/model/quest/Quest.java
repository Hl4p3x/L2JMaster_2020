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
package com.l2jserver.gameserver.model.quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Predicate;
import java.util.logging.Level;

import com.l2jserver.Config;
import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.data.xml.impl.NpcData;
import com.l2jserver.gameserver.datatables.SpawnTable;
import com.l2jserver.gameserver.enums.CategoryType;
import com.l2jserver.gameserver.enums.Race;
import com.l2jserver.gameserver.enums.TrapAction;
import com.l2jserver.gameserver.enums.audio.IAudio;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TrapInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.base.AcquireSkillType;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.events.AbstractScript;
import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.listeners.AbstractEventListener;
import com.l2jserver.gameserver.model.events.returns.TerminateReturn;
import com.l2jserver.gameserver.model.interfaces.IIdentifiable;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.olympiad.CompetitionType;
import com.l2jserver.gameserver.model.olympiad.Participant;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.TutorialShowHtml;
import com.l2jserver.gameserver.scripting.ScriptManager;
import com.l2jserver.util.Rnd;
import com.l2jserver.util.Util;

/**
 * Quest main class.
 * @author Luis Arias
 */
public class Quest extends AbstractScript implements IIdentifiable
{
	/** Map containing lists of timers from the name of the timer. */
	private volatile Map<String, List<QuestTimer>> _questTimers = null;
	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();
	private final WriteLock _writeLock = _rwLock.writeLock();
	private final ReadLock _readLock = _rwLock.readLock();
	/** Map containing all the start conditions. */
	private volatile Map<Predicate<L2PcInstance>, String> _startCondition = null;
	
	private final int _questId;
	private final String _name;
	private final String _descr;
	private final byte _initialState = State.CREATED;
	protected boolean _onEnterWorld = false;
	private boolean _isCustom = false;
	
	public int[] questItemIds = null;
	
	private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String DEFAULT_ALREADY_COMPLETED_MSG = "<html><body>This quest has already been completed.</body></html>";
	
	private static final String QUEST_DELETE_FROM_CHAR_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?";
	
	private static final int RESET_HOUR = 6;
	private static final int RESET_MINUTES = 30;
	
	/**
	 * The Quest object constructor.<br>
	 * Constructing a quest also calls the {@code init_LoadGlobalData} convenience method.
	 * @param questId ID of the quest
	 * @param name String corresponding to the name of the quest
	 * @param descr String for the description of the quest
	 */
	public Quest(int questId, String name, String descr)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
		if (questId > 0)
		{
			QuestManager.getInstance().addQuest(this);
		}
		else
		{
			QuestManager.getInstance().addScript(this);
		}
		
		loadGlobalData();
	}
	
	/**
	 * Gets the reset hour for a daily quest.
	 * @return the reset hour
	 */
	public int getResetHour()
	{
		return RESET_HOUR;
	}
	
	/**
	 * Gets the reset minutes for a daily quest.
	 * @return the reset minutes
	 */
	public int getResetMinutes()
	{
		return RESET_MINUTES;
	}
	
	/**
	 * This method is, by default, called by the constructor of all scripts.<br>
	 * Children of this class can implement this function in order to define what variables to load and what structures to save them in.<br>
	 * By default, nothing is loaded.
	 */
	protected void loadGlobalData()
	{
		
	}
	
	/**
	 * The function saveGlobalData is, by default, called at shutdown, for all quests, by the QuestManager.<br>
	 * Children of this class can implement this function in order to convert their structures<br>
	 * into <var, value> tuples and make calls to save them to the database, if needed.<br>
	 * By default, nothing is saved.
	 */
	public void saveGlobalData()
	{
		
	}
	
	/**
	 * Gets the quest ID.
	 * @return the quest ID
	 */
	@Override
	public int getId()
	{
		return _questId;
	}
	
	/**
	 * Add a new quest state of this quest to the database.
	 * @param player the owner of the newly created quest state
	 * @return the newly created {@link QuestState} object
	 */
	public QuestState newQuestState(L2PcInstance player)
	{
		return new QuestState(this, player, _initialState);
	}
	
	/**
	 * Get the specified player's {@link QuestState} object for this quest.<br>
	 * If the player does not have it and initIfNode is {@code true},<br>
	 * create a new QuestState object and return it, otherwise return {@code null}.
	 * @param player the player whose QuestState to get
	 * @param initIfNone if true and the player does not have a QuestState for this quest,<br>
	 *            create a new QuestState
	 * @return the QuestState object for this quest or null if it doesn't exist
	 */
	public QuestState getQuestState(L2PcInstance player, boolean initIfNone)
	{
		final QuestState qs = player.getQuestState(_name);
		if ((qs != null) || !initIfNone)
		{
			return qs;
		}
		return newQuestState(player);
	}
	
	/**
	 * @return the initial state of the quest
	 */
	public byte getInitialState()
	{
		return _initialState;
	}
	
	@Override
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the description of the quest
	 */
	public String getDescr()
	{
		return _descr;
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onAdvEvent(String, L2Npc, L2PcInstance)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the npc associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @see #startQuestTimer(String, long, L2Npc, L2PcInstance, boolean)
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	/**
	 * Gets the quest timers.
	 * @return the quest timers
	 */
	public final Map<String, List<QuestTimer>> getQuestTimers()
	{
		if (_questTimers == null)
		{
			synchronized (this)
			{
				if (_questTimers == null)
				{
					_questTimers = new ConcurrentHashMap<>(1);
				}
			}
		}
		return _questTimers;
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onAdvEvent(String, L2Npc, L2PcInstance)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the npc associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @param repeating indicates whether the timer is repeatable or one-time.<br>
	 *            If {@code true}, the task is repeated every {@code time} milliseconds until explicitly stopped.
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player, boolean repeating)
	{
		final List<QuestTimer> timers = getQuestTimers().computeIfAbsent(name, k -> new ArrayList<>(1));
		// if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
		// nulls act as wildcards
		if (getQuestTimer(name, npc, player) == null)
		{
			_writeLock.lock();
			try
			{
				timers.add(new QuestTimer(this, name, time, npc, player, repeating));
			}
			finally
			{
				_writeLock.unlock();
			}
		}
	}
	
	/**
	 * Get a quest timer that matches the provided name and parameters.
	 * @param name the name of the quest timer to get
	 * @param npc the NPC associated with the quest timer to get
	 * @param player the player associated with the quest timer to get
	 * @return the quest timer that matches the specified parameters or {@code null} if nothing was found
	 */
	public QuestTimer getQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		if (_questTimers == null)
		{
			return null;
		}
		
		final List<QuestTimer> timers = getQuestTimers().get(name);
		if (timers != null)
		{
			_readLock.lock();
			try
			{
				for (QuestTimer timer : timers)
				{
					if (timer != null)
					{
						if (timer.isMatch(this, name, npc, player))
						{
							return timer;
						}
					}
				}
			}
			finally
			{
				_readLock.unlock();
			}
		}
		return null;
	}
	
	/**
	 * Cancel all quest timers with the specified name.
	 * @param name the name of the quest timers to cancel
	 */
	public void cancelQuestTimers(String name)
	{
		if (_questTimers == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = getQuestTimers().get(name);
		if (timers != null)
		{
			_writeLock.lock();
			try
			{
				for (QuestTimer timer : timers)
				{
					if (timer != null)
					{
						timer.cancel();
					}
				}
				timers.clear();
			}
			finally
			{
				_writeLock.unlock();
			}
		}
	}
	
	/**
	 * Cancel the quest timer that matches the specified name and parameters.
	 * @param name the name of the quest timer to cancel
	 * @param npc the NPC associated with the quest timer to cancel
	 * @param player the player associated with the quest timer to cancel
	 */
	public void cancelQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		final QuestTimer timer = getQuestTimer(name, npc, player);
		if (timer != null)
		{
			timer.cancelAndRemove();
		}
	}
	
	/**
	 * Remove a quest timer from the list of all timers.<br>
	 * Note: does not stop the timer itself!
	 * @param timer the {@link QuestState} object to remove
	 */
	public void removeQuestTimer(QuestTimer timer)
	{
		if ((timer != null) && (_questTimers != null))
		{
			final List<QuestTimer> timers = getQuestTimers().get(timer.getName());
			if (timers != null)
			{
				_writeLock.lock();
				try
				{
					timers.remove(timer);
				}
				finally
				{
					_writeLock.unlock();
				}
			}
		}
	}
	
	// These are methods to call within the core to call the quest events.
	
	/**
	 * @param npc the NPC that was attacked
	 * @param attacker the attacking player
	 * @param damage the damage dealt to the NPC by the player
	 * @param isSummon if {@code true}, the attack was actually made by the player's summon
	 * @param skill the skill used to attack the NPC (can be null)
	 */
	public final void notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon, Skill skill)
	{
		String res = null;
		try
		{
			res = onAttack(npc, attacker, damage, isSummon, skill);
		}
		catch (Exception e)
		{
			showError(attacker, e);
			return;
		}
		showResult(attacker, res);
	}
	
	/**
	 * @param killer the character that killed the {@code victim}
	 * @param victim the character that was killed by the {@code killer}
	 * @param qs the quest state object of the player to be notified of this event
	 */
	public final void notifyDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch (Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}
	
	/**
	 * @param item
	 * @param player
	 */
	public final void notifyItemUse(L2Item item, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onItemUse(item, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param instance
	 * @param player
	 * @param skill
	 */
	public final void notifySpellFinished(L2Npc instance, L2PcInstance player, Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(instance, player, skill);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * Notify quest script when something happens with a trap.
	 * @param trap the trap instance which triggers the notification
	 * @param trigger the character which makes effect on the trap
	 * @param action 0: trap casting its skill. 1: trigger detects the trap. 2: trigger removes the trap
	 */
	public final void notifyTrapAction(L2TrapInstance trap, L2Character trigger, TrapAction action)
	{
		String res = null;
		try
		{
			res = onTrapAction(trap, trigger, action);
		}
		catch (Exception e)
		{
			if (trigger.getActingPlayer() != null)
			{
				showError(trigger.getActingPlayer(), e);
			}
			_log.log(Level.WARNING, "Exception on onTrapAction() in notifyTrapAction(): " + e.getMessage(), e);
			return;
		}
		if (trigger.getActingPlayer() != null)
		{
			showResult(trigger.getActingPlayer(), res);
		}
	}
	
	/**
	 * @param npc the spawned NPC
	 */
	public final void notifySpawn(L2Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onSpawn() in notifySpawn(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc the teleport NPC
	 */
	public final void notifyTeleport(L2Npc npc)
	{
		try
		{
			onTeleport(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onTeleport() in notifyTeleport(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 */
	public final void notifyEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onAdvEvent(event, npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res, npc);
	}
	
	/**
	 * @param player the player entering the world
	 */
	public final void notifyEnterWorld(L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	public final void notifyTutorialEvent(L2PcInstance player, String command)
	{
		try
		{
			onTutorialEvent(player, command);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
	}
	
	public final void notifyTutorialClientEvent(L2PcInstance player, int event)
	{
		try
		{
			onTutorialClientEvent(player, event);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
	}
	
	public final void notifyTutorialQuestionMark(L2PcInstance player, int number)
	{
		String res = null;
		try
		{
			res = onTutorialQuestionMark(player, number);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	public final void notifyTutorialCmd(L2PcInstance player, String command)
	{
		String res = null;
		try
		{
			res = onTutorialCmd(player, command);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param killer
	 * @param isSummon
	 */
	public final void notifyKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onKill(npc, killer, isSummon);
		}
		catch (Exception e)
		{
			showError(killer, e);
			return;
		}
		showResult(killer, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 */
	public final void notifyTalk(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			final String startConditionHtml = getStartConditionHtml(player);
			if (!player.hasQuestState(_name) && (startConditionHtml != null))
			{
				res = startConditionHtml;
			}
			else
			{
				res = onTalk(npc, player);
			}
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		player.setLastQuestNpcObject(npc.getObjectId());
		showResult(player, res, npc);
	}
	
	/**
	 * Override the default NPC dialogs when a quest defines this for the given NPC.<br>
	 * Note: If the default html for this npc needs to be shown, onFirstTalk should call npc.showChatWindow(player) and then return null.
	 * @param npc the NPC whose dialogs to override
	 * @param player the player talking to the NPC
	 */
	public final void notifyFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res, npc);
	}
	
	/**
	 * Notify the quest engine that an skill has been acquired.
	 * @param npc the NPC
	 * @param player the player
	 * @param skill the skill
	 * @param type the skill learn type
	 */
	public final void notifyAcquireSkill(L2Npc npc, L2PcInstance player, Skill skill, AcquireSkillType type)
	{
		String res = null;
		try
		{
			res = onAcquireSkill(npc, player, skill, type);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param item
	 * @param player
	 */
	public final void notifyItemTalk(L2ItemInstance item, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onItemTalk(item, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param item
	 * @param player
	 * @return
	 */
	public String onItemTalk(L2ItemInstance item, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * @param item
	 * @param player
	 * @param event
	 */
	public final void notifyItemEvent(L2ItemInstance item, L2PcInstance player, String event)
	{
		String res = null;
		try
		{
			res = onItemEvent(item, player, event);
			if (res != null)
			{
				if (res.equalsIgnoreCase("true") || res.equalsIgnoreCase("false"))
				{
					return;
				}
			}
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param caster
	 * @param skill
	 * @param targets
	 * @param isSummon
	 */
	public final void notifySkillSee(L2Npc npc, L2PcInstance caster, Skill skill, L2Object[] targets, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onSkillSee(npc, caster, skill, targets, isSummon);
		}
		catch (Exception e)
		{
			showError(caster, e);
			return;
		}
		showResult(caster, res);
	}
	
	/**
	 * @param npc
	 * @param caller
	 * @param attacker
	 * @param isSummon
	 */
	public final void notifyFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isSummon);
		}
		catch (Exception e)
		{
			showError(attacker, e);
			return;
		}
		showResult(attacker, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param isSummon
	 */
	public final void notifyAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onAggroRangeEnter(npc, player, isSummon);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc the NPC that sees the creature
	 * @param creature the creature seen by the NPC
	 * @param isSummon
	 */
	public final void notifySeeCreature(L2Npc npc, L2Character creature, boolean isSummon)
	{
		L2PcInstance player = null;
		if (isSummon || creature.isPlayer())
		{
			player = creature.getActingPlayer();
		}
		String res = null;
		try
		{
			res = onSeeCreature(npc, creature, isSummon);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
			return;
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - L2Object to pass, if needed
	 */
	public final void notifyEventReceived(String eventName, L2Npc sender, L2Npc receiver, L2Object reference)
	{
		try
		{
			onEventReceived(eventName, sender, receiver, reference);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onEventReceived() in notifyEventReceived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param character
	 * @param zone
	 */
	public final void notifyEnterZone(L2Character character, L2ZoneType zone)
	{
		L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onEnterZone(character, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
			return;
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param character
	 * @param zone
	 */
	public final void notifyExitZone(L2Character character, L2ZoneType zone)
	{
		L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onExitZone(character, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
			return;
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param winner
	 * @param looser
	 * @param type
	 */
	public final void notifyOlympiadMatch(Participant winner, Participant looser, CompetitionType type)
	{
		try
		{
			onOlympiadMatchFinish(winner, looser, type);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Execution on onOlympiadMatchFinish() in notifyOlympiadMatch(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public final void notifyMoveFinished(L2Npc npc)
	{
		try
		{
			onMoveFinished(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onMoveFinished() in notifyMoveFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public final void notifyNodeArrived(L2Npc npc)
	{
		try
		{
			onNodeArrived(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onNodeArrived() in notifyNodeArrived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public final void notifyRouteFinished(L2Npc npc)
	{
		try
		{
			onRouteFinished(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onRouteFinished() in notifyRouteFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 * @param player
	 * @return {@code true} if player can see this npc, {@code false} otherwise.
	 */
	public final boolean notifyOnCanSeeMe(L2Npc npc, L2PcInstance player)
	{
		try
		{
			return onCanSeeMe(npc, player);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onCanSeeMe() in notifyOnCanSeeMe(): " + e.getMessage(), e);
		}
		return false;
	}
	
	// These are methods that java calls to invoke scripts.
	
	/**
	 * This function is called in place of {@link #onAttack(L2Npc, L2PcInstance, int, boolean, Skill)} if the former is not implemented.<br>
	 * If a script contains both onAttack(..) implementations, then this method will never be called unless the script's {@link #onAttack(L2Npc, L2PcInstance, int, boolean, Skill)} explicitly calls this method.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked the NPC.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's pet.
	 * @return
	 */
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player attacks an NPC that is registered for the quest.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onAttack(L2Npc, L2PcInstance, int, boolean)} override.<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked the NPC.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's summon
	 * @param skill parameter is the skill that player used to attack NPC.
	 * @return
	 */
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon, Skill skill)
	{
		return onAttack(npc, attacker, damage, isSummon);
	}
	
	/**
	 * This function is called whenever an <b>exact instance</b> of a character who was previously registered for this event dies.<br>
	 * The registration for {@link #onDeath(L2Character, L2Character, QuestState)} events <b>is not</b> done via the quest itself, but it is instead handled by the QuestState of a particular player.
	 * @param killer this parameter contains a reference to the exact instance of the NPC that <b>killed</b> the character.
	 * @param victim this parameter contains a reference to the exact instance of the character that got killed.
	 * @param qs this parameter contains a reference to the QuestState of whomever was interested (waiting) for this kill.
	 * @return
	 */
	public String onDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		return onAdvEvent("", ((killer instanceof L2Npc) ? ((L2Npc) killer) : null), qs.getPlayer());
	}
	
	/**
	 * This function is called whenever a player clicks on a link in a quest dialog and whenever a timer fires.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onEvent(String, QuestState)} override.<br>
	 * If the player has a quest state, use it as parameter in the next call, otherwise return null.
	 * @param event this parameter contains a string identifier for the event.<br>
	 *            Generally, this string is passed directly via the link.<br>
	 *            For example:<br>
	 *            <code>
	 *            &lt;a action="bypass -h Quest 626_ADarkTwilight 31517-01.htm"&gt;hello&lt;/a&gt;
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param npc this parameter contains a reference to the instance of NPC associated with this event.<br>
	 *            This may be the NPC registered in a timer, or the NPC with whom a player is speaking, etc.<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @param player this parameter contains a reference to the player participating in this function.<br>
	 *            It may be the player speaking to the NPC, or the player who caused a timer to start (and owns that timer).<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (player != null)
		{
			final QuestState qs = player.getQuestState(getName());
			if (qs != null)
			{
				return onEvent(event, qs);
			}
		}
		return null;
	}
	
	/**
	 * This function is called in place of {@link #onAdvEvent(String, L2Npc, L2PcInstance)} if the former is not implemented.<br>
	 * If a script contains both {@link #onAdvEvent(String, L2Npc, L2PcInstance)} and this implementation, then this method will never be called unless the script's {@link #onAdvEvent(String, L2Npc, L2PcInstance)} explicitly calls this method.
	 * @param event this parameter contains a string identifier for the event.<br>
	 *            Generally, this string is passed directly via the link.<br>
	 *            For example:<br>
	 *            <code>
	 *            &lt;a action="bypass -h Quest 626_ADarkTwilight 31517-01.htm"&gt;hello&lt;/a&gt;
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param qs this parameter contains a reference to the quest state of the player who used the link or started the timer.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onEvent(String event, QuestState qs)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player kills a NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got killed.
	 * @param killer this parameter contains a reference to the exact instance of the player who killed the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the killer was the player's pet.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player clicks to the "Quest" link of an NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param talker this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player talks to an NPC that is registered for the quest.<br>
	 * That is, it is triggered from the very first click on the NPC, not via another dialog.<br>
	 * <b>Note 1:</b><br>
	 * Each NPC can be registered to at most one quest for triggering this function.<br>
	 * In other words, the same one NPC cannot respond to an "onFirstTalk" request from two different quests.<br>
	 * Attempting to register an NPC in two different quests for this function will result in one of the two registration being ignored.<br>
	 * <b>Note 2:</b><br>
	 * Since a Quest link isn't clicked in order to reach this, a quest state can be invalid within this function.<br>
	 * The coder of the script may need to create a new quest state (if necessary).<br>
	 * <b>Note 3:</b><br>
	 * The returned value of onFirstTalk replaces the default HTML that would have otherwise been loaded from a sub-folder of DatapackRoot/game/data/html/.<br>
	 * If you wish to show the default HTML, within onFirstTalk do npc.showChatWindow(player) and then return ""<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param player this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 * @since <a href="http://trac.l2jserver.com/changeset/771">Jython AI support for "onFirstTalk"</a>
	 */
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * @param item
	 * @param player
	 * @param event
	 * @return
	 */
	public String onItemEvent(L2ItemInstance item, L2PcInstance player, String event)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player request a skill list.<br>
	 * TODO: Re-implement, since Skill Trees rework it's support was removed.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill list.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill list.
	 * @return
	 */
	public String onAcquireSkillList(L2Npc npc, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player request a skill info.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill info.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill info.
	 * @param skill this parameter contains a reference to the skill that the player requested its info.
	 * @return
	 */
	public String onAcquireSkillInfo(L2Npc npc, L2PcInstance player, Skill skill)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player acquire a skill.<br>
	 * TODO: Re-implement, since Skill Trees rework it's support was removed.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill.
	 * @param skill this parameter contains a reference to the skill that the player requested.
	 * @param type the skill learn type
	 * @return
	 */
	public String onAcquireSkill(L2Npc npc, L2PcInstance player, Skill skill, AcquireSkillType type)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player uses a quest item that has a quest events list.<br>
	 * TODO: complete this documentation and unhardcode it to work with all item uses not with those listed.
	 * @param item the quest item that the player used
	 * @param player the player who used the item
	 * @return
	 */
	public String onItemUse(L2Item item, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player casts a skill near a registered NPC (1000 distance).<br>
	 * <b>Note:</b><br>
	 * If a skill does damage, both onSkillSee(..) and onAttack(..) will be triggered for the damaged NPC!<br>
	 * However, only onSkillSee(..) will be triggered if the skill does no damage,<br>
	 * or if it damages an NPC who has no onAttack(..) registration while near another NPC who has an onSkillSee registration.<br>
	 * TODO: confirm if the distance is 1000 and unhardcode.
	 * @param npc the NPC that saw the skill
	 * @param caster the player who cast the skill
	 * @param skill the actual skill that was used
	 * @param targets an array of all objects (can be any type of object, including mobs and players) that were affected by the skill
	 * @param isSummon if {@code true}, the skill was actually cast by the player's summon, not the player himself
	 * @return
	 */
	public String onSkillSee(L2Npc npc, L2PcInstance caster, Skill skill, L2Object[] targets, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC finishes casting a skill.
	 * @param npc the NPC that casted the skill.
	 * @param player the player who is the target of the skill. Can be {@code null}.
	 * @param skill the actual skill that was used by the NPC.
	 * @return
	 */
	public String onSpellFinished(L2Npc npc, L2PcInstance player, Skill skill)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a trap action is performed.
	 * @param trap this parameter contains a reference to the exact instance of the trap that was activated.
	 * @param trigger this parameter contains a reference to the exact instance of the character that triggered the action.
	 * @param action this parameter contains a reference to the action that was triggered.
	 * @return
	 */
	public String onTrapAction(L2TrapInstance trap, L2Character trigger, TrapAction action)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC spawns or re-spawns and passes a reference to the newly (re)spawned NPC.<br>
	 * Currently the only function that has no reference to a player.<br>
	 * It is useful for initializations, starting quest timers, displaying chat (NpcSay), and more.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who just (re)spawned.
	 * @return
	 */
	public String onSpawn(L2Npc npc)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC is teleport.<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC who just teleport.
	 */
	protected void onTeleport(L2Npc npc)
	{
	}
	
	/**
	 * This function is called whenever an NPC is called by another NPC in the same faction.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who is being asked for help.
	 * @param caller this parameter contains a reference to the exact instance of the NPC who is asking for help.<br>
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the attacker was the player's summon.
	 * @return
	 */
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player enters an NPC aggression range.
	 * @param npc this parameter contains a reference to the exact instance of the NPC whose aggression range is being transgressed.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering the NPC's aggression range.
	 * @param isSummon this parameter if it's {@code false} it denotes that the character that entered the aggression range was indeed the player, else it specifies that the character was the player's summon.
	 * @return
	 */
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a NPC "sees" a creature.
	 * @param npc the NPC who sees the creature
	 * @param creature the creature seen by the NPC
	 * @param isSummon this parameter if it's {@code false} it denotes that the character seen by the NPC was indeed the player, else it specifies that the character was the player's summon
	 * @return
	 */
	public String onSeeCreature(L2Npc npc, L2Character creature, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player enters the game.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering to the world.
	 * @return
	 */
	public String onEnterWorld(L2PcInstance player)
	{
		return null;
	}
	
	public void onTutorialEvent(L2PcInstance player, String command)
	{
	}
	
	public void onTutorialClientEvent(L2PcInstance player, int event)
	{
	}
	
	public String onTutorialQuestionMark(L2PcInstance player, int number)
	{
		return null;
	}
	
	public String onTutorialCmd(L2PcInstance player, String command)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a character enters a registered zone.
	 * @param character this parameter contains a reference to the exact instance of the character who is entering the zone.
	 * @param zone this parameter contains a reference to the zone.
	 * @return
	 */
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a character exits a registered zone.
	 * @param character this parameter contains a reference to the exact instance of the character who is exiting the zone.
	 * @param zone this parameter contains a reference to the zone.
	 * @return
	 */
	public String onExitZone(L2Character character, L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - L2Object to pass, if needed
	 * @return
	 */
	public String onEventReceived(String eventName, L2Npc sender, L2Npc receiver, L2Object reference)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player wins an Olympiad Game.
	 * @param winner in this match.
	 * @param looser in this match.
	 * @param type the competition type.
	 */
	public void onOlympiadMatchFinish(Participant winner, Participant looser, CompetitionType type)
	{
		
	}
	
	/**
	 * This function is called whenever a player looses an Olympiad Game.
	 * @param loser this parameter contains a reference to the exact instance of the player who lose the competition.
	 * @param type this parameter contains a reference to the competition type.
	 */
	public void onOlympiadLose(L2PcInstance loser, CompetitionType type)
	{
		
	}
	
	/**
	 * This function is called whenever a NPC finishes moving
	 * @param npc registered NPC
	 */
	public void onMoveFinished(L2Npc npc)
	{
		
	}
	
	/**
	 * This function is called whenever a walker NPC (controlled by WalkingManager) arrive a walking node
	 * @param npc registered NPC
	 */
	public void onNodeArrived(L2Npc npc)
	{
		
	}
	
	/**
	 * This function is called whenever a walker NPC (controlled by WalkingManager) arrive to last node
	 * @param npc registered NPC
	 */
	public void onRouteFinished(L2Npc npc)
	{
		
	}
	
	/**
	 * @param mob
	 * @param player
	 * @param isSummon
	 * @return {@code true} if npc can hate the playable, {@code false} otherwise.
	 */
	public boolean onNpcHate(L2Attackable mob, L2PcInstance player, boolean isSummon)
	{
		return true;
	}
	
	/**
	 * @param summon
	 */
	public void onSummonSpawn(L2Summon summon)
	{
		
	}
	
	/**
	 * @param summon
	 */
	public void onSummonTalk(L2Summon summon)
	{
		
	}
	
	/**
	 * @param npc
	 * @param player
	 * @return {@code true} if player can see this npc, {@code false} otherwise.
	 */
	public boolean onCanSeeMe(L2Npc npc, L2PcInstance player)
	{
		return false;
	}
	
	/**
	 * Show an error message to the specified player.
	 * @param player the player to whom to send the error (must be a GM)
	 * @param t the {@link Throwable} to get the message/stacktrace from
	 * @return {@code false}
	 */
	public boolean showError(L2PcInstance player, Throwable t)
	{
		_log.log(Level.WARNING, getScriptFile().getAbsolutePath(), t);
		if (t.getMessage() == null)
		{
			_log.warning(getClass().getSimpleName() + ": " + t.getMessage());
		}
		if ((player != null) && player.getAccessLevel().isGm())
		{
			String res = "<html><body><title>Script error</title>" + Util.getStackTrace(t) + "</body></html>";
			return showResult(player, res);
		}
		return false;
	}
	
	/**
	 * @param player the player to whom to show the result
	 * @param res the message to show to the player
	 * @return {@code false} if the message was sent, {@code true} otherwise
	 * @see #showResult(L2PcInstance, String, L2Npc)
	 */
	public boolean showResult(L2PcInstance player, String res)
	{
		return showResult(player, res, null);
	}
	
	/**
	 * Show a message to the specified player.<br>
	 * <u><i>Concept:</i></u><br>
	 * Three cases are managed according to the value of the {@code res} parameter:<br>
	 * <ul>
	 * <li><u>{@code res} ends with ".htm" or ".html":</u> the contents of the specified HTML file are shown in a dialog window</li>
	 * <li><u>{@code res} starts with "&lt;html&gt;":</u> the contents of the parameter are shown in a dialog window</li>
	 * <li><u>all other cases :</u> the text contained in the parameter is shown in chat</li>
	 * </ul>
	 * @param player the player to whom to show the result
	 * @param npc npc to show the result for
	 * @param res the message to show to the player
	 * @return {@code false} if the message was sent, {@code true} otherwise
	 */
	public boolean showResult(L2PcInstance player, String res, L2Npc npc)
	{
		if ((res == null) || res.isEmpty() || (player == null))
		{
			return true;
		}
		
		if (res.endsWith(".htm") || res.endsWith(".html"))
		{
			showHtmlFile(player, res, npc);
		}
		else if (res.startsWith("<html"))
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, res);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.sendMessage(res);
		}
		return false;
	}
	
	/**
	 * Loads all quest states and variables for the specified player.
	 * @param player the player who is entering the world
	 */
	public static final void playerEnter(L2PcInstance player)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ?");
			PreparedStatement ps1 = con.prepareStatement("SELECT name, value FROM character_quests WHERE charId = ? AND var = ?"))
		{
			// Get list of quests owned by the player from database
			
			ps1.setInt(1, player.getObjectId());
			ps1.setString(2, "<state>");
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					// Get the ID of the quest and its state
					String questId = rs.getString("name");
					String statename = rs.getString("value");
					
					// Search quest associated with the ID
					Quest q = QuestManager.getInstance().getQuest(questId);
					if (q == null)
					{
						_log.finer("Unknown quest " + questId + " for player " + player.getName());
						if (Config.AUTODELETE_INVALID_QUEST_DATA)
						{
							invalidQuestData.setInt(1, player.getObjectId());
							invalidQuestData.setString(2, questId);
							invalidQuestData.executeUpdate();
						}
						continue;
					}
					
					// Create a new QuestState for the player that will be added to the player's list of quests
					new QuestState(q, player, State.getStateId(statename));
				}
			}
			
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			try (PreparedStatement ps2 = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE charId = ? AND var <> ?"))
			{
				ps2.setInt(1, player.getObjectId());
				ps2.setString(2, "<state>");
				try (ResultSet rs = ps2.executeQuery())
				{
					while (rs.next())
					{
						String questId = rs.getString("name");
						String var = rs.getString("var");
						String value = rs.getString("value");
						// Get the QuestState saved in the loop before
						QuestState qs = player.getQuestState(questId);
						if (qs == null)
						{
							_log.finer("Lost variable " + var + " in quest " + questId + " for player " + player.getName());
							if (Config.AUTODELETE_INVALID_QUEST_DATA)
							{
								invalidQuestDataVar.setInt(1, player.getObjectId());
								invalidQuestDataVar.setString(2, questId);
								invalidQuestDataVar.setString(3, var);
								invalidQuestDataVar.executeUpdate();
							}
							continue;
						}
						// Add parameter to the quest
						qs.setInternal(var, value);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
		
		// events
		for (String name : QuestManager.getInstance().getScripts().keySet())
		{
			player.processQuestEvent(name, "enter");
		}
	}
	
	/**
	 * Insert (or update) in the database variables that need to stay persistent for this quest after a reboot.<br>
	 * This function is for storage of values that do not related to a specific player but are global for all characters.<br>
	 * For example, if we need to disable a quest-gatekeeper until a certain time (as is done with some grand-boss gatekeepers), we can save that time in the DB.
	 * @param var the name of the variable to save
	 * @param value the value of the variable
	 */
	public final void saveGlobalQuestVar(String var, String value)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)"))
		{
			ps.setString(1, getName());
			ps.setString(2, var);
			ps.setString(3, value);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert global quest variable:", e);
		}
	}
	
	/**
	 * Read from the database a previously saved variable for this quest.<br>
	 * Due to performance considerations, this function should best be used only when the quest is first loaded.<br>
	 * Subclasses of this class can define structures into which these loaded values can be saved.<br>
	 * However, on-demand usage of this function throughout the script is not prohibited, only not recommended.<br>
	 * Values read from this function were entered by calls to "saveGlobalQuestVar".
	 * @param var the name of the variable to load
	 * @return the current value of the specified variable, or an empty string if the variable does not exist
	 */
	public final String loadGlobalQuestVar(String var)
	{
		String result = "";
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?"))
		{
			ps.setString(1, getName());
			ps.setString(2, var);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.first())
				{
					result = rs.getString(1);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not load global quest variable:", e);
		}
		return result;
	}
	
	/**
	 * Permanently delete from the database a global quest variable that was previously saved for this quest.
	 * @param var the name of the variable to delete
	 */
	public final void deleteGlobalQuestVar(String var)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?"))
		{
			ps.setString(1, getName());
			ps.setString(2, var);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete global quest variable:", e);
		}
	}
	
	/**
	 * Permanently delete from the database all global quest variables that were previously saved for this quest.
	 */
	public final void deleteAllGlobalQuestVars()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?"))
		{
			ps.setString(1, getName());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete global quest variables:", e);
		}
	}
	
	/**
	 * Insert in the database the quest for the player.
	 * @param qs the {@link QuestState} object whose variable to insert
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?"))
		{
			ps.setInt(1, qs.getPlayer().getObjectId());
			ps.setString(2, qs.getQuestName());
			ps.setString(3, var);
			ps.setString(4, value);
			ps.setString(5, value);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * Update the value of the variable "var" for the specified quest in database
	 * @param qs the {@link QuestState} object whose variable to update
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?"))
		{
			ps.setString(1, value);
			ps.setInt(2, qs.getPlayer().getObjectId());
			ps.setString(3, qs.getQuestName());
			ps.setString(4, var);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not update char quest:", e);
		}
	}
	
	/**
	 * Delete a variable of player's quest from the database.
	 * @param qs the {@link QuestState} object whose variable to delete
	 * @param var the name of the variable to delete
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?"))
		{
			ps.setInt(1, qs.getPlayer().getObjectId());
			ps.setString(2, qs.getQuestName());
			ps.setString(3, var);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * Delete from the database all variables and states of the specified quest state.
	 * @param qs the {@link QuestState} object whose variables to delete
	 * @param repeatable if {@code false}, the state variable will be preserved, otherwise it will be deleted as well
	 */
	public static void deleteQuestInDb(QuestState qs, boolean repeatable)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(repeatable ? QUEST_DELETE_FROM_CHAR_QUERY : QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY))
		{
			ps.setInt(1, qs.getPlayer().getObjectId());
			ps.setString(2, qs.getQuestName());
			if (!repeatable)
			{
				ps.setString(3, "<state>");
			}
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Unable to delete char quest!", e);
		}
	}
	
	/**
	 * Create a database record for the specified quest state.
	 * @param qs the {@link QuestState} object whose data to write in the database
	 */
	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * Update a quest state record of the specified quest state in database.
	 * @param qs the {@link QuestState} object whose data to update in the database
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when no quest is available: "You are either not on a quest that involves this NPC.."
	 */
	public static String getNoQuestMsg(L2PcInstance player)
	{
		final String result = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/noquest.htm");
		if ((result != null) && (result.length() > 0))
		{
			return result;
		}
		return DEFAULT_NO_QUEST_MSG;
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when no quest is already completed: "This quest has already been completed."
	 */
	public static String getAlreadyCompletedMsg(L2PcInstance player)
	{
		final String result = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/alreadycompleted.htm");
		if ((result != null) && (result.length() > 0))
		{
			return result;
		}
		return DEFAULT_ALREADY_COMPLETED_MSG;
	}
	
	// TODO: Remove after all Jython scripts are replaced with Java versions.
	public void addStartNpc(int npcId)
	{
		setNpcQuestStartId(npcId);
	}
	
	public void addFirstTalkId(int npcId)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getActiveChar()), npcId);
	}
	
	public void addTalkId(int npcId)
	{
		setNpcTalkId(npcId);
	}
	
	public void addKillId(int npcId)
	{
		setAttackableKillId(kill -> notifyKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcId);
	}
	
	public void addAttackId(int npcId)
	{
		setAttackableAttackId(attack -> notifyAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcId);
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addStartNpc(int... npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addStartNpc(Collection<Integer> npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog).
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(int... npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getActiveChar()), npcIds);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog).
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(Collection<Integer> npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getActiveChar()), npcIds);
	}
	
	/**
	 * Add the NPC to the AcquireSkill dialog.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAcquireSkillId(int... npcIds)
	{
		setPlayerSkillLearnId(event -> notifyAcquireSkill(event.getTrainer(), event.getActiveChar(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Add the NPC to the AcquireSkill dialog.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAcquireSkillId(Collection<Integer> npcIds)
	{
		setPlayerSkillLearnId(event -> notifyAcquireSkill(event.getTrainer(), event.getActiveChar(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemBypassEventId(int... itemIds)
	{
		setItemBypassEvenId(event -> notifyItemEvent(event.getItem(), event.getActiveChar(), event.getEvent()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemBypassEventId(Collection<Integer> itemIds)
	{
		setItemBypassEvenId(event -> notifyItemEvent(event.getItem(), event.getActiveChar(), event.getEvent()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemTalkId(int... itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getActiveChar()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemTalkId(Collection<Integer> itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getActiveChar()), itemIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for attack events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAttackId(int... npcIds)
	{
		setAttackableAttackId(attack -> notifyAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for attack events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAttackId(Collection<Integer> npcIds)
	{
		setAttackableAttackId(attack -> notifyAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for kill events.
	 * @param npcIds
	 */
	public void addKillId(int... npcIds)
	{
		setAttackableKillId(kill -> notifyKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest event to the collection of NPC IDs that will respond to for on kill events.
	 * @param npcIds the collection of NPC IDs
	 */
	public void addKillId(Collection<Integer> npcIds)
	{
		setAttackableKillId(kill -> notifyKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTalkId(int... npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	public void addTalkId(Collection<Integer> npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Teleport Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTeleportId(int... npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Teleport Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTeleportId(Collection<Integer> npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for spawn events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpawnId(int... npcIds)
	{
		setNpcSpawnId(event -> notifySpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for spawn events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpawnId(Collection<Integer> npcIds)
	{
		setNpcSpawnId(event -> notifySpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for skill see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSkillSeeId(int... npcIds)
	{
		setNpcSkillSeeId(event -> notifySkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for skill see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSkillSeeId(Collection<Integer> npcIds)
	{
		setNpcSkillSeeId(event -> notifySkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpellFinishedId(int... npcIds)
	{
		setNpcSkillFinishedId(event -> notifySpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpellFinishedId(Collection<Integer> npcIds)
	{
		setNpcSkillFinishedId(event -> notifySpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTrapActionId(int... npcIds)
	{
		setTrapActionId(event -> notifyTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTrapActionId(Collection<Integer> npcIds)
	{
		setTrapActionId(event -> notifyTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for faction call events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFactionCallId(int... npcIds)
	{
		setAttackableFactionIdId(event -> notifyFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for faction call events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFactionCallId(Collection<Integer> npcIds)
	{
		setAttackableFactionIdId(event -> notifyFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for character see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAggroRangeEnterId(int... npcIds)
	{
		setAttackableAggroRangeEnterId(event -> notifyAggroRangeEnter(event.getNpc(), event.getActiveChar(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for character see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAggroRangeEnterId(Collection<Integer> npcIds)
	{
		setAttackableAggroRangeEnterId(event -> notifyAggroRangeEnter(event.getNpc(), event.getActiveChar(), event.isSummon()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSeeCreatureId(int... npcIds)
	{
		setNpcCreatureSeeId(event -> notifySeeCreature(event.getNpc(), event.getCreature(), event.isSummon()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSeeCreatureId(Collection<Integer> npcIds)
	{
		setNpcCreatureSeeId(event -> notifySeeCreature(event.getNpc(), event.getCreature(), event.isSummon()), npcIds);
	}
	
	/**
	 * Register onEnterZone trigger for zone
	 * @param zoneId the ID of the zone to register
	 */
	public void addEnterZoneId(int zoneId)
	{
		setCreatureZoneEnterId(event -> notifyEnterZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Register onEnterZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addEnterZoneId(int... zoneIds)
	{
		setCreatureZoneEnterId(event -> notifyEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onEnterZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addEnterZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneEnterId(event -> notifyEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onExitZone trigger for zone
	 * @param zoneId the ID of the zone to register
	 */
	public void addExitZoneId(int zoneId)
	{
		setCreatureZoneExitId(event -> notifyExitZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Register onExitZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addExitZoneId(int... zoneIds)
	{
		setCreatureZoneExitId(event -> notifyExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onExitZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addExitZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneExitId(event -> notifyExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addEventReceivedId(int... npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addEventReceivedId(Collection<Integer> npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addMoveFinishedId(int... npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addMoveFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNodeArrived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addNodeArrivedId(int... npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNodeArrived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addNodeArrivedId(Collection<Integer> npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onRouteFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addRouteFinishedId(int... npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onRouteFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addRouteFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNpcHate trigger for NPC
	 * @param npcIds
	 */
	public void addNpcHateId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getActiveChar(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Register onNpcHate trigger for NPC
	 * @param npcIds
	 */
	public void addNpcHateId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getActiveChar(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Register onSummonSpawn trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonSpawnId(int... npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonSpawn trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonSpawnId(Collection<Integer> npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonTalk trigger when master talked to summon.
	 * @param npcIds
	 */
	public void addSummonTalkId(int... npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonTalk trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonTalkId(Collection<Integer> npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Registers onCanSeeMe trigger whenever an npc info must be sent to player.
	 * @param npcIds
	 */
	public void addCanSeeMeId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getActiveChar()), false, false), npcIds);
	}
	
	/**
	 * Registers onCanSeeMe trigger whenever an npc info must be sent to player.
	 * @param npcIds
	 */
	public void addCanSeeMeId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getActiveChar()), false, false), npcIds);
	}
	
	public void addOlympiadMatchFinishId()
	{
		setOlympiadMatchResult(event -> notifyOlympiadMatch(event.getWinner(), event.getLoser(), event.getCompetitionType()));
	}
	
	/**
	 * Use this method to get a random party member from a player's party.<br>
	 * Useful when distributing rewards after killing an NPC.
	 * @param player this parameter represents the player whom the party will taken.
	 * @return {@code null} if {@code player} is {@code null}, {@code player} itself if the player does not have a party, and a random party member in all other cases
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player)
	{
		if (player == null)
		{
			return null;
		}
		final L2Party party = player.getParty();
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			return player;
		}
		return party.getMembers().get(Rnd.get(party.getMembers().size()));
	}
	
	/**
	 * Get a random party member with required cond value.
	 * @param player the instance of a player whose party is to be searched
	 * @param cond the value of the "cond" variable that must be matched
	 * @return a random party member that matches the specified condition, or {@code null} if no match was found
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, int cond)
	{
		return getRandomPartyMember(player, "cond", String.valueOf(cond));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the instance of a player whose party is to be searched
	 * @param var the quest variable to look for in party members. If {@code null}, it simply unconditionally returns a random party member
	 * @param value the value of the specified quest variable the random party member must have
	 * @return a random party member that matches the specified conditions or {@code null} if no match was found.<br>
	 *         If the {@code var} parameter is {@code null}, a random party member is selected without any conditions.<br>
	 *         The party member must be within a range of 1500 ingame units of the target of the reference player, or, if no target exists, within the same range of the player itself
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, String var, String value)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// for null var condition, return any random party member.
		if (var == null)
		{
			return getRandomPartyMember(player);
		}
		
		// normal cases...if the player is not in a party, check the player's state
		QuestState temp = null;
		L2Party party = player.getParty();
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && temp.isSet(var) && temp.get(var).equalsIgnoreCase(value))
			{
				return player; // match
			}
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly including this player)
		List<L2PcInstance> candidates = new ArrayList<>();
		// get the target for enforcing distance limitations.
		L2Object target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (L2PcInstance partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.get(var) != null) && (temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the player whose random party member is to be selected
	 * @param state the quest state required of the random party member
	 * @return {@code null} if nothing was selected or a random party member that has the specified quest state
	 */
	public L2PcInstance getRandomPartyMemberState(L2PcInstance player, byte state)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// normal cases...if the player is not in a party check the player's state
		QuestState temp = null;
		L2Party party = player.getParty();
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		List<L2PcInstance> candidates = new ArrayList<>();
		
		// get the target for enforcing distance limitations.
		L2Object target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (L2PcInstance partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Get a random party member from the specified player's party.<br>
	 * If the player is not in a party, only the player himself is checked.<br>
	 * The lucky member is chosen by standard loot roll rules -<br>
	 * each member rolls a random number, the one with the highest roll wins.
	 * @param player the player whose party to check
	 * @param npc the NPC used for distance and other checks (if {@link #checkPartyMember(L2PcInstance, L2Npc)} is overriden)
	 * @return the random party member or {@code null}
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, L2Npc npc)
	{
		if ((player == null) || !checkDistanceToTarget(player, npc))
		{
			return null;
		}
		final L2Party party = player.getParty();
		L2PcInstance luckyPlayer = null;
		if (party == null)
		{
			if (checkPartyMember(player, npc))
			{
				luckyPlayer = player;
			}
		}
		else
		{
			int highestRoll = 0;
			
			for (L2PcInstance member : party.getMembers())
			{
				final int rnd = getRandom(1000);
				
				if ((rnd > highestRoll) && checkPartyMember(member, npc))
				{
					highestRoll = rnd;
					luckyPlayer = member;
				}
			}
		}
		if ((luckyPlayer != null) && checkDistanceToTarget(luckyPlayer, npc))
		{
			return luckyPlayer;
		}
		return null;
	}
	
	/**
	 * This method is called for every party member in {@link #getRandomPartyMember(L2PcInstance, L2Npc)}.<br>
	 * It is intended to be overriden by the specific quest implementations.
	 * @param player the player to check
	 * @param npc the NPC that was passed to {@link #getRandomPartyMember(L2PcInstance, L2Npc)}
	 * @return {@code true} if this party member passes the check, {@code false} otherwise
	 */
	public boolean checkPartyMember(L2PcInstance player, L2Npc npc)
	{
		return true;
	}
	
	/**
	 * Get a random party member from the player's party who has this quest at the specified quest progress.<br>
	 * If the player is not in a party, only the player himself is checked.
	 * @param player the player whose random party member state to get
	 * @param condition the quest progress step the random member should be at (-1 = check only if quest is started)
	 * @param playerChance how many times more chance does the player get compared to other party members (3 - 3x more chance).<br>
	 *            On retail servers, the killer usually gets 2-3x more chance than other party members
	 * @param target the NPC to use for the distance check (can be null)
	 * @return the {@link QuestState} object of the random party member or {@code null} if none matched the condition
	 */
	public QuestState getRandomPartyMemberState(L2PcInstance player, int condition, int playerChance, L2Npc target)
	{
		if ((player == null) || (playerChance < 1))
		{
			return null;
		}
		
		QuestState qs = player.getQuestState(getName());
		if (!player.isInParty())
		{
			if (!checkPartyMemberConditions(qs, condition, target))
			{
				return null;
			}
			if (!checkDistanceToTarget(player, target))
			{
				return null;
			}
			return qs;
		}
		
		final List<QuestState> candidates = new ArrayList<>();
		if (checkPartyMemberConditions(qs, condition, target) && (playerChance > 0))
		{
			for (int i = 0; i < playerChance; i++)
			{
				candidates.add(qs);
			}
		}
		
		for (L2PcInstance member : player.getParty().getMembers())
		{
			if (member == player)
			{
				continue;
			}
			
			qs = member.getQuestState(getName());
			if (checkPartyMemberConditions(qs, condition, target))
			{
				candidates.add(qs);
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		
		qs = candidates.get(getRandom(candidates.size()));
		if (!checkDistanceToTarget(qs.getPlayer(), target))
		{
			return null;
		}
		return qs;
	}
	
	private boolean checkPartyMemberConditions(QuestState qs, int condition, L2Npc npc)
	{
		return ((qs != null) && ((condition == -1) ? qs.isStarted() : qs.isCond(condition)) && checkPartyMember(qs, npc));
	}
	
	private static boolean checkDistanceToTarget(L2PcInstance player, L2Npc target)
	{
		return ((target == null) || com.l2jserver.gameserver.util.Util.checkIfInRange(1500, player, target, true));
	}
	
	/**
	 * This method is called for every party member in {@link #getRandomPartyMemberState(L2PcInstance, int, int, L2Npc)} if/after all the standard checks are passed.<br>
	 * It is intended to be overridden by the specific quest implementations.<br>
	 * It can be used in cases when there are more checks performed than simply a quest condition check,<br>
	 * for example, if an item is required in the player's inventory.
	 * @param qs the {@link QuestState} object of the party member
	 * @param npc the NPC that was passed as the last parameter to {@link #getRandomPartyMemberState(L2PcInstance, int, int, L2Npc)}
	 * @return {@code true} if this party member passes the check, {@code false} otherwise
	 */
	public boolean checkPartyMember(QuestState qs, L2Npc npc)
	{
		return true;
	}
	
	public void showPage(L2PcInstance player, String fileName)
	{
		showPage(player, fileName, false);
	}
	
	public void showPage(L2PcInstance player, String fileName, boolean haveQuest)
	{
		String content = getHtm(player.getHtmlPrefix(), fileName);
		if (content != null)
		{
			L2Npc npc = player.getLastFolkNPC();
			if (haveQuest && (npc != null))
			{
				content = content.replace("%objectId%", npc.getObjectId() + "");
			}
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, content);
			player.sendPacket(npcReply);
		}
	}
	
	public void showQuestPage(L2PcInstance player, String fileName, int questId)
	{
		String content = getHtm(player.getHtmlPrefix(), fileName);
		if (content != null)
		{
			L2Npc npc = player.getLastFolkNPC();
			NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(npc != null ? npc.getObjectId() : 0, questId);
			npcReply.setHtml(content);
			player.sendPacket(npcReply);
		}
	}
	
	public void showTutorialHTML(L2PcInstance player, String fileName)
	{
		String content = getHtm(player.getHtmlPrefix(), fileName);
		if (content != null)
		{
			player.sendPacket(new TutorialShowHtml(content));
		}
	}
	
	/**
	 * Send an HTML file to the specified player.
	 * @param player the player to send the HTML to
	 * @param filename the name of the HTML file to show
	 * @return the contents of the HTML file that was sent to the player
	 * @see #showHtmlFile(L2PcInstance, String, L2Npc)
	 */
	public String showHtmlFile(L2PcInstance player, String filename)
	{
		return showHtmlFile(player, filename, null);
	}
	
	/**
	 * Send an HTML file to the specified player.
	 * @param player the player to send the HTML file to
	 * @param filename the name of the HTML file to show
	 * @param npc the NPC that is showing the HTML file
	 * @return the contents of the HTML file that was sent to the player
	 * @see #showHtmlFile(L2PcInstance, String, L2Npc)
	 */
	public String showHtmlFile(L2PcInstance player, String filename, L2Npc npc)
	{
		boolean questwindow = !filename.endsWith(".html");
		int questId = getId();
		
		// Create handler to file linked to the quest
		String content = getHtm(player.getHtmlPrefix(), filename);
		
		// Send message to client if message not empty
		if (content != null)
		{
			if (npc != null)
			{
				content = content.replaceAll("%objectId%", String.valueOf(npc.getObjectId()));
			}
			
			if (questwindow && (questId > 0) && (questId < 20000) && (questId != 999))
			{
				NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(npc != null ? npc.getObjectId() : 0, questId);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			else
			{
				final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		return content;
	}
	
	/**
	 * @param prefix player's language prefix.
	 * @param fileName the html file to be get.
	 * @return the HTML file contents
	 */
	public String getHtm(String prefix, String fileName)
	{
		final HtmCache hc = HtmCache.getInstance();
		String content = hc.getHtm(prefix, fileName.startsWith("data/") ? fileName : "data/scripts/" + getDescr().toLowerCase() + "/" + getName() + "/" + fileName);
		if (content == null)
		{
			content = hc.getHtm(prefix, "data/scripts/" + getDescr() + "/" + getName() + "/" + fileName);
			if (content == null)
			{
				content = hc.getHtmForce(prefix, "data/scripts/quests/" + getName() + "/" + fileName);
			}
		}
		return content;
	}
	
	/**
	 * @return the registered quest items IDs.
	 */
	public int[] getRegisteredItemIds()
	{
		return questItemIds;
	}
	
	/**
	 * Registers all items that have to be destroyed in case player abort the quest or finish it.
	 * @param items
	 */
	public void registerQuestItems(int... items)
	{
		questItemIds = items;
	}
	
	/**
	 * Remove all quest items associated with this quest from the specified player's inventory.
	 * @param player the player whose quest items to remove
	 */
	public void removeRegisteredQuestItems(L2PcInstance player)
	{
		takeItems(player, -1, questItemIds);
	}
	
	@Override
	public void setActive(boolean status)
	{
		// TODO: Implement me.
	}
	
	@Override
	public boolean reload()
	{
		unload();
		return super.reload();
	}
	
	@Override
	public boolean unload()
	{
		return unload(true);
	}
	
	/**
	 * @param removeFromList
	 * @return
	 */
	public boolean unload(boolean removeFromList)
	{
		saveGlobalData();
		// cancel all pending timers before reloading.
		// if timers ought to be restarted, the quest can take care of it
		// with its code (example: save global data indicating what timer must be restarted).
		if (_questTimers != null)
		{
			for (List<QuestTimer> timers : getQuestTimers().values())
			{
				_readLock.lock();
				try
				{
					for (QuestTimer timer : timers)
					{
						timer.cancel();
					}
				}
				finally
				{
					_readLock.unlock();
				}
				timers.clear();
			}
			getQuestTimers().clear();
		}
		
		if (removeFromList)
		{
			return QuestManager.getInstance().removeScript(this) && super.unload();
		}
		return super.unload();
	}
	
	@Override
	public ScriptManager<?> getManager()
	{
		return QuestManager.getInstance();
	}
	
	public void setOnEnterWorld(boolean state)
	{
		if (state)
		{
			setPlayerLoginId(event -> notifyEnterWorld(event.getActiveChar()));
		}
		else
		{
			getListeners().stream().filter(listener -> listener.getType() == EventType.ON_PLAYER_LOGIN).forEach(AbstractEventListener::unregisterMe);
		}
	}
	
	public void registerTutorialEvent()
	{
		setPlayerTutorialEvent(event -> notifyTutorialEvent(event.getActiveChar(), event.getCommand()));
	}
	
	public void registerTutorialClientEvent()
	{
		setPlayerTutorialClientEvent(event -> notifyTutorialClientEvent(event.getActiveChar(), event.getEvent()));
	}
	
	public void registerTutorialQuestionMark()
	{
		setPlayerTutorialQuestionMark(event -> notifyTutorialQuestionMark(event.getActiveChar(), event.getNumber()));
	}
	
	public void registerTutorialCmd()
	{
		setPlayerTutorialCmd(event -> notifyTutorialCmd(event.getActiveChar(), event.getCommand()));
	}
	
	/**
	 * If a quest is set as custom, it will display it's name in the NPC Quest List.<br>
	 * Retail quests are unhardcoded to display the name using a client string.
	 * @param val if {@code true} the quest script will be set as custom quest.
	 */
	public void setIsCustom(boolean val)
	{
		_isCustom = val;
	}
	
	/**
	 * Verifies if this is a custom quest.
	 * @return {@code true} if the quest script is a custom quest, {@code false} otherwise.
	 */
	public boolean isCustomQuest()
	{
		return _isCustom;
	}
	
	/**
	 * Gets the start conditions.
	 * @return the start conditions
	 */
	private Map<Predicate<L2PcInstance>, String> getStartConditions()
	{
		if (_startCondition == null)
		{
			synchronized (this)
			{
				if (_startCondition == null)
				{
					_startCondition = new LinkedHashMap<>(1);
				}
			}
		}
		return _startCondition;
	}
	
	/**
	 * Verifies if the player meets all the start conditions.
	 * @param player the player
	 * @return {@code true} if all conditions are met
	 */
	public boolean canStartQuest(L2PcInstance player)
	{
		if (_startCondition == null)
		{
			return true;
		}
		
		for (Predicate<L2PcInstance> cond : _startCondition.keySet())
		{
			if (!cond.test(player))
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Gets the HTML for the first starting condition not met.
	 * @param player the player
	 * @return the HTML
	 */
	public String getStartConditionHtml(L2PcInstance player)
	{
		if (_startCondition == null)
		{
			return null;
		}
		
		for (Entry<Predicate<L2PcInstance>, String> startRequirement : _startCondition.entrySet())
		{
			if (!startRequirement.getKey().test(player))
			{
				return startRequirement.getValue();
			}
		}
		return null;
	}
	
	/**
	 * Adds a predicate to the start conditions.
	 * @param questStartRequirement the predicate condition
	 * @param html the HTML to display if that condition is not met
	 */
	public void addCondStart(Predicate<L2PcInstance> questStartRequirement, String html)
	{
		getStartConditions().put(questStartRequirement, html);
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param maxLevel the maximum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondLevel(int minLevel, int maxLevel, String html)
	{
		getStartConditions().put(p -> (p.getLevel() >= minLevel) && (p.getLevel() <= maxLevel), html);
	}
	
	/**
	 * Adds a minimum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondMinLevel(int minLevel, String html)
	{
		getStartConditions().put(p -> p.getLevel() >= minLevel, html);
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param maxLevel the maximum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondMaxLevel(int maxLevel, String html)
	{
		getStartConditions().put(p -> p.getLevel() <= maxLevel, html);
	}
	
	/**
	 * Adds a race start condition to the quest.
	 * @param race the race
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondRace(Race race, String html)
	{
		getStartConditions().put(p -> p.getRace() == race, html);
	}
	
	/**
	 * Adds a not-race start condition to the quest.
	 * @param race the race
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondNotRace(Race race, String html)
	{
		getStartConditions().put(p -> p.getRace() != race, html);
	}
	
	/**
	 * Adds a quest completed start condition to the quest.
	 * @param name the quest name
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondCompletedQuest(String name, String html)
	{
		getStartConditions().put(p -> p.hasQuestState(name) && p.getQuestState(name).isCompleted(), html);
	}
	
	/**
	 * Adds a class ID start condition to the quest.
	 * @param classId the class ID
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondClassId(ClassId classId, String html)
	{
		getStartConditions().put(p -> p.getClassId() == classId, html);
	}
	
	/**
	 * Adds a not-class ID start condition to the quest.
	 * @param classId the class ID
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondNotClassId(ClassId classId, String html)
	{
		getStartConditions().put(p -> p.getClassId() != classId, html);
	}
	
	/**
	 * Adds a subclass active start condition to the quest.
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondIsSubClassActive(String html)
	{
		getStartConditions().put(p -> p.isSubClassActive(), html);
	}
	
	/**
	 * Adds a not-subclass active start condition to the quest.
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondIsNotSubClassActive(String html)
	{
		getStartConditions().put(p -> !p.isSubClassActive(), html);
	}
	
	/**
	 * Adds a category start condition to the quest.
	 * @param categoryType the category type
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondInCategory(CategoryType categoryType, String html)
	{
		getStartConditions().put(p -> p.isInCategory(categoryType), html);
	}
	
	public boolean haveMemo(L2PcInstance talker, int questId)
	{
		Quest quest = QuestManager.getInstance().getQuest(questId);
		if ((quest != null) && talker.hasQuestState(quest.getName()))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @param talker Player
	 * @param questId Quest Id
	 * @param flag 0 = false / 1 = true
	 */
	public void setOneTimeQuestFlag(L2PcInstance talker, int questId, int flag)
	{
		Quest quest = QuestManager.getInstance().getQuest(questId);
		if (quest != null)
		{
			quest.getQuestState(talker, true).setState(flag == 1 ? State.COMPLETED : State.STARTED);
		}
	}
	
	public int getOneTimeQuestFlag(L2PcInstance talker, int questId)
	{
		Quest quest = QuestManager.getInstance().getQuest(questId);
		if ((quest != null) && quest.getQuestState(talker, true).isCompleted())
		{
			return 1;
		}
		return 0;
	}
	
	public static void playSound(L2PcInstance player, IAudio sound)
	{
		player.sendPacket(sound.getPacket());
	}
	
	public void showRadar(L2PcInstance player, int x, int y, int z, int type)
	{
		player.getRadar().addMarker(x, y, z);
	}
	
	public boolean isVisibleInQuestWindow()
	{
		return true;
	}
	
	public L2Npc spawnNpc(int npcId, Location loc, int heading, int instId)
	{
		L2NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
		Instance inst = InstanceManager.getInstance().getInstance(instId);
		try
		{
			L2Spawn npcSpawn = new L2Spawn(npcTemplate);
			npcSpawn.setX(loc.getX());
			npcSpawn.setY(loc.getY());
			npcSpawn.setZ(loc.getZ());
			npcSpawn.setHeading(loc.getHeading());
			npcSpawn.setAmount(1);
			npcSpawn.setInstanceId(instId);
			SpawnTable.getInstance().addNewSpawn(npcSpawn, false);
			L2Npc npc = npcSpawn.spawnOne(false);
			inst.addNpc(npc);
			return npc;
		}
		catch (Exception ignored)
		{
		}
		return null;
	}
}
