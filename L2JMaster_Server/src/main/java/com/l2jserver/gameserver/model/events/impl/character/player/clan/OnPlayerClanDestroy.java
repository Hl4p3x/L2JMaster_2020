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
package com.l2jserver.gameserver.model.events.impl.character.player.clan;

import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.impl.IBaseEvent;

/**
 * @author UnAfraid
 */
public class OnPlayerClanDestroy implements IBaseEvent
{
	private final L2ClanMember _activeChar;
	private final L2Clan _clan;
	
	public OnPlayerClanDestroy(L2ClanMember activeChar, L2Clan clan)
	{
		_activeChar = activeChar;
		_clan = clan;
	}
	
	public L2ClanMember getActiveChar()
	{
		return _activeChar;
	}
	
	public L2Clan getClan()
	{
		return _clan;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_DESTROY;
	}
}
