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
package ai.npc.Summons.Servitors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import quests.Q00230_TestOfTheSummoner.Q00230_TestOfTheSummoner;
import ai.npc.AbstractNpcAI;

import com.l2jserver.gameserver.enums.audio.Sound;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2ServitorInstance;
import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.ListenerRegisterType;
import com.l2jserver.gameserver.model.events.annotations.Id;
import com.l2jserver.gameserver.model.events.annotations.RegisterEvent;
import com.l2jserver.gameserver.model.events.annotations.RegisterType;
import com.l2jserver.gameserver.model.events.impl.character.OnCreatureKill;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.util.Util;

/**
 * Servitor AI for quest Test Of The Summoner (230).
 * @author Zoey76
 */
public final class Servitors extends AbstractNpcAI
{
	// Quest Monster
	private static final int PAKO_THE_CAT = 27102;
	private static final int UNICORN_RACER = 27103;
	private static final int SHADOW_TUREN = 27104;
	private static final int MIMI_THE_CAT = 27105;
	private static final int UNICORN_PHANTASM = 27106;
	private static final int SILHOUETTE_TILFO = 27107;
	// Items
	private static final int CRYSTAL_OF_STARTING_1ST = 3360;
	private static final int CRYSTAL_OF_INPROGRESS_1ST = 3361;
	private static final int CRYSTAL_OF_DEFEAT_1ST = 3363;
	private static final int CRYSTAL_OF_STARTING_2ND = 3365;
	private static final int CRYSTAL_OF_INPROGRESS_2ND = 3366;
	private static final int CRYSTAL_OF_DEFEAT_2ND = 3368;
	private static final int CRYSTAL_OF_STARTING_3RD = 3370;
	private static final int CRYSTAL_OF_INPROGRESS_3RD = 3371;
	private static final int CRYSTAL_OF_DEFEAT_3RD = 3373;
	private static final int CRYSTAL_OF_STARTING_4TH = 3375;
	private static final int CRYSTAL_OF_INPROGRESS_4TH = 3376;
	private static final int CRYSTAL_OF_DEFEAT_4TH = 3378;
	private static final int CRYSTAL_OF_STARTING_5TH = 3380;
	private static final int CRYSTAL_OF_INPROGRESS_5TH = 3381;
	private static final int CRYSTAL_OF_DEFEAT_5TH = 3383;
	private static final int CRYSTAL_OF_STARTING_6TH = 3385;
	private static final int CRYSTAL_OF_INPROGRESS_6TH = 3386;
	private static final int CRYSTAL_OF_DEFEAT_6TH = 3388;
	
	private static final Map<Integer, List<Integer>> MONSTERS = new HashMap<>();
	static
	{
		MONSTERS.put(PAKO_THE_CAT, Arrays.asList(CRYSTAL_OF_STARTING_1ST, CRYSTAL_OF_INPROGRESS_1ST, CRYSTAL_OF_DEFEAT_1ST));
		MONSTERS.put(UNICORN_RACER, Arrays.asList(CRYSTAL_OF_STARTING_3RD, CRYSTAL_OF_INPROGRESS_3RD, CRYSTAL_OF_DEFEAT_3RD));
		MONSTERS.put(SHADOW_TUREN, Arrays.asList(CRYSTAL_OF_STARTING_5TH, CRYSTAL_OF_INPROGRESS_5TH, CRYSTAL_OF_DEFEAT_5TH));
		MONSTERS.put(MIMI_THE_CAT, Arrays.asList(CRYSTAL_OF_STARTING_2ND, CRYSTAL_OF_INPROGRESS_2ND, CRYSTAL_OF_DEFEAT_2ND));
		MONSTERS.put(UNICORN_PHANTASM, Arrays.asList(CRYSTAL_OF_STARTING_4TH, CRYSTAL_OF_INPROGRESS_4TH, CRYSTAL_OF_DEFEAT_4TH));
		MONSTERS.put(SILHOUETTE_TILFO, Arrays.asList(CRYSTAL_OF_STARTING_6TH, CRYSTAL_OF_INPROGRESS_6TH, CRYSTAL_OF_DEFEAT_6TH));
	}
	
	private Servitors()
	{
		super(Servitors.class.getSimpleName(), "ai/npc/Summons");
	}
	
	@RegisterEvent(EventType.ON_CREATURE_KILL)
	@RegisterType(ListenerRegisterType.NPC)
	// @formatter:off
	@Id({
		// Kat the Cat
		14111, 14112, 14113, 14114,
		// Mew the Cat
		14159, 14160, 14161, 14162,
		// Boxer the Unicorn
		14295, 14296, 14297, 14298,
		// Mirage the Unicorn
		14343, 14344, 14345, 14346,
		// Shadow
		14479, 14480, 14481, 14482,
		// Silhouette
		14527, 14528, 14529, 14530
	})
	// @formatter:on
	public void onCreatureKill(OnCreatureKill event)
	{
		if (event.getAttacker().isNpc() && event.getTarget().isServitor() //
			&& Util.checkIfInRange(1500, event.getAttacker(), event.getTarget(), true))
		{
			final L2ServitorInstance target = (L2ServitorInstance) event.getTarget();
			final L2PcInstance master = target.getOwner();
			final QuestState qs = master.getQuestState(Q00230_TestOfTheSummoner.class.getSimpleName());
			
			if ((qs != null) && hasQuestItems(master, CRYSTAL_OF_INPROGRESS_3RD))
			{
				final L2Npc killer = (L2Npc) event.getAttacker();
				final List<Integer> items = MONSTERS.get(killer.getId());
				giveItems(master, items.get(2), 1); // Crystal of Defeat
				playSound(master, Sound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(master, items.get(1), -1); // Crystal of Inprogress
				takeItems(master, items.get(0), -1); // Crystal of Starting
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Servitors();
	}
}