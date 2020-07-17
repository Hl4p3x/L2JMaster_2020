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
package ai.individual;

import java.util.concurrent.ScheduledFuture;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.network.serverpackets.NpcSay;
import com.l2jserver.gameserver.util.Util;

import ai.npc.AbstractNpcAI;
import quests.Q00288_HandleWithCare.Q00288_HandleWithCare;
import quests.Q00423_TakeYourBestShot.Q00423_TakeYourBestShot;

/**
 * Seer Ugoros AI.
 */
public final class SeerUgoros extends AbstractNpcAI
{
	protected static ScheduledFuture<?> _thinkTask = null;
	
	private static final int UGOROS_PASS = 15496;
	private static final int MID_SCALE = 15498;
	private static final int HIGH_SCALE = 15497;
	private static final int SEER_UGOROS = 18863;
	private static final int BATRACOS = 32740;
	private static final int WEED_ID = 18867;
	
	protected static L2Npc _ugoros = null;
	protected static L2Npc _weed = null;
	protected static boolean _weed_attack = false;
	private static boolean _weed_killed_by_player = false;
	private static boolean _killed_one_weed = false;
	protected static L2PcInstance _player = null;
	
	private static final byte ALIVE = 0;
	private static final byte FIGHTING = 1;
	private static final byte DEAD = 2;
	
	protected static byte STATE = DEAD;
	
	private static final Skill UGOROS_SKILL = SkillData.getInstance().getSkill(6426, 1); // Priest's Ire
	
	private SeerUgoros()
	{
		super(SeerUgoros.class.getSimpleName(), "ai/individual");
		
		addStartNpc(BATRACOS);
		addTalkId(BATRACOS);
		addAttackId(WEED_ID);
		addKillId(SEER_UGOROS);
		
		startQuestTimer("ugoros_respawn", 60000, null, null);
	}
	
