/*
 * Copyright (C) 2004-2020 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;

/**
 * @author Gnacik
 */
public class L2EventMonsterInstance extends L2MonsterInstance
{
	// Block offensive skills usage on event mobs
	// mainly for AoE skills, disallow kill many event mobs
	// with one skill
	public boolean block_skill_attack = false;
	
	// Event mobs should drop items to ground
	// but item pickup must be protected to killer
	// TODO: Some mobs need protect drop for spawner
	public boolean drop_on_ground = false;
	
	/**
	 * Creates an event monster.
	 * @param template the event monster NPC template
	 */
	public L2EventMonsterInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2EventMobInstance);
	}
	
	public void eventSetBlockOffensiveSkills(boolean value)
	{
		block_skill_attack = value;
	}
	
	public void eventSetDropOnGround(boolean value)
	{
		drop_on_ground = value;
	}
	
	public boolean eventDropOnGround()
	{
		return drop_on_ground;
	}
	
	public boolean eventSkillAttackBlocked()
	{
		return block_skill_attack;
	}
}