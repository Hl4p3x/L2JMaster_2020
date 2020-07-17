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
package ai.individual;

import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

import ai.npc.AbstractNpcAI;

/**
 * Drake Leader AI.
 * @author Sacrifice
 */
public final class DrakeLeader extends AbstractNpcAI
{
	private static final int DRAKE_LEADER = 22848;
	
	private static final int[] DRAKE_LEADER_MINIONS =
	{
		22849, // Drake Warrior
		22850, // Drake Scout
		22851 // Drake Mage
	};
	
	private DrakeLeader()
	{
		super(DrakeLeader.class.getSimpleName(), "ai/individual");
		addKillId(DRAKE_LEADER);
		addSpawnId(DRAKE_LEADER);
	}
	
	public static void main(String[] args)
	{
		new DrakeLeader();
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		((L2MonsterInstance) npc).getMinionList().onMasterDie(true);
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		if (npc.isWalker())
		{
			npc.setIsNoRndWalk(true);
			
			for (int i = 0; i < 4; i++)
			{
				addMinion((L2MonsterInstance) npc, DRAKE_LEADER_MINIONS[getRandom(DRAKE_LEADER_MINIONS.length)]);
			}
			
			for (L2MonsterInstance minions : ((L2MonsterInstance) npc).getMinionList().getSpawnedMinions())
			{
				minions.setIsNoRndWalk(true);
				minions.setIsRunning(true);
			}
		}
		else
		{
			for (int i = 0; i < 4; i++)
			{
				addMinion((L2MonsterInstance) npc, DRAKE_LEADER_MINIONS[getRandom(DRAKE_LEADER_MINIONS.length)]);
			}
		}
		return super.onSpawn(npc);
	}
}