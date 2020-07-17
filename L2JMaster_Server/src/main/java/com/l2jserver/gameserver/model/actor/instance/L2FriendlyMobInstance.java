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
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.knownlist.FriendlyMobKnownList;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;

/**
 * This class represents Friendly Mobs lying over the world.<br>
 * These friendly mobs should only attack players with karma > 0 and it is always aggro, since it just attacks players with karma.
 */
public class L2FriendlyMobInstance extends L2Attackable
{
	/**
	 * Creates a friendly monster.
	 * @param template the friendly monster NPC template
	 */
	public L2FriendlyMobInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2FriendlyMobInstance);
	}
	
	@Override
	public final FriendlyMobKnownList getKnownList()
	{
		return (FriendlyMobKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new FriendlyMobKnownList(this));
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker instanceof L2PcInstance)
		{
			return ((L2PcInstance) attacker).getKarma() > 0;
		}
		return false;
	}
	
	@Override
	public boolean isAggressive()
	{
		return true;
	}
}
