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
package com.l2jserver.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.ClanPrivilege;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.entity.clanhall.SiegableHall;

public class L2CastleDoormenInstance extends L2DoormenInstance
{
	public L2CastleDoormenInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2CastleDoormenInstance);
	}
	
	@Override
	protected final void openDoors(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			if (getConquerableHall() != null)
			{
				getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), true);
			}
			else
			{
				getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
			}
		}
	}
	
	@Override
	protected final void closeDoors(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
		st.nextToken();
		
		while (st.hasMoreTokens())
		{
			if (getConquerableHall() != null)
			{
				getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), false);
			}
			else
			{
				getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
			}
		}
	}
	
	@Override
	protected final boolean isOwnerClan(L2PcInstance player)
	{
		if ((player.getClan() != null) && player.hasClanPrivilege(ClanPrivilege.CS_OPEN_DOOR))
		{
			SiegableHall hall = getConquerableHall();
			// save in variable because it's a costly call
			if (hall != null)
			{
				if (player.getClanId() == hall.getOwnerId())
				{
					return true;
				}
			}
			else if (getCastle() != null)
			{
				if (player.getClanId() == getCastle().getOwnerId())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	protected final boolean isUnderSiege()
	{
		SiegableHall hall = getConquerableHall();
		if (hall != null)
		{
			return hall.isInSiege();
		}
		return getCastle().getZone().isActive();
	}
}
