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

import java.util.Map;
import java.util.Map.Entry;

import com.l2jserver.gameserver.instancemanager.KrateisCubeManager;

public class ExPVPMatchCCRecord extends L2GameServerPacket
{
	private final Map<String, Integer> _listPlayers;
	private final int _state;
	
	public ExPVPMatchCCRecord(int state)
	{
		_listPlayers = KrateisCubeManager.getParticipantsMatch();
		_state = state;
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0xFE);
		writeH(0x89);
		writeD(_state); // 0x00 - disabled, 0x01 - in progress, 0x02 - finished
		writeD(_listPlayers.size());
		for (Entry<String, Integer> player : _listPlayers.entrySet())
		{
			if (player != null)
			{
				writeS(player.getKey());
				writeD(player.getValue());
			}
		}
	}
}