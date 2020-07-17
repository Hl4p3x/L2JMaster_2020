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
package com.l2jserver.gameserver.model.variables;

import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * NPC Variables implementation.
 * @author GKR
 */
public class NpcVariables extends AbstractVariables
{
	@Override
	public int getInt(String key)
	{
		return super.getInt(key, 0);
	}
	
	@Override
	public boolean restoreMe()
	{
		return true;
	}
	
	@Override
	public boolean storeMe()
	{
		return true;
	}
	
	/**
	 * Gets the stored player.
	 * @param name the name of the variable
	 * @return the stored player or {@code null}
	 */
	public L2PcInstance getPlayer(String name)
	{
		return getObject(name, L2PcInstance.class);
	}
	
	/**
	 * Gets the stored summon.
	 * @param name the name of the variable
	 * @return the stored summon or {@code null}
	 */
	public L2Summon getSummon(String name)
	{
		return getObject(name, L2Summon.class);
	}
}