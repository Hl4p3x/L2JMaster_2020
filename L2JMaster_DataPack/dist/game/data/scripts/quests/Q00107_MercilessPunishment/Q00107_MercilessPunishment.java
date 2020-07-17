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
package quests.Q00107_MercilessPunishment;

import quests.Q00281_HeadForTheHills.Q00281_HeadForTheHills;

import com.l2jserver.gameserver.enums.audio.Sound;
import com.l2jserver.gameserver.enums.Race;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.holders.ItemHolder;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.util.Util;

/**
 * Merciless Punishment (107)
 * @author janiko
 */
public final class Q00107_MercilessPunishment extends Quest
{
	// Npc
	private static final int URUTU_CHIEF_HATOS = 30568;
	private static final int CENTURION_PARUGON = 30580;
	// Items
	private static final int HATOSS_ORDER_1 = 1553;
	private static final int HATOSS_ORDER_2 = 1554;
	private static final int HATOSS_ORDER_3 = 1555;
	private static final int LETTER_TO_DARK_ELF = 1556;
	private static final int LETTER_TO_HUMAN = 1557;
	private static final int LETTER_TO_ELF = 1558;
	// Monster
	private static final int BARANKA_MESSENGER = 27041;
	// Rewards
	private static final ItemHolder[] REWARDS =
	{
		new ItemHolder(1510, 100), // Butcher's Sword
		new ItemHolder(1060, 100), // Lesser Healing Potion
		new ItemHolder(4412, 10), // Echo Crystal - Theme of Battle
		new ItemHolder(4413, 10), // Echo Crystal - Theme of Love
		new ItemHolder(4414, 10), // Echo Crystal - Theme of Solitude
		new ItemHolder(4415, 10), // Echo Crystal - Theme of Feast
		new ItemHolder(4416, 10), // Echo Crystal - Theme of Celebration
		new ItemHolder(5789, 7000), // Soulshot: No Grade for Beginners
	};
	// Misc
	private static final int MIN_LVL = 10;
	
	public Q00107_MercilessPunishment()
	{
		super(107, Q00107_MercilessPunishment.class.getSimpleName(), "Merciless Punishment");
		addStartNpc(URUTU_CHIEF_HATOS);
		addTalkId(URUTU_CHIEF_HATOS, CENTURION_PARUGON);
		addKillId(BARANKA_MESSENGER);
		registerQuestItems(HATOSS_ORDER_1, HATOSS_ORDER_2, HATOSS_ORDER_3, LETTER_TO_DARK_ELF, LETTER_TO_HUMAN, LETTER_TO_ELF);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		switch (event)
		{
			case "30568-04.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					giveItems(player, HATOSS_ORDER_1, 1);
					htmltext = event;
				}
				break;
			}
			case "30568-07.html":
			{
				giveAdena(player, 200, true);
				playSound(player, Sound.ITEMSOUND_QUEST_GIVEUP);
				qs.exitQuest(true);
				htmltext = event;
				break;
			}
			case "30568-08.html":
			{
				if (qs.isCond(3) && hasQuestItems(player, HATOSS_ORDER_1))
				{
					qs.setCond(4);
					takeItems(player, HATOSS_ORDER_1, -1);
					giveItems(player, HATOSS_ORDER_2, 1);
					htmltext = event;
				}
				break;
			}
			case "30568-10.html":
			{
				if (qs.isCond(5) && hasQuestItems(player, HATOSS_ORDER_2))
				{
					qs.setCond(6);
					takeItems(player, HATOSS_ORDER_2, -1);
					giveItems(player, HATOSS_ORDER_3, 1);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (npc.getId())
		{
			case URUTU_CHIEF_HATOS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						if (talker.getRace() != Race.ORC)
						{
							htmltext = "30568-01.htm";
						}
						else if (talker.getLevel() < MIN_LVL)
						{
							htmltext = "30568-02.htm";
						}
						else
						{
							htmltext = "30568-03.htm";
						}
						break;
					}
					case State.STARTED:
					{
						switch (qs.getCond())
						{
							case 1:
							case 2:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_1))
								{
									htmltext = "30568-05.html";
								}
								break;
							}
							case 3:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_1, LETTER_TO_HUMAN))
								{
									htmltext = "30568-06.html";
								}
								break;
							}
							case 4:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_2, LETTER_TO_HUMAN))
								{
									htmltext = "30568-08.html";
								}
								break;
							}
							case 5:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_2, LETTER_TO_HUMAN, LETTER_TO_DARK_ELF))
								{
									htmltext = "30568-09.html";
								}
								break;
							}
							case 6:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_3, LETTER_TO_HUMAN, LETTER_TO_DARK_ELF))
								{
									htmltext = "30568-10.html";
								}
								break;
							}
							case 7:
							{
								if (hasQuestItems(talker, HATOSS_ORDER_3, LETTER_TO_HUMAN, LETTER_TO_DARK_ELF, LETTER_TO_ELF))
								{
									Q00281_HeadForTheHills.giveNewbieReward(talker);
									giveAdena(talker, 14666, true);
									for (ItemHolder reward : REWARDS)
									{
										giveItems(talker, reward);
									}
									addExpAndSp(talker, 34565, 2962);
									qs.exitQuest(false, true);
									htmltext = "30568-11.html";
									talker.sendPacket(new SocialAction(talker.getObjectId(), 3));
									showOnScreenMsg(talker, NpcStringId.ACQUISITION_OF_RACE_SPECIFIC_WEAPON_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 1, 5000);
								}
								break;
							}
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
				}
				break;
			}
			case CENTURION_PARUGON:
			{
				if (qs.isStarted())
				{
					if (qs.isCond(1) && hasQuestItems(talker, HATOSS_ORDER_1))
					{
						qs.setCond(2, true);
						htmltext = "30580-01.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && Util.checkIfInRange(1500, npc, killer, true))
		{
			switch (qs.getCond())
			{
				case 2:
				{
					if (hasQuestItems(killer, HATOSS_ORDER_1))
					{
						giveItems(killer, LETTER_TO_HUMAN, 1);
						qs.setCond(3, true);
					}
					break;
				}
				case 4:
				{
					if (hasQuestItems(killer, HATOSS_ORDER_2))
					{
						giveItems(killer, LETTER_TO_DARK_ELF, 1);
						qs.setCond(5, true);
					}
					break;
				}
				case 6:
				{
					if (hasQuestItems(killer, HATOSS_ORDER_3))
					{
						giveItems(killer, LETTER_TO_ELF, 1);
						qs.setCond(7, true);
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
}