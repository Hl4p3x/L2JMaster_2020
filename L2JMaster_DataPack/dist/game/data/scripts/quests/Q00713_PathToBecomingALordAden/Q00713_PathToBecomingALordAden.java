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
package quests.Q00713_PathToBecomingALordAden;

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

/**
 * Path to Becoming a Lord - Aden (713)
 * @author MaGa
 */
public final class Q00713_PathToBecomingALordAden extends Quest
{
	private static final int LOGAN = 35274;
	private static final int ORVEN = 30857;
	
	private static final int[] MOBS =
	{
		20669, // Taik Orc Supply Leader
		20665 // Taik Orc Supply
	};
	
	private static final int ADEN_CASTLE = 5;
	
	public Q00713_PathToBecomingALordAden()
	{
		super(713, Q00713_PathToBecomingALordAden.class.getSimpleName(), "Path to Becoming a Lord - Aden");
		addStartNpc(LOGAN);
		addKillId(MOBS);
		addTalkId(LOGAN, ORVEN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(ADEN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		if (event.equals("35274-02.html"))
		{
			qs.startQuest();
		}
		else if (event.equals("30857-03.html"))
		{
			qs.setCond(2);
		}
		else if (event.equals("35274-05.html"))
		{
			if (castle.getOwner().getLeader().getPlayerInstance() != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_ADEN_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_ADEN);
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
		if ((qs != null) && qs.isCond(4))
		{
			if (qs.getInt("mobs") < 100)
			{
				qs.set("mobs", String.valueOf(qs.getInt("mobs") + 1));
			}
			else
			{
				qs.setCond(5);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final Castle castle = CastleManager.getInstance().getCastleById(ADEN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case LOGAN:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35274-01.html";
						}
						else
						{
							htmltext = "35274-00.html";
							qs.exitQuest(true);
						}
					}
					else
					{
						htmltext = "35274-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					htmltext = "35274-03.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "35274-04.html";
				}
				break;
			}
			case ORVEN:
			{
				if (qs.isCond(1))
				{
					htmltext = "30857-01.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "30857-04.html";
				}
				else if (qs.isCond(4))
				{
					htmltext = "30857-05.html";
				}
				else if (qs.isCond(5))
				{
					qs.setCond(7);
					htmltext = "30857-06.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30857-06.html";
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
			if (fortress.getContractedCastleId() == ADEN_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
}