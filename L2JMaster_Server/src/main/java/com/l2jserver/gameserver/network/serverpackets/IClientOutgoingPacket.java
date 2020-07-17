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

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.IOutgoingPacket;
import com.l2jserver.gameserver.network.PacketWriter;

public interface IClientOutgoingPacket extends IOutgoingPacket
{
	public static final Logger LOGGER = Logger.getLogger(IClientOutgoingPacket.class.getName());
	public static final int[] PAPERDOLL_ORDER = new int[]
	{
		0,
		8,
		9,
		4,
		13,
		14,
		1,
		5,
		7,
		10,
		6,
		11,
		12,
		23,
		5,
		2,
		3,
		16,
		15,
		17,
		18,
		19,
		20,
		21,
		22,
		24
	};
	
	default public int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
	
	default public void sendTo(L2PcInstance player)
	{
		player.sendPacket(this);
	}
	
	default public void runImpl(L2PcInstance player)
	{
	}
	
	default public void writeOptionalD(PacketWriter packet, int value)
	{
		if (value >= 32767)
		{
			packet.writeH(32767);
			packet.writeD(value);
		}
		else
		{
			packet.writeH(value);
		}
	}
}