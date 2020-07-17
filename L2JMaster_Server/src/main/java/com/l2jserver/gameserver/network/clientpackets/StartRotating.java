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

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.StartRotation;

/**
 * This class ...
 * @version $Revision: 1.1.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class StartRotating extends L2GameClientPacket
{
	private static final String _C__5B_STARTROTATING = "[C] 5B StartRotating";
	
	private int _degree;
	private int _side;
	
	@Override
	protected void readImpl()
	{
		_degree = readD();
		_side = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final StartRotation br;
		if (activeChar.isInAirShip() && activeChar.getAirShip().isCaptain(activeChar))
		{
			br = new StartRotation(activeChar.getAirShip().getObjectId(), _degree, _side, 0);
			activeChar.getAirShip().broadcastPacket(br);
		}
		else
		{
			br = new StartRotation(activeChar.getObjectId(), _degree, _side, 0);
			activeChar.broadcastPacket(br);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__5B_STARTROTATING;
	}
}
