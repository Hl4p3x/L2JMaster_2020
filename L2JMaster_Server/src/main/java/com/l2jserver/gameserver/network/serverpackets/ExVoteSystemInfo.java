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

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * ExVoteSystemInfo packet implementation.
 * @author Gnacik
 */
public class ExVoteSystemInfo extends L2GameServerPacket
{
	private final int _recomLeft;
	private final int _recomHave;
	private final int _bonusTime;
	private final int _bonusVal;
	private final int _bonusType;
	
	public ExVoteSystemInfo(L2PcInstance player)
	{
		_recomLeft = player.getRecomLeft();
		_recomHave = player.getRecomHave();
		_bonusTime = player.getNevitHourglassTime();
		_bonusVal = player.getNevitHourglassBonus();
		_bonusType = player.getNevitHourglassStatus();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xC9);
		writeD(_recomLeft);
		writeD(_recomHave);
		writeD(_bonusTime);
		writeD(_bonusVal);
		writeD(_bonusType);
	}
}
