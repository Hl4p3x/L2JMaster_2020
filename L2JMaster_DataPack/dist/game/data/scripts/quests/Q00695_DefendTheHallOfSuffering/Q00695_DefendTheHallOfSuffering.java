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
package quests.Q00695_DefendTheHallOfSuffering;

import java.util.Calendar;

import com.l2jserver.gameserver.enums.audio.Sound;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.SeedOfInfinityManager;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.instancezone.InstanceWorld;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Defend The Hall Of Suffering (695)
 * @author MaGa1, Sacrifice
 */
public final class Q00695_DefendTheHallOfSuffering extends Quest
{
	// NPC's
	private static final int TEPIOS = 32603;
	private static final int TEPIOS2 = 32530;
	
	// Items
	private static final int SOE = 736;
	
	// Etc
	private static final int INSTANCE_ID = 116;
	
	public Q00695_DefendTheHallOfSuffering()
	{
		super(695, Q00695_DefendTheHallOfSuffering.class.getSimpleName(), "Defend The Hall Of Suffering");
		
		addStartNpc(TEPIOS);
		addTalkId(TEPIOS, TEPIOS2);
	}
	
	@Override
	public final String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		
		QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equalsIgnoreCase("32603-02.htm"))
		{
			st.set("cond", "1");
			st.setState(State.STARTED);
			st.playSound(Sound.ITEMSOUND_QUEST_ACCEPT);
		}
		return htmltext;
	}
	
	@Override
	public final String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
				if ((player.getLevel() >= 75) && (player.getLevel() <= 82))
				{
					if (SeedOfInfinityManager.getCurrentStage() == 4)
					{
						htmltext = "32603-01.htm";
					}
					else
					{
						htmltext = "32603-04.htm";
					}
				}
				else
				{
					htmltext = "32603-00.htm";
					st.exitQuest(true);
				}
				break;
			case State.STARTED:
				switch (npc.getId())
				{
					case TEPIOS:
						htmltext = "32603-01a.htm";
						break;
					case TEPIOS2:
						InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
						
						if ((world != null) && (world.getTemplateId() == INSTANCE_ID))
						{
							if (world.tag == -1)
							{
								htmltext = "32530-11.htm";
							}
							else if ((player.getParty() != null) && (player.getParty().getLeaderObjectId() == player.getObjectId()))
							{
								for (L2PcInstance member : player.getParty().getMembers())
								{
									QuestState st1 = member.getQuestState(Q00695_DefendTheHallOfSuffering.class.getSimpleName());
									if (st1 != null)
									{
										if (world.tag == 13777)
										{
											st1.giveItems(13777, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-00.htm";
											finishInstance(player);
										}
										else if (world.tag == 13778)
										{
											st1.giveItems(13778, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-01.htm";
											finishInstance(player);
										}
										else if (world.tag == 13779)
										{
											st1.giveItems(13779, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-02.htm";
											finishInstance(player);
										}
										else if (world.tag == 13780)
										{
											st1.giveItems(13780, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-03.htm";
											finishInstance(player);
										}
										else if (world.tag == 13781)
										{
											st1.giveItems(13781, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-04.htm";
											finishInstance(player);
										}
										else if (world.tag == 13782)
										{
											st1.giveItems(13782, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-05.htm";
											finishInstance(player);
										}
										else if (world.tag == 13783)
										{
											st1.giveItems(13783, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-06.htm";
											finishInstance(player);
										}
										else if (world.tag == 13784)
										{
											st1.giveItems(13784, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-07.htm";
											finishInstance(player);
										}
										else if (world.tag == 13785)
										{
											st1.giveItems(13785, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-08.htm";
											finishInstance(player);
										}
										else if (world.tag == 13786)
										{
											st1.giveItems(13786, 1);
											st1.giveItems(SOE, 1);
											st1.unset("cond");
											st1.exitQuest(true);
											st1.playSound(Sound.ITEMSOUND_QUEST_FINISH);
											htmltext = "32530-09.htm";
											finishInstance(player);
										}
										else
										{
											htmltext = "32530-11.htm";
										}
									}
								}
							}
							else
							{
								return "32530-10.htm";
							}
						}
						else
						{
							htmltext = "32530-11.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}
	
	private static final void finishInstance(L2PcInstance player)
	{
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		Calendar reenter = Calendar.getInstance();
		reenter.set(Calendar.MINUTE, 30);
		
		if (reenter.get(Calendar.HOUR_OF_DAY) >= 6)
		{
			reenter.add(Calendar.DATE, 1);
		}
		reenter.set(Calendar.HOUR_OF_DAY, 6);
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.INSTANT_ZONE_FROM_HERE_S1_S_ENTRY_HAS_BEEN_RESTRICTED);
		sm.addInstanceName(world.getTemplateId());
		
		for (int objectId : world.getAllowed())
		{
			L2PcInstance obj = L2World.getInstance().getPlayer(objectId);
			if ((obj != null) && obj.isOnline())
			{
				InstanceManager.getInstance().setInstanceTime(objectId, world.getTemplateId(), reenter.getTimeInMillis());
				obj.sendPacket(sm);
			}
		}
		Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
		inst.setDuration(5 * 60000);
		inst.setEmptyDestroyTime(0);
	}
}