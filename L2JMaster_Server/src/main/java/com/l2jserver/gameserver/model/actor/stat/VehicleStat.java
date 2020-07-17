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
package com.l2jserver.gameserver.model.actor.stat;

import com.l2jserver.gameserver.model.actor.L2Vehicle;

public class VehicleStat extends CharStat
{
	private float _moveSpeed = 0;
	private int _rotationSpeed = 0;
	
	public VehicleStat(L2Vehicle activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public double getMoveSpeed()
	{
		return _moveSpeed;
	}
	
	public final void setMoveSpeed(float speed)
	{
		_moveSpeed = speed;
	}
	
	public final double getRotationSpeed()
	{
		return _rotationSpeed;
	}
	
	public final void setRotationSpeed(int speed)
	{
		_rotationSpeed = speed;
	}
}