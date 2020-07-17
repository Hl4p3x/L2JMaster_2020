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
package quests.Q00505_BloodOffering;

import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.enums.audio.Sound;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;

import quests.Q00635_IntoTheDimensionalRift.Q00635_IntoTheDimensionalRift;

/**
 * Blood Offering (505)
 * @author U3Games
 */
public final class Q00505_BloodOffering extends Quest
{
	private static final int[] FESTIVAL_GUIDE =
	{
		31127,
		31128,
		31129,
		31130,
		31131,
		31137,
		31138,
		31139,
		31140,
		31141
	};
	
	private static final int[] FESTIVAL_WITCH =
	{
		31132,
		31133,
		31134,
		31135,
		31136,
		31142,
		31143,
		31144,
		31145,
		31146
	};
	
	private static final int[] RIFT_POST =
	{
		31488,
		31489,
		31490,
		31491,
		31492,
		31493
	};
	
	public Q00505_BloodOffering()
	{
		super(505, Q00505_BloodOffering.class.getSimpleName(), "Blood Offering");
		addStartNpc(FESTIVAL_GUIDE);
		addStartNpc(FESTIVAL_WITCH);
		addStartNpc(RIFT_POST);
		addTalkId(FESTIVAL_GUIDE);
		addTalkId(FESTIVAL_WITCH);
		addTalkId(RIFT_POST);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final QuestState st = getQuestState(player, true);
		final QuestState qs635 = player.getQuestState(Q00635_IntoTheDimensionalRift.class.getSimpleName());
		String htmltext = getNoQuestMsg(player);
		
		switch (npc.getId())
		{
			// FESTIVAL_GUIDE
			case 31127:
			case 31128:
			case 31129:
			case 31130:
			case 31131:
			case 31137:
			case 31138:
			case 31139:
			case 31140:
			case 31141:
			{
				player.teleToLocation(-114796, -179334, -6752);
				
				if (st != null)
				{
					if (qs635.isStarted())
					{
						qs635.setCond(1);
						playSound(player, Sound.ITEMSOUND_QUEST_ACCEPT);
					}
					else
					{
						playSound(player, Sound.ITEMSOUND_QUEST_FINISH);
					}
					
					st.exitQuest(true);
				}
				
				htmltext = "guide.htm";
				break;
			}
			
			// FESTIVAL_WITCH
			case 31132:
			{
				player.teleToLocation(-80204, 87056, -5154);
				htmltext = "witch.htm";
				break;
			}
			case 31133:
			{
				player.teleToLocation(-77198, 87678, -5182);
				htmltext = "witch.htm";
				break;
			}
			case 31134:
			{
				player.teleToLocation(-76183, 87135, -5179);
				htmltext = "witch.htm";
				break;
			}
			case 31135:
			{
				player.teleToLocation(-76945, 86602, 5153);
				htmltext = "witch.htm";
				break;
			}
			case 31136:
			{
				player.teleToLocation(-79970, 85997, -5154);
				htmltext = "witch.htm";
				break;
			}
			case 31142:
			{
				player.teleToLocation(-79182, 111893, -4898);
				htmltext = "witch.htm";
				break;
			}
			case 31143:
			{
				player.teleToLocation(-76176, 112505, -4899);
				htmltext = "witch.htm";
				break;
			}
			case 31144:
			{
				player.teleToLocation(-75198, 111969, -4898);
				htmltext = "witch.htm";
				break;
			}
			case 31145:
			{
				player.teleToLocation(-75920, 111435, -4900);
				htmltext = "witch.htm";
				break;
			}
			case 31146:
			{
				player.teleToLocation(-78928, 110825, -4926);
				htmltext = "witch.htm";
				break;
			}
			
			// RIFT_POST
			case 31488:
			case 31489:
			case 31490:
			case 31491:
			case 31492:
			case 31493:
			{
				final boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
				final int cabalState = SevenSigns.getInstance().getPlayerCabal(player.getId());
				if (!isSealValidationPeriod)
				{
					if (cabalState == SevenSigns.CABAL_DAWN)
					{
						st.startQuest();
						st.setCond(1);
						
						if (qs635.isStarted())
						{
							qs635.unset("cond");
						}
						
						st.getPlayer().teleToLocation(-80157, 111344, -4901);
						htmltext = "riftpost-1.htm";
					}
					else
					{
						if (cabalState == SevenSigns.CABAL_DUSK)
						{
							st.startQuest();
							st.setCond(1);
							
							if (qs635.isStarted())
							{
								qs635.unset("cond");
							}
							
							st.getPlayer().teleToLocation(-81261, 86531, -5157);
							htmltext = "riftpost-1.htm";
						}
						else
						{
							htmltext = "riftpost-2.htm";
						}
					}
				}
				else
				{
					htmltext = "riftpost-2.htm";
				}
				
				break;
			}
			
		}
		
		return htmltext;
	}
}
