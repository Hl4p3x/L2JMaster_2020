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
package quests.Q10274_CollectingInTheAir;

import quests.Q10273_GoodDayToFly.Q10273_GoodDayToFly;

import com.l2jserver.gameserver.enums.audio.Sound;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.model.skills.Skill;

/**
 * Collecting in the Air (10274)<br>
 * Original Jython script by Kerberos v1.0 on 2009/04/26.
 * @author nonom
 */
public class Q10274_CollectingInTheAir extends Quest
{
	// NPC
	private static final int LEKON = 32557;
	// Items
	private static final int SCROLL = 13844;
	private static final int RED = 13858;
	private static final int BLUE = 13859;
	private static final int GREEN = 13860;
	// Reward
	private static final int EXPERT_TEXT = 13728;
	// Monsters
	private static final int MOBS[] =
	{
		18684, // Red Star Stone
		18685, // Red Star Stone
		18686, // Red Star Stone
		18687, // Blue Star Stone
		18688, // Blue Star Stone
		18689, // Blue Star Stone
		18690, // Green Star Stone
		18691, // Green Star Stone
		18692 // Green Star Stone
	};
	
	public Q10274_CollectingInTheAir()
	{
		super(10274, Q10274_CollectingInTheAir.class.getSimpleName(), "Collecting in the Air");
		addStartNpc(LEKON);
		addTalkId(LEKON);
		addSkillSeeId(MOBS);
		registerQuestItems(SCROLL, RED, BLUE, GREEN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		if (event.equals("32557-03.html"))
		{
			st.startQuest();
			st.giveItems(SCROLL, 8);
		}
		return event;
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, Skill skill, L2Object[] targets, boolean isSummon)
	{
		final QuestState st = getQuestState(caster, false);
		if ((st == null) || !st.isStarted())
		{
			return null;
		}
		
		if (st.isCond(1) && (skill.getId() == 2630))
		{
			switch (npc.getId())
			{
				case 18684:
				case 18685:
				case 18686:
					st.takeItems(SCROLL, 1);
					st.giveItems(RED, 1);
					break;
				case 18687:
				case 18688:
				case 18689:
					st.takeItems(SCROLL, 1);
					st.giveItems(BLUE, 1);
					break;
				case 18690:
				case 18691:
				case 18692:
					st.takeItems(SCROLL, 1);
					st.giveItems(GREEN, 1);
					break;
			}
			st.playSound(Sound.ITEMSOUND_QUEST_ITEMGET);
			npc.doDie(caster);
		}
		if (st.isCond(1) && ((st.getQuestItemsCount(RED) >= 8) || (st.getQuestItemsCount(BLUE) >= 8) || (st.getQuestItemsCount(GREEN) >= 8)))
		{
			st.setCond(2);
			st.playSound(Sound.ITEMSOUND_QUEST_MIDDLE);
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		QuestState st = getQuestState(player, true);
		switch (st.getState())
		{
			case State.COMPLETED:
				htmltext = "32557-0a.html";
				break;
			case State.CREATED:
				htmltext = ((player.getLevel() >= 75) && player.hasQuestCompleted(Q10273_GoodDayToFly.class.getSimpleName())) ? "32557-01.htm" : "32557-00.html";
				break;
			case State.STARTED:
				if ((st.getQuestItemsCount(RED) >= 8) || (st.getQuestItemsCount(BLUE) >= 8) || (st.getQuestItemsCount(GREEN) >= 8) || ((st.getQuestItemsCount(RED) + st.getQuestItemsCount(BLUE) + st.getQuestItemsCount(GREEN)) >= 8))
				{
					htmltext = "32557-05.html";
					st.giveItems(EXPERT_TEXT, 1);
					st.addExpAndSp(25160, 2525);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "32557-04.html";
				}
				break;
		}
		return htmltext;
	}
}