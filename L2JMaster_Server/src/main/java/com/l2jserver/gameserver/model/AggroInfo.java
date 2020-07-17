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
package com.l2jserver.gameserver.model;

import com.l2jserver.gameserver.model.actor.L2Character;

/**
 * @author xban1x
 */
public final class AggroInfo
{
	private final L2Character _attacker;
	private long _hate = 0;
	private int _damage = 0;
	
	public AggroInfo(L2Character pAttacker)
	{
		_attacker = pAttacker;
	}
	
	public L2Character getAttacker()
	{
		return _attacker;
	}
	
	public long getHate()
	{
		return _hate;
	}
	
	public long checkHate(L2Character owner)
	{
		if (_attacker.isAlikeDead() || !_attacker.isVisible() || !owner.getKnownList().knowsObject(_attacker))
		{
			_hate = 0;
		}
		
		return _hate;
	}
	
	public void addHate(long value)
	{
		_hate = Math.min(_hate + value, 999999999);
	}
	
	public void stopHate()
	{
		_hate = 0;
	}
	
	public int getDamage()
	{
		return _damage;
	}
	
	public void addDamage(int value)
	{
		_damage = (int) Math.min(_damage + (long) value, 999999999);
	}
	
	@Override
	public final boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj instanceof AggroInfo)
		{
			return (((AggroInfo) obj).getAttacker() == _attacker);
		}
		
		return false;
	}
	
	@Override
	public final int hashCode()
	{
		return _attacker.getObjectId();
	}
	
	@Override
	public String toString()
	{
		return "AggroInfo [attacker=" + _attacker + ", hate=" + _hate + ", damage=" + _damage + "]";
	}
}
