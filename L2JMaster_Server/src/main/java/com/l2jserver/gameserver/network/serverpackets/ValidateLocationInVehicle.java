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

import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

public class ValidateLocationInVehicle extends L2GameServerPacket
{
	private final int _charObjId;
	private final int _boatObjId;
	private final int _heading;
	private final Location _pos;
	
	/**
	 * @param player
	 */
	public ValidateLocationInVehicle(L2PcInstance player)
	{
		_charObjId = player.getObjectId();
		_boatObjId = player.getBoat().getObjectId();
		_heading = player.getHeading();
		_pos = player.getInVehiclePosition();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x80);
		writeD(_charObjId);
		writeD(_boatObjId);
		writeD(_pos.getX());
		writeD(_pos.getY());
		writeD(_pos.getZ());
		writeD(_heading);
	}
}
