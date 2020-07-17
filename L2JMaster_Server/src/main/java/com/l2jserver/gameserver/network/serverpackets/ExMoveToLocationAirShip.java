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
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.gameserver.model.actor.L2Character;

public class ExMoveToLocationAirShip extends L2GameServerPacket
{
	private final int _objId, _tx, _ty, _tz, _x, _y, _z;
	
	public ExMoveToLocationAirShip(L2Character cha)
	{
		_objId = cha.getObjectId();
		_tx = cha.getXdestination();
		_ty = cha.getYdestination();
		_tz = cha.getZdestination();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x65);
		
		writeD(_objId);
		writeD(_tx);
		writeD(_ty);
		writeD(_tz);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}
}