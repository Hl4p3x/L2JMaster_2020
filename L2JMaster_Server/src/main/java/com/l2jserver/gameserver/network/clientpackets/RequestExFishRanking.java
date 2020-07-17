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
import com.l2jserver.gameserver.instancemanager.FishingChampionshipManager;
import com.l2jserver.gameserver.instancemanager.Leaderboards.FishermanLeaderboard;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Format: (ch) just a trigger
 * @author -Wooden-
 */
public final class RequestExFishRanking extends L2GameClientPacket
{
	private static final String _C__D0_18_REQUESTEXFISHRANKING = "[C] D0:18 RequestExFishRanking";
	
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.RANK_FISHERMAN_ENABLED)
		{
			NpcHtmlMessage htm = new NpcHtmlMessage(0);
			htm.setHtml(FishermanLeaderboard.getInstance().showHtm(getClient().getActiveChar().getObjectId()));
			getClient().getActiveChar().sendPacket(htm);
		}
		else
		{
			final L2PcInstance activeChar = getClient().getActiveChar();
			if (activeChar == null)
			{
				return;
			}
			
			if (Config.ALT_FISH_CHAMPIONSHIP_ENABLED)
			{
				FishingChampionshipManager.getInstance().showMidResult(activeChar);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _C__D0_18_REQUESTEXFISHRANKING;
	}
}