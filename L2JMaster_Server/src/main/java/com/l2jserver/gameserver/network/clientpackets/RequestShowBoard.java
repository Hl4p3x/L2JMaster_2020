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
import com.l2jserver.gameserver.communitybbs.CommunityBoard;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * packet type id 0x57 sample 57 01 00 00 00 // unknown (always 1?) format: cd
 */
public final class RequestShowBoard extends L2GameClientPacket
{
	private static final String _C__5E_REQUESTSHOWBOARD = "[C] 5E RequestShowBoard";
	
	@SuppressWarnings("unused")
	private int _unknown;
	
	@Override
	protected final void readImpl()
	{
		_unknown = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.ENABLE_COMMUNITY_BOARD)
		{
			L2PcInstance activeChar = getClient().getActiveChar();
			
			if (activeChar == null)
			{
				return;
			}
			CommunityBoard.getInstance().handleCommands(getClient(), Config.BBS_DEFAULT);
		}
	}
	
	@Override
	public final String getType()
	{
		return _C__5E_REQUESTSHOWBOARD;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
