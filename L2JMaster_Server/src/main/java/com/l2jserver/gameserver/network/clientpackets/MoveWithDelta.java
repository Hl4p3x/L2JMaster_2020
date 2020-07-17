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
package com.l2jserver.gameserver.network.clientpackets;

/**
 * Format: (c) ddd d: dx d: dy d: dz
 * @author -Wooden-
 */
public class MoveWithDelta extends L2GameClientPacket
{
	private static final String _C__52_MOVEWITHDELTA = "[C] 52 MoveWithDelta";
	
	@SuppressWarnings("unused")
	private int _dx;
	@SuppressWarnings("unused")
	private int _dy;
	@SuppressWarnings("unused")
	private int _dz;
	
	@Override
	protected void readImpl()
	{
		_dx = readD();
		_dy = readD();
		_dz = readD();
	}
	
	@Override
	protected void runImpl()
	{
		// TODO this
	}
	
	@Override
	public String getType()
	{
		return _C__52_MOVEWITHDELTA;
	}
}
