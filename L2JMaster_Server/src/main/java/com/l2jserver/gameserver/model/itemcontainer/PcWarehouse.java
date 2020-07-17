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
package com.l2jserver.gameserver.model.itemcontainer;

import com.l2jserver.gameserver.enums.ItemLocation;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

public class PcWarehouse extends Warehouse
{
	// private static final Logger _log = Logger.getLogger(PcWarehouse.class.getName());
	
	private final L2PcInstance _owner;
	
	public PcWarehouse(L2PcInstance owner)
	{
		_owner = owner;
	}
	
	@Override
	public String getName()
	{
		return "Warehouse";
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.WAREHOUSE;
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		return ((_items.size() + slots) <= _owner.getWareHouseLimit());
	}
}
