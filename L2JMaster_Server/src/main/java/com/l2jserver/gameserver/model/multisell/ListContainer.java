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
package com.l2jserver.gameserver.model.multisell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DS
 */
public class ListContainer
{
	private final int _listId;
	private boolean _applyTaxes = false;
	private boolean _maintainEnchantment = false;
	private double _useRate = 1.0;
	
	protected List<Entry> _entries = new ArrayList<>();
	protected Set<Integer> _npcsAllowed = null;
	
	public ListContainer(int listId)
	{
		_listId = listId;
	}
	
	public final List<Entry> getEntries()
	{
		return _entries;
	}
	
	public final int getListId()
	{
		return _listId;
	}
	
	public final void setApplyTaxes(boolean applyTaxes)
	{
		_applyTaxes = applyTaxes;
	}
	
	public final boolean getApplyTaxes()
	{
		return _applyTaxes;
	}
	
	public final void setMaintainEnchantment(boolean maintainEnchantment)
	{
		_maintainEnchantment = maintainEnchantment;
	}
	
	public double getUseRate()
	{
		return _useRate;
	}
	
	/**
	 * Set this to create multisell with increased products, all product counts will be multiplied by the rate specified.<br>
	 * <b>NOTE:</b> It affects only parser, it won't change values of already parsed multisell since MultiSells' parseEntry method handles this feature.
	 * @param rate
	 */
	public void setUseRate(double rate)
	{
		_useRate = rate;
	}
	
	public final boolean getMaintainEnchantment()
	{
		return _maintainEnchantment;
	}
	
	public void allowNpc(int npcId)
	{
		if (_npcsAllowed == null)
		{
			_npcsAllowed = new HashSet<>();
		}
		_npcsAllowed.add(npcId);
	}
	
	public boolean isNpcAllowed(int npcId)
	{
		return (_npcsAllowed == null) || _npcsAllowed.contains(npcId);
	}
	
	public boolean isNpcOnly()
	{
		return _npcsAllowed != null;
	}
}