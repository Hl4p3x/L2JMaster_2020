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

import com.l2jserver.gameserver.ai.L2CharacterAI;
import com.l2jserver.gameserver.ai.L2ControllableMobAI;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;

/**
 * @author littlecrow
 */
public class L2ControllableMobInstance extends L2MonsterInstance
{
	private boolean _isInvul;
	
	/**
	 * Creates a controllable monster.
	 * @param template the controllable monster NPC template
	 */
	public L2ControllableMobInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2ControllableMobInstance);
	}
	
	@Override
	public boolean isAggressive()
	{
		return true;
	}
	
	@Override
	public int getAggroRange()
	{
		// force mobs to be aggro
		return 500;
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2ControllableMobAI(this);
	}
	
	@Override
	public boolean isInvul()
	{
		return _isInvul;
	}
	
	public void setInvul(boolean isInvul)
	{
		_isInvul = isInvul;
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		setAI(null);
		return true;
	}
	
	@Override
	public void detachAI()
	{
		// do nothing, AI of controllable mobs can't be detached automatically
	}
}