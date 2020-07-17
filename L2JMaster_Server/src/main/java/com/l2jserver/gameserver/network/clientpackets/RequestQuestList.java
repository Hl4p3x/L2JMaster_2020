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

import com.l2jserver.gameserver.network.serverpackets.QuestList;

/**
 * This class ...
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestQuestList extends L2GameClientPacket
{
	private static final String _C__62_REQUESTQUESTLIST = "[C] 62 RequestQuestList";
	
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		QuestList ql = new QuestList();
		sendPacket(ql);
	}
	
	@Override
	public String getType()
	{
		return _C__62_REQUESTQUESTLIST;
	}
}
