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
package quests.Q00029_ChestCaughtWithABaitOfEarth;

import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;

import quests.Q00052_WilliesSpecialBait.Q00052_WilliesSpecialBait;

/**
 * Chest Caught With A Bait Of Earth (29)<br>
 * Original Jython script by Skeleton.
 * @author nonom
 */
public class Q00029_ChestCaughtWithABaitOfEarth extends Quest
{
	// NPCs
	private static final int WILLIE = 31574;
	private static final int ANABEL = 30909;
	// Items
	private static final int PURPLE_TREASURE_BOX = 6507;
	private static final int SMALL_GLASS_BOX = 7627;
	private static final int PLATED_LEATHER_GLOVES = 2455;
	
	public Q00029_ChestCaughtWithABaitOfEarth()
	{
		super(29, Q00029_ChestCaughtWithABaitOfEarth.class.getSimpleName(), "Chest Caught With A Bait Of Earth");
		addStartNpc(WILLIE);
		addTalkId(WILLIE, ANABEL);
		registerQuestItems(SMALL_GLASS_BOX);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31574-04.htm":
				st.startQuest();
				break;
			case "31574-08.htm":
				if (st.isCond(1) && st.hasQuestItems(PURPLE_TREASURE_BOX))
				{
					st.giveItems(SMALL_GLASS_BOX, 1);
					st.takeItems(PURPLE_TREASURE_BOX, -1);
					st.setCond(2, true);
					htmltext = "31574-07.htm";
				}
				break;
			case "30909-03.htm":
				if (st.isCond(2) && st.hasQuestItems(SMALL_GLASS_BOX))
				{
					st.giveItems(PLATED_LEATHER_GLOVES, 1);
					st.exitQuest(false, true);
					htmltext = "30909-02.htm";
				}
				break;
			
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		final int npcId = npc.getId();
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
			case State.CREATED:
				if (npcId == WILLIE)
				{
					htmltext = ((player.getLevel() >= 48) && player.hasQuestCompleted(Q00052_WilliesSpecialBait.class.getSimpleName())) ? "31574-01.htm" : "31574-02.htm";
				}
				break;
			case State.STARTED:
				switch (npcId)
				{
					case WILLIE:
						switch (st.getCond())
						{
							case 1:
								htmltext = "31574-06.htm";
								if (st.hasQuestItems(PURPLE_TREASURE_BOX))
								{
									htmltext = "31574-05.htm";
								}
								break;
							case 2:
								htmltext = "31574-09.htm";
								break;
						}
						break;
					case ANABEL:
						if (st.isCond(2))
						{
							htmltext = "30909-01.htm";
						}
						break;
				}
				break;
		}
		return htmltext;
	}
}
