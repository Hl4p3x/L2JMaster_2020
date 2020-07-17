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

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.serverpackets.GetOnVehicle;
import com.l2jserver.gameserver.network.serverpackets.ValidateLocation;

/**
 * This class ...
 * @version $Revision: 1.13.4.7 $ $Date: 2005/03/27 15:29:30 $
 */
public class ValidatePosition extends L2GameClientPacket
{
	private static final String _C__59_VALIDATEPOSITION = "[C] 59 ValidatePosition";
	
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private int _data; // vehicle id
	
	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
		_data = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if ((activeChar == null) || activeChar.isTeleporting() || activeChar.inObserverMode())
		{
			return;
		}
		
		final int realX = activeChar.getX();
		final int realY = activeChar.getY();
		int realZ = activeChar.getZ();
		
		if (Config.DEVELOPER)
		{
			_log.fine("client pos: " + _x + " " + _y + " " + _z + " head " + _heading);
			_log.fine("server pos: " + realX + " " + realY + " " + realZ + " head " + activeChar.getHeading());
		}
		
		if ((_x == 0) && (_y == 0))
		{
			if (realX != 0)
			{
				return;
			}
		}
		
		int dx, dy, dz;
		double diffSq;
		
		if (activeChar.isInBoat())
		{
			if (Config.COORD_SYNCHRONIZE == 2)
			{
				dx = _x - activeChar.getInVehiclePosition().getX();
				dy = _y - activeChar.getInVehiclePosition().getY();
				// dz = _z - activeChar.getInVehiclePosition().getZ();
				diffSq = ((dx * dx) + (dy * dy));
				if (diffSq > 250000)
				{
					sendPacket(new GetOnVehicle(activeChar.getObjectId(), _data, activeChar.getInVehiclePosition()));
				}
			}
			return;
		}
		if (activeChar.isInAirShip())
		{
			// Zoey76: TODO: Implement or cleanup.
			// if (Config.COORD_SYNCHRONIZE == 2)
			// {
			// dx = _x - activeChar.getInVehiclePosition().getX();
			// dy = _y - activeChar.getInVehiclePosition().getY();
			// dz = _z - activeChar.getInVehiclePosition().getZ();
			// diffSq = ((dx * dx) + (dy * dy));
			// if (diffSq > 250000)
			// {
			// sendPacket(new GetOnVehicle(activeChar.getObjectId(), _data, activeChar.getInBoatPosition()));
			// }
			// }
			return;
		}
		
		if (activeChar.isFalling(_z))
		{
			return; // disable validations during fall to avoid "jumping"
		}
		
		dx = _x - realX;
		dy = _y - realY;
		dz = _z - realZ;
		diffSq = ((dx * dx) + (dy * dy));
		
		// Zoey76: TODO: Implement or cleanup.
		// L2Party party = activeChar.getParty();
		// if ((party != null) && (activeChar.getLastPartyPositionDistance(_x, _y, _z) > 150))
		// {
		// activeChar.setLastPartyPosition(_x, _y, _z);
		// party.broadcastToPartyMembers(activeChar, new PartyMemberPosition(activeChar));
		// }
		
		// Don't allow flying transformations outside gracia area!
		if (activeChar.isFlyingMounted() && (_x > L2World.GRACIA_MAX_X))
		{
			activeChar.untransform();
		}
		
		if (activeChar.isFlying() || activeChar.isInsideZone(ZoneId.WATER))
		{
			activeChar.setXYZ(realX, realY, _z);
			if (diffSq > 90000)
			{
				activeChar.sendPacket(new ValidateLocation(activeChar));
			}
		}
		else if (diffSq < 360000) // if too large, messes observation
		{
			if (Config.COORD_SYNCHRONIZE == -1) // Only Z coordinate synched to server,
			// mainly used when no geodata but can be used also with geodata
			{
				activeChar.setXYZ(realX, realY, _z);
				return;
			}
			if (Config.COORD_SYNCHRONIZE == 1) // Trusting also client x,y coordinates (should not be used with geodata)
			{
				if (!activeChar.isMoving() || !activeChar.validateMovementHeading(_heading)) // Heading changed on client = possible obstacle
				{
					// character is not moving, take coordinates from client
					if (diffSq < 2500)
					{
						activeChar.setXYZ(realX, realY, _z);
					}
					else
					{
						activeChar.setXYZ(_x, _y, _z);
					}
				}
				else
				{
					activeChar.setXYZ(realX, realY, _z);
				}
				
				activeChar.setHeading(_heading);
				return;
			}
			// Sync 2 (or other),
			// intended for geodata. Sends a validation packet to client
			// when too far from server calculated true coordinate.
			// Due to geodata/zone errors, some Z axis checks are made. (maybe a temporary solution)
			// Important: this code part must work together with L2Character.updatePosition
			if ((diffSq > 250000) || (Math.abs(dz) > 200))
			{
				// if ((_z - activeChar.getClientZ()) < 200 && Math.abs(activeChar.getLastServerPosition().getZ()-realZ) > 70)
				
				if ((Math.abs(dz) > 200) && (Math.abs(dz) < 1500) && (Math.abs(_z - activeChar.getClientZ()) < 800))
				{
					activeChar.setXYZ(realX, realY, _z);
					realZ = _z;
				}
				else
				{
					if (Config.DEVELOPER)
					{
						_log.info(activeChar.getName() + ": Synchronizing position Server --> Client");
					}
					
					activeChar.sendPacket(new ValidateLocation(activeChar));
				}
			}
		}
		
		activeChar.setClientX(_x);
		activeChar.setClientY(_y);
		activeChar.setClientZ(_z);
		activeChar.setClientHeading(_heading); // No real need to validate heading.
		activeChar.setLastServerPosition(realX, realY, realZ);
	}
	
	@Override
	public String getType()
	{
		return _C__59_VALIDATEPOSITION;
	}
}
