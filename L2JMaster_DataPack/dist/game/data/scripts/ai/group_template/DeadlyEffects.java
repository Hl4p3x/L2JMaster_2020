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
package ai.group_template;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

import ai.npc.AbstractNpcAI;

/**
 * Deadly Effects AI.
 * @author Sacrifice
 */
public final class DeadlyEffects extends AbstractNpcAI
{
	private static final int[] MONSTERS =
	{
		22825, // Giant Scorpion Bones
		22861, // Hard Scorpion Bones
		25718, // Emerald Horn
		25720, // Bleeding Fly
		25729 // Death Knight
	};
	
	private static final int DEADLY_POISON = 6815;
	private static final int DEADLY_BLEED = 6816;
	
	private static final Map<L2PcInstance, L2Npc> EFFECTED_PLAYERS = new HashMap<>();
	
	private static final int CLEANUP_TASK = 120; // Minutes
	
	private DeadlyEffects()
	{
		super(DeadlyEffects.class.getSimpleName(), "ai/group_template");
		addAttackId(MONSTERS);
		addKillId(MONSTERS);
		
		ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(() ->
		{
			if (!EFFECTED_PLAYERS.isEmpty())
			{
				for (L2PcInstance players : L2World.getInstance().getPlayers())
				{
					if (!players.isOnline())
					{
						EFFECTED_PLAYERS.remove(players);
					}
				}
			}
		}, CLEANUP_TASK, CLEANUP_TASK, TimeUnit.MINUTES);
	}
	
	public static void main(String[] args)
	{
		new DeadlyEffects();
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		if (!EFFECTED_PLAYERS.containsKey(attacker) || !EFFECTED_PLAYERS.containsValue(npc))
		{
			EFFECTED_PLAYERS.put(attacker, npc);
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		if (killer.isAffectedBySkill(DEADLY_POISON) && EFFECTED_PLAYERS.containsKey(killer))
		{
			EFFECTED_PLAYERS.remove(killer);
			killer.getEffectList().stopSkillEffects(true, DEADLY_POISON);
		}
		
		if (killer.isAffectedBySkill(DEADLY_BLEED) && EFFECTED_PLAYERS.containsKey(killer))
		{
			EFFECTED_PLAYERS.remove(killer);
			killer.getEffectList().stopSkillEffects(true, DEADLY_BLEED);
		}
		return super.onKill(npc, killer, isSummon);
	}
}
