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
package quests.Q00708_PathToBecomingALordGludio;

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
import com.l2jserver.util.Rnd;

/**
 * Path to Becoming a Lord - Gludio (708)
 * @author MaGa
 */
public final class Q00708_PathToBecomingALordGludio extends Quest
{
	private static final int SAYRES = 35100;
	private static final int PINTER = 30298;
	private static final int BATHIS = 30332;
	private static final int HEADLESS_KNIGHT = 20280;
	
	private static final int COKES = 1879;
	private static final int IRON_ORE = 1869;
	private static final int ANIMAL_SKIN = 1867;
	private static final int VARNISH = 1865;
	private static final int HEADLESS_KNIGHT_ARMOR = 13848;
	
	private static final int[] MOBS =
	{
		20045, // Skeleton Scout
		20051, // Skeleton Bowman
		20099, // Skeleton
		HEADLESS_KNIGHT
	};
	
	private static final int GLUDIO_CASTLE = 1;
	
	public Q00708_PathToBecomingALordGludio()
	{
		super(708, Q00708_PathToBecomingALordGludio.class.getSimpleName(), "Path to Becoming a Lord - Gludio");
		addStartNpc(SAYRES);
		addKillId(MOBS);
		addTalkId(SAYRES, PINTER, BATHIS);
		questItemIds = new int[]
		{
			HEADLESS_KNIGHT_ARMOR
		};
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (event.equals("35100-03.html"))
		{
			qs.startQuest();
		}
		else if (event.equals("35100-05.html"))
		{
			qs.setCond(2);
		}
		else if (event.equals("35100-08.html"))
		{
			if (isLordAvailable(2, qs))
			{
				castleOwner.getQuestState(getName()).set("confidant", String.valueOf(qs.getPlayer().getObjectId()));
				castleOwner.getQuestState(getName()).setCond(3);
				qs.setState(State.STARTED);
			}
			else
			{
				htmltext = "35100-05a.html";
			}
		}
		else if (event.equals("30298-03.html"))
		{
			if (isLordAvailable(3, qs))
			{
				castleOwner.getQuestState(getName()).setCond(4);
			}
			else
			{
				htmltext = "30298-03a.html";
			}
		}
		else if (event.equals("30332-02.html"))
		{
			qs.setCond(6);
		}
		else if (event.equals("30332-05.html"))
		{
			takeItems(player, HEADLESS_KNIGHT_ARMOR, 1);
			qs.setCond(8);
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.LISTEN_YOU_VILLAGERS_OUR_LIEGE_WHO_WILL_SOON_BECOME_A_LORD_HAS_DEFEATED_THE_HEADLESS_KNIGHT_YOU_CAN_NOW_REST_EASY));
		}
		else if (event.equals("30298-05.html"))
		{
			if (isLordAvailable(8, qs))
			{
				takeItems(player, ANIMAL_SKIN, 100);
				takeItems(player, VARNISH, 100);
				takeItems(player, IRON_ORE, 100);
				takeItems(player, COKES, 50);
				castleOwner.getQuestState(getName()).setCond(9);
			}
			else
			{
				htmltext = "30298-03a.html";
			}
		}
		else if (event.equals("35100-12.html"))
		{
			if (castleOwner != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_LORD_OF_THE_TOWN_OF_GLUDIO_LONG_MAY_HE_REIGN);
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
		if ((qs != null) && qs.isCond(6))
		{
			if ((npc.getId() != HEADLESS_KNIGHT) && (Rnd.get(9) == 0))
			{
				addSpawn(HEADLESS_KNIGHT, npc, true, 300000);
			}
			else if (npc.getId() == HEADLESS_KNIGHT)
			{
				giveItems(killer, HEADLESS_KNIGHT_ARMOR, 1);
				qs.setCond(7);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case SAYRES:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35100-01.html";
						}
						else
						{
							htmltext = "35100-00.html";
							qs.exitQuest(true);
						}
					}
					else if (isLordAvailable(2, qs))
					{
						if (castleOwner.calculateDistance(npc, false, false) <= 200)
						{
							htmltext = "35100-07.html";
						}
						else
						{
							htmltext = "35100-05a.html";
						}
					}
					else if (qs.getState() == State.STARTED)
					{
						htmltext = "35100-08a.html";
					}
					else
					{
						htmltext = "35100-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					htmltext = "35100-04.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "35100-06.html";
				}
				else if (qs.isCond(4))
				{
					qs.set("cond", "5");
					htmltext = "35100-09.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "35100-10.html";
				}
				else if ((qs.getCond() > 5) && (qs.getCond() < 9))
				{
					htmltext = "35100-08.html";
				}
				else if (qs.isCond(9))
				{
					htmltext = "35100-11.html";
				}
				break;
			}
			case PINTER:
			{
				if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(3, qs))
				{
					if (castleOwner.getQuestState(getName()).getInt("confidant") == qs.getPlayer().getObjectId())
					{
						htmltext = "30298-01.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(8, qs))
				{
					if ((getQuestItemsCount(talker, ANIMAL_SKIN) >= 100) && (getQuestItemsCount(talker, VARNISH) >= 100) && (getQuestItemsCount(talker, IRON_ORE) >= 100) && (getQuestItemsCount(talker, COKES) >= 50))
					{
						htmltext = "30298-04.html";
					}
					else
					{
						htmltext = "30298-04a.html";
					}
				}
				else if ((qs.getState() == State.STARTED) && qs.isCond(0) && isLordAvailable(9, qs))
				{
					htmltext = "30298-06.html";
				}
				break;
			}
			case BATHIS:
			{
				if (qs.isCond(5))
				{
					htmltext = "30332-01.html";
				}
				else if (qs.isCond(6))
				{
					htmltext = "30332-03.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30332-04.html";
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
			if (fortress.getContractedCastleId() == GLUDIO_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isLordAvailable(int cond, QuestState qs)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(GLUDIO_CASTLE);
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