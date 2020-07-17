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
package ai.npc.Kier;

import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.QuestState;

import ai.npc.AbstractNpcAI;
import quests.Q00115_TheOtherSideOfTruth.Q00115_TheOtherSideOfTruth;
import quests.Q10283_RequestOfIceMerchant.Q10283_RequestOfIceMerchant;

/**
 * Kier AI.
 * @author Adry_85
 * @since 2.6.0.0
 */
public final class Kier extends AbstractNpcAI
{
	// NPC
	private static final int KIER = 32022;
	
	private Kier()
	{
		super(Kier.class.getSimpleName(), "ai/npc");
		addFirstTalkId(KIER);
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = null;
		final QuestState st_Q00115 = player.getQuestState(Q00115_TheOtherSideOfTruth.class.getSimpleName());
		if (st_Q00115 == null)
		{
			htmltext = "32022-02.html";
		}
		else if (!st_Q00115.isCompleted())
		{
			htmltext = "32022-01.html";
		}
		
		final QuestState st_Q10283 = player.getQuestState(Q10283_RequestOfIceMerchant.class.getSimpleName());
		if (st_Q10283 != null)
		{
			if (st_Q10283.isMemoState(2))
			{
				htmltext = "32022-03.html";
			}
			else if (st_Q10283.isCompleted())
			{
				htmltext = "32022-04.html";
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new Kier();
	}
}