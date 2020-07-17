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
package com.l2jserver.gameserver.util;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.network.serverpackets.CreatureSay;

public final class BuilderUtil
{
	private BuilderUtil()
	{
	}
	
	public static void sendSysMessage(L2PcInstance player, String message)
	{
		if (Config.GM_STARTUP_INVISIBLE)
		{
			player.sendPacket(new CreatureSay(0, Say2.ALL, "SYS", message));
		}
		else
		{
			player.sendMessage(message);
		}
	}
	
	public static void sendHtmlMessage(L2PcInstance player, String message)
	{
		player.sendPacket(new CreatureSay(0, Say2.ALL, "HTML", message));
	}
	
	public static boolean setHiding(L2PcInstance player, boolean hide)
	{
		if (player.isInvisible() && hide)
		{
			return false;
		}
		if (!player.isInvisible() && !hide)
		{
			return false;
		}
		player.setSilenceMode(hide);
		player.setIsInvul(hide);
		player.setInvisible(hide);
		player.broadcastUserInfo();
		return true;
	}
}