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
package quests.Q00714_PathToBecomingALordSchuttgart;

import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.network.serverpackets.NpcSay;

import quests.Q00114_ResurrectionOfAnOldManager.Q00114_ResurrectionOfAnOldManager;
import quests.Q00120_PavelsLastResearch.Q00120_PavelsLastResearch;
import quests.Q00121_PavelTheGiant.Q00121_PavelTheGiant;

/**
 * Path to Becoming a Lord - Schuttgart (714)
 * @author MaGa
 */
public final class Q00714_PathToBecomingALordSchuttgart extends Quest
{
	private static final int AUGUST = 35555;
	private static final int NEWYEAR = 31961;
	private static final int YASHENI = 31958;
	
	private static final int GOLEM_SHARD_PIECE = 17162;
	
	private static final int[] MOBS =
	{
		22801, // Cruel Pincer Golem
		22802, // Cruel Pincer Golem
		22803, // Cruel Pincer Golem
		22804, // Horrifying Jackhammer Golem
		22805, // Horrifying Jackhammer Golem
		22806, // Horrifying Jackhammer Golem
		22807, // Scout-type Golem No. 28
		22808, // Scout-type Golem No. 2
		22809, // Guard Golem
		22810, // Micro Scout Golem
		22811 // Great Chaos Golem
	};
	
	private static final int SCHUTTGART_CASTLE = 9;
	
	public Q00714_PathToBecomingALordSchuttgart()
	{
		super(714, Q00714_PathToBecomingALordSchuttgart.class.getSimpleName(), "Path to Becoming a Lord - Schuttgart");
		addStartNpc(AUGUST);
		addTalkId(AUGUST, NEWYEAR, YASHENI);
		addKillId(MOBS);
		questItemIds = new int[]
		{
			GOLEM_SHARD_PIECE
		};
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(SCHUTTGART_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		if (event.equals("35555-03.html"))
		{
			qs.startQuest();
		}
		else if (event.equals("35555-05.html"))
		{
			qs.setCond(2);
		}
		else if (event.equals("31961-03.html"))
		{
			qs.setCond(3);
		}
		else if (event.equals("31958-02.html"))
		{
			qs.setCond(5);
		}
		else if (event.equals("35555-08.html"))
		{
			if (castle.getOwner().getLeader().getPlayerInstance() != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_SCHUTTGART_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_SCHUTTGART);
				packet.addStringParameter(player.getName());
				npc.broadcastPacket(packet);
				qs.exitQuest(true, true);
			}
		}
		return event;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs = killer.getQuestState(getName());
		if ((qs != null) && qs.isCond(5))
		{
			if (getQuestItemsCount(killer, GOLEM_SHARD_PIECE) < 300)
			{
				giveItems(killer, GOLEM_SHARD_PIECE, 1);
			}
			
			if (getQuestItemsCount(killer, GOLEM_SHARD_PIECE) >= 300)
			{
				qs.setCond(6);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final Castle castle = CastleManager.getInstance().getCastleById(SCHUTTGART_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case AUGUST:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35555-01.html";
						}
						else
						{
							htmltext = "35555-00.html";
							qs.exitQuest(true);
						}
					}
					else
					{
						htmltext = "35555-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					htmltext = "35555-04.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "35555-06.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "35555-07.html";
				}
				break;
			}
			case NEWYEAR:
			{
				if (qs.isCond(2))
				{
					htmltext = "31961-01.html";
				}
				else if (qs.isCond(3))
				{
					final QuestState q1 = qs.getPlayer().getQuestState(Q00114_ResurrectionOfAnOldManager.class.getSimpleName());
					final QuestState q2 = qs.getPlayer().getQuestState(Q00120_PavelsLastResearch.class.getSimpleName());
					final QuestState q3 = qs.getPlayer().getQuestState(Q00121_PavelTheGiant.class.getSimpleName());
					if ((q3 != null) && q3.isCompleted())
					{
						if ((q1 != null) && q1.isCompleted())
						{
							if ((q2 != null) && q2.isCompleted())
							{
								qs.setCond(4);
								htmltext = "31961-04.html";
							}
							else
							{
								htmltext = "31961-04a.html";
							}
						}
						else
						{
							htmltext = "31961-04b.html";
						}
					}
					else
					{
						htmltext = "31961-04c.html";
					}
				}
				break;
			}
			case YASHENI:
			{
				if (qs.isCond(4))
				{
					htmltext = "31958-01.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "31958-03.html";
				}
				else if (qs.isCond(6))
				{
					takeItems(player, GOLEM_SHARD_PIECE, -1);
					qs.setCond(7);
					htmltext = "31958-04.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	private boolean hasFort()
	{
		for (Fort fortress : FortManager.getInstance().getForts())
		{
			if (fortress.getContractedCastleId() == SCHUTTGART_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
}