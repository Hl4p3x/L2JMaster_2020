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
package quests.Q00715_PathToBecomingALordGoddard;

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
 * Path to Becoming a Lord - Goddard (715)
 * @author MaGa
 */
public final class Q00715_PathToBecomingALordGoddard extends Quest
{
	private static final int ALFRED = 35363;
	
	private static final int WATER_SPIRIT_ASHUTAR = 25316;
	private static final int FIRE_SPIRIT_NASTRON = 25306;
	
	private static final int GODDARD_CASTLE = 7;
	
	public Q00715_PathToBecomingALordGoddard()
	{
		super(715, Q00715_PathToBecomingALordGoddard.class.getSimpleName(), "Path to Becoming a Lord - Goddard");
		addStartNpc(ALFRED);
		addKillId(WATER_SPIRIT_ASHUTAR, FIRE_SPIRIT_NASTRON);
		addTalkId(ALFRED);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(GODDARD_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		if (event.equals("35363-03.html"))
		{
			qs.startQuest();
		}
		else if (event.equals("35363-04a.html"))
		{
			qs.setCond(3);
		}
		else if (event.equals("35363-04b.html"))
		{
			qs.setCond(2);
		}
		else if (event.equals("35363-08.html"))
		{
			if (castle.getOwner().getLeader().getPlayerInstance() != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_GODDARD_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_GODDARD);
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
		if (qs == null)
		{
			return super.onKill(npc, killer, isSummon);
		}
		
		if (qs.isCond(2) && (npc.getId() == FIRE_SPIRIT_NASTRON))
		{
			qs.setCond(4);
		}
		else if (qs.isCond(3) && (npc.getId() == WATER_SPIRIT_ASHUTAR))
		{
			qs.setCond(5);
		}
		
		if (qs.isCond(6) && (npc.getId() == WATER_SPIRIT_ASHUTAR))
		{
			qs.setCond(9);
		}
		else if (qs.isCond(7) && (npc.getId() == FIRE_SPIRIT_NASTRON))
		{
			qs.setCond(8);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		final Castle castle = CastleManager.getInstance().getCastleById(GODDARD_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		if (qs.isCond(0))
		{
			if (castleOwner == qs.getPlayer())
			{
				if (!hasFort())
				{
					htmltext = "35363-01.html";
				}
				else
				{
					htmltext = "35363-00.html";
					qs.exitQuest(true);
				}
			}
			else
			{
				htmltext = "35363-00a.html";
				qs.exitQuest(true);
			}
		}
		else if (qs.isCond(1))
		{
			htmltext = "35363-03.html";
		}
		else if (qs.isCond(2))
		{
			htmltext = "35363-05b.html";
		}
		else if (qs.isCond(3))
		{
			htmltext = "35363-05a.html";
		}
		else if (qs.isCond(4))
		{
			qs.setCond(6);
			htmltext = "35363-06b.html";
		}
		else if (qs.isCond(5))
		{
			qs.setCond(7);
			htmltext = "35363-06a.html";
		}
		else if (qs.isCond(6))
		{
			htmltext = "35363-06b.html";
		}
		else if (qs.isCond(7))
		{
			htmltext = "35363-06a.html";
		}
		else if (qs.isCond(8) || qs.isCond(9))
		{
			htmltext = "35363-07.html";
		}
		return htmltext;
	}
	
	private boolean hasFort()
	{
		for (Fort fortress : FortManager.getInstance().getForts())
		{
			if (fortress.getContractedCastleId() == GODDARD_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
}