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
package quests.Q00712_PathToBecomingALordOren;

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

/**
 * Path to Becoming a Lord - Oren (712)
 * @author MaGa
 */
public final class Q00712_PathToBecomingALordOren extends Quest
{
	private static final int BRASSEUR = 35226;
	private static final int CROOP = 30676;
	private static final int MARTY = 30169;
	private static final int VALLERIA = 30176;
	
	private static final int NEBULITE_ORB = 13851;
	
	private static final int[] OEL_MAHUMS =
	{
		20575, // Oel Mahum Warrior
		20576 // Oel Mahum Witch Doctor
	};
	
	private static final int OREN_CASTLE = 4;
	
	public Q00712_PathToBecomingALordOren()
	{
		super(712, Q00712_PathToBecomingALordOren.class.getSimpleName(), "Path to Becoming a Lord - Oren");
		addStartNpc(BRASSEUR, MARTY);
		addKillId(OEL_MAHUMS);
		addTalkId(BRASSEUR, CROOP, MARTY, VALLERIA);
		questItemIds = new int[]
		{
			NEBULITE_ORB
		};
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		final Castle castle = CastleManager.getInstance().getCastleById(OREN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (event.equals("35226-03.html"))
		{
			qs.startQuest();
		}
		else if (event.equals("30676-03.html"))
		{
			qs.setCond(3);
		}
		else if (event.equals("30169-02.html"))
		{
			if (isLordAvailable(3, qs))
			{
				castleOwner.getQuestState(getName()).setCond(4);
				qs.setState(State.STARTED);
			}
		}
		else if (event.equals("30176-02.html"))
		{
			if (isLordAvailable(4, qs))
			{
				castleOwner.getQuestState(getName()).setCond(5);
				qs.exitQuest(true);
			}
		}
		else if (event.equals("30676-05.html"))
		{
			qs.setCond(6);
		}
		else if (event.equals("30676-07.html"))
		{
			takeItems(player, NEBULITE_ORB, -1);
			qs.setCond(8);
		}
		else if (event.equals("35226-06.html"))
		{
			if (castleOwner != null)
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), Say2.NPC_SHOUT, npc.getId(), NpcStringId.S1_HAS_BECOME_THE_LORD_OF_THE_TOWN_OF_OREN_MAY_THERE_BE_GLORY_IN_THE_TERRITORY_OF_OREN);
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
		if ((qs != null) && qs.isCond(6))
		{
			if (getQuestItemsCount(killer, NEBULITE_ORB) < 300)
			{
				giveItems(killer, NEBULITE_ORB, 1);
			}
			if (getQuestItemsCount(killer, NEBULITE_ORB) >= 300)
			{
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
		final Castle castle = CastleManager.getInstance().getCastleById(OREN_CASTLE);
		if (castle.getOwner() == null)
		{
			return "Castle has no lord";
		}
		
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		
		switch (npc.getId())
		{
			case BRASSEUR:
			{
				if (qs.isCond(0))
				{
					if (castleOwner == qs.getPlayer())
					{
						if (!hasFort())
						{
							htmltext = "35226-01.html";
						}
						else
						{
							htmltext = "35226-00.html";
							qs.exitQuest(true);
						}
					}
					else
					{
						htmltext = "35226-00a.html";
						qs.exitQuest(true);
					}
				}
				else if (qs.isCond(1))
				{
					qs.setCond(2);
					htmltext = "35226-04.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "35226-04.html";
				}
				else if (qs.isCond(8))
				{
					htmltext = "35226-05.html";
				}
				break;
			}
			case CROOP:
			{
				if (qs.isCond(2))
				{
					htmltext = "30676-01.html";
				}
				else if (qs.isCond(3) || qs.isCond(4))
				{
					htmltext = "30676-03.html";
				}
				else if (qs.isCond(5))
				{
					htmltext = "30676-04.html";
				}
				else if (qs.isCond(6))
				{
					htmltext = "30676-05.html";
				}
				else if (qs.isCond(7))
				{
					htmltext = "30676-06.html";
				}
				else if (qs.isCond(8))
				{
					htmltext = "30676-08.html";
				}
				break;
			}
			case MARTY:
			{
				if (qs.isCond(0))
				{
					if (isLordAvailable(3, qs))
					{
						htmltext = "30169-01.html";
					}
					else
					{
						htmltext = "30169-00.html";
					}
				}
				break;
			}
			case VALLERIA:
			{
				if ((qs.getState() == State.STARTED) && isLordAvailable(4, qs))
				{
					htmltext = "30176-01.html";
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
			if (fortress.getContractedCastleId() == OREN_CASTLE)
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isLordAvailable(int cond, QuestState qs)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(OREN_CASTLE);
		final L2Clan owner = castle.getOwner();
		final L2PcInstance castleOwner = castle.getOwner().getLeader().getPlayerInstance();
		if (owner != null)
		{
			if ((castleOwner != null) && (castleOwner != qs.getPlayer()) && (owner == qs.getPlayer().getClan()) && (castleOwner.getQuestState(getName()) != null) && (castleOwner.getQuestState(getName()).isCond(cond)))
			{
				return true;
			}
		}
		return false;
	}
}