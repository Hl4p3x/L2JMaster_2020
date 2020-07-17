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
package quests.Q00401_PathOfTheWarrior;

import com.l2jserver.Config;
import com.l2jserver.gameserver.enums.audio.Sound;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.util.Util;

/**
 * Path Of The Warrior (401)
 * @author ivantotov
 */
public final class Q00401_PathOfTheWarrior extends Quest
{
	// NPCs
	private static final int MASTER_AURON = 30010;
	private static final int TRADER_SIMPLON = 30253;
	// Items
	private static final int AURONS_LETTER = 1138;
	private static final int WARRIOR_GUILD_MARK = 1139;
	private static final int RUSTED_BRONZE_SWORD1 = 1140;
	private static final int RUSTED_BRONZE_SWORD2 = 1141;
	private static final int RUSTED_BRONZE_SWORD3 = 1142;
	private static final int SIMPLONS_LETTER = 1143;
	private static final int VENOMOUS_SPIDERS_LEG = 1144;
	// Reward
	private static final int MEDALLION_OF_WARRIOR = 1145;
	// Monster
	private static final int TRACKER_SKELETON = 20035;
	private static final int VENOMOUS_SPIDERS = 20038;
	private static final int TRACKER_SKELETON_LIDER = 20042;
	private static final int ARACHNID_TRACKER = 20043;
	// Misc
	private static final int MIN_LEVEL = 18;
	
