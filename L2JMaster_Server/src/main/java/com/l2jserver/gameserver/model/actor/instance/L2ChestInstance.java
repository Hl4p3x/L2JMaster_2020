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

import com.l2jserver.gameserver.data.xml.impl.NpcData;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;

/**
 * This class manages all chest.
 * @author Julian
 */
public final class L2ChestInstance extends L2MonsterInstance
{
	private volatile boolean _specialDrop;
	
	/**
	 * Creates a chest.
	 * @param template the chest NPC template
	 */
	public L2ChestInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2ChestInstance);
		setIsNoRndWalk(true);
		_specialDrop = false;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_specialDrop = false;
		setMustRewardExpSp(true);
	}
	
	public synchronized void setSpecialDrop()
	{
		_specialDrop = true;
	}
	
	@Override
	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character lastAttacker)
	{
		int id = getTemplate().getId();
		
		if (!_specialDrop)
		{
			if ((id == 18287) || (id == 18288))
			{
				id = 21671;
			}
			else if ((id == 18289) || (id == 18290))
			{
				id = 21694;
			}
			else if ((id == 18291) || (id == 18292))
			{
				id = 21717;
			}
			else if ((id == 18293) || (id == 18294))
			{
				id = 21740;
			}
			else if ((id == 18295) || (id == 18296))
			{
				id = 21763;
			}
			else if ((id == 18297) || (id == 18298))
			{
				id = 21786;
			}
		}
		super.doItemDrop(NpcData.getInstance().getTemplate(id), lastAttacker);
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return true;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
