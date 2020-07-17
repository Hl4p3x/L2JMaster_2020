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
package quests.Q00709_PathToBecomingALordDion;

import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.network.serverpackets.NpcSay;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Rnd;

/**
 * Path to Becoming a Lord - Dion (709)
 * @author MaGa
 */
public final class Q00709_PathToBecomingALordDion extends Quest
{
	private static final int CROSBY = 35142;
	private static final int ROUKE = 31418;
	private static final int SOPHYA = 30735;
	private static final int BLOODY_AXE_AIDE = 27392;
	
	private static final int MANDRAGORA_ROOT = 13849;
	private static final int BLOODY_AXE_BLACK_EPAULETTE = 13850;
	
	private static final int[] OL_MAHUMS =
	{
		20208, // Ol Mahum Raider
		20209, // Ol Mahum Marksman
		20210, // Ol Mahum Sergeant
		20211, // Ol Mahum Captain
		BLOODY_AXE_AIDE
	};
	
	private static final int[] MANDRAGORAS =
	{
		20154, // Mandragora Sprout
		20155, // Mandragora Sapling
		20156 // Mandragora Blossom
	};
	
	private static final int DION_CASTLE = 2;
	
	public Q00709_PathToBecomingALordDion()
	{
		super(709, Q00709_PathToBecomingALordDion.class.getSimpleName(), "Path to Becoming a Lord - Dion");
		addStartNpc(CROSBY);
		addKillId(OL_MAHUMS);
		addKillId(MANDRAGORAS);
		addTalkId(CROSBY, SOPHYA, ROUKE);
		questItemIds = new int[]
		{
			BLOODY_AXE_BLACK_EPAULETTE,
			MANDRAGORA_ROOT
		};
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(DION_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (event.equals("35142-03.html"))
		{
			qs.startQuest();
		}
		else if (event.equals("35142-06.html"))
		{
			if (isLordAvailable(2, qs))
			{
				castleOwner.getQuestState(getName()).set("confidant", String.valueOf(qs.getPlayer().getObjectId()));
				castleOwner.getQuestState(getName()).setCond(3);
				qs.setState(State.STARTED);
			}
			else
			{
				htmltext = "35142-05a.html";
			}
		}
		else if (event.equals("31418-03.html"))
		{
			if (isLordAvailable(3, qs))
			{
				castleOwner.getQuestState(getName()).setCond(4);
			}
			else
			{
				htmltext = "35142-05a.html";
			}
		}
		else if (event.equals("30735-02.html"))
		{
			qs.set("cond", "6");
		}
		else if (event.equals("30735-05.html"))
		{
			takeItems(player, BLOODY_AXE_BLACK_EPAULETTE, 1);
			qs.set("cond", "8");
		}
		else if (event.equals("31418-05.html"))
		{
			if (isLordAvailable(8, qs))
			{
				takeItems(player, MANDRAGORA_ROOT, -1);
				castleOwner.getQuestState(getName()).setCond(9);
			}
		}
		else if (event.equals("35142-10.html"))
		{
			if (castleOwner != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_LORD_OF_THE_TOWN_OF_DION_LONG_MAY_HE_REIGN);
				packet.addStringParameter(player.getName());
				npc.broadcastPacket(packet);
				qs.exitQuest(true, true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs = killer.getQuestState(getName());
		if ((qs != null) && qs.isCond(6) && Util.contains(OL_MAHUMS, npc.getId()))
		{
			if ((npc.getId() != BLOODY_AXE_AIDE) && (Rnd.get(9) == 0))
			{
				addSpawn(BLOODY_AXE_AIDE, npc, true, 300000);
			}
			else if (npc.getId() == BLOODY_AXE_AIDE)
			{
				giveItems(killer, BLOODY_AXE_BLACK_EPAULETTE, 1);
				qs.setCond(7);
			}
		}
		
		if ((qs != null) && (qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(8, qs) && Util.contains(MANDRAGORAS, npc.getId()))
		{
			if (getQuestItemsCount(killer, MANDRAGORA_ROOT) < 100)
			{
				giveItems(killer, MANDRAGORA_ROOT, 1);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(DION_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case CROSBY:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35142-01.html";
						}
						else
						{
							htmltext = "35142-00.html";
							qs.exitQuest(true);
						}
					}
					else if (isLordAvailable(2, qs))
					{
						if (castleOwner.calculateDistance(npc, false, false) <= 200)
						{
							htmltext = "35142-05.html";
						}
						else
						{
							htmltext = "35142-05a.html";
						}
					}
					else
					{
						htmltext = "35142-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					qs.set("cond", "2");
					htmltext = "35142-04.html";
				}
				else if (qs.isCond(2) || qs.isCond(3))
				{
					htmltext = "35142-04a.html";
				}
				else if (qs.isCond(4))
				{
					qs.set("cond", "5");
					htmltext = "35142-07.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "35142-07.html";
				}
				else if ((qs.getCond() > 5) && (qs.getCond() < 9))
				{
					htmltext = "35142-08.html";
				}
				else if (qs.isCond(9))
				{
					htmltext = "35142-09.html";
				}
				break;
			}
			case ROUKE:
			{
				if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(3, qs))
				{
					if (castleOwner.getQuestState(getName()).getInt("confidant") == qs.getPlayer().getObjectId())
					{
						htmltext = "31418-01.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(8, qs))
				{
					if (getQuestItemsCount(talker, MANDRAGORA_ROOT) >= 100)
					{
						htmltext = "31418-04.html";
					}
					else
					{
						htmltext = "31418-04a.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(9, qs))
				{
					htmltext = "31418-06.html";
				}
				break;
			}
			case SOPHYA:
			{
				if (qs.isCond(5))
				{
					htmltext = "30735-01.html";
				}
				else if (qs.isCond(6))
				{
					htmltext = "30735-03.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30735-04.html";
				}
				else if (qs.isCond(8))
				{
					htmltext = "30735-06.html";
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
			if (fortress.getContractedCastleId() == DION_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isLordAvailable(int cond, QuestState qs)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(DION_CASTLE);
		final L2Clan owner = castle.getOwner();
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (owner != null)
		{
			if ((castleOwner != null) && (castleOwner != qs.getPlayer()) && (owner == qs.getPlayer().getClan()) && (castleOwner.getQuestState(getName()) != null) && castleOwner.getQuestState(getName()).isCond(cond))
			{
				return true;
			}
		}
		return false;
	}
}