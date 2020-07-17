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

public final class CSShowComBoard extends L2GameServerPacket
{
	private final byte[] _html;
	
	public CSShowComBoard(final byte[] html)
	{
		_html = html;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0x7b);
		writeC(0x01); // c4 1 to show community 00 to hide
		writeB(_html);
	}
}
