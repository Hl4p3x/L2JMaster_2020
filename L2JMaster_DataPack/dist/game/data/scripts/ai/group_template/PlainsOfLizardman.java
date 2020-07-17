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

import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.holders.SkillHolder;

import ai.npc.AbstractNpcAI;

/**
 * Plains of Lizardmen AI.
 * @author Gnacik, malyelfik
 */
public final class PlainsOfLizardman extends AbstractNpcAI
{
	// NPCs
	private static final int INVISIBLE_NPC = 18919;
	private static final int TANTA_GUARD = 18862;
	private static final int FANTASY_MUSHROOM = 18864;
	private static final int STICKY_MUSHROOM = 18865;
	private static final int RAINBOW_FROG = 18866;
	private static final int ENERGY_PLANT = 18868;
	private static final int TANTA_SCOUT = 22768;
	private static final int TANTA_MAGICIAN = 22773;
	private static final int TANTA_SUMMONER = 22774;
	private static final int[] TANTA_LIZARDMEN =
	{
		22768, // Tanta Lizardman Scout
		22769, // Tanta Lizardman Warrior
		22770, // Tanta Lizardman Soldier
		22771, // Tanta Lizardman Berserker
		22772, // Tanta Lizardman Archer
		22773, // Tanta Lizardman Magician
		22774, // Tanta Lizardman Summoner
	};
	// Skills
	private static final SkillHolder STUN_EFFECT = new SkillHolder(6622);
	private static final SkillHolder DEMOTIVATION_HEX = new SkillHolder(6425);
	private static final SkillHolder FANTASY_MUSHROOM_SKILL = new SkillHolder(6427);
	private static final SkillHolder RAINBOW_FROG_SKILL = new SkillHolder(6429);
	private static final SkillHolder STICKY_MUSHROOM_SKILL = new SkillHolder(6428);
	private static final SkillHolder ENERGY_PLANT_SKILL = new SkillHolder(6430);
	// Buffs
	private static final SkillHolder[] BUFFS =
	{
		new SkillHolder(6625, 1), // Energy of Life
		new SkillHolder(6626, 2), // Energy of Life's Power
		new SkillHolder(6627, 3), // Energy of Life's Highest Power
		new SkillHolder(6628, 1), // Energy of Mana
		new SkillHolder(6629, 2), // Energy of Mana's Power
		new SkillHolder(6630, 3), // Energy of Mana's Highest Power
		new SkillHolder(6631, 1), // Energy of Power
		new SkillHolder(6633, 1), // Energy of Attack Speed
		new SkillHolder(6635, 1), // Energy of Crt Rate
		new SkillHolder(6636, 1), // Energy of Moving Speed
		new SkillHolder(6638, 1), // Aura of Mystery
		new SkillHolder(6639, 1), // Bane of Auras - Damage
		new SkillHolder(6640, 1), // Energizing Aura
		new SkillHolder(6674, 1), // Energy of Range Increment
	};
	// Misc
	// @formatter:off
	private static final int[] BUFF_LIST =
	{
		6, 7, 8, 11, 13
	};
	// @formatter:on
	
	private PlainsOfLizardman()
	{
		super(PlainsOfLizardman.class.getSimpleName(), "ai/group_template");
		addAttackId(FANTASY_MUSHROOM, RAINBOW_FROG, STICKY_MUSHROOM, ENERGY_PLANT, TANTA_SUMMONER);
		addKillId(TANTA_LIZARDMEN);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equals("fantasy_mushroom") && (npc != null) && (player != null))
		{
			npc.doCast(FANTASY_MUSHROOM_SKILL);
			for (L2Character target : npc.getKnownList().getKnownCharactersInRadius(200))
			{
				if ((target != null) && target.isAttackable())
				{
					final L2Npc monster = (L2Npc) target;
					npc.setTarget(monster);
					npc.doCast(STUN_EFFECT);
					addAttackDesire(monster, player);
				}
			}
			npc.doDie(player);
		}
		return null;
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		switch (npc.getId())
		{
			case TANTA_SUMMONER:
				if (!npc.isAffectedBySkill(DEMOTIVATION_HEX.getSkillId()))
				{
					npc.doCast(DEMOTIVATION_HEX);
				}
				break;
			case RAINBOW_FROG:
				castSkill(npc, attacker, RAINBOW_FROG_SKILL);
				break;
			case ENERGY_PLANT:
				castSkill(npc, attacker, ENERGY_PLANT_SKILL);
				break;
			case STICKY_MUSHROOM:
				castSkill(npc, attacker, STICKY_MUSHROOM_SKILL);
				break;
			case FANTASY_MUSHROOM:
				if (npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					npc.setIsInvul(true);
					for (L2Character target : npc.getKnownList().getKnownCharactersInRadius(1000))
					{
						if ((target != null) && target.isAttackable())
						{
							final L2Attackable monster = (L2Attackable) target;
							if ((monster.getId() == TANTA_MAGICIAN) || (monster.getId() == TANTA_SCOUT))
							{
								target.setIsRunning(true);
								target.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(npc.getX(), npc.getY(), npc.getZ(), 0));
							}
						}
					}
					startQuestTimer("fantasy_mushroom", 4000, npc, attacker);
				}
				break;
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		// Tanta Guard
		if (getRandom(1000) == 0)
		{
			addAttackDesire(addSpawn(TANTA_GUARD, npc), killer);
		}
		
		// Invisible buff npc
		final int random = getRandom(100);
		final L2Npc buffer = addSpawn(INVISIBLE_NPC, npc.getLocation(), false, 6000);
		buffer.setTarget(killer);
		
		if (random <= 42)
		{
			castRandomBuff(buffer, 7, 45, BUFFS[0], BUFFS[1], BUFFS[2]);
		}
		if (random <= 11)
		{
			castRandomBuff(buffer, 8, 60, BUFFS[3], BUFFS[4], BUFFS[5]);
			castRandomBuff(buffer, 3, 6, BUFFS[9], BUFFS[10], BUFFS[12]);
		}
		if (random <= 25)
		{
			buffer.doCast(BUFFS[BUFF_LIST[getRandom(BUFF_LIST.length)]]);
		}
		if (random <= 10)
		{
			buffer.doCast(BUFFS[13].getSkill());
		}
		if (random <= 1)
		{
			final int i = getRandom(100);
			if (i <= 34)
			{
				buffer.doCast(BUFFS[6]);
				buffer.doCast(BUFFS[7]);
				buffer.doCast(BUFFS[8]);
			}
			else if (i < 67)
			{
				buffer.doCast(BUFFS[13]);
			}
			else
			{
				buffer.doCast(BUFFS[2]);
				buffer.doCast(BUFFS[5]);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private void castRandomBuff(L2Npc npc, int chance1, int chance2, SkillHolder... buffs)
	{
		final int rand = getRandom(100);
		if (rand <= chance1)
		{
			npc.doCast(buffs[2]);
		}
		else if (rand <= chance2)
		{
			npc.doCast(buffs[1]);
		}
		else
		{
			npc.doCast(buffs[0]);
		}
	}
	
	@Override
	protected void castSkill(L2Npc npc, L2Playable target, SkillHolder skill)
	{
		npc.doDie(target);
		super.castSkill(addSpawn(INVISIBLE_NPC, npc, false, 6000), target, skill);
	}
	
	public static void main(String[] args)
	{
		new PlainsOfLizardman();
	}
}
