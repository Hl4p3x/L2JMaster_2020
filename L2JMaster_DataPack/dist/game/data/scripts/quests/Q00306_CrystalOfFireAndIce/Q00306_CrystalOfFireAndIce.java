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
package quests.Q00306_CrystalOfFireAndIce;

import java.util.HashMap;
import java.util.Map;

import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.holders.ItemHolder;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.util.Util;

/**
 * Crystals of Fire and Ice (306)
 * @author ivantotov
 */
public final class Q00306_CrystalOfFireAndIce extends Quest
{
	// NPC
	private static final int KATERINA = 30004;
	// Items
	private static final int FLAME_SHARD = 1020;
	private static final int ICE_SHARD = 1021;
	// Misc
	private static final int MIN_LEVEL = 17;
	// Monsters
	private static final int UNDINE_NOBLE = 20115;
	private static final Map<Integer, ItemHolder> MONSTER_DROPS = new HashMap<>();
	static
	{
		MONSTER_DROPS.put(20109, new ItemHolder(FLAME_SHARD, 925)); // Salamander
		MONSTER_DROPS.put(20110, new ItemHolder(ICE_SHARD, 900)); // Undine
		MONSTER_DROPS.put(20112, new ItemHolder(FLAME_SHARD, 900)); // Salamander Elder
		MONSTER_DROPS.put(20113, new ItemHolder(ICE_SHARD, 925)); // Undine Elder
		MONSTER_DROPS.put(20114, new ItemHolder(FLAME_SHARD, 925)); // Salamander Noble
		MONSTER_DROPS.put(UNDINE_NOBLE, new ItemHolder(ICE_SHARD, 950)); // Undine Noble
	}
	
	public Q00306_CrystalOfFireAndIce()
	{
		super(306, Q00306_CrystalOfFireAndIce.class.getSimpleName(), "Crystals of Fire and Ice");
		addStartNpc(KATERINA);
		addTalkId(KATERINA);
		addKillId(MONSTER_DROPS.keySet());
		registerQuestItems(FLAME_SHARD, ICE_SHARD);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return null;
		}
		String htmltext = null;
		switch (event)
		{
			case "30004-04.htm":
			{
				if (st.isCreated())
				{
					st.startQuest();
					htmltext = event;
				}
				break;
			}
			case "30004-08.html":
			{
				st.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "30004-09.html":
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		final QuestState qs;
		if (npc.getId() == UNDINE_NOBLE) // Undine Noble gives quest drops only for the killer
		{
			qs = getQuestState(killer, false);
			if ((qs != null) && qs.isStarted())
			{
				giveKillReward(killer, npc);
			}
		}
		else
		{
			qs = getRandomPartyMemberState(killer, -1, 3, npc);
			if (qs != null)
			{
				giveKillReward(qs.getPlayer(), npc);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		switch (st.getState())
		{
			case State.CREATED:
			{
				htmltext = player.getLevel() >= MIN_LEVEL ? "30004-03.htm" : "30004-02.htm";
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(player, getRegisteredItemIds()))
				{
					final long flame = st.getQuestItemsCount(FLAME_SHARD);
					final long ice = st.getQuestItemsCount(ICE_SHARD);
					st.giveAdena(((flame * 40) + (ice * 40) + ((flame + ice) >= 10 ? 5000 : 0)), true);
					takeItems(player, -1, getRegisteredItemIds());
					htmltext = "30004-07.html";
				}
				else
				{
					htmltext = "30004-05.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	private static final void giveKillReward(L2PcInstance player, L2Npc npc)
	{
		if (Util.checkIfInRange(1500, npc, player, false))
		{
			final ItemHolder item = MONSTER_DROPS.get(npc.getId());
			giveItemRandomly(player, npc, item.getId(), 1, 0, 1000.0 / item.getCount(), true);
		}
	}
}