	public Q00401_PathOfTheWarrior()
	{
		super(401, Q00401_PathOfTheWarrior.class.getSimpleName(), "Path Of The Warrior");
		addStartNpc(MASTER_AURON);
		addTalkId(MASTER_AURON, TRADER_SIMPLON);
		addAttackId(VENOMOUS_SPIDERS, ARACHNID_TRACKER);
		addKillId(TRACKER_SKELETON, VENOMOUS_SPIDERS, TRACKER_SKELETON_LIDER, ARACHNID_TRACKER);
		registerQuestItems(AURONS_LETTER, WARRIOR_GUILD_MARK, RUSTED_BRONZE_SWORD1, RUSTED_BRONZE_SWORD2, RUSTED_BRONZE_SWORD3, SIMPLONS_LETTER, VENOMOUS_SPIDERS_LEG);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "ACCEPT":
			{
				if (player.getClassId() == ClassId.fighter)
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						if (hasQuestItems(player, MEDALLION_OF_WARRIOR))
						{
							htmltext = "30010-04.htm";
						}
						else
						{
							htmltext = "30010-05.htm";
						}
					}
					else
					{
						htmltext = "30010-02.htm";
					}
				}
				else if (player.getClassId() == ClassId.warrior)
				{
					htmltext = "30010-02a.htm";
				}
				else
				{
					htmltext = "30010-03.htm";
				}
				break;
			}
			case "30010-06.htm":
			{
				if (!hasQuestItems(player, AURONS_LETTER))
				{
					qs.startQuest();
					giveItems(player, AURONS_LETTER, 1);
					htmltext = event;
				}
				break;
			}
			case "30010-10.html":
			{
				htmltext = event;
				break;
			}
			case "30010-11.html":
			{
				if (hasQuestItems(player, SIMPLONS_LETTER, RUSTED_BRONZE_SWORD2))
				{
					takeItems(player, RUSTED_BRONZE_SWORD2, 1);
					giveItems(player, RUSTED_BRONZE_SWORD3, 1);
					takeItems(player, SIMPLONS_LETTER, 1);
					qs.setCond(5, true);
					htmltext = event;
				}
				break;
			}
			case "30253-02.html":
			{
				if (hasQuestItems(player, AURONS_LETTER))
				{
					takeItems(player, AURONS_LETTER, 1);
					giveItems(player, WARRIOR_GUILD_MARK, 1);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		final QuestState qs = getQuestState(attacker, false);
		if ((qs != null) && qs.isStarted())
		{
			switch (npc.getScriptValue())
			{
				case 0:
				{
					npc.getVariables().set("lastAttacker", attacker.getObjectId());
					if (!checkWeapon(attacker))
					{
						npc.setScriptValue(2);
					}
					else
					{
						npc.setScriptValue(1);
					}
					break;
				}
				case 1:
				{
					if (!checkWeapon(attacker))
					{
						npc.setScriptValue(2);
					}
					else if (npc.getVariables().getInt("lastAttacker") != attacker.getObjectId())
					{
						npc.setScriptValue(2);
					}
					break;
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isStarted() && Util.checkIfInRange(1500, npc, killer, true))
		{
			switch (npc.getId())
			{
				case TRACKER_SKELETON:
				case TRACKER_SKELETON_LIDER:
				{
					if (hasQuestItems(killer, WARRIOR_GUILD_MARK) && (getQuestItemsCount(killer, RUSTED_BRONZE_SWORD1) < 10))
					{
						if (getRandom(10) < 4)
						{
							giveItems(killer, RUSTED_BRONZE_SWORD1, 1 * (int) Config.RATE_QUEST_DROP);
							if (getQuestItemsCount(killer, RUSTED_BRONZE_SWORD1) >= 10)
							{
								qs.setCond(3, true);
							}
							else
							{
								playSound(killer, Sound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
					}
					break;
				}
				case VENOMOUS_SPIDERS:
				case ARACHNID_TRACKER:
				{
					if ((getQuestItemsCount(killer, VENOMOUS_SPIDERS_LEG) < 20) && npc.isScriptValue(1))
					{
						giveItems(killer, VENOMOUS_SPIDERS_LEG, 1 * (int) Config.RATE_QUEST_DROP);
						if (getQuestItemsCount(killer, VENOMOUS_SPIDERS_LEG) >= 20)
						{
							qs.setCond(6, true);
						}
						else
						{
							playSound(killer, Sound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated() || qs.isCompleted())
		{
			if (npc.getId() == MASTER_AURON)
			{
				htmltext = "30010-01.htm";
			}
		}
		else if (qs.isStarted())
		{
			switch (npc.getId())
			{
				case MASTER_AURON:
				{
					if (hasQuestItems(player, AURONS_LETTER))
					{
						htmltext = "30010-07.html";
					}
					else if (hasQuestItems(player, WARRIOR_GUILD_MARK))
					{
						htmltext = "30010-08.html";
					}
					else if (hasQuestItems(player, SIMPLONS_LETTER, RUSTED_BRONZE_SWORD2) && !hasAtLeastOneQuestItem(player, WARRIOR_GUILD_MARK, AURONS_LETTER))
					{
						htmltext = "30010-09.html";
					}
					else if (hasQuestItems(player, RUSTED_BRONZE_SWORD3) && !hasAtLeastOneQuestItem(player, WARRIOR_GUILD_MARK, AURONS_LETTER))
					{
						if (getQuestItemsCount(player, VENOMOUS_SPIDERS_LEG) < 20)
						{
							htmltext = "30010-12.html";
						}
						else
						{
							giveAdena(player, 163800, true);
							giveItems(player, MEDALLION_OF_WARRIOR, 1);
							final int level = player.getLevel();
							if (level >= 20)
							{
								addExpAndSp(player, 320534, 21012);
							}
							else if (level == 19)
							{
								addExpAndSp(player, 456128, 27710);
							}
							else
							{
								addExpAndSp(player, 160267, 34408);
							}
							qs.exitQuest(false, true);
							player.sendPacket(new SocialAction(player.getObjectId(), 3));
							qs.saveGlobalQuestVar("1ClassQuestFinished", "1");
							htmltext = "30010-13.html";
						}
					}
					break;
				}
				case TRADER_SIMPLON:
				{
					if (hasQuestItems(player, AURONS_LETTER))
					{
						htmltext = "30253-01.html";
					}
					else if (hasQuestItems(player, WARRIOR_GUILD_MARK))
					{
						if (!hasQuestItems(player, RUSTED_BRONZE_SWORD1))
						{
							htmltext = "30253-03.html";
						}
						else if (getQuestItemsCount(player, RUSTED_BRONZE_SWORD1) < 10)
						{
							htmltext = "30253-04.html";
						}
						else
						{
							takeItems(player, WARRIOR_GUILD_MARK, 1);
							takeItems(player, RUSTED_BRONZE_SWORD1, -1);
							giveItems(player, RUSTED_BRONZE_SWORD2, 1);
							giveItems(player, SIMPLONS_LETTER, 1);
							qs.setCond(4, true);
							htmltext = "30253-05.html";
						}
					}
					else if (hasQuestItems(player, SIMPLONS_LETTER))
					{
						htmltext = "30253-06.html";
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	private static boolean checkWeapon(L2PcInstance player)
	{
		L2ItemInstance weapon = player.getActiveWeaponInstance();
		return ((weapon != null) && ((weapon.getId() == RUSTED_BRONZE_SWORD3)));
	}
}