	public static void main(String[] args)
	{
		new SeerUgoros();
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("ugoros_respawn") && (_ugoros == null))
		{
			_ugoros = addSpawn(SEER_UGOROS, 96804, 85604, -3720, 34360, false, 0);
			broadcastInRegion(_ugoros, NpcStringId.LISTEN_OH_TANTAS_I_HAVE_RETURNED_THE_PROPHET_YUGOROS_OF_THE_BLACK_ABYSS_IS_WITH_ME_SO_DO_NOT_BE_AFRAID);
			STATE = ALIVE;
			startQuestTimer("ugoros_shout", 120000, null, null);
		}
		else if (event.equalsIgnoreCase("ugoros_shout"))
		{
			if (STATE == FIGHTING)
			{
				if (_player == null)
				{
					STATE = ALIVE;
				}
			}
			else if (STATE == ALIVE)
			{
				broadcastInRegion(_ugoros, NpcStringId.LISTEN_OH_TANTAS_THE_BLACK_ABYSS_IS_FAMISHED_FIND_SOME_FRESH_OFFERINGS);
			}
			
			startQuestTimer("ugoros_shout", 120000, null, null);
		}
		else if (event.equalsIgnoreCase("ugoros_attack"))
		{
			if (_player != null)
			{
				changeAttackTarget(_player);
				NpcSay packet = new NpcSay(_ugoros.getObjectId(), Say2.NPC_ALL, _ugoros.getId(), NpcStringId.WELCOME_S1_LET_US_SEE_IF_YOU_HAVE_BROUGHT_A_WORTHY_OFFERING_FOR_THE_BLACK_ABYSS);
				packet.addStringParameter(_player.getName().toString());
				_ugoros.broadcastPacket(packet);
				
				stopTask();
				
				_thinkTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ThinkTask(), 500, 3000);
			}
		}
		else if (event.equalsIgnoreCase("weed_check"))
		{
			if ((_weed_attack == true) && (_ugoros != null) && (_weed != null))
			{
				if (_weed.isDead() && !_weed_killed_by_player)
				{
					_killed_one_weed = true;
					_weed = null;
					_weed_attack = false;
					_ugoros.getStatus().setCurrentHp(_ugoros.getStatus().getCurrentHp() + (_ugoros.getMaxHp() * 0.2));
					_ugoros.broadcastPacket(new NpcSay(_ugoros.getObjectId(), 0, _ugoros.getId(), NpcStringId.WHAT_A_FORMIDABLE_FOE_BUT_I_HAVE_THE_ABYSS_WEED_GIVEN_TO_ME_BY_THE_BLACK_ABYSS_LET_ME_SEE));
				}
				else
				{
					startQuestTimer("weed_check", 2000, null, null);
				}
			}
			else
			{
				_weed = null;
				_weed_attack = false;
			}
		}
		else if (event.equalsIgnoreCase("ugoros_expel"))
		{
			if (_player != null)
			{
				_player.teleToLocation(94701, 83053, -3580);
				_player = null;
			}
		}
		else if (event.equalsIgnoreCase("teleportInside"))
		{
			if ((player != null) && (STATE == ALIVE))
			{
				if (player.getInventory().getItemByItemId(UGOROS_PASS) != null)
				{
					STATE = FIGHTING;
					_player = player;
					_killed_one_weed = false;
					player.teleToLocation(95984, 85692, -3720);
					player.destroyItemByItemId("SeerUgoros", UGOROS_PASS, 1, npc, true);
					startQuestTimer("ugoros_attack", 2000, null, null);
					QuestState st = player.getQuestState(Q00288_HandleWithCare.class.getSimpleName());
					if (st != null)
					{
						st.set("drop", "1");
					}
				}
				else
				{
					QuestState st = player.getQuestState(Q00423_TakeYourBestShot.class.getSimpleName());
					if (st == null)
					{
						return "<html><body>Gatekeeper Batracos:<br>You look too inexperienced to make a journey to see Tanta Seer Ugoros. If you can convince Chief Investigator Johnny that you should go, then I will let you pass. Johnny has been everywhere and done everything. He may not be of my people but he has my respect, and anyone who has his will in turn have mine as well.<br></body></html>";
					}
					
					return "<html><body>Gatekeeper Batracos:<br>Tanta Seer Ugoros is hard to find. You'll just have to keep looking.<br></body></html>";
				}
			}
			else
			{
				return "<html><body>Gatekeeper Batracos:<br>Tanta Seer Ugoros is hard to find. You'll just have to keep looking.<br></body></html>";
			}
		}
		else if (event.equalsIgnoreCase("teleport_back"))
		{
			if (player != null)
			{
				player.teleToLocation(94792, 83542, -3424);
				_player = null;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		if (npc.isDead())
		{
			return null;
		}
		
		if (npc.getId() == WEED_ID)
		{
			if ((_ugoros != null) && (_weed != null) && npc.equals(_weed))
			{
				_weed = null;
				_weed_attack = false;
				_weed_killed_by_player = true;
				_ugoros.broadcastPacket(new NpcSay(_ugoros.getObjectId(), 0, _ugoros.getId(), NpcStringId.NO_HOW_DARE_YOU_STOP_ME_FROM_USING_THE_ABYSS_WEED_DO_YOU_KNOW_WHAT_YOU_HAVE_DONE));
				
				stopTask();
				
				_thinkTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ThinkTask(), 1000, 3000);
			}
			npc.doDie(attacker);
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		if (npc.getId() == SEER_UGOROS)
		{
			stopTask();
			
			STATE = DEAD;
			broadcastInRegion(_ugoros, NpcStringId.AH_HOW_COULD_I_LOSE_OH_BLACK_ABYSS_RECEIVE_ME);
			_ugoros = null;
			addSpawn(BATRACOS, 96782, 85918, -3720, 34360, false, 50000);
			startQuestTimer("ugoros_expel", 50000, null, null);
			startQuestTimer("ugoros_respawn", 60000, null, null);
			QuestState st = player.getQuestState(Q00288_HandleWithCare.class.getSimpleName());
			if ((st != null) && (st.getInt("cond") == 1) && (st.getInt("drop") == 1))
			{
				if (_killed_one_weed)
				{
					player.addItem("SeerUgoros", MID_SCALE, 1, npc, true);
					st.set("cond", "2");
				}
				else
				{
					player.addItem("SeerUgoros", HIGH_SCALE, 1, npc, true);
					st.set("cond", "3");
				}
				st.unset("drop");
			}
		}
		return null;
	}
	
	protected void changeAttackTarget(L2Character target)
	{
		((L2Attackable) _ugoros).getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		((L2Attackable) _ugoros).clearAggroList();
		((L2Attackable) _ugoros).setTarget(target);
		
		if (target instanceof L2Attackable)
		{
			_weed_killed_by_player = false;
			_ugoros.disableSkill(UGOROS_SKILL, 100000);
			((L2Attackable) _ugoros).setIsRunning(true);
			((L2Attackable) _ugoros).addDamageHate(target, 0, Integer.MAX_VALUE);
		}
		else
		{
			_ugoros.enableSkill(UGOROS_SKILL);
			((L2Attackable) _ugoros).addDamageHate(target, 0, 99);
			((L2Attackable) _ugoros).setIsRunning(false);
		}
		((L2Attackable) _ugoros).getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
	}
	
	protected void stopTask()
	{
		if (_thinkTask != null)
		{
			_thinkTask.cancel(true);
			_thinkTask = null;
		}
	}
	
	private void broadcastInRegion(L2Npc npc, NpcStringId npcString)
	{
		if (npc == null)
		{
			return;
		}
		
		NpcSay cs = new NpcSay(npc.getObjectId(), 1, npc.getId(), npcString);
		int region = MapRegionManager.getInstance().getMapRegionLocId(npc.getX(), npc.getY());
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			if (region == MapRegionManager.getInstance().getMapRegionLocId(player.getX(), player.getY()))
			{
				if (Util.checkIfInRange(6000, npc, player, false))
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	private class ThinkTask implements Runnable
	{
		public ThinkTask()
		{
		}
		
		@Override
		public void run()
		{
			if ((STATE == FIGHTING) && (_player != null) && !_player.isDead())
			{
				if (_weed_attack && (_weed != null))
				{
					// Do nothing
				}
				else if (getRandom(10) < 6)
				{
					_weed = null;
					for (L2Character target : _ugoros.getKnownList().getKnownCharactersInRadius(2000))
					{
						if ((target instanceof L2Attackable) && !target.isDead() && (((L2Attackable) target).getId() == WEED_ID))
						{
							_weed_attack = true;
							_weed = (L2Attackable) target;
							changeAttackTarget(_weed);
							startQuestTimer("weed_check", 1000, null, null);
							break;
						}
					}
					if (_weed == null)
					{
						changeAttackTarget(_player);
					}
				}
				else
				{
					changeAttackTarget(_player);
				}
			}
			else
			{
				STATE = ALIVE;
				_player = null;
				stopTask();
			}
		}
	}
}